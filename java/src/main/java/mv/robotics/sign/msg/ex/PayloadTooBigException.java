/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.ex;

/**
 * Exception for payloads that are bigger than the receiver can handle.
 * 
 * @author  David Vogt (david@kondra.com) 
 * @version Jun-13-16
 */
public class PayloadTooBigException extends MsgException
{
   static private final long serialVersionUID = -5942035340437721486L;

   /**
    * Create a new exception.
    */
   public PayloadTooBigException(String msg)
   {
      super(msg);
   }
   
   /**
    * Create a new exception with a message and cause.
    */
   public PayloadTooBigException(String msg,Throwable t)
   {
      super(msg,t);
   }
}
