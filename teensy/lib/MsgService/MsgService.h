/*----------------------------------------------------------------------*/
/* File : MsgService.h                                                  */
/*----------------------------------------------------------------------*/
/* Author      : David Vogt                                             */
/* Date        : 13-Jun-16                                              */
/* Description : Header file for serial messaging.                      */
/*----------------------------------------------------------------------*/
/*  (C) Copyright 2016, David Vogt                                      */
/*  All rights reserved.                                                */
/*----------------------------------------------------------------------*/
#ifndef _MSGSERVICE_H_
#define _MSGSERVICE_H_

#include "Arduino.h"
#include "SerialWrapper.h"

/*----------------------------------------------------------------------*/
/* Define some useful constants...                                      */
/*----------------------------------------------------------------------*/

/*----------
- This is the magic start byte in front of each packet.
----------*/
#define MSG_PACKET_START_BYTE      '#'

/*----------
- The max time between characters when reading a packet before
- we reset the state machine.
----------*/
#define MSG_MAX_READ_DELAY         5

/*----------------------------------------------------------------------*/
/* Define a few structures...                                           */
/*----------------------------------------------------------------------*/

class MsgService;

/*----------
- This is a message handler callback signature.
----------*/
typedef void (*msgHandler)(MsgService *service,struct msgHdr *hdr,void *userData);

/*----------
- This is a reset handler callback signature.
----------*/
typedef void (*msgResetHandler)();

/*----------
- This is a single message handler in the handler list.
----------*/
struct msgHandlerNode {
   int msgType;                          /* the message type this handler is for */
   msgHandler handler;                   /* the callback handler function */
   void *userData;                       /* user supplied data */
   struct msgHandlerNode *next;          /* next handler in the list */
};

/*----------
- This is a single reset handler in the handler list.
----------*/
struct msgResetHandlerNode {
   msgResetHandler handler;              /* the callback handler function */
   struct msgResetHandlerNode *next;     /* next handler in the list */
};

/*----------
- This is a message header.
----------*/
struct msgHdr {
   char startByte;                       /* magic start of packet byte */
   uint8_t msgType;                      /* message type */
   uint8_t msgId;                        /* message id */
   uint8_t reserved1;                    /* not used */
   uint16_t len;                         /* length of the packet payload */
   uint16_t reserved2;                   /* not used */
};

/*----------
- This represents a single messaging service bound to a single serial port.
----------*/
class MsgService {
   public:
      MsgService(SerialWrapper *port,uint16_t connType,unsigned int inBufSize,unsigned int outBufSize);
      void poll();
      int addHandler(int msgType,msgHandler handler,void *data);
      int addResetHandler(msgResetHandler handler);
      int send(int msgType,int msgId);
      int send(int msgType,int msgId,int len);
      uint8_t *getTxBuf();
      uint8_t *getRxBuf();
      void txInt8(int8_t v);
      void txUint8(uint8_t v);
      void txChar(char v);
      void txBool(int v);
      void txInt16(int16_t v);
      void txUint16(uint16_t v);
      void txInt32(int32_t v);
      void txUint32(uint32_t v);
      void txSkip(int skip);
      int8_t rxInt8();
      uint8_t rxUint8();
      char rxChar();
      int rxBool();
      int16_t rxInt16();
      uint16_t rxUint16();
      int32_t rxInt32();
      uint32_t rxUint32();
      void rxSkip(int skip);

   private:
      void resetReceiveState();
      void dispatchMsg();
      static void connect(MsgService *service,struct msgHdr *hdr,void *data);
      
      SerialWrapper *port;                  /* serial port this service is bound to */
      uint16_t connType;                    /* the application specific connection type */
      uint32_t connId;                      /* the current connection id */
      uint16_t remoteConnType;              /* connection type of the remote node */
      uint32_t remoteConnId;                /* connection id of the other side */
      struct msgHandlerNode *msgHandlers;   /* list of message handlers */
      struct msgResetHandlerNode *resetHandlers; /* list of reset handlers */
      unsigned int payloadSize;             /* expected size of the current payload */
      unsigned int packetSize;              /* expected size of the current packet */
      unsigned int inBufSize;               /* size of the input buffer */
      unsigned int outBufSize;              /* size of the output buffer */
      uint8_t *inBuf;                       /* buffer the incoming packet */
      uint8_t *outBuf;                      /* output buffer for sending */
      unsigned int inPos;                   /* receive position in input buffer */
      unsigned int rxPos;                   /* input payload read position */
      unsigned int txPos;                   /* output payload write position */
      unsigned long timeout;                /* when the next read should happen by */
};

/*----------
- Connect message
----------*/
struct msgConnect {
   uint32_t connId;                      /* connection id */
   uint16_t maxPayloadSize;              /* max payload size supported */
   uint16_t connType;                    /* connection type */
};

#endif /* _MSGSERVICE_H_ */
