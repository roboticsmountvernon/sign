/*----------------------------------------------------------------------*/
/* File : SerialWrapper.h                                               */
/*----------------------------------------------------------------------*/
/* Author      : David Vogt                                             */
/* Date        : 13-Jun-16                                              */
/* Description : Wrapper for HardwareSerial and usb_serial_class.       */
/*----------------------------------------------------------------------*/
/*  (C) Copyright 2016, Look Left Studio                                */
/*  All rights reserved.                                                */
/*----------------------------------------------------------------------*/
#ifndef _SERIALWRAPPER_H_
#define _SERIALWRAPPER_H_

#include "Arduino.h"

/*----------------------------------------------------------------------*/
/* Define a few structures...                                           */
/*----------------------------------------------------------------------*/

class SerialWrapper
{
   public:
      virtual int available();
      virtual int readBytes(uint8_t *buf,int cnt);
      virtual int write(uint8_t *buf,int cnt);
};

class HardwareSerialWrapper: public SerialWrapper
{
   public:
      HardwareSerialWrapper(HardwareSerial *dev);
      int available();
      int readBytes(uint8_t *buf,int cnt);
      int write(uint8_t *buf,int cnt);

   private:
      HardwareSerial *dev;
};

class UsbSerialWrapper: public SerialWrapper
{
   public:
      UsbSerialWrapper(usb_serial_class *dev);
      int available();
      int readBytes(uint8_t *buf,int cnt);
      int write(uint8_t *buf,int cnt);

   private:
      usb_serial_class *dev;
};

#endif /* _SERIALWRAPPER_H_ */
