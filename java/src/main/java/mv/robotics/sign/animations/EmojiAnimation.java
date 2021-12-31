/**
 * (C) Copyright 2021, David Vogt, All rights reserved.
 */
package mv.robotics.sign.animations;

import java.awt.image.BufferedImage;
import java.util.Random;

import javax.imageio.ImageIO;

import mv.robotics.sign.Display;

/**
 * Simple animation that loads an emoji image and scrolls a view portal
 * around the image.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Dec-29-21
 */
public class EmojiAnimation extends Animation
{
   private BufferedImage image;   /* the emoji image */
   private Random random;         /* random number generator */
   private double dx;             /* delta X value for scrolling */
   private double dy;             /* delta Y value for scrolling */
   private double x;              /* current X position of the view */
   private double y;              /* current Y position of the view */
   
   {
      /* allocate a random number generator */
      random = new Random();
   }
   
   @Override
   public void start() throws Exception
   {
      /* load the image */
      image = ImageIO.read(getClass().getResourceAsStream("/emoji.jpg"));
      
      /* pick random scroll values */
      dx = random.nextDouble();
      dy = random.nextDouble();
   }
   
   @Override
   public void stop()
   {
      /* release the image */
      image = null;
   }
   
   @Override
   public BufferedImage tick()
   {
      /* move the view */
      x += dx;
      y += dy;
      
      /* check if left side of the view is outside of the image */
      if(x <= 0)
      {
         x = 0;
         dx = -dx;
      }
      
      /* check if right side of the view is outside of the image */
      if((x+Display.WIDTH) >= image.getWidth())
      {
         x = image.getWidth() - Display.WIDTH;
         dx = -dx;
      }
      
      /* check if the top of the view is outside of the image */
      if(y <= 0)
      {
         y = 0;
         dy = -dy;
      }
      
      /* check if the bottom of the view is outside of the image */
      if((y+Display.HEIGHT) >= image.getHeight())
      {
         y = image.getHeight() - Display.HEIGHT;
         dy = -dy;
      }
      
      /* return the view of the image */
      return(image.getSubimage((int)x,(int)y,Display.WIDTH,Display.HEIGHT));
   }
}
