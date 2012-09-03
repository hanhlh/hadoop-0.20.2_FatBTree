/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.Serializable;

import org.apache.hadoop.io.Writable;

/**
 * @author hanhlh
 *
 */
public interface Message extends Serializable, Cloneable{

	public String getMessageID();

    public void setMessageID(String messageID);

    public String getHandlerID();

    public void setHandlerID(String handlerID);

    public EndPoint getSource();

    public void setSource(EndPoint source);

    public EndPoint getDestination();

    public void setDestination(EndPoint destination);

    /**
     * Messageの送信直前にSenderによって呼び出されます.
     */
    public void sendPrepare();

    public Object clone() throws CloneNotSupportedException;

}
