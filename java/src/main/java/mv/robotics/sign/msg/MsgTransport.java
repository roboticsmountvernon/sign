/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg;

/**
 * Base class for transports.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
abstract public class MsgTransport
{
   private boolean up;                 /* true when the transport is up */
   protected MsgConn conn;             /* the associated connection */
   
   /**
    * Called when the transport is up.
    */
   public void up()
   {
      /* flag the transport up */
      up = true;
      
      /* notify the connection if we're linked */
      if(conn != null)
         conn.transportUp();
      
      /* notify anyone waiting for the transport to come up */
      synchronized(this) {
         this.notifyAll();
      }
   }
   
   /**
    * Called when the transport is down.
    */
   public void down()
   {
      /* flag the transport down */
      up = false;
      
      /* notify the connection if we're linked */
      if(conn != null)
         conn.transportDown();
   }
   
   /**
    * Return true if the transport is up.
    */
   public boolean isUp()
   {
      return(up);
   }

   /**
    * Called periodically when the transport is down
    * in an attempt to get it back up again.
    */
   public void attemptUp()
   {
      /* override as necessary */
   }
   
   /**
    * Return true to include crc16 checksum.
    */
   public boolean includeChecksum()
   {
      return(false);
   }
   
   /**
    * Return true if the connection should be periodically
    * checked to make sure it's up.  This is useful for serial
    * connections which can't necessarily detect that the other
    * side has gone down based on the transport status.
    */
   public boolean isChecked()
   {
      return(false);
   }
   
   /**
    * Return true if the transport can come back up after
    * going down.  Server sockets can't, while client sockets
    * can reconnect for example.
    */
   abstract public boolean isDurable();
   
   /**
    * Set the read timeout to the specifed number of ms.
    * 
    * @param timeout
    */
   abstract public void setReadTimeout(int timeout);
   
   /**
    * Read the specified number of bytes with the specified timeout.
    * 
    * @param buf       the buffer to read into
    * @param offset    the offset into the buffer
    * @param len       the number of bytes to read
    */
   abstract public int read(byte[] buf,int offset,int len);
   
   /**
    * Write the specified bytes.
    * 
    * @param buf   the bytes to write
    */
   abstract public void write(byte[] buf);
   
   /**
    * Close the transport.
    */
   abstract public void close();
}
