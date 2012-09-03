/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;

/**
 * @author hanhlh
 *
 */
public abstract class Request extends RuleEvent{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * <p>���٥��ȯ�����λ��ȤȤ��� EventObject �Υ��󥹥ȥ饯����Ϳ����
     * ���ߡ��ΰ���Ǥ����ºݤ� source �� setSource �᥽�åɤˤ�ä�������
     * �˽��뤳�Ȥ��Ǥ��ޤ���</p>
     */
    private static final String _dummy = "";

// instance attributes ////////////////////////////////////////////////////

    /**
     * �׵���Ф������Ԥ���Ǿ�̤� node ��ؤ��ݥ���.
     * null �ΤȤ��� MetaNode ��������Ԥ���.
     */
    private VPointer _target;
    private int thread_number;
    private int thread_restart;
    private EndPoint dest;
    public  String _messageID;
    private String _directoryName;
    /**
     * �ꥯ�����Ȥ�ź�դ����å�������
     */
    private byte[] _message;

 // constructors //////////////////////////////////////////////////////

    /**
     * <p>�������׵ᥪ�֥������Ȥ��������ޤ���</p>
     *
     * @param source �׵᤬�ǽ��ȯ���������֥�������
     */
    public Request(Object source) {
        super(source);
        _target = null;
    }

    /**
     * <p>�������׵ᥪ�֥������Ȥ��������ޤ�.
     * target �� null �˻��ꤵ��ޤ�.</p>
     */
    public Request() {
        this(_dummy);
    }

    /**
     * �������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
     */
    public Request(VPointer target) {
        this();
        _target = target;
    }

    // accessors //////////////////////////////////////////////////////////////

    public VPointer getTarget() {
        return _target;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

    public int getThreadNo() {
        return thread_number;
    }

    public void setThreadNo(int thread) {
    	thread_number = thread;
    }

    public int getThreadRestart() {
        return thread_restart;
    }

    public void setThreadRestart(int restart) {
    	thread_restart = restart;
    }



    public String getFrom_To() {
        return _messageID;
    }

    public void setFrom_To(String ID) {
    /*	try {
		    while (_messageID!=null) {
		   	// System.err.println("!!!!!!!!!!!!!!!!!!!In CallHandler.java222  handle call wait and call is : "+ call.toString());
		        wait();
		    }
		} catch (InterruptedException e) {
		    e.printStackTrace();
		} */
    	_messageID = ID;
    }



    public EndPoint getDestnation() {
        return dest;
    }

    public void setDestnation(EndPoint ep) {
    	dest = ep;
    }



    public byte[] getMessage() {
    	return _message;
    }

    public void setMessage(byte[] message) {
    	_message = message;
    }

    // instance methods //////////////////////////////////////////////////

    /**
     * <p>�׵ᥪ�֥������ȤΥ���������ޤ���</p>
     *
     * @param source �׵᤬�ǽ��ȯ���������֥�������
     */
    public void setSource(Object source) {
	this.source = source;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append("target= ");
        buf.append(_target);
        buf.append(", transactionID= ");
        buf.append(super.getTransactionID());
        buf.append("directoryName= ");
        buf.append(_directoryName);

        return buf.toString();
    }

    public Class getEventClass() {
        return getClass();
    }

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }

    public String getDirectoryName() {
    	return _directoryName;
    }

}
