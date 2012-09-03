/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.Service;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;

/**
 * @author hanhlh
 *
 */
public class FBTServer implements Runnable, Service {

	private RequestFactory requestFactory;
	private ResponseClassFactory responseClassFactory;

	private Messenger messenger;
	private EndPoint localHost;

	private static int port ;
	private static int buffer_capacity;
	private Selector selector;


	/*
	 * Setter
	 */

	private void setRequestFactory(String name) throws Exception {
		Class requestFactoryClass = Class.forName(name);
		requestFactory = (RequestFactory) requestFactoryClass.newInstance();
	}

	private void setResponseClassFactory(String name) throws Exception {
		Class responseClassFactoryClass = Class.forName(name);
		responseClassFactory = (ResponseClassFactory) responseClassFactoryClass.newInstance();
	}


	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#initialize(org.apache.hadoop.conf.Configuration)
	 */
	public void initialize(Configuration conf) throws ServiceException {
		// TODO 自動生成されたメソッド・スタブ
		RuleManager ruleManager;

		try {
			setRequestFactory(
					"org.apache.hadoop.hdfs.server.namenodeFBT.rule.ThreadedRuleManager");
			setResponseClassFactory(
					"org.apache.hadoop.hdfs.server.namenodeFBT.response.DefaultResponseClassFactory");
		} catch (Exception e) {
			throw new ServiceException(e);
		}

		ruleManager = (RuleManager) NameNodeFBTProcessor.lookup("/manager");
		messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
		messenger.setDefaultHandler(new CallHandler(messenger, ruleManager));

		localHost = messenger.getEndPoint();

		port = 8023;

		if (NameNode.LOG.isDebugEnabled()) {
			StringBuffer s = new StringBuffer();

			s.append("RequestFactory: " + requestFactory + "\n");
			s.append("ResponseClassFactory: " + responseClassFactory + "\n");
			s.append("Messenger: " + messenger + "\n");
			s.append("Port: " + port + "\n");

		}
		try {
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

			serverSocketChannel.configureBlocking(false);

			InetSocketAddress address = new InetSocketAddress(port);
			serverSocketChannel.socket().bind(address);

			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

			NameNode.LOG.info("FBTServer started");
		} catch (Exception e) {
			throw new ServiceException(e);
		}

		Thread serverThread = new Thread(this, "FatBtreeServer");
		serverThread.start();

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#terminate()
	 */
	public void terminate() throws ServiceException {
		// TODO 自動生成されたメソッド・スタブ
		try {
			selector.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (非 Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
		NameNode.LOG.info("FBTServer running.....");
	}

}
