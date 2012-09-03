/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.io.ObjectStreamField;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.AbstractMessage;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorInputStream;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorOutputStream;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.Exteriorizable;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class Call extends AbstractMessage implements MessageHandler
					,Exteriorizable
					{

	private static final ObjectStreamField[] serialPersistentFields;

    static {
        serialPersistentFields = new ObjectStreamField[] {
                new ObjectStreamField("_request", Request.class, true),
                new ObjectStreamField("_responseClass", Class.class, true),
                new ObjectStreamField("_redirected", Boolean.TYPE, true)
        };
    }

    // instance attributes ////////////////////////////////////////////////////

    protected transient Messenger _messenger;

    protected Request _request;

    protected transient CallResult _result;

    protected Class _responseClass;

    protected boolean _redirected;

    protected transient Response _response;


    /**
     * ����Call�ؤ��ֿ����Ϥ�����˹Ԥ�����ؤλ��ȤǤ���
     * �Ϥ�������ʤ����ˤϡ�null�����롣
     */
    protected final transient Responser _responser;

// constructors ///////////////////////////////////////////////////////////

    public Call () {
    	super();
    	_messenger = null;
        _request = null;
        _result = null;
        _responseClass = null;
        _responser = null;
        _response = null;
    }
    public Call(Messenger messenger) {
        super();
        _messenger = messenger;
        _request = null;
        _result = null;
        _responseClass = null;
        _responser = null;
        _response = null;
    }

    /**
     * @param messenger
     */
    public Call(Messenger messenger, Responser responser) {
    	super();
    	_messenger = messenger;
    	_request = null;
    	_result = null;
    	_responseClass = null;
        _responser = responser;
        _response = null;
    }

 // accessors //////////////////////////////////////////////////////////////

    /**
     * <p>����Ȥ����֤����٤����֥������ȤΥ��饹��������ޤ���</p>
     *
     * @return ����֥������ȤΥ��饹
     */
    public Class getResponseClass() {
        return _responseClass;
    }

    /**
     * <p>����Ȥ��ƴ��Ԥ��륪�֥������ȤΥ��饹�����ꤷ�ޤ��������Ϳ����
     * ���饹�� Response ���饹�λ�¹�Ǥ���ɬ�פ�����ޤ���</p>
     *
     * @param responseClass ����֥������ȤΥ��饹
     */
    public void setResponseClass(Class responseClass) {
        if (Response.class.isAssignableFrom(responseClass)) {
            _responseClass = responseClass;
        } else {
            throw new IllegalArgumentException(
                    "argument must be a subclass of " + Response.class.getName()
            );
        }
    }

    public Request getRequest() {
        return _request;
    }

    public void setRequest(Request request) {
        _result = null;
        _request = request;
        _redirected = false;
        setMessageID(null);
        _response = null;
    }

    public void setRequestWithoutResetID(Request request) {
        _result = null;
        _request = request;
        _redirected = false;
        _response = null;
    }

    public synchronized Response getResponse() {
    	//StringUtility.debugSpace("Call.getResponse");
        try {
            while (! isFinished()) {
            	//System.out.println("wait()");
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //System.out.println("getResponse "+_result.getResponse());
        return _result.getResponse();
        //return _response;
    }

    public void setResponse(Response response) {
        _response = response;
    }

    public synchronized boolean isFinished() {
    	//StringUtility.debugSpace("Call.isFinished()");
        return _result != null;
//        return _response != null;
    }

    public void setMessenger(Messenger messenger) {
        _messenger = messenger;
    }

    // instance methods ///////////////////////////////////////////////////////

    public Response invoke() {
        try {
			invokeOneWay();
		} catch (MessageException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
        return getResponse();
    }

    public void invoke_backup() throws MessageException {
        invokeOneWay_backup();
       // return getResponse();
    }

    public synchronized void invokeOneWay_backup() throws MessageException {
    	(_messenger).send(this);
    	if(_response != null) {
    	;//	_handler.addMessageID(getMessageID());
    	}
    }


    public synchronized void invokeOneWay() throws MessageException {
    	//System.out.println("Call.invokeOneWay() messenger send");
    	_messenger.send(this);
    	//System.out.println("Call.invokeOneWay() responser "+ _responser.toString());
    	if(_responser != null) {
    		_responser.addMessageID(getMessageID());
    	}
    }

    /**
     * ����Call���Ф���CallResult�Υϥ�ɥ����Ͽ���ޤ�.
     * @see jp.ac.titech.cs.de.autodisk.msg.Message#sendPrepare()
     */
    public void sendPrepare() {
    	//StringUtility.debugSpace("Call.sendPrepare()");
    	if (! _redirected) {
    	    _messenger.addHandler(getMessageID(), this);
    	}
    }

    public void redirect(EndPoint destination) throws MessageException {
        _messenger.removeReplyCall(this);
        _redirected = true;
        setDestination(destination);
//        try {
//            _request = (Request) _request.clone();
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }
        //System.out.println("set new directoryName: /directory."+destination.getHostName());
        getRequest().setDirectoryName("/directory."+destination.getHostName());
        invokeOneWay();
    }

    public void redirect(String url) throws MessageException {

    	//TODO: Set destination
    	EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(url);
    	redirect(destination);
    }

    public EndPoint getResponseFrom() {
    	return _result.getSource();
//        return getSource();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("request= ");
        buf.append(_request);
        buf.append(", result= ");
        buf.append(_result);
        buf.append(", responseClass= ");
        buf.append(_responseClass);
        if (_redirected) {
            buf.append(", redirected");
        }
        buf.append(", response= ");
        buf.append(_response);
        return buf.toString();
    }

    public  synchronized void handle(Message message) {
        _result = (CallResult) message;
        _messenger.removeHandler(getMessageID());
        if (_responser != null){
            _responser.handleResult((CallResult) message);
        }
        notifyAll();
    }

    public void writeExterior(ExteriorOutputStream out) throws IOException {
        super.writeExterior(out);
        out.writeExterior(_request);
        out.writeExterior(_responseClass);
        out.writeBoolean(_redirected);
    }

    public void readExterior(ExteriorInputStream in) throws IOException {
        super.readExterior(in);
        _request = (Request) in.readExterior();
        _responseClass = (Class) in.readExterior();
        _redirected = in.readBoolean();
    }

    public static Exteriorizable newInstance(ExteriorInputStream in) {
        return new Call((Messenger) NameNodeFBTProcessor.lookup(Messenger.NAME));
    }
}
