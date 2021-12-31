/**
 * (C) Copyright 2021, David Vogt, All rights reserved.
 */
package mv.robotics.sign;

import java.awt.FlowLayout;

import javax.swing.JFrame;

import mv.robotics.sign.animations.Animation;
import mv.robotics.sign.animations.EmojiAnimation;
import mv.robotics.sign.msg.MsgService;
import mv.robotics.sign.msg.serial.MsgSerialTransport;

/**
 * Main entry point for the sign app.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Dec-29-21
 */
public class SignApp
{
   private MsgService msgService;  /* used to communicate with the teensy */
   private Display display;        /* the display to send pixels to */
   private JFrame frame;           /* the ui frame to show the display */
   private Animation animation;    /* the active animation */
   
   private SignApp(String[] args)
   {
      /* create a display to send the pixels to */
      display = new Display();
      
      /* create the message service */
      msgService = new MsgService();
      
      /* add the display as a listener so it knows when the teensy on */
      /* the physical display connects and disconnects */
      msgService.addListener(display);
      
      /* add a transport for the usb port so we can talk to the teensy board on the display*/
      msgService.addTransport(new MsgSerialTransport("/dev/ttyACM0",115200),0);

      /* the ui code to show the simulated display */
      frame = new JFrame();
      frame.getContentPane().setLayout(new FlowLayout());
      frame.getContentPane().add(display);
      frame.pack();
      frame.setResizable(false);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setAlwaysOnTop(true);
      frame.setVisible(true);
      
      /* setup an animation */
      animation = new EmojiAnimation();
   }
   
   /**
    * Start the app.
    */
   private void start()
   {
      /* start the message service */
      msgService.start();

      /* dummy code to drive an animation... should be replaced with */
      /* something like timeline logic that allows multiple animations */
      /* to be queued and stacked in layers, etc... */
      try {
         animation.start();
         
         for(int i=0;i<100000;i++)
         {
            display.render(animation.tick());
            Thread.sleep(30);            
         }
         
         animation.stop();
      } catch(Exception ex) {
         ex.printStackTrace();
      }
   }
   
   /**
    * Main program entry point
    */
   static public final void main(String[] args)
   {
      /* create an instance of the object and pass it the command line arguments */
      SignApp app = new SignApp(args);
      app.start();
   }
}
