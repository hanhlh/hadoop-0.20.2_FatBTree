/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongs;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.UnregisteredDatanodeException;
import org.apache.hadoop.hdfs.server.common.GenerationStamp;
import org.apache.hadoop.hdfs.server.common.UpgradeStatusReport;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.StartupOption;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.FSImage;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.Forwards;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.WriteOffLoading;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.apache.hadoop.hdfs.server.namenode.metrics.NameNodeMetrics;
import org.apache.hadoop.hdfs.server.namenode.writeOffLoading.WriteOffLoadingCommand;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Locker;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.WOLCReceiver;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.WOLReceiver;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.ThreadedRuleManager;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.protocol.DatanodeCommand;
import org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.FBTDatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.FBTProtocol;
import org.apache.hadoop.hdfs.server.protocol.NNClusterProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.hdfs.server.protocol.WOLProtocol;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor.BlocksTargetPair;

import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;


/**
 * @author hanhlh
 *
 */
public class NameNodeFBT extends NameNode implements ClientProtocol, FBTProtocol,
							FBTDatanodeProtocol, FSConstants, DatanodeProtocol,
							WOLProtocol
							{

	static{
	    Configuration.addDefaultResource("hdfs-default.xml");
	    Configuration.addDefaultResource("hdfs-site.xml");
	    Configuration.addDefaultResource("hdfs-site-fbt.xml");
	    Configuration.addDefaultResource("gear.xml");
	    Configuration.addDefaultResource("WOLLocation.xml");
	}

	public static final int DEFAULT_PORT = 8020;
	public static final int FBT_DEFAULT_PORT = 8021;
	public static final int FBT_DATANODE_DEFAULT_PORT = 8022;
	public static final int FBT_MESSAGE_PORT = 8023;
	public static final int FBT_WOLCOMMAND_PORT = 8024;
	/** 1�Ĥ���³�����륪�֥������Ȥκ���� */
	protected static final int MAX_OBJECTS_PER_CONNECTION = 4;
	public static final int FBT_SENDER_THREADS = 8;
	public static final int DEFAULT_TOTAL_DATANODES = 2;

	private int forwardsBlockReceiveCount=0;
	//public final Selector selector;
	public static final String AUDIT_FORMAT =
	    "ugi=%s\t" +  // ugi
	    "ip=%s\t" +   // remote IP
	    "cmd=%s\t" +  // command
	    "src=%s\t" +  // src path
	    "dst=%s\t" +  // dst path (optional)
	    "perm=%s";    // permissions (optional)

	private static final ThreadLocal<Formatter> auditFormatter =
	    new ThreadLocal<Formatter>() {
	      protected Formatter initialValue() {
	        return new Formatter(new StringBuilder(AUDIT_FORMAT.length() * 4));
	      }
	  };

	  private static final void logAuditEvent(UserGroupInformation ugi,
	      InetAddress addr, String cmd, String src, String dst,
	      FileStatus stat) {
	    final Formatter fmt = auditFormatter.get();
	    ((StringBuilder)fmt.out()).setLength(0);
	    auditLog.info(fmt.format(AUDIT_FORMAT, ugi, addr, cmd, src, dst,
	                  (stat == null)
	                    ? null
	                    : stat.getOwner() + ':' + stat.getGroup() + ':' +
	                      stat.getPermission()
	          ).toString());

	  }

	  public static final Log auditLog = LogFactory.getLog(
		      NameNodeFBT.class.getName() + ".audit");


	public static Log LOG = LogFactory.getLog(NameNode.class.getName());
	public static final Log stateChangeLog =  LogFactory.getLog(
												"org.apache.hadoop.hdfs.StateChange");
	/**
	 * RPC Servers
	 * */
	private Server clientProtocolServer;
	private Server FBTProtocolServer;
	private Server FBTDatanodeProtocolServer;
	private Server FBTServer;

	private FBTDirectory _directory;

	private Map<String, FBTDirectory> _endPointFBTMapping;

	String machineName;

	public static String[] fsNameNodeFBTs = null;

	private WOLReceiver wolReceiver;

	private WOLCReceiver wolcReceiver;

		/**
	 * Start NameNodeFBT
	 * Start 3 RPC servers.
	 * @throws MessageException
	 * @throws IOException
	 * @throws Exception
	 *
	 * */

	public NameNodeFBT(Configuration conf) throws IOException,
									MessageException {
		super(conf);
		try {
			initialize(conf);
		} catch (Exception e) {
			// TODO ��ư�������줿 catch �֥�å�
			NameNode.LOG.info("NameNodeFBT Exception");
			e.printStackTrace();
		}
	}
	public void initialize(Configuration conf) throws Exception {

		try {
			if (conf.get("slave.host.name")!=null) {
				machineName = conf.get("slave.host.name");
			}
			if (machineName == null) {
				machineName = DNS.getDefaultHost(
									conf.get("dfs.datanode.dns.interface","default"),
									conf.get("dfs.datanode.dns.nameserver", "default"));
			}
			initClientProtocolServer(conf);
			this.serverAddress = clientProtocolServer.getListenerAddress();

			this.fsNameNodeFBTs = conf.getStrings("fs.namenodeFBTs");

			Locker locker = new Locker();
			locker.initialize(conf);
			NameNodeFBTProcessor.bind("/locker", locker);

			int max_objects_per_connection =  conf.getInt("dfs.namenodeFBT.maxObjectPerConnection",
					MAX_OBJECTS_PER_CONNECTION);
			int senderThreads = conf.getInt("dfs.namenodeFBT.senderThreads", Messenger.DEFAULT_RECEIVER_THREADS);
			int receiverThreads = conf.getInt("dfs.namenodeFBT.receiverThreads", Messenger.DEFAULT_RECEIVER_THREADS);
			Messenger messenger = new Messenger(FBT_MESSAGE_PORT, max_objects_per_connection,
										senderThreads, receiverThreads);
			ThreadedRuleManager trm = new ThreadedRuleManager();
			trm.initialize(conf);
			NameNodeFBTProcessor.bind("/manager", trm);
			setRuleSet(conf);

			RuleManager ruleManager = (RuleManager)NameNodeFBTProcessor.
										lookup("/manager");
			messenger.setDefaultHandler(new CallHandler(messenger, ruleManager));
			messenger.initialize(conf);
			NameNodeFBTProcessor.bind("/messenger", messenger);

			//format
			/*Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
		    Collection<File> editDirsToFormat =
		                 FSNamesystem.getNamespaceEditsDirs(conf);
			 */

			NameNode.myMetrics = new NameNodeMetrics(conf, this);
			_endPointFBTMapping = new TreeMap<String, FBTDirectory>();
			ArrayList<String> containedFBTLists = new ArrayList<String>();
			containedFBTLists.add(serverAddress.getHostName());
			System.out.println("containedFBTDirectories: "+
					Arrays.toString(containedFBTLists.toArray()));
			_directory = new FBTDirectory(this, conf,
					serverAddress.getHostName());
			_endPointFBTMapping.put(serverAddress.getHostName(), _directory);
			NameNodeFBTProcessor.bind("/directory.".concat(
									serverAddress.getHostName()), _directory);
			String[] backUpDirectories = conf.getStrings("fbt.backupNode");
			System.out.println("backupDirectories "+Arrays.toString(backUpDirectories));
			for (String backUpDir:backUpDirectories) {
				System.out.println("backUpDir "+backUpDir);
				if (!backUpDir.equals("null")) {
					FBTDirectory fbt = new FBTDirectory(this, conf, backUpDir);
					_endPointFBTMapping.put(backUpDir, fbt);
					NameNodeFBTProcessor.bind("/directory.".concat(backUpDir), fbt);
					containedFBTLists.add(backUpDir);
				}
			}
			System.out.println("containedFBTDirectories: "+Arrays.toString(containedFBTLists.toArray()));

			StringUtility.debugSpace("resetPointerAtRoot");
			for (String containedFBT : containedFBTLists) {
				System.out.println(containedFBT);
				_endPointFBTMapping.get(containedFBT).resetPointerAtRoot(conf);
			}
			// appended //

			this.namenodeID = getNamenodeID(conf);
			_directory.setNamenodeID(namenodeID);
			System.out.println("register namenode");
			// appended
			nnRPCAddrs = new TreeMap<Integer, InetSocketAddress>();
			InetSocketAddress leadAddr = getLeaderAddress(conf);
			if (leadAddr.equals(serverAddress))
				nnRPCAddrs.put(new Integer(namenodeID), leadAddr);
			else {
				nnRPCAddrs.put(new Integer(namenodeID), serverAddress);
				nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
						NNClusterProtocol.class, NNClusterProtocol.versionID,
						leadAddr, conf);
				String host = serverAddress.getHostName();
				int port = serverAddress.getPort();
				int id = namenodeID;
				nnNamenode.namenodeRegistration(host, port, id);
			}
			// end of appended

			initWOLReceiver();
			//initWOLCReceiver();

			NameNode.LOG.info("[namenodeRegistration] NameNode " + namenodeID);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void initWOLReceiver() throws IOException {
		wolReceiver = new WOLReceiver(FBT_DEFAULT_PORT, this);
		//wolReceiver.run();
	}


	public void initClientProtocolServer(Configuration conf) {
		int handlerCount = conf.getInt("dfs.namenode.handler.count", 100);
		InetSocketAddress clientProtocolServerAddress =
							NetUtils.createSocketAddr(machineName, DEFAULT_PORT);
		try {
			clientProtocolServer = RPC.getServer(this,
											clientProtocolServerAddress.getHostName(),
											clientProtocolServerAddress.getPort(),
											handlerCount,
											//100,
											false,
											conf);
			clientProtocolServer.start();
		} catch (IOException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}

		LOG.info("NameNodeFBT clientProtocolServer up at "+
								clientProtocolServerAddress);
	}

	public void initFBTProtocolServer(Configuration conf) throws IOException {
		InetSocketAddress FBTProtocolServerAddress =
							NetUtils.createSocketAddr(machineName, FBT_DEFAULT_PORT);
		this.FBTProtocolServer = RPC.getServer(this,
										FBTProtocolServerAddress.getHostName(),
										FBTProtocolServerAddress.getPort(),
										10,
										false,
										conf);
		this.FBTProtocolServer.start();
		LOG.info("NameNodeFBT FBTProtocolServer up at "+
								FBTProtocolServerAddress);

	}

	public void initFBTDatanodeProtocolServer(Configuration conf) throws IOException {
		InetSocketAddress FBTDatanodeProtocolServerAddress =
							NetUtils.createSocketAddr(
													machineName,
													FBT_DATANODE_DEFAULT_PORT);
		this.FBTDatanodeProtocolServer = RPC.getServer(this,
										FBTDatanodeProtocolServerAddress.getHostName(),
										FBTDatanodeProtocolServerAddress.getPort(),
										conf);
		this.FBTDatanodeProtocolServer.start();
		LOG.info("NameNodeFBT FBTDatanodeProtocolServer up at "+
								FBTDatanodeProtocolServerAddress);
	}

	public void initFBTServer(Configuration conf) throws IOException {
		InetSocketAddress FBTServerAddress =
							NetUtils.createSocketAddr(
													machineName,
													FBT_MESSAGE_PORT);
		this.FBTServer = RPC.getServer(this,
										FBTServerAddress.getHostName(),
										FBTServerAddress.getPort(),
										conf);
		this.FBTServer.start();
		LOG.info("NameNodeFBT FBTServer up at "+
								FBTServerAddress);
	}
	/**
	 * Wait for service to finish.
	 * (Normally, it runs forever.)
	 */
	public void join() {
		NameNode.LOG.info("NameNodeFBT.join()");
		try {
		    this.clientProtocolServer.join();
		    //this.FBTDatanodeProtocolServer.join();
		    //this.FBTProtocolServer.join();
		    //this.FBTServer.join();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			System.out.println("NameNodeFBT.join() interrupted exception");
		}
	}

	public void stop() {
		//TODO
		NameNode.LOG.info("NameNodeFBT.stop()");
		if (clientProtocolServer!=null) {
			clientProtocolServer.stop();
		}
		if (FBTProtocolServer!=null) {
			FBTProtocolServer.stop();
		}
		if (FBTDatanodeProtocolServer!=null) {
			FBTDatanodeProtocolServer.stop();
		}
		if (FBTServer!=null) {
			FBTServer.stop();
		}
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.ipc.VersionedProtocol#getProtocolVersion(java.lang.String, long)
	 *//*
	public long getProtocolVersion(String protocol, long clientVersion)
			throws IOException {
		if (protocol.equals(ClientProtocol.class.getName())) {
			  return ClientProtocol.versionID;
		  } else if (protocol.equals(RefreshAuthorizationPolicyProtocol.class.getName())){
			  return RefreshAuthorizationPolicyProtocol.versionID;
		  } else if (protocol.equals(FBTProtocol.class.getName())){
			  return FBTProtocol.versionID;
		  } else if (protocol.equals(DatanodeProtocol.class.getName())){
			  return DatanodeProtocol.versionID;
		  }
		  else if (protocol.equals(NNClusterProtocol.class.getName())){
			  return NNClusterProtocol.versionID;
		  }else {
		      throw new IOException("Unknown protocol to name node: " + protocol);
		    }
	}*/

	public void setRuleSet(Configuration conf) throws
				ClassNotFoundException, SecurityException,
				NoSuchMethodException,
				IllegalArgumentException,
				InstantiationException,
				IllegalAccessException,
				InvocationTargetException {
    	RuleManager ruleManager = (RuleManager)
    							NameNodeFBTProcessor.lookup("/manager");

    	String[] ruleClassName = conf.getStrings("ruleSet");
    	//System.out.println("ruleClassName length "+ ruleClassName.length);
    	for (int i=0; i<ruleClassName.length; i++) {
    		//System.out.println("ruleClassName " + ruleClassName[i]);
    		Class<?> ruleClass = Class.forName(ruleClassName[i]);
    		Constructor<?> constructor = ruleClass.getConstructor(
    						new Class[] { RuleManager.class }
    		);
        Rule rule =
            (Rule) constructor.newInstance(new Object[] { ruleManager });
        rule.initialize(conf);
    	}
    }


	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getBlockLocations(java.lang.String, long, long)
	 */
	public LocatedBlocks getBlockLocations(String src, long offset, long length)
			//throws IOException, MessageException {
			{
		LocatedBlocks lbs = null;
		myMetrics.numGetBlockLocations.inc();
	    try {
			lbs = _directory.getBlockLocations(getClientMachine(),
			                                    src, offset, length);
		} catch (IOException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		} catch (MessageException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
		return lbs;
		}

		/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#create(java.lang.String, org.apache.hadoop.fs.permission.FsPermission, java.lang.String, boolean, short, long)
	 */
	public boolean create(String src, FsPermission masked, String clientName,
			boolean overwrite, short replication, long blockSize)
			throws IOException, MessageException, ServiceException {
		NameNode.LOG.info("NameNodeFBT.create "+src);
		boolean success = false;

		String clientMachine = getClientMachine();
	    if (stateChangeLog.isDebugEnabled()) {
	      stateChangeLog.debug("*DIR* NameNode.create: file "
	                         +src+" for "+clientName+" at "+clientMachine);
	    }
	    if (!checkPathLength(src)) {
	      throw new IOException("create: Pathname too long.  Limit "
	                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
	    }
	    _directory.startFile(src,
	        new PermissionStatus(UserGroupInformation.getCurrentUGI().getUserName(),
	            null, masked),
	        clientName, clientMachine, overwrite, replication, blockSize);
	    //TODO myMetrics should be done at FBTDirectory level
	    myMetrics.numFilesCreated.inc();
	    myMetrics.numCreateFileOps.inc();
	    NameNode.LOG.info("NameNodeFBT.create "+src +" ended");
	    success = true;
	    return success;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#append(java.lang.String, java.lang.String)
	 */
	public LocatedBlock append(String src, String clientName)
			throws IOException, MessageException, ServiceException {
		// TODO ��ư�������줿�᥽�åɡ�������
		String clientMachine = getClientMachine();
	    if (stateChangeLog.isDebugEnabled()) {
	      stateChangeLog.debug("*DIR* NameNode.append: file "
	          +src+" for "+clientName+" at "+clientMachine);
	    }
	    LocatedBlock info = _directory.appendFile(src, clientName, clientMachine);
	    //myMetrics.numFilesAppended.inc();
	    return info;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setReplication(java.lang.String, short)
	 */
	public boolean setReplication(String src, short replication)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setPermission(java.lang.String, org.apache.hadoop.fs.permission.FsPermission)
	 */
	public void setPermission(String src, FsPermission permission)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setOwner(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void setOwner(String src, String username, String groupname)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#abandonBlock(org.apache.hadoop.hdfs.protocol.Block, java.lang.String, java.lang.String)
	 */
	public void abandonBlock(Block b, String src, String holder)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#addBlock(java.lang.String, java.lang.String)
	 */
	public LocatedBlock addBlock(String src, String clientName)
			throws IOException, MessageException {
		StringUtility.debugSpace("*BLOCK* NameNodeFBT.addBlock: file "
                +src+" for "+clientName);
		LocatedBlock locatedBlock = _directory.getAdditionalBlock(src, clientName);
		//System.out.println("LocatedBlock "+locatedBlock.toString());
		if (locatedBlock != null){
			myMetrics.numAddBlockOps.inc();
		}
		return locatedBlock;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#complete(java.lang.String, java.lang.String)
	 */
	public boolean complete(String src, String clientName) throws IOException,
												MessageException{
		CompleteFileStatus returnCode = _directory.completeFile(src, clientName);
	    if (returnCode == CompleteFileStatus.STILL_WAITING) {
	      return false;
	    } else if (returnCode == CompleteFileStatus.COMPLETE_SUCCESS) {

	      return true;
	    } else {
	      throw new IOException("Could not complete write to file " + src + " by " + clientName);
	    }
	}
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#reportBadBlocks(org.apache.hadoop.hdfs.protocol.LocatedBlock[])
	 */
	public void reportBadBlocks(LocatedBlock[] blocks) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#rename(java.lang.String, java.lang.String)
	 */
	public boolean rename(String src, String dst) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#delete(java.lang.String)
	 */
	public boolean delete(String src) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#delete(java.lang.String, boolean)
	 */
	public boolean delete(String src, boolean recursive) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#mkdirs(java.lang.String, org.apache.hadoop.fs.permission.FsPermission)
	 */
	public boolean mkdirs(String src, FsPermission masked, boolean isDirectory) throws
						IOException, MessageException {
		// TODO ��ư�������줿�᥽�åɡ�������
		StringUtility.debugSpace("NameNodeFBT.mkdirs "+src);
		PermissionStatus permissions = new PermissionStatus(
										UserGroupInformation.getCurrentUGI().getUserName(),
										null,
										masked);
		try {
			return _directory.mkdirs(src, permissions, false, now(), isDirectory);
		} catch (ServiceException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
			return false;
		}

	}
	public boolean synchronizeRootNodes() throws ServiceException, MessageException, IOException {
		StringUtility.debugSpace("NameNodeFBT.synchronizedRootNode");
		if (_directory.synchronizeRootNodes()) {
			return true;
		} return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getListing(java.lang.String)
	 */
	public FileStatus[] getListing(String src) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#renewLease(java.lang.String)
	 */
	public void renewLease(String clientName) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getStats()
	 */
	public long[] getStats() throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getDatanodeReport(org.apache.hadoop.hdfs.protocol.FSConstants.DatanodeReportType)
	 */
	public DatanodeInfo[] getDatanodeReport(DatanodeReportType type)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getPreferredBlockSize(java.lang.String)
	 */
	public long getPreferredBlockSize(String filename) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return 0;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setSafeMode(org.apache.hadoop.hdfs.protocol.FSConstants.SafeModeAction)
	 */
	public boolean setSafeMode(SafeModeAction action) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#saveNamespace()
	 */
	public void saveNamespace() throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#refreshNodes()
	 */
	public void refreshNodes() throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#finalizeUpgrade()
	 */
	public void finalizeUpgrade() throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#distributedUpgradeProgress(org.apache.hadoop.hdfs.protocol.FSConstants.UpgradeAction)
	 */
	public UpgradeStatusReport distributedUpgradeProgress(UpgradeAction action)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#metaSave(java.lang.String)
	 */
	public void metaSave(String filename) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getFileInfo(java.lang.String)
	 */
	public FileStatus getFileInfo(String src) throws IOException, MessageException {
		StringUtility.debugSpace("NameNodeFBT.getFileInfo");

		return _directory.getFileInfo(src);
	}
	public FileStatus getFileInfo(String src, boolean isDirectory)
									throws IOException, MessageException {
		StringUtility.debugSpace("NameNodeFBT.getFileInfo with isDirectory");

		return _directory.getFileInfo(src, isDirectory);
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#getContentSummary(java.lang.String)
	 */
	public ContentSummary getContentSummary(String path) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setQuota(java.lang.String, long, long)
	 */
	public void setQuota(String path, long namespaceQuota, long diskspaceQuota)
			throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#fsync(java.lang.String, java.lang.String)
	 */
	public void fsync(String src, String client) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.protocol.ClientProtocol#setTimes(java.lang.String, long, long)
	 */
	public void setTimes(String src, long mtime, long atime) throws IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}



	  ////////////////////////////////////////////////////////////////
	  // DatanodeProtocol
	  ////////////////////////////////////////////////////////////////
	  /**
	   */
	  public DatanodeRegistration register(DatanodeRegistration nodeReg
		                                       ) throws IOException {
		    verifyVersion(nodeReg.getVersion());
		    _directory.registerDatanode(nodeReg);
		    forwardRegister(nodeReg);
		    return nodeReg;
		  }

	  /**
	   * Verify request.
	   *
	   * Verifies correctness of the datanode version, registration ID, and
	   * if the datanode does not need to be shutdown.
	   *
	   * @param nodeReg data node registration
	   * @throws IOException
	   */
	  public void verifyRequest(DatanodeRegistration nodeReg) throws IOException {
	    verifyVersion(nodeReg.getVersion());
	    if (!_directory.getRegistrationID().equals(nodeReg.getRegistrationID()))
	      throw new UnregisteredDatanodeException(nodeReg);
	  }

	  /**
	   * Data node notify the name node that it is alive
	   * Return an array of block-oriented commands for the datanode to execute.
	   * This will be either a transfer or a delete operation.
	   */
	  public DatanodeCommand[] sendHeartbeat(DatanodeRegistration nodeReg,
	                                       long capacity,
	                                       long dfsUsed,
	                                       long reing,
	                                       int xmitsInProgress,
	                                       int xceiverCount) throws IOException {
		  //StringUtility.debugSpace("NameNodeFBT.sendHeartbeat");
	    verifyRequest(nodeReg);
	    //forwardHeartBeat(nodeReg, capacity, dfsUsed, reing, xmitsInProgress,
	    //				xceiverCount);
	    return _directory.handleHeartbeat(nodeReg, capacity, dfsUsed, reing,
	        xceiverCount, xmitsInProgress);
	  }

	  public DatanodeCommand blockReport(DatanodeRegistration nodeReg,
              long[] blocks) throws IOException {
		  StringUtility.debugSpace("NameNodeFBT.blockReport()");
		  verifyRequest(nodeReg);
		  BlockListAsLongs blist = new BlockListAsLongs(blocks);
		  stateChangeLog.debug("*BLOCK* NameNode.blockReport: "
				  	+"from "+nodeReg.getName()+" "+blist.getNumberOfBlocks() +" blocks");
		  //forwardBlockReport(nodeReg, blocks);
		  _directory.processReport(nodeReg, blist);
		  if (getFSImage().isUpgradeFinalized())
			  return DatanodeCommand.FINALIZE;
		  return null;
	  }

	  public
	  synchronized
	  void blockReceived(DatanodeRegistration nodeReg,
              Block blocks[],
              String delHints[]) throws IOException {
		  Date start = new Date();
		  NameNode.stateChangeLog.info("*BLOCK* NameNodeFBT.blockReceived: "
		           +"from "+nodeReg.getName()+" "+blocks.length+" blocks.");
		  verifyRequest(nodeReg);
		  //NameNode.LOG.info("verifyRequest done");

		for (int i = 0; i < blocks.length; i++) {
			_directory.blockReceived(nodeReg, blocks[i], delHints[i]);
		}

		//forwardBlockReceived(nodeReg, blocks, delHints);

		NameNode.stateChangeLog.info(String.format("%s%s%d",
										"blockReceived,",
										(new Date().getTime()-start.getTime())/1000.0+",",
										getBlockReceiveCount(blocks.length)));
	  }

	  private int getBlockReceiveCount(int length) {
		  return _blockReceivedCount.addAndGet(length);
	  }


	  public void forwardBlockReceived(DatanodeRegistration nodeReg, Block[] blocks,
			String[] delHints) throws IOException {
		NameNode.LOG.info("NameNodeFBT.forwardBlockReceived at namenode "+namenodeID+" " +
				blocks.length +" blocks "+ (forwardsBlockReceiveCount++) + " times");
		Configuration conf = new Configuration();
		Forwards[] f = new Forwards[nnRPCAddrs.size()];
		for(int i = 1;i <= nnRPCAddrs.size();i++) {
			NameNode.LOG.info("namenodeID "+i);
			if(!(i == namenodeID)) {
				nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
													NNClusterProtocol.class,
													NNClusterProtocol.versionID,
													nnRPCAddrs.get(new Integer(i)),
													conf);
				NameNode.LOG.info("nnNamenode "+nnNamenode.toString());
				f[i-1] = new Forwards(nodeReg, blocks, delHints, nnNamenode);
				f[i-1].start();
			}
		}

		/*for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
				.entrySet().iterator(); i.hasNext();) {
			InetSocketAddress a = i.next().getValue();
			NameNode.LOG.info("serverAddress "+serverAddress);
			NameNode.LOG.info("inetSocketAddress "+a);
			if (!a.equals(serverAddress)) {
				nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
						NNClusterProtocol.class,
						NNClusterProtocol.versionID,
						a,
						new Configuration());
				NameNode.LOG.info("nnNamenode "+nnNamenode);
				nnNamenode.catchBlockReceived(nodeReg, blocks, delHints);
			}
		}*/
		NameNode.LOG.info("NameNodeFBT.forwardBlockReceived at namenode "+namenodeID+" " +
				blocks.length +" blocks "+ (forwardsBlockReceiveCount++) + " times finshed");
	}



	/**
	   * Current system time.
	   * @return current time in msec.
	   */
	  static long now() {
	    return System.currentTimeMillis();
	  }

	  /**
	   * Verify that configured directories exist, then
	   * Interactively confirm that formatting is desired
	   * for each existing directory and format them.
	   *
	   * @param conf
	   * @param isConfirmationNeeded
	   * @return true if formatting was aborted, false otherwise
	 * @throws Exception
	   */
	  private static boolean format(Configuration conf,
	                                boolean isConfirmationNeeded
	                                ) throws Exception {
		  StringUtility.debugSpace("NameNodeFBT.format");
	    Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
	    Collection<File> editDirsToFormat =
	                 FSNamesystem.getNamespaceEditsDirs(conf);
	    for(Iterator<File> it = dirsToFormat.iterator(); it.hasNext();) {
	      File curDir = it.next();
	      if (!curDir.exists())
	        continue;
	      if (isConfirmationNeeded) {
	        System.err.print("Re-format filesystem in " + curDir +" ? (Y or N) ");
	        if (!(System.in.read() == 'Y')) {
	          System.err.println("Format aborted in "+ curDir);
	          return true;
	        }
	        while(System.in.read() != '\n'); // discard the enter-key
	      }
	    }
	    FBTDirectory directory = new FBTDirectory(new FSImage(dirsToFormat,
	    											editDirsToFormat), conf);
	    //NameNodeFBTProcessor.bind("/directory", directory);
	    //directory.initialize(this,conf);
	    directory.dir.getFSImage().format();;
	    return false;
	  }


	  public NamespaceInfo versionRequest() throws IOException {
		    return _directory.getNamespaceInfo();
	  }
	public static NameNodeFBT createNameNode(String argv[], Configuration conf)
								//throws Exception {
	{

		if (conf == null)
	      conf = new Configuration();
	    StartupOption startOpt = parseArguments(argv);
	    if (startOpt == null) {
	      printUsage();
	      return null;
	    }
	    setStartupOption(conf, startOpt);

	    try {
	    switch (startOpt) {
	    	case FORMAT:
	        boolean aborted;
			aborted = format(conf, true);
			NameNode.LOG.info("NameNodeFBT exit");
			System.exit(aborted ? 1 : 0);
		    case FINALIZE:
		        aborted = finalize(conf, true);
		        NameNode.LOG.info("NameNodeFBT exit");
		        System.exit(aborted ? 1 : 0);
		    default:
	    	}
	    } catch (Exception e1) {
				// TODO ��ư�������줿 catch �֥�å�
				e1.printStackTrace();
		}

		NameNodeFBT namenodeFBT;
		try {
			namenodeFBT = new NameNodeFBT(conf);
			return namenodeFBT;
		} catch (Exception e) {
			// TODO ��ư�������줿 catch �֥�å�
			NameNode.LOG.info("createNameNode() exception");
			e.printStackTrace();
		}
		return null;
	}

	public void namenodeRegistration(String host, int port, int nID)
	throws IOException {
		NameNode.LOG.info("NameNodeFBT.namenodeRegistration for "+host);
		InetSocketAddress addr = new InetSocketAddress(host, port);
		nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
				NNClusterProtocol.class, NNClusterProtocol.versionID, addr,
				new Configuration());
		String h;
		int p;
		int ID;
		Map<Integer, InetSocketAddress> NNR = new
						HashMap<Integer, InetSocketAddress>(nnRPCAddrs);
		for (Iterator<Map.Entry<Integer, InetSocketAddress>> i =
					NNR.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Integer, InetSocketAddress> m = i.next();
			h = m.getValue().getHostName();
			p = m.getValue().getPort();
			ID = m.getKey();
			nnNamenode.catchAddr(h, p, ID);
		}
		for (Iterator<Map.Entry<Integer, InetSocketAddress>> i =
					NNR.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Integer, InetSocketAddress> m = i.next();
			InetSocketAddress a = m.getValue();
			if (!a.equals(serverAddress)) {
				nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
						NNClusterProtocol.class, NNClusterProtocol.versionID,
						a, new Configuration());
				nnNamenode.catchAddr(host, port, nID);
			}
		}
		NameNode.LOG.info("nnRPCAddrs put "+addr);
		nnRPCAddrs.put(nID, addr);
		//namesystem.dir.setNNAddrs(nnRPCAddrs);
		//namesystem.dir.rootDir.addINodeLocation(nID);

		if(nID == namenodeID + 1)
			right = addr;
		if(nID == namenodeID - 1)
			left = addr;
		NameNode.LOG.info("[namenodeRegistration] NameNode " + nID);

	}

public void catchAddr(String host, int port, int nID) throws IOException {
	InetSocketAddress addr = new InetSocketAddress(host, port);
	nnRPCAddrs.put(nID, addr);
//namesystem.dir.setNNAddrs(nnRPCAddrs);
//namesystem.dir.rootDir.addINodeLocation(nID);
	if(nID == namenodeID + 1)
		right = addr;
	if(nID == namenodeID - 1)
		left = addr;
}


	public void forwardRegister(DatanodeRegistration registration)
	throws IOException {
	NameNode.LOG.info("NameNodeFBT.forwardRegister");
	NameNode.LOG.info("nnRPCAddrs size "+nnRPCAddrs.size());
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
				.entrySet().iterator(); i.hasNext();) {
			InetSocketAddress a = i.next().getValue();
			NameNode.LOG.info("InetSocketAddress "+a);
			NameNode.LOG.info("serverAddress "+serverAddress);
			if (!a.equals(serverAddress)) {
				nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
						NNClusterProtocol.class, NNClusterProtocol.versionID,
						a, new Configuration());
				nnNamenode.catchRegister(registration);
			}
		}

	}

	public void catchRegister(DatanodeRegistration registration) throws IOException {
		NameNode.LOG.info("NameNodeFBT.catchRegister of "+registration.getName());
		verifyVersion(registration.getVersion());
		_directory.registerDatanode(registration);
	}
	public void catchBlockReceived(DatanodeRegistration nodeReg, Block[] blocks,
			String[] delHints) throws IOException {

		NameNode.LOG.info("*BLOCK* NameNodeFBT.catchBlockReceived: " + "from "
				+ nodeReg.getName() + " " + blocks.length + " blocks.");
		for (int i = 0; i < blocks.length; i++) {
			NameNode.LOG.info("receiving block "+blocks[i]);
			_directory.blockReceived(nodeReg, blocks[i], delHints[i]);
		}
	}

	public void catchHeartBeat(DatanodeRegistration nodeReg, long capacity,
			long dfsUsed, long reing, int xmitsInProgress, int xceiverCount)
			throws IOException {
		_directory.handleHeartbeat(nodeReg, capacity, dfsUsed, reing,
				xceiverCount, xmitsInProgress);

	}

	public void catchBlockReport(DatanodeRegistration nodeReg, long[] blocks)
	throws IOException {
		BlockListAsLongs blist = new BlockListAsLongs(blocks);
		stateChangeLog.debug("*BLOCK* NameNode.blockReport: " + "from "
				+ nodeReg.getName() + " " + blist.getNumberOfBlocks()
				+ " blocks");

		_directory.processReport(nodeReg, blist);
	}
	public void catchErrorReport(DatanodeRegistration nodeReg, int errorCode,
			String msg) throws IOException {
		// Log error message from datanode
		String dnName = (nodeReg == null ? "unknown DataNode" : nodeReg
				.getName());
		LOG.info("Error report from " + dnName + ": " + msg);
		if (errorCode == DatanodeProtocol.NOTIFY) {
			return;
		}
		// verifyRequest(nodeReg);
		if (errorCode == DatanodeProtocol.DISK_ERROR) {
			LOG.warn("Volume failed on " + dnName);
		} //else if (errorCode == DatanodeProtocol.FATAL_DISK_ERROR) {
			_directory.removeDatanode(nodeReg);
		//}

	}
	public void catchReportBadBlocks(LocatedBlock[] blocks) throws IOException {
		stateChangeLog.info("*DIR* NameNode.reportBadBlocks");
		for (int i = 0; i < blocks.length; i++) {
			Block blk = blocks[i].getBlock();
			DatanodeInfo[] nodes = blocks[i].getLocations();
			for (int j = 0; j < nodes.length; j++) {
				DatanodeInfo dn = nodes[j];
				_directory.markBlockAsCorrupt(blk, dn);
			}
		}
	}

	public void catchNextGenerationStamp(Block block, boolean fromNN) throws IOException {
		_directory.nextGenerationStampForBlock(block, fromNN);

	}

	public void catchCommitBlockSynchronization(Block block,
			long newgenerationstamp, long newlength, boolean closeFile,
			boolean deleteblock, DatanodeID[] newtargets) throws IOException {
		_directory.commitBlockSynchronization(block, newgenerationstamp,
				newlength, closeFile, deleteblock, newtargets);
	}


	public FSImage getFSImage() {
		return _directory.dir.getFSImage();
	}

	public boolean transferNamespace(String targetMachine) throws IOException, ClassNotFoundException {
		NameNode.LOG.info("NameNodeFBT.Namespace to "+targetMachine);
		Date start = new Date();
		Configuration conf = new Configuration();
		boolean result = false;
		result = transferDirectory(targetMachine, conf);
		NameNode.LOG.info("NameNodeFBT.transferNamespace to "+targetMachine+","+
							(new Date().getTime()-start.getTime())/1000.0);
		return result;
	}

	//targetNode = edn13
	public boolean transferDirectory(String targetMachine, Configuration conf)
					throws IOException, ClassNotFoundException {
		boolean result = false;
		NameNode.LOG.info("NameNodeFBT.transferDirectory to "+targetMachine);
		Date xferDirectoryStart = new Date();
		FBTDirectory directory = _endPointFBTMapping.get(targetMachine);
		directory.blocksMap = _endPointFBTMapping.get(serverAddress.getHostName()).blocksMap;
		InetSocketAddress isa = getAddress(targetMachine);
		Socket socket = new Socket(isa.getAddress(), FBT_DEFAULT_PORT);
		//System.out.println("socket: "+socket);
		socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);

        ObjectOutputStream _oos= new ObjectOutputStream(
                new BufferedOutputStream(socket.getOutputStream()));
        _oos.writeObject(directory);
        _oos.flush();
        //_oos.close();
        //socket.close();
        Date writeDirectorytoStream = new Date();
        resetOffloading();
        Date resetOffloadingEnd = new Date();
        modifyAfterTransfer(targetMachine);
        Date modifyAfterXfer = new Date();
        NameNode.LOG.info("\n"+"writeDirectoryToStream,"+(writeDirectorytoStream.getTime()-xferDirectoryStart.getTime())/1000.0
        					+"\n"+"resetOffLoading,"+(resetOffloadingEnd.getTime()-writeDirectorytoStream.getTime())/1000.0
        					+"\n"+"modifyAfterTransfer,"+(modifyAfterXfer.getTime()-resetOffloadingEnd.getTime())/1000.0
        					);
        result = true;
        return result;
	}

	private void modifyAfterTransfer(String targetMachine) {
		_directory.modifyAfterTransfer(targetMachine);
	}

	public void getTransferedDirectory(FBTDirectory directory) {
		StringUtility.debugSpace("NameNodeFBT.getTransferedDirectory "+directory);
		System.out.println("Meta node partID "+directory.getMetaNode().getPartitionID());
		System.out.println("getTransferedDirectory done");
	}

	public synchronized void getTransferedDirectoryHandler(FBTDirectory directory) throws UnknownHostException {
		NameNode.LOG.info("NameNodeFBT.getTransferedDirectoryHandler");
		Date start = new Date();
		directory.setFSDirectory(_directory.getFSDirectory());
		directory.setFSImage(_directory.getFSImage());
		directory.hostsReader = _directory.hostsReader;
		directory.dnsToSwitchMapping = _directory.dnsToSwitchMapping;
		directory.pendingReplications = _directory.pendingReplications;
		directory.setDatanodeMap(_directory.getDatanodeMap());
		directory.setHeartbeat(_directory.getHeartbeats());
		directory.setUpgradeManager(_directory.getUpgradeManager());
		directory.setRandom(_directory.getRandom());
		directory.setCorruptReplicasMap(_directory.getCorruptReplicasMap());
		directory.setNodeReplicationLimit(_directory.getNodeReplicationLimit());
		directory.setNodeVisitorFactory(_directory.getNodeVisitorFactory());
		directory.setFBTDirectory(_directory.getFBTDirectory());
		getEndPointFBTMapping().put(serverAddress.getHostName(), directory);
		setDirectory(directory);
		NameNodeFBTProcessor.bind("/directory.".concat(
								serverAddress.getHostName()), directory);
		getDirectory().modify(directory);
		NameNode.LOG.info("NameNodeFBT.getTransferedDirectoryHandler,"+(new Date().getTime()-start.getTime())/1000.0);
	}

	public boolean resetOffloading() throws IOException {
		//return resetOffloading(WriteOffLoading.writeOffLoadingFile);
		return resetOffloadingUseRangeSearch(WriteOffLoading.writeOffLoadingFile.concat(
								"."+serverAddress.getHostName()));
	}

	private boolean resetOffloadingUseRangeSearch(String logFile) throws IOException {
		NameNode.LOG.info("NameNodeFBT.resetOffloadingUseRangeSearch,"+logFile);
		Date start = new Date();
		boolean result = false;
		File file = new File(logFile);
		if (!file.exists())
			if (!file.canRead()) System.out.println("cannot open" + logFile);

		FileInputStream fis = new FileInputStream(file);
		DataInputStream dis = new DataInputStream(fis);
		BufferedReader br = new BufferedReader(new InputStreamReader(dis));
		String line = br.readLine();
		String[] record = line.split(",");
		String src = record[2];
		String wolDst = record[3];
		String dst = record[4];

		Date startSearchBlock = new Date();
		String[] range = _directory.getWriteOffLoadingRangeMap().get(dst);
		NameNode.LOG.info("range,"+dst+","+range[0]+","+range[1]);
		List<INode> inodes = _directory.rangeSearchResponse(range[0], range[1]).
							getINodes();
		NameNode.LOG.info("searchBlock,"+ (new Date().getTime()-startSearchBlock.getTime())/1000.0);
		Date startAddBlocks = new Date();
		ArrayList<Block[]> blocks = new ArrayList<Block[]>();
		for (INode inode:inodes) {
			blocks.add(((INodeFile) inode).getBlocks());
		}
		NameNode.LOG.info("addBlock,"+ (new Date().getTime()-startAddBlocks.getTime())/1000.0);

		writeOffLoadingCommandHandler(new
				WriteOffLoadingCommand(blocks, dst, dst));
		NameNode.LOG.info("resetOffloadingRangeSearch,"+(new Date().getTime()-start.getTime())/1000.0);
		return result;
	}

	private synchronized boolean resetOffloading(String logFile) throws IOException {
		NameNode.LOG.info("NameNodeFBT.resetOffloading");
		Date start= new Date();
		boolean result = false;
		File file = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		BufferedReader br = null;
		String line = null;
		//PrintWriter pw = null;
		//NameNode.LOG.info("FSNamesystem.reOffloading offset "+getOffset());
			try {
				file = new File(WriteOffLoading.writeOffLoadingFile);
				if (!file.exists())
					if (!file.canRead()) System.out.println("cannot open" +
												WriteOffLoading.writeOffLoadingFile);

				fis = new FileInputStream(file);
				dis = new DataInputStream(fis);
				br = new BufferedReader(new InputStreamReader(dis));
				int lineCount=0;
				while ((line = br.readLine())!=null) {

					//NameNode.LOG.info("FSNameSystem read write off loading log file");
					String[] record = line.split(",");
					String src = record[2];
					String wolDst = record[3];
					String dst = record[4];
					ArrayList<Block[]> blocks = new ArrayList<Block[]>();
					Date startSearchBlock = new Date();
					Block[] block = (((INodeFile) _directory.searchResponse(src).getINode()).
												getBlocks());
					NameNode.LOG.info("searchBlock,"+ (new Date().getTime()-startSearchBlock.getTime())/1000.0);
					blocks.add(block);
					lineCount++;
					WriteOffLoadingCommand wolc = new WriteOffLoadingCommand(
							blocks, dst, dst);
					writeOffLoadingCommandHandler(wolc);
					}

				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			result = true;

			NameNode.LOG.info("NameNodeFBT.resetOffLoading,"+(new Date().getTime()-start.getTime())/1000.0);
			return result;
	}
	public void writeOffLoadingCommandHandler(WriteOffLoadingCommand command) {
		NameNode.LOG.info("NameNodeFBT.writeOffLoadingCommandHandler:\n"+command.toString());
		Date start=new Date();
		DatanodeDescriptor WOLSrc = ((DatanodeDescriptor) _directory.getClusterMap().getNode(
							"/default-rack/"+serverAddress.getAddress().getHostAddress()+
							":50010"));
		DatanodeDescriptor dst = ((DatanodeDescriptor) _directory.getClusterMap().getNode(
				"/default-rack/192.168.0.1"+command.getDestination().substring(3, 5)+
				":50010"));
		//NameNode.LOG.info("WOLSrc,dst:"+WOLSrc.getHostName()+","+dst.getHostName());
		DatanodeDescriptor targets[][] = new DatanodeDescriptor[1][1];
		targets[0][0] = dst;
		/*BlocksTargetPair replicateBlocks = new BlocksTargetPair(
				command.getBlocksArray(),
				targets);
		NameNode.LOG.info("replicateBlocks,"+replicateBlocks);*/
		WOLSrc.addBlocksTobeReplicated(command.getBlocksArray(), targets);
		/*for (Block[]bs : command.getBlocks()) {
			for (Block b:bs) {
				//NameNode.LOG.info("reoffload,"+b.getBlockName()+","+dst.getHostName());

				WOLSrc.addBlockToBeReplicated(b, new DatanodeDescriptor[] {dst});
			}
		}
*/
		//NameNode.LOG.info("WOLCHandler,"+(new Date().getTime()-start.getTime())/1000.0);
	}

	void deleteLogFile(String fileName) throws FileNotFoundException {
		  File file = new File (fileName);
		  if (file.exists()) {
			  file.delete();
		  } else throw new FileNotFoundException ("cannot delete file writeOffloadinglog");
	  }
	boolean copy (String src, String dst) throws IOException {
		  boolean success = false;
		  File srcFile = new File (src);
		  File dstFile = new File (dst);
		  FileInputStream in = new FileInputStream(srcFile);
		  FileOutputStream out = new FileOutputStream(dstFile);
		  try {
			  byte[] buf = new byte[4096];
			  int len;
			 while ((len = in.read(buf)) > 0){

				 out.write(buf, 0, len);
			 }
		  }finally {
				 if (in!=null) in.close();
				 if (out!=null) out.close();
				 success = true;
			 }

		 return success;
		}

	public FBTDirectory getDirectory() {
		return _directory;
	}

	public void setDirectory(FBTDirectory directory) {
		_directory = directory;
	}

	public Map<String, FBTDirectory> getEndPointFBTMapping() {
		return _endPointFBTMapping;
	}

	public static String[] getFsNameNodeFBTs() {
		return fsNameNodeFBTs;
	}

	@Override
	public boolean rangeSearch(String low, String high) {
		//StringUtility.debugSpace("NameNodeFBT.rangeSearch,"+low+","+high);
		boolean result = false;
		List<INode> results = _directory.rangeSearchResponse(low, high).
								getINodes();
		if (results!=null) {
			result = true;
		}
		/*for (INode inode:results) {
			System.out.println(((INodeFile) inode).getBlocks());
		}*/
		return result;
	}

}


