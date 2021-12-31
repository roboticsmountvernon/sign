/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

import java.util.LinkedList;
import java.util.List;

import mv.robotics.sign.msg.ex.InvalidResponseException;
import mv.robotics.sign.msg.ex.MsgException;
import mv.robotics.sign.msg.ex.PayloadTooBigException;
import mv.robotics.sign.msg.util.ByteUtil;

/**
 * A serial message passing connection.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
public class MsgConn
{
   /* timeout value when waiting for a response from the client */
   static private final int RECEIVE_TIMEOUT = 5000;
   
   /* the size of the packet header in bytes */
   static private final int HDR_SIZE = 8;
   
   /* the size of the crc in bytes */
   static private final int CRC_SIZE = 2;
   
   /* the start byte for packets */
   static private final byte START_BYTE = '#';
   
   /* max receive size */
   static private final int MAX_RX_PAYLOAD_SIZE = 4096;

   private MsgTransport transport;        /* the underlying transport */
   private int connType;                  /* the connection type to advertise */
   private byte nextMsgId;                /* next available message id */
   private List<MsgHandler<?>> handlers;  /* message handlers */
   private boolean closed;                /* true when closed */
   protected int connectAttempts;         /* number of connect attempts */
   protected long connectTime;            /* when we last connected */
   protected boolean connected;           /* true when connected */
   protected MsgService service;          /* the service the connection is linked to */
   protected int connId;                  /* unique id for this connection */
   protected int remoteConnId;            /* unique id for the remote side of the connection */
   protected int remoteConnType;          /* type of the remote connection */
   protected int remoteMaxPayloadSize;    /* the max payload size the other side can handle */
   
   /**
    * Create a new connection for the specified serial port.
    * 
    * @param transport   the transport to use
    * @param connType    connection type to include in connect messages
    */
   public MsgConn(MsgTransport transport,int connType)
   {
      this.transport = transport;
      this.connType = connType;

      /* hook the transport to the connection */
      transport.conn = this;
      
      /* set the read timeout */
      transport.setReadTimeout(RECEIVE_TIMEOUT);

      /* setup the handler list */
      handlers = new LinkedList<>();
      
      /* start the receiver thread */
      Thread thread = new Thread(new Runnable() {
         public void run() {
            receiveThread();
         }
      });
      thread.start();
   }
   
   /**
    * Close the connection.
    */
   public void close()
   {
      if(!closed)
      {
         /* flag closed */
         closed = true;

         /* close the transport */
         transport.close();
         
         /* notify anyone waiting for the transport to come up */
         synchronized(transport) {
            transport.notifyAll();
         }
         
         /*  no longer connected */
         service.disconnect(this);
      }
   }

   /**
    * Return true if closed.
    */
   public boolean isClosed()
   {
      return(closed);
   }
   
   /**
    * Get the transport.
    */
   public MsgTransport getTransport()
   {
      return(transport);
   }

   /**
    * Get the local connection type.
    */
   public int getConnType()
   {
      return(connType);
   }
   
   /**
    * Get the remote connection type.
    */
   public int getRemoteConnType()
   {
      return(remoteConnType);
   }
   
   /**
    * Send a message without a response.
    */
   public void sendMsg(int msgType,Msg msg)
   {
      sendMsg(msgType,msg,null);
   }
   
   /**
    * Send a message and wait for a response of the specified type.
    */
   public <T extends Msg> T sendMsg(int msgType,Msg msg,Class<T> responseClass)
   {
      byte msgId = 0;
      byte[] bytes = null;
      
      /* if we have a valid message, get the relevant bits */
      if(msg != null)
      {
         msgId = msg.getMsgId();
         bytes = msg.toBytes();
      }
      
      return(sendMsg(msgType,msgId,bytes,responseClass));
   }
   
   /**
    * Send the message.
    */
   private <T extends Msg> T sendMsg(int msgType,byte msgId,byte[] payload,
      Class<T> responseClass)
   {
      /* if closed, do nothing */
      if(closed)
         return(null);
      
      /* if no msgId, allocate one */
      if(msgId == 0)
         msgId = nextMsgId();
      
      /* if no payload, use an empty one */
      if(payload == null)
         payload = new byte[0];
      
      /* make sure the payload is within the size constraints of the receiver */
      if((remoteMaxPayloadSize > 0) && (payload.length > remoteMaxPayloadSize))
         throw(new PayloadTooBigException("Payload is larger than receiver can handle: "+payload.length));
      
      /* allocate a buffer to hold the packet */
      byte[] bytes = new byte[HDR_SIZE + payload.length + CRC_SIZE];

      /* fill in the header */
      bytes[0] = START_BYTE;
      bytes[1] = (byte)msgType;
      bytes[2] = (byte)msgId;
      bytes[3] = 0;
      ByteUtil.setShort(bytes,4,payload.length);
      ByteUtil.setShort(bytes,6,0);

      /* fill in the payload */
      System.arraycopy(payload,0,bytes,8,payload.length);
      
      /* compute the crc and add it if needed */
      if(transport.includeChecksum())
      {
         int crc = crc16(0,bytes,0,HDR_SIZE+payload.length);
         ByteUtil.setShort(bytes,HDR_SIZE+payload.length,crc);
      }
      
      /* send the packet */
      synchronized(this) {
         transport.write(bytes);
      }
      
      /* if a response is expected, wait for it */
      if(responseClass != null)
         return(receiveMsg(msgType,msgId,responseClass));
      return(null);
   }

   /**
    * Receive a single packet from the connection and try to create
    * an instance of the specified class from the payload.
    */
   private <T extends Msg> T receiveMsg(int msgType,int msgId,Class<T> responseClass)
   {
      /* create a handler to listen for the response */
      MsgHandler<T> handler = new MsgHandler<T>(msgType,msgId,responseClass,true) {
         public void onReceive(MsgConn conn,T msg) {
            resp = msg;
            synchronized(this) {
               this.notify();
            }
         }
      };
      
      try {
         synchronized(handler) {
            /* add the handler */
            synchronized(handlers) {
               handlers.add(handler);
            }
            
            /* wait for a response */
            handler.wait(RECEIVE_TIMEOUT);
         }
      } catch(InterruptedException ex) {
         throw(new MsgException("Receive timed out"));
      }
      
      return(handler.resp);
   }
   
   /**
    * Receiver thread that processes incoming messages.
    */
   private void receiveThread()
   {
      while(!closed)
      {
         try {
            /* if the transport is down, wait for it to come up */
            if(!transport.isUp())
            {
               synchronized(transport) {
                  transport.wait();
               }
            }
            
            /* no timeout waiting for a header */
            transport.setReadTimeout(0);
         
            /* allocate a buffer for header */
            byte[] hdr = new byte[HDR_SIZE];
   
            /* read the header */
            if(read(hdr) != hdr.length)
               throw(new InvalidResponseException("Incomplete header read"));
         
            /* verify the start byte */
            if(hdr[0] != START_BYTE)
               throw(new InvalidResponseException("Invalid start byte"));

            /* make sure the payload size is valid */
            int payloadLen = ByteUtil.getShort(hdr,4);
            if(payloadLen > MAX_RX_PAYLOAD_SIZE)
               throw(new InvalidResponseException("Invalid paylod size: "+payloadLen));
         
            /* set a timeout for the rest of the message */
            transport.setReadTimeout(RECEIVE_TIMEOUT);
         
            /* read the payload */
            byte[] payload = new byte[payloadLen];
            if(read(payload) != payload.length)
               throw(new InvalidResponseException("Incomplete payload"));
         
            /* read the crc  */
            byte[] crcBuf = new byte[CRC_SIZE];
            if(read(crcBuf) != crcBuf.length)
               throw(new InvalidResponseException("Incomplete crc read"));            

            /* verify the crc if needed */
            if(transport.includeChecksum())
            {
               /* compute the crc */
               int crc = crc16(0,hdr,0,hdr.length);
               crc = crc16(crc,payload,0,payload.length);            
         
               /* compare to the crc in the packet */
               if(ByteUtil.getUnsignedShort(crcBuf,0) != crc)
                  throw(new InvalidResponseException("Invalid crc"));
            }

            /* try to dispatch to any connection level handlers and if none */
            /* found then try to dispatch to the service level handlers */
            if(!service.dispatchMsg(handlers,this,hdr[1],hdr[2],payload))
               service.dispatchMsg(this,hdr[1],hdr[2],payload);
         } catch(Exception ex) {
            /* if the transport isn't durable or is closed, cleanup */
            if(!transport.isDurable() || closed)
            {
               service.removeConn(this);
               return;
            }
         }
      }
   }
   
   /**
    * Read the contents of the specified buffer or return a lower read count
    * if there is a timeout.
    */
   private int read(byte[] buf) throws Exception
   {
      int cnt = 0;
      int offset = 0;
      
      do
      {
         /* try to read the next block of data unless there is an error or timeout */
         if((cnt = transport.read(buf,offset,buf.length-offset)) <= 0)
            break;
         
         /* adjust the offset */
         offset += cnt;
      }
      while(offset < buf.length);
      
      return(offset);
   }
   
   /**
    * Return the crc16 value for the specified bytes.
    */
   private int crc16(int crc, byte[] bytes,int offset,int len)
   { 
      for(int i=0;i<len;i++)
      {
         crc ^= ((int)bytes[offset+i]) & 0xff;
         for(int j=0;j<8;++j)
         {
            if((crc & 1) == 1)
               crc = ((crc >> 1) ^ 0xA001) & 0xffff;
            else
               crc = (crc >> 1);
         }
      }
      return(crc);
   }
   
   /**
    * Return the next available message id.  The java side always allocated id's in
    * the range of 1..127 whereas the board will allocate id's in the range of -128..-1.
    * This ensures that id's never overlap even though there are two allocators.
    */
   synchronized private byte nextMsgId()
   {
      if(++nextMsgId <= 0)
         nextMsgId = 1;
      return(nextMsgId);
   }
   
   /**
    * Called by the transport when it goes up.
    */
   protected void transportUp()
   {
      /* try to send a connect message */
      service.sendConnectRequest(this);
   }
   
   /**
    * Called by the transport when it goes down.
    */
   protected void transportDown()
   {
      /* no longer connected */
      service.disconnect(this);
   }
}
