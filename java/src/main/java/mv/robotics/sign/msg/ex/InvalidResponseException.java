/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.ex;

/**
 * Exceptions for failures in response processing.
 * 
 * @author  David Vogt (david@kondra.com) 
 * @version Jun-13-16
 */
public class InvalidResponseException extends MsgException
{
   static private final long serialVersionUID = 7066581104074035841L;

   /**
    * Create a new exception.
    */
   public InvalidResponseException(String msg)
   {
      super(msg);
   }
   
   /**
    * Create a new exception with a message and cause.
    */
   public InvalidResponseException(String msg,Throwable t)
   {
      super(msg,t);
   }
}
