/**
 * (C) Copyright 2021, David Vogt, All rights reserved.
 */
package mv.robotics.sign;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import mv.robotics.sign.msg.MsgConn;
import mv.robotics.sign.msg.MsgServiceListener;
import mv.robotics.sign.msg.ex.MsgException;

/**
 * This represents the display we're pushing pixels to.  This is
 * both an on-screen display as well as a controller for the
 * physical display when available.  This allows the user to see
 * the display on their monitor for testing but also pass the
 * pixel data to the hardware when it is connected.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Dec-29-21
 */
public class Display extends JPanel implements MsgServiceListener
{
   static private final long serialVersionUID = -3159573991302027764L;
   
   /* size of the display... don't change this unless the teensy code is also changed */
   static public final int WIDTH  = 30;
   static public final int HEIGHT = 10;
   
   /* size of a pixel in the simulated ui */
   static public final int PIXEL_SIZE = 20;

   private MsgConn displayConn;    /* usb connection to the hardware */
   private int[] pixels;           /* latest pixel data from render() call */

   public Display()
   {
      /* set the preferred size of this component so that it shows all the pixels */
      Dimension dim = new Dimension(WIDTH*PIXEL_SIZE, HEIGHT*PIXEL_SIZE);
      setPreferredSize(dim);
      
      /* start with black pixels */
      pixels = new int[WIDTH*HEIGHT];
   }
   
   /**
    * Render the pixels to the virtual and physical display.
    */
   public void render(BufferedImage img)
   {
      /* extract the pixels from the image */
      pixels = img.getRGB(0,0,WIDTH,HEIGHT,null,0,WIDTH);
      
      /* request the ui get repainted using the new pixel data */
      repaint();

      /* if we have a connection to the physical display, send the pixels */
      if(displayConn != null)
      {
         try {
            displayConn.sendMsg(1,new ImageDataMsg(pixels));
         } catch(MsgException ex) {
            /* ignore message exceptions */
         }
      }
   }
   
   @Override
   public void paint(Graphics gr)
   {
      /* pixel data is stacked in horizontal rows so iterate through it that way */
      for(int y=0;y<HEIGHT;y++)
      {
         for(int x=0;x<WIDTH;x++)
         {
            /* set the color for the next pixel */            
            int pixel = pixels[(y*WIDTH)+x];
            gr.setColor(new Color(pixel));
            
            /* draw the circle on the screen to represent the pixel */
            gr.fillOval(x*PIXEL_SIZE, y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);
         }
      }
   }
   
   @Override
   public void onConnect(MsgConn conn)
   {
      System.out.println("display connected");
      displayConn = conn;
   }

   @Override
   public void onDisconnect(MsgConn conn)
   {
      System.out.println("display disconnected");
      displayConn = null;      
   }
}
