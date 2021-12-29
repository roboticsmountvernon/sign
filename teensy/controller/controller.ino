/*----------------------------------------------------------------------*/
/* File : controller.ino                                                */
/*----------------------------------------------------------------------*/
/* Author      : David Vogt                                             */
/* Date        : 29-Dec-21                                              */
/* Description : LED controller firmware.                               */
/*----------------------------------------------------------------------*/
#define FASTLED_ALLOW_INTERRUPTS 0
#include <Entropy.h>
#include <FastLED.h>
#include <MsgService.h>

/*----------------------------------------------------------------------*/
/* Define a few constants...                                            */
/*----------------------------------------------------------------------*/

// The number of leds in the display
#define LED_WIDTH             30
#define LED_HEIGHT            10
#define LED_COUNT             (LED_WIDTH * LED_HEIGHT)

// The pin to use to send data to the leds
#define LED_PIN               2

// Max milliamps for the display... keeps power requirements under control
#define MAX_MILLIAMPS         3000

// Message id's that we can receive from the computer
#define MSG_SET_IMAGE_DATA    1

/*----------------------------------------------------------------------*/
/* Globals private to this file...                                      */
/*----------------------------------------------------------------------*/

// Messaging service used to talk to an external computer via usb.
MsgService *msgService;

// The array of leds that hold color data for the display.
CRGB leds[LED_COUNT];

/*----------------------------------------------------------------------*/
/* Functions private to this file...                                    */
/*----------------------------------------------------------------------*/

/**
 * Called when image data is received from the computer
 */
static void
setImageData(
   MsgService *service,
   struct msgHdr *hdr,
   void *userData)
{
   uint8_t *color;
   int offset;
   
   /* copy the data into the leds array */
   color = service->getRxBuf();

   /* image data comes in rows (left to right, top to bottom) from */
   /* java so we need to transform it into the up/down, left to right */
   /* format that the display uses in order for it to look correct */
   for(int y=0;y<LED_HEIGHT;y++)
   {
      for(int x=0;x<LED_WIDTH;x++)
      {
         /* y is normal in odd columns... */
         if(x & 1)
            offset = (x*LED_HEIGHT)+y;

         /* ...but reversed in even columns */
         else
            offset = ((x+1)*LED_HEIGHT)-(y+1);

         /* copy color data to the correct led */
         leds[offset].r = color[0];
         leds[offset].g = color[1];
         leds[offset].b = color[2];

         /* skip to the next pixel in the input data */
         color+=3;
      }
   }

   /* update the display */
   FastLED.show();
}

/**
 * Set all leds to the specified rgb color and show them, followed
 * by the specified delay.  Used for startup test.
 */
static void
showColor(
   int r,
   int g,
   int b,
   int delayMS)
{
   fill_solid(leds, LED_COUNT, CRGB(r,g,b));
   FastLED.show();
   delay(delayMS);
}

/*----------------------------------------------------------------------*/
/* Functions exported from this file...                                 */
/*----------------------------------------------------------------------*/

/**
 * Standard setup function for adruino.
 */
void
setup()
{
   /* setup the usb serial port for communicating with computer */
   Serial.begin(115200);

   /* setup the debug serial port */
   Serial1.begin(115200);

   delay(1000);
   Serial1.println("controller starting");

   /* setup random number generator */
   Entropy.Initialize();

   /* setup leds */
   FastLED.addLeds<NEOPIXEL,LED_PIN>(leds, LED_COUNT);
   FastLED.setMaxPowerInVoltsAndMilliamps(5, MAX_MILLIAMPS);

   /* quick led test */
   showColor(0,0,0,1000);   // black for 1 sec
   showColor(64,0,0,1000);  // red for 1 sec
   showColor(0,64,0,1000);  // green for 1 sec
   showColor(0,0,64,1000);  // blue for 1 sec
   showColor(0,0,0,0);      // back to black

   /* create the message service so we can talk to java */
   msgService = new MsgService(new UsbSerialWrapper(&Serial), 1, 4096, 256);

   /* register message handlers so we can receive image data */
   msgService->addHandler(MSG_SET_IMAGE_DATA, setImageData, NULL);
}

/**
 * Standard loop function for arduino.
 */
void
loop()
{
   /* process incoming messages from java forever */
   msgService->poll();
}
