/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

/**
 * Base class for beans that we can receive.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
abstract public class Msg
{
   private byte msgId;                    /* id of this message */
   
   public byte getMsgId() {
      return msgId;
   }
   public void setMsgId(byte id) {
      this.msgId = id;
   }

   /**
    * Return a byte representation of this bean.
    */
   public byte[] toBytes()
   {
      return(new byte[0]);
   }
   
   /**
    * Populate the bean from a byte representation.
    */
   public void fromBytes(byte[] bytes)
   {
   }
}
