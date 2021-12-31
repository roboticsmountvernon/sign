/**
 * (C) Copyright 2016, David Vogt, All rights reserved.
 */
package mv.robotics.sign.msg.msg;

import mv.robotics.sign.msg.Msg;
import mv.robotics.sign.msg.util.ByteUtil;

/**
 * Connect message used to coordinate connections.
 * 
 * @author David Vogt (david@kondra.com)
 * @version Jun-13-16
 */
public class ConnectMsg extends Msg
{
   private int connId;                 /* unique id for this connection */
   private int maxPayloadSize;         /* max payload size the board can handle */
   private int connType;               /* connection type */
   
   public ConnectMsg(int connId,int maxPayloadSize,int connType)
   {
      this.connId = connId;
      this.maxPayloadSize = maxPayloadSize;
      this.connType = connType;
   }
   
   public ConnectMsg()
   {
   }
   
   public int getConnId() {
      return connId;
   }
   public void setConnId(int connId) {
      this.connId = connId;
   }
   public int getMaxPayloadSize() {
      return maxPayloadSize;
   }
   public void setMaxPayloadSize(int maxPayloadSize) {
      this.maxPayloadSize = maxPayloadSize;
   }
   public int getConnType() {
      return connType;
   }
   public void setConnType(int connType) {
      this.connType = connType;
   }

   @Override
   public byte[] toBytes()
   {
      byte[] bytes = new byte[8];
      ByteUtil.setInt(bytes,0,connId);
      ByteUtil.setShort(bytes,4,maxPayloadSize);
      ByteUtil.setShort(bytes,6,connType);
      return(bytes);
   }
   
   @Override
   public void fromBytes(byte[] bytes)
   {
      connId = ByteUtil.getInt(bytes,0);
      maxPayloadSize = ByteUtil.getShort(bytes,4);
      connType = ByteUtil.getShort(bytes,6);
   }
}
