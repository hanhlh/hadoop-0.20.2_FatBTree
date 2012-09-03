/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.GroupedThreadFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.ThreadPoolExecutorObserver;


/**
 * @author hanhlh
 *
 */
public class PooledReceiver extends Receiver{
	/** ����åɥס��� */
    protected final ThreadPoolExecutor _executor;

    protected final ThreadPoolExecutorObserver _observer;

    /** ���Υ��󥹥��󥹤��������֤��ݤ� */
    protected AtomicBoolean _active;

    /**
     * <p> �ǥե���ȤΥ�å������ϥ�ɥ�Ȥ��� NullHandler
     * (���⤷�ʤ���å������ϥ�ɥ�ˤ�����
     * ����ǻ��ꤵ�줿 port ���Ե�����Receiver�ס����������ޤ���
     * </p>
     * @param port �Ԥ��ݡ����ֹ�
     * @throws IOException �����Х����åȤκ����Ǽ��Ԥ��������֤�ޤ���
     * @throws SocketException �����Х����åȤκ����Ǽ��Ԥ��������֤�ޤ���
     */
    public PooledReceiver(int port, BlockingQueue<Message> queue)
            throws IOException, SocketException {
        this(port, NullHandler.getInstance(), queue);
    }



    public PooledReceiver(int port, BlockingQueue<Message> queue, int threads)
    throws IOException, SocketException {
    	this(port, NullHandler.getInstance(), queue, threads);
    	//this(port, NullHandler.getInstance(), queue);
    }
    public PooledReceiver(int port, MessageHandler defaultHandler,
            BlockingQueue<Message> queue, int threads) throws IOException,
            SocketException {
        super(port, defaultHandler, queue);
        _executor = new ThreadPoolExecutor(threads, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new GroupedThreadFactory("Receiver"));
        _observer = new ThreadPoolExecutorObserver(_executor);
        _active = new AtomicBoolean(true);
    }
    /**
     * <p> �ǥե���ȤΥ�å������ϥ�ɥ�Ȥ��ư���ǻ��ꤵ�줿
     * �ϥ�ɥ顼������
     * ����ǻ��ꤵ�줿 port ���Ե�����Receiver�ס����������ޤ���
     * </p>
     * @param port �Ԥ��ݡ����ֹ�
     * @param defHandler ��Ͽ����Ƥ��ʤ���å������򰷤�����Υǥե���ȤΥϥ�ɥ�
     * @throws IOException �����Х����åȤκ����Ǽ��Ԥ��������֤�ޤ���
     * @throws SocketException �����Х����åȤκ����Ǽ��Ԥ��������֤�ޤ���
     */
    public PooledReceiver(int port, MessageHandler defaultHandler,
            BlockingQueue<Message> queue) throws IOException, SocketException {
        super(port, defaultHandler, queue);
        _executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new GroupedThreadFactory("Receiver"));
        _observer = new ThreadPoolExecutorObserver(_executor);
        _active = new AtomicBoolean(true);
    }

    // instance method ////////////////////////////////////////////////////////

    /**
     * ���Υ쥷���Хס����λ���ޤ���
     * ̤����Υ��ͥ������ˤĤ��Ƥϡ����ξ�����Ǥ���ޤ���
     */
    public void close() throws IOException{
    	//NameNode.LOG.info("PooledReceiver.close()");
        _active.set(false);
        List restlist = _executor.shutdownNow();
        ListIterator lit = restlist.listIterator();
        while(lit.hasNext()) {
            ReceiverThread rv = (ReceiverThread) lit.next();
            rv.terminate();
        }
    }

    public void finalize() {
        try {
            this.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // interface Runnable /////////////////////////////////////////////////////

    public void run() {
        startForwarder();
        new Thread(_observer, "ReceiverObserver").start();
        while (_active.get()) {
            try {
                _ssock.setSoTimeout(5000);
                Socket sock = _ssock.accept();
                ReceiverThread rt  = new ReceiverThread(sock);
                _executor.execute(rt);
                //rt.run();

            } catch (SocketTimeoutException ste) {
                // NOP
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * �����������åȤ���Message���ɤ߹��ߡ�Ŭ�ڤʥϥ�ɥ���Ϥ��Ƽ¹Ԥ��ޤ���
     * @author daik
     * @since 2005/06/10
     */
    protected class ReceiverThread implements Runnable {

        /** TCP/IP socket */
        private final Socket _sock;

        /**
         * <p> ����ǻ��ꤵ�줿�����åȤ����å�����s �������ꡤ
         * �б�����ϥ�ɥ��ư���ƽ���򤹤륪�֥������Ȥ�������ޤ���</p>
         * @param sock ��Ω�������ͥ������
         */
        public ReceiverThread(Socket sock) {
            _sock = sock;
        }

        /**
         * ���Υ��ͥ��������˴����ޤ�.
         * @throws IOException
         */
        public void terminate() throws IOException {
            System.out.println("socket " + _sock.toString() + " closed");
            _sock.close();
        }

        /**
         * �����åȤ����å�����s �������ꡤ
         * �б�����ϥ�ɥ��ư���ƽ���򤷤ޤ���
         *
         * �ϥ�ɥ���ν���ϡ��ϥ�ɥ��������Ū�˥���åɤ�������ʤ��¤ꡤ
         * ���Υ���åɤ�Ʊ������åɤǹԤ��ޤ���
         * @throws IOException
         */
        private void receive() throws IOException {
        	//StringUtility.debugSpace("PooledReceiver.receive()");
            _sock.setTcpNoDelay(true);
            _sock.setKeepAlive(true);
            ObjectInputStream ois =
                new ObjectInputStream(
                    new BufferedInputStream(_sock.getInputStream()));

            Message message;
            FBTDirectory fbtDirectory;
            MessageHandler handler;
            while (true) {
            	//System.out.println("true");
                try {
                    message = (Message) ois.readObject();
                	//message = (Message) ois.readUnshared();
                    //System.out.println("message "+message);
                    String handleID = (message.getHandlerID() == null) ? "default" :
                    								message.getHandlerID();
                    //System.out.println("handlerID "+handleID);
                    handler = getHandler(handleID);
                    //handler = getHandler(message.getHandlerID());
                    //System.out.println("Pooled Receiver handle "+handler.toString());

                    if (NameNode.LOG.isDebugEnabled()) {
                        NameNode.LOG.debug("received." + message);
                        NameNode.LOG.debug("handler = " + handler);
                    }

                    handler.handle(message);
                } catch (EOFException e) {
                    ois.close();
                    break;
                } catch (ClassNotFoundException e) {
                    handleException(e);
                }
            }
        }

        /**
         * <p> ���Υ��饹��Ǥ��㳰������å����줿�Ȥ��˸ƤФ�ޤ���</p>
         * TODO: �ºݤˤ��̿��μ��ԤʤΤǡ��ξ����ƤФ�롩
         *        �������ξ�����Ƥ���ʬ��SEND¦�˼������٤��ʤΤǡ�
         *        �����Ǥϡ�����¾���㳰�װ���Ѥ��鸫�Ф��ƽ���٤�
         * @param e
         */
        private void handleException(Exception e){
            // do nothing
            e.printStackTrace();
        }

        // interface Runnable /////////////////////////////////////////////////

        public void run() {
        	//StringUtility.debugSpace("PooledReceiver.run()");
            try{
                receive();
            } catch (IOException ioe) {
                handleException(ioe);
            }
        }

        public Socket getSocket(){
            return _sock;
        }
    } // end-of class ReceiverThread

}
