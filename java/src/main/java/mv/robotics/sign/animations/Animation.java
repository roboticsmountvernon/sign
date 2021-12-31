/**
 * (C) Copyright 2021, David Vogt, All rights reserved.
 */
package mv.robotics.sign.animations;

import java.awt.image.BufferedImage;

/**
 * Base class for animations.  An instance of Animation may be used multiple
 * times, but will never be used concurrently.  That is, start()/stop() may
 * be called multiple times with the expectation that the resulting animation
 * is the same each time, but there will never be two calls to start() without
 * a corresponding stop() for the first start().
 * 
 * Animations are the result of implementing tick() which returns an image
 * to display.  The tick() method will be called every 30ms while the animation
 * is active.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Dec-29-21
 */
abstract public class Animation
{
   /**
    * Called before the first call to tick().  This is generally used
    * to setup the initial state of the animation.  As animations can
    * be reused, this may be called more than once.  Every call to start()
    * will have a corresponding call to stop() which allows the animation
    * to cleanup state.  Override as needed.
    */
   public void start() throws Exception
   {
   }
   
   /**
    * Called when an animation has ended.  Once this is called there will
    * be no more calls to tick() without first calling start() again.
    * This is generally used to cleanup any resources allocated by the
    * animation process.  Override as needed.
    */
   public void stop() throws Exception
   {
   }
   
   /**
    * Called for every tick of animation.  Returns a BufferedImage that
    * contains the content to render.  This will be merged with other
    * animation images to yield a final composite image for display.
    * Alpha channel data is used during the composite phase so this
    * can be used to support transparency, masks, etc...
    * 
    * The returned image should be the size of the display.  If not, a
    * sub-image will be generated from the origin.  If the image is smaller
    * than the display then it will not be used at all.  If the animation
    * uses a larger image to implement scrolling effects simply use the
    * getSubimage() method of BufferedImage to return the correct size
    * image.  This makes it easy to render a complex image in start() and
    * then simply return a view into that complex image without needing
    * to re-render it over and over.
    * 
    * @return   the image to composite into a final frame for the display
    */
   abstract public BufferedImage tick();
}
