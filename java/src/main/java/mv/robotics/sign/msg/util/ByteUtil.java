/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A collection of byte oriented de/serialization methods.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
public class ByteUtil
{
   /**
    * Read a boolean from the backing array.
    * 
    * @param bytes    the byte array to read from
    * @param offset   start of the boolean in the array
    */
   static public boolean getBool(byte[] bytes,int offset)
   {
      return(bytes[offset] != 0);
   }
   
   /**
    * Write a boolean into the backing array.
    * 
    * @param bytes    the byte array to write to
    * @param offset   the start of the boolean in the array
    * @param val      the value to write
    */
   static public int setBool(byte[] bytes,int offset,boolean val)
   {
      bytes[offset] = (byte)(val?1:0);
      return(1);
   }
      
   /**
    * Read a little endian encoded short from the backing array.
    * 
    * @param bytes    the byte array to read from
    * @param offset   start of the short in the array
    */
   static public int getShort(byte[] bytes,int offset)
   {
      int ch1 = (((int)bytes[offset]) & 0xff) << 0;
      int ch2 = (((int)bytes[offset+1]) & 0xff) << 8;
      return((short)(ch1|ch2));
   }
   
   /**
    * Read a little endian encoded unsigned short from the backing array.
    * 
    * @param bytes    the byte array to read from
    * @param offset   start of the short in the array
    */
   static public int getUnsignedShort(byte[] bytes,int offset)
   {
      int ch1 = (((int)bytes[offset]) & 0xff) << 0;
      int ch2 = (((int)bytes[offset+1]) & 0xff) << 8;
      return((ch1|ch2) & 0xffff);
   }
   
   /**
    * Write a little endian encoded short into the backing array.
    * 
    * @param bytes    the byte array to write to
    * @param offset   the start of the short in the array
    * @param val      the value to write
    */
   static public int setShort(byte[] bytes,int offset,int val)
   {
      bytes[offset] = (byte)val;
      bytes[offset+1] = (byte)(val>>8);
      return(2);
   }
   
   /**
    * Read a little endian encoded int from the backing array.
    * 
    * @param bytes    the byte array to read from
    * @param offset   start of the int in the array
    */
   static public int getInt(byte[] bytes,int offset)
   {
      int ch1 = (((int)bytes[offset]) & 0xff) << 0;
      int ch2 = (((int)bytes[offset+1]) & 0xff) << 8;
      int ch3 = (((int)bytes[offset+2]) & 0xff) << 16;
      int ch4 = (((int)bytes[offset+3]) & 0xff) << 24;
      return(ch1|ch2|ch3|ch4);
   }
   
   /**
    * Write a little endian encoded int into the backing array.
    * 
    * @param bytes    the byte array to write to
    * @param offset   the start of the int in the array
    * @param val      the value to write
    */
   static public int setInt(byte[] bytes,int offset,int val)
   {
      bytes[offset] = (byte)val;
      bytes[offset+1] = (byte)(val>>8);
      bytes[offset+2] = (byte)(val>>16);
      bytes[offset+3] = (byte)(val>>24);      
      return(4);
   }

   /**
    * Write a boolean into the stream.
    * 
    * @param os    the output stream
    * @param val   the value to write
    */
   static public int setBool(OutputStream os,boolean val)
   {
      try {
         os.write(val?1:0);
      } catch(IOException ex) {
         throw(new RuntimeException(ex));
      }
      return(1);
   }
      
   /**
    * Write a little endian encoded short into the stream.
    * 
    * @param os    the output stream
    * @param val   the value to write
    */
   static public int setShort(OutputStream os,int val)
   {
      try {
         os.write((byte)val);
         os.write((byte)(val>>8));
      } catch(IOException ex) {
         throw(new RuntimeException(ex));
      }
      return(2);
   }
   
   /**
    * Write a little endian encoded int into the stream.
    * 
    * @param os    the output stream
    * @param val   the value to write
    */
   static public int setInt(OutputStream os,int val)
   {
      try {
         os.write((byte)val);
         os.write((byte)(val>>8));
         os.write((byte)(val>>16));
         os.write((byte)(val>>24));      
      } catch(IOException ex) {
         throw(new RuntimeException(ex));
      }
      return(4);
   }
}
