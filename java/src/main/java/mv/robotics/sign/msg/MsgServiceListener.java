/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

/**
 * Interface for listening to connection events.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
public interface MsgServiceListener
{
   /**
    * Called when a connection to the board is established.
    * 
    * @param conn   the connection
    */
   public void onConnect(MsgConn conn);
   
   /**
    * Called when a connection to the board is lost.
    * 
    * @param conn   the connection
    */
   public void onDisconnect(MsgConn conn);
}
