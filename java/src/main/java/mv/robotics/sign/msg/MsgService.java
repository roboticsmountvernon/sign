/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import mv.robotics.sign.msg.msg.ConnectMsg;

/**
 * A message passing service that can run over various transports.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-17-16
 */
public class MsgService
{
   /* internal message types */
   static public final int MSG_TYPE_CONNECT_REQ  = 0;
   static public final int MSG_TYPE_CONNECT_RESP = 1;

   /* the max number of connect attempts before we close the connection */
   static private final int MAX_CONNECT_ATTEMPTS = 3;
   
   /* how long a checked connection can go between connect responses */
   /* before we call it down */
   static private final int MAX_QUIET_TIME = 3000;
   
   /* how often we scan the connections to make sure they're alive */
   static private final long CONN_SCAN_INTERVAL = 1000;
   
   private List<MsgServiceListener> listeners;   /* service listeners */
   private List<MsgConn> conns;                  /* active connections */
   private List<MsgHandler<?>> handlers;         /* message handlers */
   private ScheduledExecutorService monitorPool; /* thread pool for monitoring */
   private ExecutorService dispatchPool;         /* message dispatch pool */
   private AtomicInteger nextConnId;             /* connection id allocator */
   private boolean started;                      /* true when afterPropertiesSet is called */
   
   /**
    * Create a new message service.
    */
   public MsgService()
   {
      /* setup the lists */
      conns = new LinkedList<>();
      handlers = new LinkedList<>();
      listeners = new LinkedList<>();

      /* pick a random connId starting point, but restricted enough that */
      /* we will never roll over to zero as that's an invalid connId */
      Random random = new Random();
      nextConnId = new AtomicInteger(random.nextInt(Integer.MAX_VALUE/2));
      
      /* register a connection request message handler */
      addHandler(new MsgHandler<ConnectMsg>(MSG_TYPE_CONNECT_REQ,ConnectMsg.class) {
         public void onReceive(MsgConn conn,ConnectMsg msg) {
            handleConnectRequest(conn,msg);
         }
      });
      
      /* register a connection response message handler */
      addHandler(new MsgHandler<ConnectMsg>(MSG_TYPE_CONNECT_RESP,ConnectMsg.class) {
         public void onReceive(MsgConn conn,ConnectMsg msg) {
            handleConnectResponse(conn,msg);
         }
      });
   }
   
   /**
    * Start the message service.
    */
   public void start()
   {
      /* if there are no listeners, add an empty list */
      if(listeners == null)
         listeners = new ArrayList<>();

      /* mark the service started */
      started = true;
      
      /* fire up all the connections by sending connect requests */
      for(MsgConn conn : conns)
      {
         /* if the transport is up, send a connect request */
         if(conn.getTransport().isUp())
            sendConnectRequest(conn);
      }

      /* start the dispatch pool */
      dispatchPool = Executors.newFixedThreadPool(3);
      
      /* start the monitoring pool */
      monitorPool = Executors.newSingleThreadScheduledExecutor();
      monitorPool.scheduleAtFixedRate(new Runnable() {
         public void run() {
            checkConnections();
         }
      },CONN_SCAN_INTERVAL,CONN_SCAN_INTERVAL,TimeUnit.MILLISECONDS);
   }

   /**
    * Destroy the message service.
    */
   public void destroy()
   {
      if(started)
      {
         /* shutdown the monitor */
         monitorPool.shutdown();

         /* shutdown the dispatch pool */
         dispatchPool.shutdown();
      
         /* close all connections */
         for(MsgConn conn : conns)
            conn.close();
         
         /* no longer running */
         started = false;
      }
   }

   /**
    * Add a message handler to the service.
    * 
    * @param handler   the new handler to add
    */
   public void addHandler(MsgHandler<?> handler)
   {
      synchronized(handlers) {
         handlers.add(handler);
      }
   }
   
   /**
    * Wrap the transport in a connection and add to the service.
    * 
    * @param transport   the transport to add
    * @param connType    the connection type to advertise
    */
   public void addTransport(MsgTransport transport,int connType)
   {
      addConn(new MsgConn(transport,connType));
   }
   
   /**
    * Add a new connection to the service.
    * 
    * @param conn   the connection to add
    */
   public void addConn(MsgConn conn)
   {
      /* link the connection to this service */
      conn.service = this;
      
      /* assign it a connection id */
      conn.connId = nextConnId.incrementAndGet();

      /* add to the connection list */
      synchronized(conns) {
         conns.add(conn);
      }
      
      /* if the service has started, try to send a connect request immediately */
      if(started)
      {
         /* if the transport is up, send a connect request */
         if(conn.getTransport().isUp())
            sendConnectRequest(conn);
      }
   }
   
   /**
    * Remove the specified connection.
    * 
    * @param conn   the connection to remove
    */
   public void removeConn(MsgConn conn)
   {
      /* remove from the connection list */
      synchronized(conns) {
         conns.remove(conn);
      }
      
      /* close the connection */
      conn.close();
   }
   
   /**
    * Add a service listener.
    * 
    * @param listener   the listener to add
    */
   public void addListener(MsgServiceListener listener)
   {
      synchronized(listeners) {
         listeners.add(listener);
      }
   }
   
   /**
    * Called from the monitoring thread to check all connections.
    */
   private void checkConnections()
   {
      try {
         List<MsgConn> cachedConns = new ArrayList<>();
      
         /* copy the list so we don't need to hold the lock */
         synchronized(conns) {
            cachedConns.addAll(conns);
         }
      
         /* send a connect request over every connection where the */
         /* transport is up but a connection is not yet established */
         for(MsgConn conn : cachedConns)
         {
            /* check if the transport is up */
            if(conn.getTransport().isUp())
            {
               /* if not connected, or a checked transport */
               /* then send a connect request */
               if(!conn.connected || conn.getTransport().isChecked())
                  sendConnectRequest(conn);
            
               /* if checked but we haven't seen a response in a while */
               /* then mark the connection down */
               if(conn.getTransport().isChecked() && conn.connected &&
                  ((System.currentTimeMillis() - conn.connectTime) > MAX_QUIET_TIME))
               {
                  /* mark the connection disconnected */
                  disconnect(conn);
               }
            }
         
            /* not up, try to bring it up */
            else
               conn.getTransport().attemptUp();
         }
      } catch(Exception ex) {
         ex.printStackTrace();
      }
   }
   
   /**
    * Mark a connection as connected.
    */
   protected void connect(MsgConn conn)
   {
      /* must be disconnected to connect */
      if(!conn.connected)
      {
         /* connected */
         conn.connected = true;
         conn.connectAttempts = 0;
         
         /* notify listeners */
         synchronized(listeners) {
            for(MsgServiceListener listener : listeners)
               listener.onConnect(conn);
         }
      }
   }
   
   /**
    * Mark a connection disconnected.
    */
   protected void disconnect(MsgConn conn)
   {
      /* must be connected to disconnect */
      if(conn.connected)
      {
         /* no longer connected */
         conn.connected = false;
         conn.remoteConnId = 0;
         conn.remoteMaxPayloadSize = 0;
         
         /* assign the connection a new connId so that when it */
         /* comes back up, the remote side will see it as a new */
         /* connection and reset the state */
         conn.connId = nextConnId.incrementAndGet();

         /* notify listeners */
         fireDisconnect(conn);
      }
   }
   
   /**
    * Fire a disconnect event.
    */
   private void fireDisconnect(MsgConn conn)
   {
      /* notify listeners */
      synchronized(listeners) {
         for(MsgServiceListener listener : listeners)
            listener.onDisconnect(conn);
      }      
   }
   
   /**
    * Dispatch an incoming message to a registered handler in the service
    * handler list.
    * 
    * @param conn       the incoming connection
    * @param msgType    the incoming message type
    * @param msgId      the incoming message id
    * @param payload    the payload of the message
    */
   protected boolean dispatchMsg(MsgConn conn,int msgType,int msgId,byte[] payload) throws Exception
   {
      return(dispatchMsg(handlers,conn,msgType,msgId,payload));
   }
   
   /**
    * Dispatch an incoming message to a registered handler in the specified
    * handler list.
    * 
    * @param handlers   the handler list to use
    * @param conn       the incoming connection
    * @param msgType    the incoming message type
    * @param msgId      the incoming message id
    * @param payload    the payload of the message
    */
   protected boolean dispatchMsg(List<MsgHandler<?>> handlers,MsgConn conn,
      int msgType,int msgId,byte[] payload) throws Exception
   {
      MsgHandler<?> handler = null;
      
      /* search the handler list */
      synchronized(handlers) {
         for(MsgHandler<?> h : handlers)
         {
            /* check for a type match */
            if(h.getMsgType() == msgType)
            {
               /* check for an id match */
               if((h.getMsgId() == 0) || (h.getMsgId() == msgId))
               {
                  /* this is the handler to use */
                  handler = h;
               }
            }
         }
         
         /* check if we should remove the handler */
         if((handler != null) && handler.isRemove())
            handlers.remove(handler);
      }

      /* if we found a handler, dispatch the message to it */
      if(handler != null)
      {
         final MsgHandler<?> fhandler = handler;
         dispatchPool.submit(new Runnable() {
            public void run() {
               try {
                  fhandler.processPayload(conn,payload);
               } catch(Exception e) {
                  /* fall through */
               }
            }
         });
         return(true);
      }
      return(false);
   }
   
   /**
    * Handle connect response messages.
    */
   private void handleConnectRequest(MsgConn conn,ConnectMsg req)
   {
      conn.sendMsg(MSG_TYPE_CONNECT_RESP,new ConnectMsg(conn.connId,4096,conn.getConnType()));
   }
   
   /**
    * Handle connect response messages.
    */
   private void handleConnectResponse(MsgConn conn,ConnectMsg resp)
   {
      /* remember the max payload size */
      conn.remoteMaxPayloadSize = resp.getMaxPayloadSize();
      
      /* remember the time */
      conn.connectTime = System.currentTimeMillis();
      
      /* check if the remote connId changed */
      if(conn.remoteConnId != resp.getConnId())
      {
         /* if the current remoteConnId is non-zero then we just */
         /* reconnected over a persistent connection (ie. serial) */
         /* so we need to logically disconnect the previous connection */
         /* even though it hasn't actually changed... to do this we */
         /* do a fake disconnect (fire events only) and then complete */
         /* the connect steps as usual */
         if(conn.remoteConnId != 0)
         {
            conn.connected = false;
            fireDisconnect(conn);
         }

         /* remember the new remote connId and type */
         conn.remoteConnId = resp.getConnId();
         conn.remoteConnType = resp.getConnType();
         
         /* fire a connect event */
         connect(conn);
      }
   }
   
   /**
    * Send a connect request to the specified connection.
    */
   protected void sendConnectRequest(MsgConn conn) 
   {
      /* if the connection isn't durable and we've tried too many times, close the connection */
      if(!conn.getTransport().isDurable() && (conn.connectAttempts++ >= MAX_CONNECT_ATTEMPTS))
         removeConn(conn);
      else
         conn.sendMsg(MSG_TYPE_CONNECT_REQ,new ConnectMsg(conn.connId,4096,conn.getConnType()));
   }
}
