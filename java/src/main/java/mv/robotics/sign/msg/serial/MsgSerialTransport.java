/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.serial;

import java.util.Enumeration;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import mv.robotics.sign.msg.ex.MsgException;

/**
 * An implementation of Transport over serial ports.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-16-16
 */
public class MsgSerialTransport extends mv.robotics.sign.msg.MsgTransport
{
   /* flag used to enable device enumeration which handles devices being */
   /* added and removed on the fly and prevents the jvm from crashing */
   /* when referring to ports that have been unplugged */
   static private boolean enumeratePorts;
   
   private String device;              /* the device name */
   private int baud;                   /* the baud rate */
   private SerialPort serialPort;      /* the opened serial port */
   private int timeout;                /* read timeout */
   
   /**
    * Create a new transport for the specified serial port.
    * 
    * @param device   the name of the device
    * @param buad     the baud rate
    */
   public MsgSerialTransport(String device,int baud)
   {
      this.device = device;
      this.baud = baud;
   }
   
   @Override
   public boolean isChecked()
   {
      return(true);
   }
   
   @Override
   public void down()
   {
      /* cleanup the serial port */
      close();
      
      /* flag that we need to enumerate on the next attemptUp() call */
      enumeratePorts = true;
      
      super.down();
   }
      
   @Override
   public void attemptUp()
   {
      try {
         /* check if we need to enumerate ports */
         if(enumeratePorts)
         {
            /* enumerate the ports as this causes the rxtx driver to */
            /* detect when ports go away such as when the usb cable is */
            /* pulled on a device or the device loses power... without */
            /* this the device remains available and the jvm crashes */
            Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
            while(ports.hasMoreElements())
               ports.nextElement();
            
            /* done enumerating */
            enumeratePorts = false;
         }
         
         /* get the port identifier from the name */
         CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(device); 
         
         /* make sure the port isn't in use */
         if(!identifier.isCurrentlyOwned())
         {
            /* attempt to open the serial port */
            CommPort commPort = identifier.open(this.getClass().getName(),2000);
            
            /* if it's a serial port, configure it */
            if(commPort instanceof SerialPort)
            {
               /* configure the port */
               serialPort = (SerialPort)commPort;
               serialPort.setSerialPortParams(baud,SerialPort.DATABITS_8,
                  SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
               
               /* set the timeout */
               setReadTimeout(timeout);
               
               /* we're up */
               up();
            }
         }
      } catch(Throwable t) {
         /* fall through */
      }
   }
   
   @Override
   public boolean isDurable()
   {
      return(true);
   }
   
   @Override
   public void setReadTimeout(int timeout)
   {
      /* remember the timeout */
      this.timeout = timeout;
      
      /* if we have a serial port, try to configure it */
      if(serialPort != null)
      {
         try {
            if(timeout == 0)
               serialPort.disableReceiveTimeout();
            else
               serialPort.enableReceiveTimeout(timeout);      
         } catch(Throwable t) {
            down();
            throw(new MsgException("Failed to set read timeout on serial port",t));
         }
      }
   }
   
   @Override
   public int read(byte[] buf,int offset,int len)
   {
      try {
         return(serialPort.getInputStream().read(buf,offset,len));
      } catch(Throwable t) {
         down();
         throw(new MsgException("Failed to read from serial port",t));
      }
   }
   
   @Override
   public void write(byte[] buf)
   {
      try {
         serialPort.getOutputStream().write(buf);
      } catch(Throwable t) {
         down();
         throw(new MsgException("Failed to write to serial port",t));
      }
   }

   @Override
   public void close()
   {
      serialPort.close();
   }
}
