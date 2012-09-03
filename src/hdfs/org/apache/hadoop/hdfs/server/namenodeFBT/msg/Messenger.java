/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Call;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBT;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.PooledReceiver.ReceiverThread;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.Service;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.ThreadPool;

/**
 * @author hanhlh
 *
 */
public class Messenger implements Service {


public static final String NAME = "/messenger";

    public static final int DEFAULT_SENDER_THREADS = 4;

    /**
     * �ǥե���Ȥμ�������åɿ��Ǥ���
     * �ǥ��������������������礭�����Ǥʤ���Ф����ޤ���
     */
    public static final int DEFAULT_RECEIVER_THREADS = 4;//16;//8;

 // instance attributes ////////////////////////////////////////////////////

    /** ��å����������ѤΥ���ݡ��ͥ�� */
    private Sender _sender;

    /** ��å����������ѤΥ���å� */
    private Thread _senderThread;

    /** ��å����������ѤΥ���ݡ��ͥ�� */
    private Receiver _receiver;

    /** ��å����������ѤΥ���å� */
    private Thread _receiverThread;

    /** ��³�׵������դ��륨��ɥݥ���� */
    private EndPoint _source;

 // constructors ///////////////////////////////////////////////////////////

    /**
     * <p>������ Messenger ��������ޤ���������� initialize �᥽�å�
     * �ǽ���ɬ�פ�����ޤ���</p>
     */
    public Messenger() {
        // NOP
    }

    /**
     * <p>���ꤵ�줿�ݡ����ֹ����Ѥ��뿷���� Messenger ��������ޤ���
     * Ǥ�դΥݡ����ֹ����Ѥ�������� port �� 0 ����ꤷ�Ƥ���������</p>
     *
     * @param port �ݡ����ֹ�
     */
    public Messenger(int port, int maxObjectPerConnection,
    						int senderThreads, int receiverThreads)
    						throws UnknownHostException, ServiceException {

        initialize(InetAddress.getLocalHost(), port,
                senderThreads, receiverThreads, maxObjectPerConnection);
    }

    public void initialize(InetAddress address, int port,
            int senderThreads, int receiverThreads,
            int maxObjectPerConnection) throws ServiceException {
        try {
        	//System.out.println("Messenger initialized");
        	BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(256);

            /* Receiver �ν�� */
            _receiver = new PooledReceiver(port, queue);
            _receiverThread = new Thread(_receiver, "ReceiverMain");
            _receiverThread.setDaemon(true);
            _receiverThread.start();
            //System.out.println("Receiver "+_receiver.toString());
            /* EndPoint �κ��� */
            _source = new EndPoint(address, _receiver.getLocalPort());
            //System.out.println("Messenger source "+_source.getHostName() +":"+
            //						_source.getPort());
            /* Sender �ν�� */
            _sender = new Sender(_source, queue, maxObjectPerConnection);
            _senderThread = new ThreadPool("Sender", _sender, senderThreads);
            _senderThread.setPriority(Thread.NORM_PRIORITY + 1);
            _senderThread.run();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new ServiceException(e);
        }
    }
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#initialize(org.apache.hadoop.conf.Configuration)
	 */
	public void initialize(Configuration conf) throws ServiceException {
		try {
			int _datanodeNumber = conf.getInt("dfs.datanode.fbt.datanodeNumber",
												NameNodeFBT.DEFAULT_TOTAL_DATANODES);
            // name="endPoint[1]" value="edn1"
            for (int endPointIndex = 1; endPointIndex<=_datanodeNumber;
            							endPointIndex++) {
            	String endPointName = conf.get("endPoint["+endPointIndex+"]");
            	//System.out.println("Messenger endPointName " +endPointName);
            	EndPoint endPoint = EndPoint.parse(endPointName);
            	NameNodeFBTProcessor.bind("/messenger/endpoint/"+endPointName,
            										endPoint);
            }

        } catch (Exception e) {
        	e.printStackTrace();
            throw new ServiceException(e);
        }


	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#terminate()
	 */
	public synchronized void terminate() throws ServiceException {
		NameNode.LOG.info("Messenger.terminate()");
        try {
            ((ThreadPool) _senderThread).shutdown();
            //_senderThread.join();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new ServiceException(e);
        }
    }

	// accessors //////////////////////////////////////////////////////////////

    /**
     * <p>���� Messenger ����³�׵������դ��Ƥ��� EndPoint ���֤��ޤ���
     * ����ͤȤ����֤���� EndPoint �ϥͥåȥ����ˤ��� Messenger ��
     * �ƥ��󥹥��󥹤��Ȥ˼��̲�ǽ�ʸ�ͭ���ͤ����ޤ���</p>
     *
     * @return ��³�׵������դ��Ƥ��� EndPoint
     */
    public EndPoint getEndPoint() {
        return _source;
    }

    // instance methods ///////////////////////////////////////////////////////

    public void send(Message message) throws MessageException {
        _sender.send(message);
        //System.out.println("NameNodeFBT.Messenger sent");
    }

    public void setDefaultHandler(MessageHandler defaultHandler) {
    	//System.out.println("Messenger.setDefaultHandler "+_receiver.toString());
        _receiver.setDefaultHandler(defaultHandler);
    }

    public void addHandler(String handlerID, MessageHandler handler) {
        _receiver.addHandler(handlerID, handler);
    }

    public void removeHandler(String handlerID) {
        _receiver.removeHandler(handlerID);
    }

    public Call getReplyCall(Response response) {
    	//System.out.println("Messenger.getReplyCall");
        return _sender.getReplyCall(response);
    }

    public void addReplyCall(Call call) {
        _sender.addReplyCall(call);
    }

    public void removeReplyCall(Call call) {
        _sender.removeReplyCall(call);
    }
}
