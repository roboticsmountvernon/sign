/**
 * (C) Copyright 2021, David Vogt, All rights reserved.
 */
package mv.robotics.sign;

import mv.robotics.sign.msg.Msg;

/**
 * Message that contains image data for the display.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Dec-29-21
 */
public class ImageDataMsg extends Msg
{
   private int[] pixels;               /* image data */
   
   public ImageDataMsg(int[] pixels)
   {
      this.pixels = pixels;
   }

   @Override
   public byte[] toBytes()
   {
      /* convert the pixel data to an array of bytes to send over usb to the teensy board */
      /* we send three bytes (r,g,b) per pixel so allocate an array large enough to hold */
      /* all the pixel data */
      byte[] bytes = new byte[pixels.length*3];
      
      /* process each pixel and copy the r,g,b values into the byte array */
      for(int i=0;i<pixels.length;i++)
      {
         /* offset into the byte array... three bytes per pixel so jump by three */
         int offset = i*3;
         
         /* grab the next pixel */
         int pixel = pixels[i];
         
         /* a pixel is 32bits in this ARGB format so pull the 8bit values out */
         bytes[offset] = (byte)(pixel >> 16);
         bytes[offset+1] = (byte)(pixel >> 8);
         bytes[offset+2] = (byte)(pixel >> 0);
      }
      return(bytes);
   }
}
