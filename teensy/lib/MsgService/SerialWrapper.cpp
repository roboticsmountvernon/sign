/*----------------------------------------------------------------------*/
/* File : SerialWrapper.cpp                                             */
/*----------------------------------------------------------------------*/
/* Author      : David Vogt                                             */
/* Date        : 13-Jun-13                                              */
/* Description : Wrapper for various types of serial interfaces.        */
/*----------------------------------------------------------------------*/
/*  (C) Copyright 2016, Look Left Studio                                */
/*  All rights reserved.                                                */
/*----------------------------------------------------------------------*/
#include "SerialWrapper.h"

/*----------------------------------------------------------------------*/
/* HardwareSerialWrapper                                                */
/*----------------------------------------------------------------------*/

HardwareSerialWrapper::HardwareSerialWrapper(
   HardwareSerial *dev)
{
   this->dev = dev;
}

int
HardwareSerialWrapper::available()
{
   return(dev->available());
}

int
HardwareSerialWrapper::readBytes(
   uint8_t *buf,
   int cnt)
{
   return(dev->readBytes(buf,cnt));
}

int
HardwareSerialWrapper::write(
   uint8_t *buf,
   int cnt)
{
   return(dev->write(buf,cnt));
}

/*----------------------------------------------------------------------*/
/* UsbSerialWrapper                                                     */
/*----------------------------------------------------------------------*/

UsbSerialWrapper::UsbSerialWrapper(
   usb_serial_class *dev)
{
   this->dev = dev;
}

int
UsbSerialWrapper::available()
{
   return(dev->available());
}

int
UsbSerialWrapper::readBytes(
   uint8_t *buf,
   int cnt)
{
   return(dev->readBytes((char*)buf,cnt));
}

int
UsbSerialWrapper::write(
   uint8_t *buf,
   int cnt)
{
   return(dev->write(buf,cnt));
}
