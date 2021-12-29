/*----------------------------------------------------------------------*/
/* File : MsgService.cpp                                                */
/*----------------------------------------------------------------------*/
/* Author      : David Vogt                                             */
/* Date        : 13-Jun-16                                              */
/* Description : Implementation of the client/server serial protocol.   */
/*----------------------------------------------------------------------*/
/*  (C) Copyright 2016, David Vogt                                      */
/*  All rights reserved.                                                */
/*----------------------------------------------------------------------*/
#include <Entropy.h>
#include "MsgService.h"

/*----------------------------------------------------------------------*/
/* Functions private to this file...                                    */
/*----------------------------------------------------------------------*/

/*----------
- Reset the receive state of the service.
----------*/
void
MsgService::resetReceiveState()
{
   payloadSize = 0;
   packetSize = 0;
   inPos = 0;
   rxPos = sizeof(struct msgHdr);
   txPos = sizeof(struct msgHdr);
   timeout = 0;
}

/*----------
- Dispatch the current message to the associated handler
-   and check for errors from the handler.
----------*/
void
MsgService::dispatchMsg()
{
   struct msgHandlerNode *node;
   struct msgHdr *hdr;

   /* map the header over the packet so we can get the message type */
   hdr = (struct msgHdr*)(inBuf);

   /* search the handler list for a matching message type */
   for(node=msgHandlers;node!=NULL;node=node->next)
   {
      /* dispatch matching message types */
      if(node->msgType == hdr->msgType)
         node->handler(this,hdr,node->userData);
   }
}

/*----------
- Process a connect message.
----------*/
void
MsgService::connect(
   MsgService *service,
   struct msgHdr *hdr,
   void *userData)
{
   struct msgResetHandlerNode *node;
   struct msgConnect *in;
   struct msgConnect *out;

   /* get the remote data */
   in = (struct msgConnect*)service->getRxBuf();
   service->remoteConnType = in->connType;

   /* check for reset */
   if(in->connId != service->remoteConnId)
   {
      /* remember the new connection id */
      service->remoteConnId = in->connId;

      /* call all the reset handlers */
      for(node=service->resetHandlers;node!=NULL;node=node->next)
         node->handler();
   }

   /* build the response */
   out = (struct msgConnect*)service->getTxBuf();
   out->connId = service->connId;
   out->maxPayloadSize = service->inBufSize - sizeof(struct msgHdr) - 4;
   out->connType = service->connType;

   /* return our connect response */
   service->send(1,0,sizeof(struct msgConnect));
}

/*----------------------------------------------------------------------*/
/* Functions exported from this file...                                 */
/*----------------------------------------------------------------------*/

/*----------
- MsgService() : Constructor.
----------*/
MsgService::MsgService(
   SerialWrapper *port,
   uint16_t connType,
   unsigned int inBufSize,
   unsigned int outBufSize)
{
   /* set the port and connection id */
   this->port = port;
   this->connType = connType;
   connId = Entropy.random();

   /* allocate input and output buffers */
   this->inBufSize = inBufSize;
   this->outBufSize = outBufSize;
   this->inBuf = (uint8_t*)malloc(inBufSize);
   this->outBuf = (uint8_t*)malloc(outBufSize);

   /* initialize some values */
   remoteConnType = 0;
   remoteConnId = 0;
   msgHandlers = NULL;
   resetHandlers = NULL;

   /* register the handler for connect messages */
   addHandler(0,connect,NULL);

   /* reset the receive state machine */
   resetReceiveState();
}

/*----------
- poll() : This function drives the receive side of the
-   service.  This should be called from the loop() function.
----------*/
void
MsgService::poll()
{
   struct msgHdr *hdr;
   unsigned int cnt;

   /* check if there are any bytes available */
   if((cnt = port->available()) > 0)
   {
      /* check if the timeout expired from the previous read */
      if(millis() > timeout)
         resetReceiveState();
         
      /* timeout for receiving the next byte */
      timeout = millis() + MSG_MAX_READ_DELAY;

      /* limit the count to the max buffer size */
      if((cnt + inPos) > inBufSize)
         cnt = inBufSize - inPos;

      /* limit the count to the packet so we don't */
      /* read beyond the current packet */
      if((packetSize > 0) && ((cnt + inPos) > packetSize))
         cnt = packetSize - inPos;

      /* read the bytes into the packet buffer */
      port->readBytes(&inBuf[inPos],cnt);

      /* check if we just finished reading the header */
      if((inPos < sizeof(struct msgHdr)) &&
         ((inPos + cnt) >= sizeof(struct msgHdr)))
      {
         /* just got a full header... map a header over top of the packet */
         hdr = (struct msgHdr*)inBuf;

         /* check the magic start byte */
         if(hdr->startByte != MSG_PACKET_START_BYTE)
            goto err;

         /* check that the length of the payload is valid */
         if(hdr->len > (inBufSize - sizeof(struct msgHdr)))
            goto err;

         /* header seems good, remember the size */
         payloadSize = hdr->len;
         packetSize = hdr->len + sizeof(struct msgHdr) + 2;
      }

      /* update the position */
      inPos += cnt;

      /* check for end of packet */
      if((packetSize > 0) && (inPos >= packetSize))
      {
//Serial.print("full packet : ");
//Serial.println(inPos);
         /* validate the crc */
//         crc = crc16(0,service->inBuf,service->packetSize-sizeof(short));
//         if(crc != *((unsigned short*)(service->inBuf+service->packetSize-sizeof(short))))
//            goto err;

         /* packet seems good, try to dispatch it */
         dispatchMsg();

         /* reset the receive state for the next packet */
         resetReceiveState();
      }      
   }

   return;
err:
   resetReceiveState();
}

/*----------
- addHandler() : Register a message handler with the service.
----------*/
int
MsgService::addHandler(
   int msgType,
   msgHandler handler,
   void *userData)
{
   struct msgHandlerNode *node;

   /* attempt to allocate a new node */
   if((node = (struct msgHandlerNode*)malloc(sizeof(struct msgHandlerNode))) == NULL)
      return(-1);

   /* setup the node */
   node->msgType = msgType;
   node->handler = handler;
   node->userData = userData;
   
   /* add the handler to the list in the service */
   node->next = msgHandlers;
   msgHandlers = node;

   return(0);
}

/*----------
- addResetHandler() : Register a reset handler with the service.
----------*/
int
MsgService::addResetHandler(
   msgResetHandler handler)
{
   struct msgResetHandlerNode *node;

   /* attempt to allocate a new node */
   if((node = (struct msgResetHandlerNode*)malloc(sizeof(struct msgResetHandlerNode))) == NULL)
      return(-1);

   /* setup the node */
   node->handler = handler;

   /* add the handler to the list in the service */
   node->next = resetHandlers;
   resetHandlers = node;

   return(0);
}

/*----------
- getTxBuf() : Return a pointer to the current position in the output buffer.
----------*/
uint8_t*
MsgService::getTxBuf()
{
   return(outBuf+txPos);
}

/*----------
- getRxBuf() : Return a pointer to the current position in the input packet.
----------*/
uint8_t*
MsgService::getRxBuf()
{
   return(inBuf+rxPos);
}

/*----------
- send() : Send a message contained in the output buffer using the output position
-   to compute the length.
----------*/
int
MsgService::send(
   int msgType,
   int msgId)
{
   return(send(msgType,msgId,txPos-sizeof(struct msgHdr)));
}
   
/*----------
- send() : Send a message contained in the output buffer.
----------*/
int
MsgService::send(
   int msgType,
   int msgId,
   int len)
{
   struct msgHdr *hdr;
   unsigned short crc = 0;
   int size;

   /* map the header just in front of the send buffer */
   hdr = (struct msgHdr*)outBuf;
   
   /* populate the header */
   hdr->startByte = MSG_PACKET_START_BYTE;
   hdr->msgType = msgType;
   hdr->msgId = msgId;
   hdr->len = len;

   /* compute the crc */
   size = sizeof(struct msgHdr) + len;
//   crc = crc16(crc,(char*)outBuf,size);
   outBuf[size++] = crc & 0xff;
   outBuf[size++] = (crc >> 8) & 0xff; 

   /* send the packet */
   port->write(outBuf,size);

   /* reset the output payload position */
   txPos = sizeof(struct msgHdr);

   return(0);
}

/*----------
- txInt8() : Add an int8_t to the output buffer.
----------*/
void
MsgService::txInt8(
   int8_t v)
{
   outBuf[txPos++] = v;
}

/*----------
- txUint8() : Add a uint8_t to the output buffer.
----------*/
void
MsgService::txUint8(
   uint8_t v)
{
   outBuf[txPos++] = v;
}

/*----------
- txChar() : Add a character to the output buffer.
----------*/
void
MsgService::txChar(
   char v)
{
   outBuf[txPos++] = v;
}

/*----------
- txBool() : Add a boolean to the output buffer.
----------*/
void
MsgService::txBool(
   int v)
{
   outBuf[txPos++] = v?1:0;
}

/*----------
- txInt16() : Add an int16_t to the output buffer.
----------*/
void
MsgService::txInt16(
   int16_t v)
{
   outBuf[txPos++] = (uint8_t)(v & 0xff);
   outBuf[txPos++] = (uint8_t)((v >> 8) & 0xff);
}

/*----------
- txUint16() : Add a uint16_t to the output buffer.
----------*/
void
MsgService::txUint16(
   uint16_t v)
{
   outBuf[txPos++] = (uint8_t)(v & 0xff);
   outBuf[txPos++] = (uint8_t)((v >> 8) & 0xff);
}

/*----------
- txInt32() : Add an int32_t to the output buffer.
----------*/
void
MsgService::txInt32(
   int32_t v)
{
   outBuf[txPos++] = (int8_t)(v & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 8) & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 16) & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 24) & 0xff);
}

/*----------
- txUint32() : Add a uint32_t to the output buffer.
----------*/
void
MsgService::txUint32(
   uint32_t v)
{
   outBuf[txPos++] = (int8_t)(v & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 8) & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 16) & 0xff);
   outBuf[txPos++] = (int8_t)((v >> 24) & 0xff);
}

/*----------
- txSkip() : Skip the specified number of bytes in the
-   output buffer.
----------*/
void
MsgService::txSkip(
   int skip)
{
   txPos += skip;
}

/*----------
- rxInt8() : Retun the next byte as int8_t.
----------*/
int8_t
MsgService::rxInt8()
{
   return(inBuf[rxPos++]);
}

/*----------
- rxUint8() : Retun the next byte as uint8_t.
----------*/
uint8_t
MsgService::rxUint8()
{
   return(inBuf[rxPos++]);
}

/*----------
- rxChar() : Retun the next byte as a char.
----------*/
char
MsgService::rxChar()
{
   return(inBuf[rxPos++]);
}

/*----------
- rxBool() : Retun the next byte as a boolean.
----------*/
int
MsgService::rxBool()
{
   return(inBuf[rxPos++]?1:0);
}

/*----------
- rxInt16() : Retun the next two bytes as an int16_t.
----------*/
int16_t
MsgService::rxInt16()
{
   int16_t val;
   val = inBuf[rxPos++];
   val |= inBuf[rxPos++] << 8;
   return(val);
}

/*----------
- rxUint16() : Retun the next two bytes as a uint16_t.
----------*/
uint16_t
MsgService::rxUint16()
{
   uint16_t val;
   val = inBuf[rxPos++];
   val |= inBuf[rxPos++] << 8;
   return(val);
}

/*----------
- rxInt32() : Retun the next four bytes as an int32_t.
----------*/
int32_t
MsgService::rxInt32()
{
   int32_t val;
   val = inBuf[rxPos++];
   val |= inBuf[rxPos++] << 8;
   val |= inBuf[rxPos++] << 16;
   val |= inBuf[rxPos++] << 24;
   return(val);
}

/*----------
- rxUint32() : Retun the next four bytes as a uint32_t.
----------*/
uint32_t
MsgService::rxUint32()
{
   uint32_t val;
   val = inBuf[rxPos++];
   val |= inBuf[rxPos++] << 8;
   val |= inBuf[rxPos++] << 16;
   val |= inBuf[rxPos++] << 24;
   return(val);
}

/*----------
- rxSkip() : Skip the specified number of input bytes.
----------*/
void
MsgService::rxSkip(
   int skip)
{
   rxPos += skip;
}

