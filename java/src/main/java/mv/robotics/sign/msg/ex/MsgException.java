/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.ex;

/**
 * Base exception class for messaging exceptions.
 * 
 * @author  David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
public class MsgException extends RuntimeException
{
   static private final long serialVersionUID = -3324399698573265012L;

   /**
    * Create a new exception.
    */
   public MsgException()
   {
   }
   
   /**
    * Create a new exception with a message.
    */
   public MsgException(String msg)
   {
      super(msg);
   }
   
   /**
    * Create a new exception with a message and cause.
    */
   public MsgException(String msg,Throwable t)
   {
      super(msg,t);
   }
}
