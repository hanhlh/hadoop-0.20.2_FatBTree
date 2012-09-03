/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.IOException;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorInputStream;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorOutputStream;



/**
 * @author hanhlh
 *
 */
public abstract class AbstractMessage implements Message {

	 /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// instance attributes ////////////////////////////////////////////////////

    /** 送信元 Messenger のエンドポイント */
    protected EndPoint _source;

    /** 送信先 Messenger のエンドポイント */
    protected EndPoint _destination;

    /** メッセージ識別子 */
    protected String _messageID;

    /** メッセージハンドラ識別子 */
    protected String _handlerID;

 // constructors ///////////////////////////////////////////////////////////

    public AbstractMessage() {
        _source = null;
        _destination = null;
        _messageID = null;
    }

 // instance methods ///////////////////////////////////////////////////////

    public String toString() {
        return new StringBuffer()
        .append("From: ")
        .append(_source)
        .append("\nTo: ")
        .append(_destination)
        .append("\nMessage-ID: ")
        .append(_messageID)
        .append("\nMessage-Handler: ")
        .append((_handlerID == null) ? "default" : _handlerID)
        .append("\n")
        .toString();
    }


	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#getMessageID()
	 */
	public String getMessageID() {
		return _messageID;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#setMessageID(java.lang.String)
	 */
	public void setMessageID(String messageID) {
		_messageID = messageID;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#getHandlerID()
	 */
	public String getHandlerID() {
		return _handlerID;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#setHandlerID(java.lang.String)
	 */
	public void setHandlerID(String handlerID) {
		_handlerID = handlerID;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#getSource()
	 */
	public EndPoint getSource() {
		return _source;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#setSource(org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint)
	 */
	public void setSource(EndPoint source) {
		_source = source;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#getDestination()
	 */
	public EndPoint getDestination() {
		return _destination;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#setDestination(org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint)
	 */
	public void setDestination(EndPoint destination) {
		_destination = destination;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message#sendPrepare()
	 */
	public void sendPrepare() {
		// TODO 自動生成されたメソッド・スタブ

	}
	public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

	public void writeExterior(ExteriorOutputStream out) throws IOException {
        EndPoint.write(_source, out);
        EndPoint.write(_destination, out);
        out.writeAscii(_messageID);
        out.writeAscii(_handlerID);
    }

    public void readExterior(ExteriorInputStream in) throws IOException {
        _source = EndPoint.read(in);
        _destination = EndPoint.read(in);
        _messageID = in.readAscii();
        _handlerID = in.readAscii();
    }


}
