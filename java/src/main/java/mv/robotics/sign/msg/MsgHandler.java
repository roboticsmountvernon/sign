/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

/**
 * A message handler.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-16-16
 */
public class MsgHandler <T extends Msg>
{
   private int msgType;                /* the message type to look for */ 
   private int msgId;                  /* the message id to look for 0=any */
   private Class<T> clazz;             /* the response class type */
   private boolean remove;             /* if true, remove the handler */
   protected T resp;                   /* used for blocking receive calls */
   
   /**
    * Create a message handler for the specified message type to be called
    * with messages of the specified type.
    * 
    * @param msgType   the message type to listen for
    * @param clazz     the class to transform the message to
    */
   public MsgHandler(int msgType,Class<T> clazz)
   {
      this(msgType,0,clazz,false);
   }
   
   /**
    * Internal constructor.
    */
   protected MsgHandler(int msgType,int msgId,Class<T> clazz,boolean remove)
   {
      this.msgType = msgType;
      this.msgId = msgId;
      this.clazz = clazz;
      this.remove = remove;
   }

   public int getMsgType() {
      return msgType;
   }
   public int getMsgId() {
      return msgId;
   }
   public Class<T> getClazz() {
      return clazz;
   }
   public boolean isRemove() {
      return remove;
   }

   protected void processPayload(MsgConn conn,byte[] payload) throws Exception
   {
      /* create an instance of the response object */
      T resp = clazz.newInstance();
      resp.fromBytes(payload);
      onReceive(conn,resp);         
   }
   
   /**
    * The method called with incoming messages.
    */
   public void onReceive(MsgConn conn,T msg)
   {
   }
}
