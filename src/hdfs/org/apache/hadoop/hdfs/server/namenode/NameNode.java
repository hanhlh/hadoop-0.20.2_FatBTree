/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.namenode;

import org.apache.commons.logging.*;

import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Trash;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.HDFSPolicyProvider;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.StartupOption;
import org.apache.hadoop.hdfs.server.common.IncorrectVersionException;
import org.apache.hadoop.hdfs.server.common.UpgradeStatusReport;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.apache.hadoop.hdfs.server.namenode.metrics.NameNodeMetrics;
import org.apache.hadoop.hdfs.server.namenode.writeOffLoading.WriteOffLoadingCommand;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBT;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.namenodeStaticPartition.NameNodeStaticPartition;
import org.apache.hadoop.hdfs.server.protocol.BlocksWithLocations;
import org.apache.hadoop.hdfs.server.protocol.DatanodeCommand;
import org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.NNClusterProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.hdfs.server.protocol.UpgradeCommand;
import org.apache.hadoop.hdfs.server.protocol.WOLProtocol;
import org.apache.hadoop.http.HttpServer;
import org.apache.hadoop.ipc.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.security.authorize.ConfiguredPolicy;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.apache.hadoop.security.authorize.RefreshAuthorizationPolicyProtocol;
import org.apache.hadoop.security.authorize.ServiceAuthorizationManager;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**********************************************************
 * NameNode serves as both directory namespace manager and
 * "inode table" for the Hadoop DFS.  There is a single NameNode
 * running in any DFS deployment.  (Well, except when there
 * is a second backup/failover NameNode.)
 *
 * The NameNode controls two critical tables:
 *   1)  filename->blocksequence (namespace)
 *   2)  block->machinelist ("inodes")
 *
 * The first table is stored on disk and is very precious.
 * The second table is rebuilt every time the NameNode comes
 * up.
 *
 * 'NameNode' refers to both this class as well as the 'NameNode server'.
 * The 'FSNamesystem' class actually performs most of the filesystem
 * management.  The majority of the 'NameNode' class itself is concerned
 * with exposing the IPC interface and the http server to the outside world,
 * plus some configuration management.
 *
 * NameNode implements the ClientProtocol interface, which allows
 * clients to ask for DFS services.  ClientProtocol is not
 * designed for direct use by authors of DFS client code.  End-users
 * should instead use the org.apache.nutch.hadoop.fs.FileSystem class.
 *
 * NameNode also implements the DatanodeProtocol interface, used by
 * DataNode programs that actually store DFS data blocks.  These
 * methods are invoked repeatedly and automatically by all the
 * DataNodes in a DFS deployment.
 *
 * NameNode also implements the NamenodeProtocol interface, used by
 * secondary namenodes or rebalancing processes to get partial namenode's
 * state, for example partial blocksMap etc.
 **********************************************************/
public class NameNode implements ClientProtocol, DatanodeProtocol,
                                 NamenodeProtocol, FSConstants,
                                 RefreshAuthorizationPolicyProtocol
                                 , NNClusterProtocol
                                 {
  static{
    Configuration.addDefaultResource("hdfs-default.xml");
    Configuration.addDefaultResource("hdfs-site.xml");
    Configuration.addDefaultResource("hdfs-site-fbt.xml");


  }

  public long getProtocolVersion(String protocol,
                                 long clientVersion) throws IOException {
    if (protocol.equals(ClientProtocol.class.getName())) {
      return ClientProtocol.versionID;
    } else if (protocol.equals(DatanodeProtocol.class.getName())){
      return DatanodeProtocol.versionID;
    } else if (protocol.equals(NamenodeProtocol.class.getName())){
      return NamenodeProtocol.versionID;
    } else if (protocol.equals(RefreshAuthorizationPolicyProtocol.class.getName())){
      return RefreshAuthorizationPolicyProtocol.versionID;
    }else if (protocol.equals(NNClusterProtocol.class.getName())){
      return NNClusterProtocol.versionID;
    }else if (protocol.equals(WOLProtocol.class.getName())){
      return WOLProtocol.versionID;
    } else {
      throw new IOException("Unknown protocol to name node: " + protocol);
    }
  }

  public static final int DEFAULT_PORT = 8020;

  public static final Log LOG = LogFactory.getLog(NameNode.class.getName());
  public static final Log stateChangeLog = LogFactory.getLog("org.apache.hadoop.hdfs.StateChange");
  public FSNamesystem namesystem; // TODO: This should private. Use getNamesystem() instead.
  /** RPC server */
  protected Server server;
  /** RPC server address */
  protected InetSocketAddress serverAddress = null;
  /** httpServer */
  private HttpServer httpServer;
  /** HTTP server address */
  private InetSocketAddress httpAddress = null;
  private Thread emptier;
  /** only used for testing purposes  */
  private boolean stopRequested = false;
  /** Is service level authorization enabled? */
  protected boolean serviceAuthEnabled = false;


  /** Add by hanhlh, to specify what kind of NameNode*/
  public static String namenodeType;
	// List of Namenode RPC address
	protected Map<Integer, InetSocketAddress> nnRPCAddrs;
	protected Map<Integer, String[]> nnEnds;

	// NameNode ID
	protected int namenodeID;
	protected InetSocketAddress left = null;
	protected InetSocketAddress right = null;

	protected String start;
	protected String end;

	private int _datanodeNumber;

	//count number of Block Receive
	protected AtomicInteger _blockReceivedCount = new AtomicInteger(0);

		//end
  /** Format a new filesystem.  Destroys any filesystem that may already
   * exist at this location.
 * @throws Exception **/
  public static void format(Configuration conf) throws Exception {
    format(conf, false);
  }

  public static NameNodeMetrics myMetrics;

  public FSNamesystem getNamesystem() {
    return namesystem;
  }

  public static NameNodeMetrics getNameNodeMetrics() {
    return myMetrics;
  }

  public static InetSocketAddress getAddress(String address) {
    return NetUtils.createSocketAddr(address, DEFAULT_PORT);
  }

  public static InetSocketAddress getAddress(Configuration conf) {
    return getAddress(FileSystem.getDefaultUri(conf).getAuthority());
  }

  public static URI getUri(InetSocketAddress namenode) {
    int port = namenode.getPort();
    String portString = port == DEFAULT_PORT ? "" : (":"+port);
    return URI.create("hdfs://"+ namenode.getHostName()+portString);
  }
  //append
  public static ArrayList<InetSocketAddress> getAddresses(Configuration conf) {
	  String[] nnaddresses = conf.getStrings("fs.namenodeFBTs");
	  ArrayList<InetSocketAddress> nns = new ArrayList<InetSocketAddress>();
	  for (String nnaddress:nnaddresses) {
		  nns.add(new InetSocketAddress(nnaddress, DEFAULT_PORT));
	  }
	return nns;
  }
  /**
   * Initialize name-node.
   *
   * @param conf the configuration
 * @throws MessageException
   */
  private void initialize(Configuration conf) throws IOException, MessageException {
	  NameNode.LOG.info("NameNode.initialze()");
    InetSocketAddress socAddr = NameNode.getAddress(conf);
    int handlerCount = conf.getInt("dfs.namenode.handler.count", 10);

    // set service-level authorization security policy
    if (serviceAuthEnabled =
          conf.getBoolean(
            ServiceAuthorizationManager.SERVICE_AUTHORIZATION_CONFIG, false)) {
      PolicyProvider policyProvider =
        (PolicyProvider)(ReflectionUtils.newInstance(
            conf.getClass(PolicyProvider.POLICY_PROVIDER_CONFIG,
                HDFSPolicyProvider.class, PolicyProvider.class),
            conf));
      SecurityUtil.setPolicy(new ConfiguredPolicy(conf, policyProvider));
    }


    // create rpc server
    this.server = RPC.getServer(this,
    							socAddr.getHostName(),
    							socAddr.getPort(),
                                handlerCount,
                                false,
                                conf);

    // The rpc-server port can be ephemeral... ensure we have the correct info
    this.serverAddress = this.server.getListenerAddress();
    FileSystem.setDefaultUri(conf, getUri(serverAddress));
    LOG.info("Namenode up at: " + this.serverAddress);

    myMetrics = new NameNodeMetrics(conf, this);

    this.namesystem = new FSNamesystem(this, conf);

    // appended //
	this.namenodeID = getNamenodeID(conf);
	namesystem.setNamenodeID(namenodeID);
	setStartAndEnd(conf);
	// end of appended//

    //startHttpServer(conf); TODO make it done
    this.server.start();  //start RPC server
 // appended
	nnRPCAddrs = new ConcurrentHashMap<Integer, InetSocketAddress>();
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

     startTrashEmptier(conf);
  }

      public void setStartAndEnd(Configuration conf) throws MessageException {
	  int namenodeNumber = conf.getInt("dfs.namenodeNumber", 1);
	//appended

		// TODO Auto-generated method stub by StringUtility
	  nnEnds = new HashMap<Integer, String[]>();
	  for (int nameID = 1; nameID <= namenodeNumber; nameID++) {
		  String[] range= new String[2];
		  start = StringUtility.generateRange(nameID,
			  		namenodeNumber,
			  		FSNamesystem.experimentDir,
			  		FSNamesystem.low,
			  		FSNamesystem.high)[0];
		  NameNode.LOG.info("start "+start);
		  range[0] = start;
		  end = StringUtility.generateRange(nameID,
				  		namenodeNumber,
				  		FSNamesystem.experimentDir,
				  		FSNamesystem.low,
				  		FSNamesystem.high)[1];
		  NameNode.LOG.info("end "+end);
		  range[1]=end;
		  nnEnds.put(new Integer(nameID), range);
	  }
	  NameNode.LOG.info("nnEnds value "+nnEnds.toString());
		/*String end1 = "/tech/dahliaName/66665";
		String end2 = "/tech/dahliaName/133332";
		String end3 = "END";
		switch(namenodeID) {o
			case 1: start = ""; end = end1; break;
			case 2: start = end1; end = end2; break;
			case 3: start = end2; end = end3; break;
			case 4: start = end3; end = "END"; break;
		}
		nnEnds = new HashMap<Integer, String>();
		nnEnds.put(new Integer(1), end1);
		nnEnds.put(new Integer(2), end2);
		nnEnds.put(new Integer(3), end3);
		nnEnds.put(new Integer(4), new String("END"));
		System.out.println("[start] " + start);
		System.out.println("[end] " + end);
		 */
		/*try {
			mkdirs("/tech/dahliaName", FsPermission.getDefault());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */

  }


protected void startTrashEmptier(Configuration conf) throws IOException, MessageException {
    this.emptier = new Thread(new Trash(conf).getEmptier(), "Trash Emptier");
    this.emptier.setDaemon(true);
    this.emptier.start();
  }

  protected void startHttpServer(Configuration conf) throws IOException {
    String infoAddr =
      NetUtils.getServerAddress(conf, "dfs.info.bindAddress",
                                "dfs.info.port", "dfs.http.address");
    InetSocketAddress infoSocAddr = NetUtils.createSocketAddr(infoAddr);
    String infoHost = infoSocAddr.getHostName();
    int infoPort = infoSocAddr.getPort();
    this.httpServer = new HttpServer("hdfs", infoHost, infoPort,
        infoPort == 0, conf);
    if (conf.getBoolean("dfs.https.enable", false)) {
      boolean needClientAuth = conf.getBoolean("dfs.https.need.client.auth", false);
      InetSocketAddress secInfoSocAddr = NetUtils.createSocketAddr(conf.get(
          "dfs.https.address", infoHost + ":" + 0));
      Configuration sslConf = new Configuration(false);
      sslConf.addResource(conf.get("dfs.https.server.keystore.resource",
          "ssl-server.xml"));
      this.httpServer.addSslListener(secInfoSocAddr, sslConf, needClientAuth);
      // assume same ssl port for all datanodes
      InetSocketAddress datanodeSslPort = NetUtils.createSocketAddr(conf.get(
          "dfs.datanode.https.address", infoHost + ":" + 50475));
      this.httpServer.setAttribute("datanode.https.port", datanodeSslPort
          .getPort());
    }
    this.httpServer.setAttribute("name.node", this);
    this.httpServer.setAttribute("name.node.address", getNameNodeAddress());
    this.httpServer.setAttribute("name.system.image", getFSImage());
    this.httpServer.setAttribute("name.conf", conf);
    this.httpServer.addInternalServlet("fsck", "/fsck", FsckServlet.class);
    this.httpServer.addInternalServlet("getimage", "/getimage", GetImageServlet.class);
    this.httpServer.addInternalServlet("listPaths", "/listPaths/*", ListPathsServlet.class);
    this.httpServer.addInternalServlet("data", "/data/*", FileDataServlet.class);
    this.httpServer.addInternalServlet("checksum", "/fileChecksum/*",
        FileChecksumServlets.RedirectServlet.class);
    this.httpServer.start();

    // The web-server port can be ephemeral... ensure we have the correct info
    infoPort = this.httpServer.getPort();
    this.httpAddress = new InetSocketAddress(infoHost, infoPort);
    conf.set("dfs.http.address", infoHost + ":" + infoPort);
    LOG.info("Web-server up at: " + infoHost + ":" + infoPort);
  }

  /**
   * Start NameNode.
   * <p>
   * The name-node can be started with one of the following startup options:
   * <ul>
   * <li>{@link StartupOption#REGULAR REGULAR} - normal name node startup</li>
   * <li>{@link StartupOption#FORMAT FORMAT} - format name node</li>
   * <li>{@link StartupOption#UPGRADE UPGRADE} - start the cluster
   * upgrade and create a snapshot of the current file system state</li>
   * <li>{@link StartupOption#ROLLBACK ROLLBACK} - roll the
   *            cluster back to the previous state</li>
   * </ul>
   * The option is passed via configuration field:
   * <tt>dfs.namenode.startup</tt>
   *
   * The conf will be modified to reflect the actual ports on which
   * the NameNode is up and running if the user passes the port as
   * <code>zero</code> in the conf.
   *
   * @param conf  confirguration
   * @throws IOException
 * @throws MessageException
   */
  public NameNode(Configuration conf) throws IOException {
	  if (namenodeType.equals("FBT") || namenodeType.equals("StaticPartition")) {
		  //Do nothing
	  } else //normal initialize
	  try {
      initialize(conf);
    } catch (IOException e) {
    	e.printStackTrace();
      this.stop();
      throw e;
    } catch (MessageException e) {
		// TODO ��ư�������줿 catch �֥�å�
    	NameNode.LOG.info("Message Exception");
		e.printStackTrace();
	}
  }

  /**
   * Wait for service to finish.
   * (Normally, it runs forever.)
   */
  public void join() {
	  NameNode.LOG.info("NameNode.join()");
    try {
      this.server.join();
    } catch (InterruptedException ie) {
    	ie.printStackTrace();
    }
  }

  /**
   * Stop all NameNode threads and wait for all to finish.
   */
  public void stop() {
	  NameNode.LOG.info("NameNode.stop()");
	  System.out.println("NameNode.stop()");
    if (stopRequested)
      return;
    stopRequested = true;
    try {
      if (httpServer != null) httpServer.stop();
    } catch (Exception e) {
    	e.printStackTrace();
      LOG.error(StringUtils.stringifyException(e));
    }
    if(namesystem != null) namesystem.close();
    if(emptier != null) emptier.interrupt();
    if(server != null) server.stop();
    if (myMetrics != null) {
      myMetrics.shutdown();
    }
    if (namesystem != null) {
      namesystem.shutdown();
    }
  }

  /////////////////////////////////////////////////////
  // NamenodeProtocol
  /////////////////////////////////////////////////////
  /**
   * return a list of blocks & their locations on <code>datanode</code> whose
   * total size is <code>size</code>
   *
   * @param datanode on which blocks are located
   * @param size total size of blocks
   */
  public BlocksWithLocations getBlocks(DatanodeInfo datanode, long size)
  throws IOException {
    if(size <= 0) {
      throw new IllegalArgumentException(
        "Unexpected not positive size: "+size);
    }

    return namesystem.getBlocks(datanode, size);
  }

  /////////////////////////////////////////////////////
  // ClientProtocol
  /////////////////////////////////////////////////////
  /** {@inheritDoc}
 * @throws MessageException */
  public LocatedBlocks   getBlockLocations(String src,
                                          long offset,
                                          long length) throws IOException, MessageException {
	  myMetrics.numGetBlockLocations.inc();
	  //NameNode.LOG.info("NameNode.getBlockLocations "+src+"at "+ this.namenodeID);
	  Integer getAt = selection(src);

		if(nnRPCAddrs.get(getAt) == null) {
			end = "END";
			getAt = selection(src);
		}
		if (getAt.intValue() == this.namenodeID) {
			System.out.println("getBlockLocations "+src+" locally at "+this.namenodeID);

			return namesystem.getBlockLocations(src, offset, length);
		} else {
			ClientProtocol name = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID, nnRPCAddrs.get(getAt),
					new Configuration());
			System.out.println("forward getBlockLocations "+src+" to "+getAt.intValue());
			return name.getBlockLocations(src, offset, length);
		}
  }

  public static String getClientMachine() {
    String clientMachine = Server.getRemoteAddress();
    if (clientMachine == null) {
      clientMachine = "";
    }
    return clientMachine;
  }

  /** {@inheritDoc}
 * @throws ServiceException
 * @throws MessageException
 * @throws IOException
 * */
  public
  //void
  boolean
  create(String src,
                     FsPermission masked,
                             String clientName,
                             boolean overwrite,
                             short replication,
                             long blockSize
                             ) throws IOException, MessageException, ServiceException {
	  boolean success = false; //;append

	  Integer createAt = selection (src);
	if(nnRPCAddrs.get(createAt) == null) {
			end = "END";
			createAt = selection(src);
		}
		if (createAt.intValue() == this.namenodeID) {
			System.out.println("createLocal "+src+" at "+createAt);
			String clientMachine = getClientMachine();
			if (stateChangeLog.isDebugEnabled()) {
				stateChangeLog.debug("*DIR* NameNode.create: file "
						+src+" for "+clientName+" at "+clientMachine);
			}
			if (!checkPathLength(src)) {
				throw new IOException("create: Pathname too long.  Limit "
	                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
			}

			namesystem.startFile(src,
		    					new PermissionStatus(
		    							UserGroupInformation.getCurrentUGI().getUserName(),
		    							null, masked),
		    					clientName, clientMachine, overwrite, replication, blockSize);
		    myMetrics.numFilesCreated.inc();
		    myMetrics.numCreateFileOps.inc();
		    success = true;

		} else {
				ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
						ClientProtocol.class, ClientProtocol.versionID,
						nnRPCAddrs.get(createAt), new Configuration());
				NameNode.LOG.info("forward from "+this.namenodeID+" create "+src+" to "+createAt);
				success =
				namenode.create(src, masked, clientName, overwrite, replication, blockSize);
		}

	return success;
  }
  private Integer selection(String src) throws IOException {

		int nodes = nnEnds.keySet().size();
		int i = namenodeID;
		start = nnEnds.get(namenodeID)[0];
		end = nnEnds.get(namenodeID)[1];
		/*NameNode.LOG.info("start, end: "+start+", "
										+end);
		NameNode.LOG.info("src: "+src);*/

		if(src == null)
			return null;
		if(src.compareTo(start)<= 0) {
			for(i-- ;i > 0;i--) {
				if(src.compareTo(nnEnds.get(new Integer(i))[1]) > 0) {
					return new Integer(i + 1);
				}
			}
			return new Integer(1);
		} else if(src.compareTo(end) > 0) {
			for(i++;i < nodes;i++) {
				if(src.compareTo(nnEnds.get(new Integer(i))[1]) <= 0) {
					return new Integer(i);
				}
			}
			return new Integer(nodes);
		} else {
			return new Integer(i);
		}
	}

  //appended //
		// end of appended //


  /** {@inheritDoc}
 * @throws ServiceException
 * @throws MessageException */
  public LocatedBlock append(String src, String clientName) throws
  			IOException, MessageException, ServiceException {
    String clientMachine = getClientMachine();
    if (stateChangeLog.isDebugEnabled()) {
      stateChangeLog.debug("*DIR* NameNode.append: file "
          +src+" for "+clientName+" at "+clientMachine);
    }
    LocatedBlock info = namesystem.appendFile(src, clientName, clientMachine);
    myMetrics.numFilesAppended.inc();
    return info;
  }

  /** {@inheritDoc} */
  public boolean setReplication(String src,
                                short replication
                                ) throws IOException {
    return namesystem.setReplication(src, replication);
  }

  /** {@inheritDoc} */
  public void setPermission(String src, FsPermission permissions
      ) throws IOException {
    namesystem.setPermission(src, permissions);
  }

  /** {@inheritDoc} */
  public void setOwner(String src, String username, String groupname
      ) throws IOException {
    namesystem.setOwner(src, username, groupname);
  }

  /**
 * @throws MessageException
   */
  public LocatedBlock addBlock(String src,
                               String clientName
                               ) throws IOException, MessageException {
	  return addBlock(src, clientName, null);
  }
  public LocatedBlock addBlock(String src, String clientName,
			DatanodeInfo[] excludedNodes) throws IOException {
	  LocatedBlock lb = null;
		// appended(if~) //
	  Integer createAt =  selection(src);

		if (createAt.intValue() == namenodeID) {
			List<Node> excludedNodeList = null;
			if (excludedNodes != null) {
				// We must copy here, since this list gets modified later on
				// in ReplicationTargetChooser
				excludedNodeList = new ArrayList<Node>(
						Arrays.<Node> asList(excludedNodes));
			}

			stateChangeLog.debug("*BLOCK* NameNode.addBlock: file " + src
					+ " for " + clientName);
			LocatedBlock locatedBlock =
				namesystem.getAdditionalBlock(src,
					clientName, excludedNodeList);
			if (locatedBlock != null)
				lb = locatedBlock;
				//myMetrics.incrNumAddBlockOps();
			// forwardAddBlock(src, clientName, excludedNodes);

		} else {
			ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID,
					nnRPCAddrs.get(new Integer(createAt)), new Configuration());
			NameNode.LOG.info("forward addBlock from "+namenodeID+ "to "+createAt);
			lb = namenode.addBlock(src, clientName, excludedNodes);
		}
		return lb;
	}


  /**
   * The client needs to give up on the block.
   */
  public void abandonBlock(Block b, String src, String holder
      ) throws IOException {
	// appended(if~) //
	  Integer createAt = selection(src);
		if (createAt.intValue() == namenodeID) {
			stateChangeLog.debug("*BLOCK* NameNode.abandonBlock: " + b
					+ " of file " + src);
			if (!namesystem.abandonBlock(b, src, holder)) {
				throw new IOException("Cannot abandon block during write to "
						+ src);
			}

		} else {
			ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID,
					nnRPCAddrs.get(createAt), new Configuration());
			namenode.abandonBlock(b, src, holder);
		}
  }

  /** {@inheritDoc}
 * @throws MessageException */
  public boolean complete(String src, String clientName) throws IOException, MessageException {
	// appended //
	  Integer completeAt = selection(src);
		if (completeAt.intValue() == this.namenodeID) {
			NameNode.LOG.info("complete locally "+src+" at "+completeAt);
			stateChangeLog.debug("*DIR* NameNode.complete: " + src + " for "
					+ clientName);
			CompleteFileStatus returnCode =
				namesystem.completeFile(src, clientName);
			// forwardComplete(src, clientName);
			if (returnCode == CompleteFileStatus.STILL_WAITING) {
				return false;
			} else if (returnCode == CompleteFileStatus.COMPLETE_SUCCESS) {
				return true;
			} else {
				throw new IOException("Could not complete write to file " + src
						+ " by " + clientName);
			}
		} else {
			ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID,
					nnRPCAddrs.get(completeAt), new Configuration());
			return namenode.complete(src, clientName);
		}
  }

  /**
   * The client has detected an error on the specified located blocks
   * and is reporting them to the server.  For now, the namenode will
   * mark the block as corrupt.  In the future we might
   * check the blocks are actually corrupt.
   */
  public void reportBadBlocks(LocatedBlock[] blocks) throws IOException {
    stateChangeLog.info("*DIR* NameNode.reportBadBlocks");
    for (int i = 0; i < blocks.length; i++) {
      Block blk = blocks[i].getBlock();
      DatanodeInfo[] nodes = blocks[i].getLocations();
      for (int j = 0; j < nodes.length; j++) {
        DatanodeInfo dn = nodes[j];
        namesystem.markBlockAsCorrupt(blk, dn);
      }
    }
  }

  /** {@inheritDoc} */
  public long nextGenerationStamp(Block block, boolean fromNN) throws IOException{
    return namesystem.nextGenerationStampForBlock(block, fromNN);
  }

  /** {@inheritDoc} */
  public void commitBlockSynchronization(Block block,
      long newgenerationstamp, long newlength,
      boolean closeFile, boolean deleteblock, DatanodeID[] newtargets
      ) throws IOException {
    namesystem.commitBlockSynchronization(block,
        newgenerationstamp, newlength, closeFile, deleteblock, newtargets);
  }

  public long getPreferredBlockSize(String filename) throws IOException {
    return namesystem.getPreferredBlockSize(filename);
  }

  /**
   */
  public boolean rename(String src, String dst) throws IOException {
    stateChangeLog.debug("*DIR* NameNode.rename: " + src + " to " + dst);
    if (!checkPathLength(dst)) {
      throw new IOException("rename: Pathname too long.  Limit "
                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
    }
    boolean ret = namesystem.renameTo(src, dst);
    if (ret) {
      myMetrics.numFilesRenamed.inc();
    }
    return ret;
  }

  /**
   */
  @Deprecated
  public boolean delete(String src) throws IOException {
    return delete(src, true);
  }

  /** {@inheritDoc} */
  public boolean delete(String src, boolean recursive) throws IOException {
    if (stateChangeLog.isDebugEnabled()) {
      stateChangeLog.debug("*DIR* Namenode.delete: src=" + src
          + ", recursive=" + recursive);
    }
    boolean ret = namesystem.delete(src, recursive);
    if (ret)
      myMetrics.numDeleteFileOps.inc();
    return ret;
  }

  /**
   * Check path length does not exceed maximum.  Returns true if
   * length and depth are okay.  Returns false if length is too long
   * or depth is too great.
   *
   */
  public boolean checkPathLength(String src) {
    Path srcPath = new Path(src);
    return (src.length() <= MAX_PATH_LENGTH &&
            srcPath.depth() <= MAX_PATH_DEPTH);
  }

  /** {@inheritDoc}
 * @throws MessageException */
  public boolean mkdirs(String src, FsPermission masked) throws IOException, MessageException {
	  Integer mkdirAt = selection(src);
	  NameNode.LOG.info("NameNode.mkdirs at "+mkdirAt);
	  if (nnRPCAddrs.get(mkdirAt) == null) {
		  end = "END";
		  mkdirAt = selection(src);
	  }
	  if (mkdirAt.intValue() == namenodeID) {
		  stateChangeLog.debug("*DIR* NameNode.mkdirs: " + src);
		  if (!checkPathLength(src)) {
			  throw new IOException("mkdirs: Pathname too long.  Limit "
                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
		  }
		  return namesystem.mkdirs(src,
				  new PermissionStatus(UserGroupInformation.getCurrentUGI().getUserName(),
						  null, masked));
	  } else {
		  ClientProtocol name = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID, nnRPCAddrs.get(mkdirAt),
					new Configuration());
		  return name.mkdirs(src, masked);
	  }
  }

  /**
   */
  public void renewLease(String clientName) throws IOException {
    namesystem.renewLease(clientName);
  }

  /**
   */
  public FileStatus[] getListing(String src) throws IOException {
    FileStatus[] files = namesystem.getListing(src);
    if (files != null) {
      myMetrics.numGetListingOps.inc();
    }
    return files;
  }

  /**
   * Get the file info for a specific file.
   * @param src The string representation of the path to the file
   * @throws IOException if permission to access file is denied by the system
   * @return object containing information regarding the file
   *         or null if file not found
 * @throws MessageException
   */
  public FileStatus getFileInfo(String src)  throws IOException, MessageException {
    myMetrics.numFileInfoOps.inc();
    return namesystem.getFileInfo(src);
  }

  /** @inheritDoc */
  public long[] getStats() throws IOException {
    return namesystem.getStats();
  }

  /**
   */
  public DatanodeInfo[] getDatanodeReport(DatanodeReportType type)
  throws IOException {
    DatanodeInfo results[] = namesystem.datanodeReport(type);
    if (results == null ) {
      throw new IOException("Cannot find datanode report");
    }
    return results;
  }

  /**
   * @inheritDoc
   */
  public boolean setSafeMode(SafeModeAction action) throws IOException {
    return namesystem.setSafeMode(action);
  }

  /**
   * Is the cluster currently in safe mode?
   */
  public boolean isInSafeMode() {
    return namesystem.isInSafeMode();
  }

  /**
   * @inheritDoc
   */
  public void saveNamespace() throws IOException {
    namesystem.saveNamespace();
  }

  /**
   * Refresh the list of datanodes that the namenode should allow to
   * connect.  Re-reads conf by creating new Configuration object and
   * uses the files list in the configuration to update the list.
   */
  public void refreshNodes() throws IOException {
    namesystem.refreshNodes(new Configuration());
  }

  /**
   * Returns the size of the current edit log.
   */
  public long getEditLogSize() throws IOException {
    return namesystem.getEditLogSize();
  }

  /**
   * Roll the edit log.
   */
  public CheckpointSignature rollEditLog() throws IOException {
    return namesystem.rollEditLog();
  }

  /**
   * Roll the image
   */
  public void rollFsImage() throws IOException {
    namesystem.rollFSImage();
  }

  public void finalizeUpgrade() throws IOException {
    namesystem.finalizeUpgrade();
  }

  public UpgradeStatusReport distributedUpgradeProgress(UpgradeAction action
                                                        ) throws IOException {
    return namesystem.distributedUpgradeProgress(action);
  }

  /**
   * Dumps namenode state into specified file
   */
  public void metaSave(String filename) throws IOException {
    namesystem.metaSave(filename);
  }

  /** {@inheritDoc} */
  public ContentSummary getContentSummary(String path) throws IOException {
    return namesystem.getContentSummary(path);
  }

  /** {@inheritDoc} */
  public void setQuota(String path, long namespaceQuota, long diskspaceQuota)
                       throws IOException {
    namesystem.setQuota(path, namespaceQuota, diskspaceQuota);
  }

  /** {@inheritDoc} */
  public void fsync(String src, String clientName) throws IOException {
    namesystem.fsync(src, clientName);
  }

  /** @throws MessageException
 * @inheritDoc */
  public void setTimes(String src, long mtime, long atime) throws IOException, MessageException {
    namesystem.setTimes(src, mtime, atime);
  }

  ////////////////////////////////////////////////////////////////
  // DatanodeProtocol
  ////////////////////////////////////////////////////////////////
  /**
   */
  public DatanodeRegistration register(DatanodeRegistration nodeReg
                                       ) throws IOException {
    verifyVersion(nodeReg.getVersion());
    namesystem.registerDatanode(nodeReg);
    return nodeReg;
  }

  /**
   * Data node notify the name node that it is alive
   * Return an array of block-oriented commands for the datanode to execute.
   * This will be either a transfer or a delete operation.
   */
  public DatanodeCommand[] sendHeartbeat(DatanodeRegistration nodeReg,
                                       long capacity,
                                       long dfsUsed,
                                       long remaining,
                                       int xmitsInProgress,
                                       int xceiverCount) throws IOException {
	// verifyRequest(nodeReg);
	  //NameNode.LOG.info("NameNode.sendHeartbeat()");
	  //forwardHeartBeat(nodeReg, capacity, dfsUsed, remaining,
		//		xmitsInProgress, xceiverCount);
		return namesystem.handleHeartbeat(nodeReg, capacity, dfsUsed,
				remaining, xceiverCount, xmitsInProgress);
  }

  public DatanodeCommand blockReport(DatanodeRegistration nodeReg,
                                     long[] blocks) throws IOException {
	// verifyRequest(nodeReg);
	//forwardBlockReport(nodeReg, blocks);
    BlockListAsLongs blist = new BlockListAsLongs(blocks);
    stateChangeLog.debug("*BLOCK* NameNode.blockReport: "
           +"from "+nodeReg.getName()+" "+blist.getNumberOfBlocks() +" blocks");
    namesystem.processReport(nodeReg, blist);
    if (getFSImage().isUpgradeFinalized())
      return DatanodeCommand.FINALIZE;
    return null;
  }

  public
  synchronized
  void blockReceived(DatanodeRegistration nodeReg,
                            Block blocks[],
                            String delHints[]) throws IOException {
    verifyRequest(nodeReg);
    stateChangeLog.debug("*BLOCK* NameNode.blockReceived: "
                         +"from "+nodeReg.getName()+" "+blocks.length+" blocks.");
    for (int i = 0; i < blocks.length; i++) {
      namesystem.blockReceived(nodeReg, blocks[i], delHints[i]);
    }
    NameNode.stateChangeLog.info(String.format("%s%d",
			"blockReceived,",
			getBlockReceiveCount(blocks.length)));
  }

  private synchronized int getBlockReceiveCount(int length) {
	  return _blockReceivedCount.addAndGet(length);
  }
  /**
   */
  public void errorReport(DatanodeRegistration nodeReg,
                          int errorCode,
                          String msg) throws IOException {
    // Log error message from datanode
    String dnName = (nodeReg == null ? "unknown DataNode" : nodeReg.getName());
    LOG.info("Error report from " + dnName + ": " + msg);
    if (errorCode == DatanodeProtocol.NOTIFY) {
      return;
    }
    verifyRequest(nodeReg);
    if (errorCode == DatanodeProtocol.DISK_ERROR) {
      namesystem.removeDatanode(nodeReg);
    }
    forwardErrorReport(nodeReg, errorCode, msg);
  }

  public NamespaceInfo versionRequest() throws IOException {
    return namesystem.getNamespaceInfo();
  }

  public UpgradeCommand processUpgradeCommand(UpgradeCommand comm) throws IOException {
    return namesystem.processDistributedUpgradeCommand(comm);
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
    if (!namesystem.getRegistrationID().equals(nodeReg.getRegistrationID()))
      throw new UnregisteredDatanodeException(nodeReg);
  }

  /**
   * Verify version.
   *
   * @param version
   * @throws IOException
   */
  public void verifyVersion(int version) throws IOException {
    if (version != LAYOUT_VERSION)
      throw new IncorrectVersionException(version, "data node");
  }

  /**
   * Returns the name of the fsImage file
   */
  public File getFsImageName() throws IOException {
    return getFSImage().getFsImageName();
  }

  public FSImage getFSImage() {
    return namesystem.dir.fsImage;
  }

  /**
   * Returns the name of the fsImage file uploaded by periodic
   * checkpointing
   */
  public File[] getFsImageNameCheckpoint() throws IOException {
    return getFSImage().getFsImageNameCheckpoint();
  }

  /**
   * Returns the address on which the NameNodes is listening to.
   * @return the address on which the NameNodes is listening to.
   */
  public InetSocketAddress getNameNodeAddress() {
    return serverAddress;
  }

  /**
   * Returns the address of the NameNodes http server,
   * which is used to access the name-node web UI.
   *
   * @return the http address.
   */
  public InetSocketAddress getHttpAddress() {
    return httpAddress;
  }

  NetworkTopology getNetworkTopology() {
    return this.namesystem.clusterMap;
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

    FSImage fsImage = new FSImage (dirsToFormat, editDirsToFormat);
    if (!namenodeType.equals("FBT")) {
    	FSNamesystem nsys = new FSNamesystem(fsImage, conf);
    	nsys.dir.fsImage.format();
    } else {
    	FBTDirectory fbtDir = new FBTDirectory(fsImage, conf);
    	fbtDir.dir.fsImage.format();
    }

    return false;
  }

  protected static boolean finalize(Configuration conf,
                               boolean isConfirmationNeeded
                               ) throws IOException {
    Collection<File> dirsToFormat = FSNamesystem.getNamespaceDirs(conf);
    Collection<File> editDirsToFormat =
                               FSNamesystem.getNamespaceEditsDirs(conf);
    FSNamesystem nsys = new FSNamesystem(new FSImage(dirsToFormat,
                                         editDirsToFormat), conf);
    System.err.print(
        "\"finalize\" will remove the previous state of the files system.\n"
        + "Recent upgrade will become permanent.\n"
        + "Rollback option will not be available anymore.\n");
    if (isConfirmationNeeded) {
      System.err.print("Finalize filesystem state ? (Y or N) ");
      if (!(System.in.read() == 'Y')) {
        System.err.println("Finalize aborted.");
        return true;
      }
      while(System.in.read() != '\n'); // discard the enter-key
    }
    nsys.dir.fsImage.finalizeUpgrade();
    return false;
  }

  public void refreshServiceAcl() throws IOException {
    if (!serviceAuthEnabled) {
      throw new AuthorizationException("Service Level Authorization not enabled!");
    }

    SecurityUtil.getPolicy().refresh();
  }

  protected static void printUsage() {
    System.err.println(
      "Usage: java NameNode [" +
      StartupOption.FORMAT.getName() + "] | [" +
      StartupOption.UPGRADE.getName() + "] | [" +
      StartupOption.ROLLBACK.getName() + "] | [" +
      StartupOption.FINALIZE.getName() + "] | [" +
      StartupOption.IMPORT.getName() + "]");
  }

  protected static StartupOption parseArguments(String args[]) {
    int argsLen = (args == null) ? 0 : args.length;
    StartupOption startOpt = StartupOption.REGULAR;
    for(int i=0; i < argsLen; i++) {
      String cmd = args[i];
      if (StartupOption.FORMAT.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.FORMAT;
      } else if (StartupOption.REGULAR.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.REGULAR;
      } else if (StartupOption.UPGRADE.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.UPGRADE;
      } else if (StartupOption.ROLLBACK.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.ROLLBACK;
      } else if (StartupOption.FINALIZE.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.FINALIZE;
      } else if (StartupOption.IMPORT.getName().equalsIgnoreCase(cmd)) {
        startOpt = StartupOption.IMPORT;
      } else
        return null;
    }
    return startOpt;
  }

  protected static void setStartupOption(Configuration conf, StartupOption opt) {
    conf.set("dfs.namenode.startup", opt.toString());
  }

  public static StartupOption getStartupOption(Configuration conf) {
    return StartupOption.valueOf(conf.get("dfs.namenode.startup",
                                          StartupOption.REGULAR.toString()));
  }

  public static NameNode createNameNode(String argv[],
                                 Configuration conf) {
	  NameNode namenode = null;
    try {
	  if (conf == null)
      conf = new Configuration();
    namenodeType = conf.get("dfs.namenodeType", "default");
    StartupOption startOpt = parseArguments(argv);
    if (startOpt == null) {
      printUsage();
      return null;
    }
    setStartupOption(conf, startOpt);

    switch (startOpt) {
      case FORMAT:
        boolean aborted = format(conf, true);
        System.out.println("NameNode.exit()");
        System.exit(aborted ? 1 : 0);
      case FINALIZE:
        aborted = finalize(conf, true);
        System.out.println("NameNode.exit()");
        System.exit(aborted ? 1 : 0);
      default:
    }


    if (namenodeType.equals("FBT")) {
    	LOG.info("NameNodeFBT");
    	namenode = new NameNodeFBT(conf);
    } else if (namenodeType.equals("StaticPartition")) {
    	LOG.info("NameNodeStaticPartition");
    	namenode = new NameNodeStaticPartition(conf);
    }
    else if (namenodeType.equals("default")){
    	LOG.info("NameNode");
    	namenode = new NameNode(conf);
    }
    } catch (Exception e) {
    	NameNode.LOG.info("NameNode Exception");
    	e.printStackTrace();
    }
    return namenode;
  }

  /**
   */
  public static void main(String argv[]) {
    try {
      StringUtils.startupShutdownMessage(NameNode.class, argv, LOG);

      NameNode namenode = createNameNode(argv, null);
      if (namenode != null)
        namenode.join();
    } catch (Throwable e) {
    	//NameNode.LOG.info("namenode = null error");
      LOG.error(StringUtils.stringifyException(e));
      e.printStackTrace();
      System.exit(-1);
    }
  }



public boolean synchronizeRootNodes() throws IOException, ServiceException, MessageException {
	// TODO ��ư�������줿�᥽�åɡ�������
	return false;
}

public boolean mkdirs(String src, FsPermission masked, boolean isDirectory)
		throws IOException, MessageException {
	return mkdirs(src, masked);
}

public FileStatus getFileInfo(String src, boolean isDirectory)
		throws IOException, MessageException {
	return getFileInfo(src);
}

//NNClusterProtocol //
public NNClusterProtocol nnNamenode;

//WOLProtocol
public WOLProtocol wolProtocol;

public void namenodeRegistration(String host, int port, int nID)
		throws IOException {
	InetSocketAddress addr = new InetSocketAddress(host, port);
	nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
			NNClusterProtocol.class, NNClusterProtocol.versionID, addr,
			new Configuration());
	String h;
	int p;
	int ID;
	Map<Integer, InetSocketAddress> NNR = new HashMap<Integer, InetSocketAddress>(
			nnRPCAddrs);
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = NNR.entrySet()
			.iterator(); i.hasNext();) {
		Map.Entry<Integer, InetSocketAddress> m = i.next();
		h = m.getValue().getHostName();
		p = m.getValue().getPort();
		ID = m.getKey();
		nnNamenode.catchAddr(h, p, ID);
	}
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = NNR.entrySet()
			.iterator(); i.hasNext();) {
		Map.Entry<Integer, InetSocketAddress> m = i.next();
		InetSocketAddress a = m.getValue();
		if (!a.equals(serverAddress)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
					NNClusterProtocol.class, NNClusterProtocol.versionID,
					a, new Configuration());
			nnNamenode.catchAddr(host, port, nID);
		}
	}
	nnRPCAddrs.put(nID, addr);
	namesystem.dir.setNNAddrs(nnRPCAddrs);
	namesystem.dir.rootDir.addINodeLocation(nID);

	if(nID == namenodeID + 1)
		right = addr;
	if(nID == namenodeID - 1)
		left = addr;
	System.out.println("[namenodeRegistration] NameNode " + nID);
}

public void catchAddr(String host, int port, int nID) throws IOException {
	InetSocketAddress addr = new InetSocketAddress(host, port);
	nnRPCAddrs.put(nID, addr);
	namesystem.dir.setNNAddrs(nnRPCAddrs);
	namesystem.dir.rootDir.addINodeLocation(nID);
	if(nID == namenodeID + 1)
		right = addr;
	if(nID == namenodeID - 1)
		left = addr;
}

public void forwardHeartBeat(DatanodeRegistration nodeReg, long capacity,
		long dfsUsed, long remaining, int xmitsInProgress, int xceiverCount)
		throws IOException {
	Configuration conf = new Configuration();
	Forwards[] f = new Forwards[nnRPCAddrs.size()];
	for(int i = 1;i <= nnRPCAddrs.size();i++) {
		if(!(i == namenodeID)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID,  nnRPCAddrs.get(new Integer(i)), conf);
			f[i-1] = new Forwards(nodeReg, capacity, dfsUsed, remaining, xmitsInProgress, xceiverCount, nnNamenode);
			f[i-1].start();
		}
	}
}

public void forwardBlockReport(DatanodeRegistration nodeReg, long[] blocks)
		throws IOException {
	Configuration conf = new Configuration();
	Forwards[] f = new Forwards[nnRPCAddrs.size()];
	for(int i = 1;i <= nnRPCAddrs.size();i++) {
		if(!(i == namenodeID)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID,  nnRPCAddrs.get(new Integer(i)), conf);
			f[i-1] = new Forwards(nodeReg, blocks, nnNamenode, Forwards.BLOCKREPORT);
			f[i-1].start();
		}
	}
}

public void forwardBlockBeingWrittenReport(DatanodeRegistration nodeReg,
		long[] blocks) throws IOException {
	Configuration conf = new Configuration();
	Forwards[] f = new Forwards[nnRPCAddrs.size()];
	for(int i = 1;i <= nnRPCAddrs.size();i++) {
		if(!(i == namenodeID)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID,  nnRPCAddrs.get(new Integer(i)), conf);
			f[i-1] = new Forwards(nodeReg, blocks, nnNamenode, Forwards.BLOCKSBEINGWRITTENREPORT);
			f[i-1].start();
		}
	}
}

public void forwardBlockReceived(DatanodeRegistration nodeReg,
		Block[] blocks, String[] delHints) throws IOException {
	Configuration conf = new Configuration();
	Forwards[] f = new Forwards[nnRPCAddrs.size()];
	for(int i = 1;i <= nnRPCAddrs.size();i++) {
		if(!(i == namenodeID)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID,  nnRPCAddrs.get(new Integer(i)), conf);
			f[i-1] = new Forwards(nodeReg, blocks, delHints, nnNamenode);
			f[i-1].start();
		}
	}
}

public void forwardErrorReport(DatanodeRegistration nodeReg, int errorCode,
		String msg) throws IOException {
	Configuration conf = new Configuration();
	Forwards[] f = new Forwards[nnRPCAddrs.size()];
	for(int i = 1;i <= nnRPCAddrs.size();i++) {
		if(!(i == namenodeID)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID,  nnRPCAddrs.get(new Integer(i)), conf);
			f[i-1] = new Forwards(nodeReg, errorCode, msg, nnNamenode);
			f[i-1].start();
		}
	}
}

public void forwardReportBadBlocks(LocatedBlock[] blocks)
		throws IOException {
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
			.entrySet().iterator(); i.hasNext();) {
		InetSocketAddress a = i.next().getValue();
		if (!a.equals(serverAddress)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
					NNClusterProtocol.class, NNClusterProtocol.versionID,
					a, new Configuration());
			nnNamenode.catchReportBadBlocks(blocks);
		}
	}
}

public void forwardNextGenerationStamp(Block block, boolean fromNN)
		throws IOException {
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
			.entrySet().iterator(); i.hasNext();) {
		InetSocketAddress a = i.next().getValue();
		if (!a.equals(serverAddress)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
					NNClusterProtocol.class, NNClusterProtocol.versionID,
					a, new Configuration());
			nnNamenode.catchNextGenerationStamp(block, fromNN);
		}
	}
}

public void forwardCommitBlockSynchronization(Block block,
		long newgenerationstamp, long newlength, boolean closeFile,
		boolean deleteblock, DatanodeID[] newtargets) throws IOException {
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
			.entrySet().iterator(); i.hasNext();) {
		InetSocketAddress a = i.next().getValue();
		if (!a.equals(serverAddress)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
					NNClusterProtocol.class, NNClusterProtocol.versionID,
					a, new Configuration());
			nnNamenode.catchCommitBlockSynchronization(block,
					newgenerationstamp, newlength, closeFile, deleteblock,
					newtargets);
		}
	}
}

public void catchHeartBeat(DatanodeRegistration nodeReg, long capacity,
		long dfsUsed, long remaining, int xmitsInProgress, int xceiverCount)
		throws IOException {
	// verifyRequest(nodeReg);
	namesystem.handleHeartbeat(nodeReg, capacity, dfsUsed, remaining,
			xceiverCount, xmitsInProgress);
}

public void catchBlockReport(DatanodeRegistration nodeReg, long[] blocks)
		throws IOException {
	// verifyRequest(nodeReg);
	BlockListAsLongs blist = new BlockListAsLongs(blocks);
	stateChangeLog.debug("*BLOCK* NameNode.blockReport: " + "from "
			+ nodeReg.getName() + " " + blist.getNumberOfBlocks()
			+ " blocks");

	namesystem.processReport(nodeReg, blist);
}

public void catchBlockBeingWrittenReport(DatanodeRegistration nodeReg,
		long[] blocks) throws IOException {
	//TODO this module doesnt exist in 0.20.0
	// verifyRequest(nodeReg);
	BlockListAsLongs blist = new BlockListAsLongs(blocks);
	//namesystem.processBlocksBeingWrittenReport(nodeReg, blist);

	stateChangeLog
			.info("*BLOCK* NameNode.blocksBeingWrittenReport: " + "from "
					+ nodeReg.getName() + " " + blocks.length + " blocks");
}

public void catchBlockReceived(DatanodeRegistration nodeReg,
		Block[] blocks, String[] delHints) throws IOException {
	// verifyRequest(nodeReg);
	stateChangeLog.debug("*BLOCK* NameNode.blockReceived: " + "from "
			+ nodeReg.getName() + " " + blocks.length + " blocks.");
	for (int i = 0; i < blocks.length; i++) {
		namesystem.blockReceived(nodeReg, blocks[i], delHints[i]);
	}
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
	} else if (errorCode == DatanodeProtocol.FATAL_DISK_ERROR) {
		namesystem.removeDatanode(nodeReg);
	}
}

public void catchReportBadBlocks(LocatedBlock[] blocks) throws IOException {
	stateChangeLog.info("*DIR* NameNode.reportBadBlocks");
	for (int i = 0; i < blocks.length; i++) {
		Block blk = blocks[i].getBlock();
		DatanodeInfo[] nodes = blocks[i].getLocations();
		for (int j = 0; j < nodes.length; j++) {
			DatanodeInfo dn = nodes[j];
			namesystem.markBlockAsCorrupt(blk, dn);
		}
	}
}

public void catchNextGenerationStamp(Block block, boolean fromNN)
		throws IOException {
	namesystem.nextGenerationStampForBlock(block, fromNN);
}

public void catchCommitBlockSynchronization(Block block,
		long newgenerationstamp, long newlength, boolean closeFile,
		boolean deleteblock, DatanodeID[] newtargets) throws IOException {
	namesystem.commitBlockSynchronization(block, newgenerationstamp,
			newlength, closeFile, deleteblock, newtargets);
}

public synchronized void forwardRegister(DatanodeRegistration registration)
		throws IOException {
	for (Iterator<Map.Entry<Integer, InetSocketAddress>> i = nnRPCAddrs
			.entrySet().iterator(); i.hasNext();) {
		InetSocketAddress a = i.next().getValue();
		if (!a.equals(serverAddress)) {
			nnNamenode = (NNClusterProtocol) RPC.waitForProxy(
					NNClusterProtocol.class, NNClusterProtocol.versionID,
					a, new Configuration());
			nnNamenode.catchRegister(registration);
		}
	}
}

public void catchRegister(DatanodeRegistration registration)
		throws IOException {
	verifyVersion(registration.getVersion());
	namesystem.registerDatanode(registration);
}

public void forwardCreate(String src, FsPermission masked,
		String clientName, boolean overwrite, short replication,
		long blockSize) throws IOException {

}

public void catchCreate(String src, FsPermission masked, String clientName,
		boolean overwrite, short replication, long blockSize)
		throws IOException, MessageException, ServiceException {
	String clientMachine = getClientMachine();
	if (stateChangeLog.isDebugEnabled()) {
		stateChangeLog.debug("*DIR* NameNode.create: file " + src + " for "
				+ clientName + " at " + clientMachine);
	}
	if (!checkPathLength(src)) {
		throw new IOException("create: Pathname too long.  Limit "
				+ MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH
				+ " levels.");
	}
	namesystem.startFile(src, new PermissionStatus(UserGroupInformation
			//.getCurrentUser().getShortUserName()
			.getCurrentUGI().getUserName()
			,null, masked),
			clientName, clientMachine, overwrite, replication, blockSize);

	//myMetrics.incrNumFilesCreated();
	//myMetrics.incrNumCreateFileOps();
}


public void forwardDelete(String src, boolean recursive) throws IOException {

}

public void catchDelete(String src, boolean recursive) throws IOException {

}

// new configurations
public InetSocketAddress getLeaderAddress(Configuration conf) {
	String la = conf.get("dfs.namenode.leader");
	return getAddress(la);
}

public int getNamenodeID(Configuration conf) {
	return conf.getInt("dfs.namenode.id", 0);
}

// processing INodes

public INodeInfo getNodeFromOther(byte[][] components, int[] visited) {
	List<Integer> vit = new ArrayList<Integer>();
	for (int i = 0; i < visited.length; i++)
		vit.add(new Integer(visited[i]));
	INode target = namesystem.dir.getNodeFromOther(components, vit);
	if (target == null)
		return null;

	return target.getInfo();
}

	public void setCopying(String src, int id, long atime, long mtime) {
		namesystem.dir.setCopying(src, id, atime, mtime);
	}

	public int getNumOfFiles() {

		return namesystem.numOfFiles;
	}

public void setStart(String src) {
	start = src;
}
/* TODO: this module doesnt exist in 0.20.2
public DirectoryListing getListingInternal(String src, byte[] startAfter)
		throws IOException {
	// TODO Auto-generated method stub
	DirectoryListing files = namesystem.getListing(src, startAfter);
	if(src == new Path(end).getParent().toString()) {
		nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID, right, new Configuration());
		DirectoryListing filesAtRight = nnNamenode.getListingInternal(src, startAfter);
		HdfsFileStatus[] partial = new HdfsFileStatus[files.getPartialListing().length + filesAtRight.getPartialListing().length];
		HdfsFileStatus[] here = files.getPartialListing();
		HdfsFileStatus[] rightFiles = filesAtRight.getPartialListing();
		System.arraycopy(here, 0, partial, 0, here.length);
		System.arraycopy(rightFiles, 0, partial, here.length, rightFiles.length);
		int rem = files.getRemainingEntries() + filesAtRight.getRemainingEntries();
		files = new DirectoryListing(partial, rem);
	}
	myMetrics.incrNumGetListingOps();
	if (files != null) {
		myMetrics
				.incrNumFilesInGetListingOps(files.getPartialListing().length);
	}
	return files;
}
*/
// normalize(have not implement yet) //
public void normalize() throws IOException, MessageException, ServiceException {
	Configuration conf = new Configuration();
	nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID, right, conf);
	int number = this.namesystem.numOfFiles - nnNamenode.getNumOfFiles();
	if(number > 0) {
		for(;number > 0 ;number--)
			sendToRight();
	}
	else if(number < -1) {
		for(;number < -1;number++)
			getFromRight();
	}
}

public void sendToRight() throws IOException, MessageException, ServiceException {
	Configuration conf = new Configuration();
	Path srcPath = new Path(end);
	Path dstPath = new Path(end + "copy");
	FileSystem srcFS = srcPath.getFileSystem(conf);
	FileSystem dstFS = srcFS;
	FileUtil.copy(srcFS, srcPath, dstFS, dstPath, true, conf);
	ClientProtocol cp = (ClientProtocol) RPC.waitForProxy(ClientProtocol.class, ClientProtocol.versionID, right, conf);
	cp.rename(end + "copy", end);
	end = searchEnd();
	nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID, right, conf);
	nnNamenode.setStart(end);
}

public void getFromRight() throws IOException, MessageException, ServiceException{
	Configuration conf = new Configuration();
	nnNamenode = (NNClusterProtocol) RPC.waitForProxy(NNClusterProtocol.class, NNClusterProtocol.versionID, right, conf);
	end = nnNamenode.searchStart();
	nnNamenode.copyFromRight(end);
}

public String searchEnd() {
	return namesystem.dir.searchEnd();
}

public String searchStart() {
	start = namesystem.dir.searchStart();
	return start;
}

public void copyFromRight(String endPath) throws IOException, MessageException, ServiceException {
	rename(endPath, endPath + "copy");
	Configuration conf = new Configuration();
	Path srcPath = new Path(endPath + "copy");
	Path dstPath = new Path(endPath);
	FileSystem srcFS = srcPath.getFileSystem(conf);
	FileSystem dstFS = srcFS;
	FileUtil.copy(srcFS, srcPath, dstFS, dstPath, true, conf);
}

public void forwardAddBlock(String src, String clientName) {
	// TODO ��ư�������줿�᥽�åɡ�������

}

public void catchAddBlock(String src, String clientName) {
	// TODO ��ư�������줿�᥽�åɡ�������

}

@Override
public boolean transferNamespace(String targetMachine) throws IOException, ClassNotFoundException {
	// TODO 自動生成されたメソッド・スタブ
	LOG.info("NameNode.transferNamespace to "+targetMachine);
	return resetOffloading();
}

	@Override
	public boolean resetOffloading() throws IOException {
		return resetOffloading(WriteOffLoading.writeOffLoadingFile.concat("."
									+serverAddress.getHostName()));
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
				file = new File(logFile);
				if (!file.exists())
					if (!file.canRead()) System.out.println("cannot open" +
												logFile);

				fis = new FileInputStream(file);
				dis = new DataInputStream(fis);
				br = new BufferedReader(new InputStreamReader(dis));
				int lineCount=0;
				while ((line = br.readLine())!=null) {
					String[] record = line.split(",");
					String src = record[2];
					String wolDst = record[3];  //IPAddress 192.168.0.114
					String dst = record[4];
					Date startSearchBlock = new Date();
					Block[] block = namesystem.getINodeFile(src).getBlocks();
					NameNode.LOG.info("searchBlock,"+ (new Date().getTime()-startSearchBlock.getTime())/1000.0);
					ArrayList<Block[]> blocks = new ArrayList<Block[]>();
					blocks.add(block);
					lineCount++;
					writeOffLoadingCommandHandler(new WriteOffLoadingCommand(blocks, dst, wolDst));
				}

				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*boolean backUp = copy(WriteOffLoading.writeOffLoadingFile,
									WriteOffLoading.writeOffLoadingBackUp.concat(
											String.format("%s.%d.log", "WriteOffLoading",
							new Date().getTime())));
			if (backUp) {
				deleteLogFile(WriteOffLoading.writeOffLoadingFile);
			}*/
			//modifyAfterTransfer(targetMachine);
			result = true;

			NameNode.LOG.info("NameNodeFBT.resetOffLoading,"+(new Date().getTime()-start.getTime())/1000.0);
			return result;
	}

	public void writeOffLoadingCommandHandler(WriteOffLoadingCommand command) {
		NameNode.LOG.info("NameNodeFBT.writeOffLoadingCommandHandler");
		Date start=new Date();
		DatanodeDescriptor WOLSrc = ((DatanodeDescriptor) namesystem.clusterMap.getNode(
							"/default-rack/"+
							command.getWOLDestination()+
							":50010"));
		DatanodeDescriptor dst = ((DatanodeDescriptor) namesystem.clusterMap.getNode(
				"/default-rack/192.168.0.1"+command.getDestination().substring(3, 5)+
				":50010"));
		for (Block[]bs : command.getBlocks()) {
			for (Block b:bs) {
				//NameNode.LOG.info("reoffload: "+b);
				WOLSrc.addBlockToBeReplicated(b, new DatanodeDescriptor[] {dst});
			}
		}

		//NameNode.LOG.info("WOLCHandler,"+(new Date().getTime()-start.getTime())/1000.0);
	}

	@Override
	public boolean rangeSearch(String low, String high) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}



}
