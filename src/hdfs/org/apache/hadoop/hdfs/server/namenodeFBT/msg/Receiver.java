/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandlerMap;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.GroupedThreadFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.ThreadPoolExecutorObserver;


/**
 * @author hanhlh
 *
 */
public class Receiver implements Runnable {


	// instance attributes ////////////////////////////////////////////////////

    /** ��³�׵������դ��륵���С������å� */
    protected final ServerSocket _ssock;

    /** ��å�����ID���餽�Υ�å����������뤿��Ρ�
     * �б������å������ϥ�ɥ�����뤿���Map */
    private final MessageHandlerMap _handlers;

    private final BlockingQueue<Message> _queue;

    private final Forwarder _forwarder;

    // constructors ///////////////////////////////////////////////////////////

    public Receiver(int port, BlockingQueue<Message> queue)
            throws IOException, SocketException {
        this(port, NullHandler.getInstance(), queue);
    }

    public Receiver(int port, MessageHandler defaultHandler,
            BlockingQueue<Message> queue) throws IOException, SocketException {
    	try {
    		/*System.out.println("Receiver used port is "+port);
    		System.out.println("Receiver queue "+queue.toString());*/
    		_ssock = new ServerSocket(port);
            _handlers = new MessageHandlerMap(defaultHandler);
            _queue = queue;
            _forwarder = new Forwarder();

    	} catch (BindException be) {
    	    if (NameNode.LOG.isDebugEnabled()) {
    	        NameNode.LOG.info("Receiver used port is " + port);
    	    }
    	    throw be;
    	}
    }

 // accessors //////////////////////////////////////////////////////////////

    protected int getLocalPort() {
        return _ssock.getLocalPort();
    }

    // instance methods ///////////////////////////////////////////////////////

    protected MessageHandler getHandler(String handlerID) {
    	//StringUtility.debugSpace("Receiver.getHandler() for "+handlerID);
    	//synchronized (_handlers) {
    		return _handlers.getHandler(handlerID);
    	//}
    }

    protected void setDefaultHandler(MessageHandler defaultHandler) {
        _handlers.setDefaultHandler(defaultHandler);
    }

    protected void addHandler(String handlerID, MessageHandler handler) {
    	StringUtility.debugSpace("Receiver.addHandler() for "+handlerID);
    	//synchronized (_handlers) {
    		_handlers.addHandler(handlerID, handler);
    	//}
    }

    protected void removeHandler(String handlerID) {
    	StringUtility.debugSpace("Receiver.removeHandler() for "+handlerID);
    	//synchronized (_handlers) {
    		_handlers.removeHandler(handlerID);
    	//}
    }

    protected void terminate() throws IOException {
        _ssock.close();
    }

    private void receive(Socket sock) throws IOException {
    	//StringUtility.debugSpace("receiver running line 98");
        sock.setTcpNoDelay(true);
        sock.setKeepAlive(true);
        ObjectInputStream ois =
            new ObjectInputStream(
                new BufferedInputStream(sock.getInputStream()));

        Message message;
        MessageHandler handler;
        while (true) {
            try {
                message = (Message) ois.readObject();
            	//message = (Message) ois.readUnshared();
                //System.out.println("line 112 message"+ message.toString());
                handler = getHandler(message.getHandlerID());
               //System.out.println("receiver running line 110");
                if (NameNode.LOG.isDebugEnabled()) {
                	NameNode.LOG.debug("received." + message);
                	NameNode.LOG.debug("handler = " + handler);
                }
                handler.handle(message);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (EOFException e) {
            	e.printStackTrace();
                ois.close();
                break;
            }
        }
    }

    protected void startForwarder() {
        new Thread(_forwarder).start();
    }


	/* (�� Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		//System.out.println("Receiver running. line 135");
		startForwarder();

        while (true) {
            try {
            	//System.out.println("Receiver running. line 140");
                receive(_ssock.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

	}
private class Forwarder implements Runnable {

        private final ThreadPoolExecutor _executor;

        private final ThreadPoolExecutorObserver _observer;

        public Forwarder() {
            _executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    new GroupedThreadFactory("Receiver"));
            _observer = new ThreadPoolExecutorObserver(_executor);
        }

        public void run() {
            new Thread(_observer, "ForwarderObserver").start();
            Message message;
            while (true) {
                try {
                    message = _queue.take();
                    //System.out.println("Forwarder.run() message "+message.toString());
                    _executor.execute(new ForwarderThread(message));
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        private class ForwarderThread implements Runnable {

            private final Message _message;

            public ForwarderThread(Message message) {
                _message = message;
            }

            public void run() {
            	//System.out.println("ForwarderThread.run()");
            	//System.out.println("ForwarderThread.run() message "+_message.toString());
                MessageHandler handler = getHandler(_message.getHandlerID());
                //System.out.println("handler "+ handler.toString());
                handler.handle(_message);
            }
        }
    }


}
