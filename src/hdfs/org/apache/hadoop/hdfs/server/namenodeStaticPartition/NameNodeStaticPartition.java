/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeStaticPartition;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.HDFSPolicyProvider;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.TargetNamespace;
import org.apache.hadoop.hdfs.server.namenode.WriteOffLoading;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.apache.hadoop.hdfs.server.namenode.metrics.NameNodeMetrics;
import org.apache.hadoop.hdfs.server.namenode.writeOffLoading.WriteOffLoadingCommand;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBT;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.WOLReceiver;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.protocol.DatanodeCommand;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.NNClusterProtocol;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.ConfiguredPolicy;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.apache.hadoop.security.authorize.ServiceAuthorizationManager;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * @author hanhlh
 *
 */
public class NameNodeStaticPartition extends NameNode {

	public NameNodeStaticPartition(Configuration conf) throws IOException {
		super(conf);
		try {
			initialize(conf);
		} catch (Exception e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
	}


	private Map<String, FSNamesystem> _endPointFSNamesystemMapping;

	private Map<Integer, String> _nameNodeIDMapping;

	public static int currentGear = 1;

	private WOLReceiver wolReceiver;

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


	    //initialize namenodeIDMapping
	    setNameNodeIDMapping(conf);

	    //backup namespace
		_endPointFSNamesystemMapping = new TreeMap<String, FSNamesystem>();
		ArrayList<String> containedFBTLists = new ArrayList<String>();
		containedFBTLists.add(serverAddress.getHostName());
		System.out.println("containedFSNamesystems: "+
				Arrays.toString(containedFBTLists.toArray()));
		namesystem = new FSNamesystem(this, conf, serverAddress.getHostName());
		_endPointFSNamesystemMapping.put
				(serverAddress.getHostName(), namesystem);

		String[] backUpDirectories = conf.getStrings("fbt.backupNode");
		System.out.println("backupDirectories "+Arrays.toString(backUpDirectories));
		for (String backUpDir:backUpDirectories) {
			System.out.println("backUpDir "+backUpDir);
			if (!backUpDir.equals("null")) {
				FSNamesystem subNamesystem = new FSNamesystem(this, conf, backUpDir);
					_endPointFSNamesystemMapping.put(backUpDir, subNamesystem);
				containedFBTLists.add(backUpDir);
			}
		}
		System.out.println("containedFBTDirectories: "+Arrays.toString(containedFBTLists.toArray()));
		//end
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
		// end of appended
		initWOLReceiver();
	    startTrashEmptier(conf);
	  }

	  /**
	   * Should be (id, hostname), ex: (1, edn13)
	   * */
	  private void setNameNodeIDMapping(Configuration conf) {
		  StringUtility.debugSpace("NameNode.setNameNodeIDMapping");
		  String[] fsNamespaces = conf.getStrings("fs.namenodeFBTs");
		  _nameNodeIDMapping = new TreeMap<Integer, String>();
		  int i =1;
		  for (String fsNamespace:fsNamespaces) {
			  _nameNodeIDMapping.put(i, fsNamespace);
			  i++;
		  }
		  System.out.println("nameNodeIDMapping: "+_nameNodeIDMapping.toString());
	  }


	  public LocatedBlocks   getBlockLocations(String src,
              long offset,
              long length) throws IOException, MessageException {
		myMetrics.numGetBlockLocations.inc();
		//NameNode.LOG.info("NameNode.getBlockLocations "+src+"at "+ this.namenodeID);
		TargetNamespace responsibleNamespace = selection(src);
		Integer getAt = responsibleNamespace.getNamenodeID();
		if(nnRPCAddrs.get(getAt) == null) {
			end = "END";
			getAt = responsibleNamespace.getNamenodeID();
		}
		if (getAt.intValue() == this.namenodeID) {
			System.out.println("getBlockLocations "+src+" locally at "+this.namenodeID);

			return _endPointFSNamesystemMapping.get(responsibleNamespace.getNamespaceOwner())
						.getBlockLocations(src, offset, length);
			} else {
					ClientProtocol name = (ClientProtocol) RPC.waitForProxy(
					ClientProtocol.class, ClientProtocol.versionID, nnRPCAddrs.get(getAt),
					new Configuration());
					System.out.println("forward getBlockLocations "+src+" to "+getAt.intValue());
					return name.getBlockLocations(src, offset, length);
			}
	  }


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

		  TargetNamespace tn = selection(src);
		  Integer createAt = tn.getNamenodeID();
		if(nnRPCAddrs.get(createAt) == null) {
				end = "END";
				createAt = tn.getNamenodeID();
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
				this._endPointFSNamesystemMapping.get(tn.getNamespaceOwner()).
							startFile(src,
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


	  public LocatedBlock addBlock(String src, String clientName,
				DatanodeInfo[] excludedNodes) throws IOException {
		  LocatedBlock lb = null;
			// appended(if~) //
		  TargetNamespace tn =  selection(src);
		  Integer addAt = tn.getNamenodeID();
			if (addAt.intValue() == namenodeID) {
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
					this._endPointFSNamesystemMapping.get(tn.getNamespaceOwner()).
						getAdditionalBlock(src,
								clientName, excludedNodeList);
				if (locatedBlock != null)
					lb = locatedBlock;
					//myMetrics.incrNumAddBlockOps();
				// forwardAddBlock(src, clientName, excludedNodes);

			} else {
				ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
						ClientProtocol.class, ClientProtocol.versionID,
						nnRPCAddrs.get(new Integer(addAt)), new Configuration());
				NameNode.LOG.info("forward addBlock from "+namenodeID+ "to "+addAt);
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
		  TargetNamespace tn = selection(src);
		  Integer abandonAt = tn.getNamenodeID();
			if (abandonAt.intValue() == namenodeID) {
				stateChangeLog.debug("*BLOCK* NameNode.abandonBlock: " + b
						+ " of file " + src);
				if (!this._endPointFSNamesystemMapping.get(tn.getNamespaceOwner())
									.abandonBlock(b, src, holder)) {
					throw new IOException("Cannot abandon block during write to "
							+ src);
				}

			} else {
				ClientProtocol namenode = (ClientProtocol) RPC.waitForProxy(
						ClientProtocol.class, ClientProtocol.versionID,
						nnRPCAddrs.get(abandonAt), new Configuration());
				namenode.abandonBlock(b, src, holder);
			}
	  }

	  /** {@inheritDoc}
	 * @throws MessageException */
	  public boolean complete(String src, String clientName) throws IOException, MessageException {
		// appended //
		  TargetNamespace tn = selection(src);
		  Integer completeAt = tn.getNamenodeID();
			if (completeAt.intValue() == this.namenodeID) {
				NameNode.LOG.info("complete locally "+src+" at "+completeAt);
				stateChangeLog.debug("*DIR* NameNode.complete: " + src + " for "
						+ clientName);
				CompleteFileStatus returnCode =
					this._endPointFSNamesystemMapping.get(tn.getNamespaceOwner())
								.completeFile(src, clientName);
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


	  public boolean mkdirs(String src, FsPermission masked) throws IOException, MessageException {
		  TargetNamespace tn = selection(src);
		  Integer mkdirAt = tn.getNamenodeID();
		  NameNode.LOG.info("NameNode.mkdirs at "+mkdirAt);
		  if (nnRPCAddrs.get(mkdirAt) == null) {
			  end = "END";
			  mkdirAt = tn.getNamenodeID();
		  }
		  if (mkdirAt.intValue() == namenodeID) {
			  stateChangeLog.debug("*DIR* NameNode.mkdirs: " + src);
			  if (!checkPathLength(src)) {
				  throw new IOException("mkdirs: Pathname too long.  Limit "
	                            + MAX_PATH_LENGTH + " characters, " + MAX_PATH_DEPTH + " levels.");
			  }
			  return this._endPointFSNamesystemMapping.get(tn.getNamespaceOwner())
			  			.mkdirs(src,
			  					new PermissionStatus(UserGroupInformation.getCurrentUGI().getUserName(),
							  null, masked));
		  } else {
			  ClientProtocol name = (ClientProtocol) RPC.waitForProxy(
						ClientProtocol.class, ClientProtocol.versionID, nnRPCAddrs.get(mkdirAt),
						new Configuration());
			  return name.mkdirs(src, masked);
		  }
	  }

	  private TargetNamespace selection(String src) throws IOException {
			int nodes = nnEnds.keySet().size();
			int i = namenodeID;

			start = nnEnds.get(namenodeID)[0];
			end = nnEnds.get(namenodeID)[1];
			NameNode.LOG.info("start, end: "+start+", "
											+end);

			if(src == null)
				return null;
			if(src.compareTo(start)<= 0) {
				for(i-- ;i > 0;i--) {
					if(src.compareTo(nnEnds.get(new Integer(i))[1]) > 0) {
						return gearHandling(new Integer(i + 1));
					}
				}
				return gearHandling(new Integer(1));
			} else if(src.compareTo(end) > 0) {
				for(i++;i < nodes;i++) {
					if(src.compareTo(nnEnds.get(new Integer(i))[1]) <= 0) {
						return gearHandling(new Integer(i));
					}
				}
				return gearHandling(new Integer(nodes));
			} else {
				return gearHandling(new Integer(i));
			}
		}

		private TargetNamespace gearHandling(int nodeID) {
			Integer targetID = nodeID;
			String target = _nameNodeIDMapping.get(nodeID);

			NameNode.LOG.info("target,"+target);
			NameNode.LOG.info("backUpMapping,"+
					namesystem._backUpMapping.toString());
			//check appropriate target with gear information
			if (!namesystem.getGearActivateNodes().get(currentGear).
									contains(target)) {
				String backUpTarget = namesystem._backUpMapping.get(target)[0];
				for (Iterator<Map.Entry<Integer, String>>
								iter = _nameNodeIDMapping.entrySet().iterator();iter.hasNext();) {
					Map.Entry<Integer, String> e = iter.next();
					if (backUpTarget.equals(e.getValue())) {
						targetID = e.getKey();
						return new TargetNamespace(targetID, backUpTarget);

					}
				}
			}

			return new TargetNamespace(targetID, serverAddress.getHostName());
		}


	/**
	 * DatanodeProtocol
	 * */
		public DatanodeRegistration register(DatanodeRegistration nodeReg
        ) throws IOException {
			verifyVersion(nodeReg.getVersion());
			namesystem.registerDatanode(nodeReg);
			forwardRegister(nodeReg);
			return nodeReg;
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

		@Override
		public boolean transferNamespace(String targetMachine) throws IOException, ClassNotFoundException {
			NameNode.LOG.info("NameNodeStaticPartition.transferNamespace to "+targetMachine);
			Date start = new Date();
			Configuration conf = new Configuration();
			boolean result = false;
			result = transferDirectory(targetMachine, conf);
			NameNode.LOG.info("NameNodeStaticPartition.transferNamespace to "+targetMachine+","+
						(new Date().getTime()-start.getTime())/1000.0);
			return result;
		  }

		//targetNode = edn13
		public boolean transferDirectory(String targetMachine, Configuration conf)
						throws IOException, ClassNotFoundException {
			boolean result = false;
			NameNode.LOG.info("NameNodeStaticPartition.transferDirectory to "+targetMachine);
			Date xferDirectoryStart = new Date();
			FSNamesystem subNamesystem = _endPointFSNamesystemMapping.
									get(targetMachine);
			subNamesystem.blocksMap = _endPointFSNamesystemMapping.get(
					serverAddress.getHostName()).blocksMap;
			InetSocketAddress isa = getAddress(targetMachine);
			Socket socket = new Socket(isa.getAddress(), NameNodeFBT.FBT_DEFAULT_PORT);
			//System.out.println("socket: "+socket);
			socket.setTcpNoDelay(true);
	        socket.setKeepAlive(true);

	        ObjectOutputStream _oos= new ObjectOutputStream(
	                new BufferedOutputStream(socket.getOutputStream()));
	        _oos.writeObject(subNamesystem);
	        _oos.flush();
	        Date writeDirectorytoStream = new Date();
	        resetOffloading();
	        Date resetOffloadingEnd = new Date();
	        Date modifyAfterXfer = new Date();
	        NameNode.LOG.info("\n"+"writeDirectoryToStream,"+(writeDirectorytoStream.getTime()-xferDirectoryStart.getTime())/1000.0
	        					+"\n"+"resetOffLoading,"+(resetOffloadingEnd.getTime()-writeDirectorytoStream.getTime())/1000.0
	        					+"\n"+"modifyAfterTransfer,"+(modifyAfterXfer.getTime()-resetOffloadingEnd.getTime())/1000.0
	        					);
	        result = true;
	        return result;
		}


		  	@Override
		  	public boolean resetOffloading() throws IOException {
		  		return resetOffloading(WriteOffLoading.writeOffLoadingFile.concat(
		  							"."+serverAddress.getHostName()));
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
		  					NameNode.LOG.info("searchBlock,"+ (new Date().getTime()-
		  							startSearchBlock.getTime())/1000.0);
		  					ArrayList<Block[]> blocks = new ArrayList<Block[]>();
		  					blocks.add(block);
		  					lineCount++;
		  					writeOffLoadingCommandHandler(new WriteOffLoadingCommand(
		  							blocks, dst, dst));
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
		  		DatanodeDescriptor WOLSrc = ((DatanodeDescriptor) namesystem.getClusterMap().getNode(
						"/default-rack/"+serverAddress.getAddress().getHostAddress()+
						":50010"));
		  		DatanodeDescriptor dst = ((DatanodeDescriptor)
		  							namesystem.getClusterMap().getNode(
		  							"/default-rack/192.168.0.1"+command.getDestination().substring(3, 5)+
		  							":50010"));
		  		for (Block[]bs : command.getBlocks()) {
		  			for (Block b:bs) {
		  				/*NameNode.LOG.info("block,WOLSrc,dst,"+b+","+
		  						WOLSrc.getHostName()+","+dst.getHostName());*/
		  				WOLSrc.addBlockToBeReplicated(b, new DatanodeDescriptor[] {dst});
		  			}
		  		}

		  		//NameNode.LOG.info("WOLCHandler,"+(new Date().getTime()-start.getTime())/1000.0);
		  	}

		  	public synchronized void getTransferedDirectoryHandler(FSNamesystem subNamesystem) throws UnknownHostException {
				NameNode.LOG.info("NameNodeStaticPartition.getTransferedDirectoryHandler");
				Date start = new Date();
				subNamesystem.setFSImage(namesystem.getFSImage());
				subNamesystem.hostsReader = namesystem.hostsReader;
				subNamesystem.dnsToSwitchMapping = namesystem.dnsToSwitchMapping;
				subNamesystem.pendingReplications = namesystem.pendingReplications;
				subNamesystem.setHeartbeat(namesystem.getHeartbeats());
				subNamesystem.setDatanodeMap(namesystem.getDatanodeMap());
				subNamesystem.setUpgradeManager(namesystem.getUpgradeManager());
				subNamesystem.setCorruptReplicasMap(namesystem.getCorruptReplicasMap());
				subNamesystem.setLeasManager(namesystem.getLeaseManager());
				subNamesystem.setRandom(namesystem.getRandom());
				subNamesystem.setNodeReplicationLimit(namesystem.getNodeReplicationLimit());
				getEndPointFSNamesystemMapping().put
							(serverAddress.getHostName(), subNamesystem);
				setDirectory(subNamesystem);
				NameNode.LOG.info("NameNodeStaticPartition.getTransferedDirectoryHandler,"+(new Date().getTime()-start.getTime())/1000.0);
			}


		  	public Map<String, FSNamesystem> getEndPointFSNamesystemMapping() {
				return this._endPointFSNamesystemMapping;
			}

		  	public void setDirectory(FSNamesystem directory) {
				namesystem = directory;
			}

		  	private void initWOLReceiver() throws IOException {
				wolReceiver = new WOLReceiver(
						NameNodeFBT.FBT_DEFAULT_PORT, this);
			}

	}
