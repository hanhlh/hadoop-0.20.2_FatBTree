/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.protocol.UnregisteredDatanodeException;
import org.apache.hadoop.hdfs.server.common.GenerationStamp;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.StartupOption;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory;
import org.apache.hadoop.hdfs.server.namenode.FSImage;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryWithQuota;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.INodeFileUnderConstruction;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.Host2NodesMap;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenode.PendingReplicationBlocks;
import org.apache.hadoop.hdfs.server.namenode.ReplicationTargetChooser;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.SafeModeException;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.CompleteFileResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.DeleteResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetAdditionalBlockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetBlockLocationsResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RangeSearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DisallowedDatanodeException;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.CachedDNSToSwitchMapping;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.net.DNSToSwitchMapping;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.ScriptBasedMapping;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.HostsFileReader;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * @author hanhlh
 *
 */
public class FBTDirectory extends FSNamesystem implements Writable, Serializable{
	/**
	 *
	 */
	private static long serialVersionUID = 1L;
	public static final String SPACE = "*********";
	private static final int PART_ID_DATANODE_MAPPING_SIZE = 20;
	private static final int DEFAULT_FANOUT = 5;
	private static final int DEFAULT_LEAFFANOUT = 1000;
	private static final int DEFAULT_PARTITION_ROOT_SIZE =
								NameNodeFBT.DEFAULT_TOTAL_DATANODES; // = number of total hard disk
	public static final int DEFAULT_MAX_FILES_PER_DIRECTORY =100;
	private static final long DEFAULT_ROOT_NODEID =0;
	public static final String DEFAULT_ROOT_DIRECTORY = "/user/hanhlh";
	public static String DEFAULT_NAME;
	// Default initial capacity and load factor of map
	  public static final int DEFAULT_INITIAL_MAP_CAPACITY = 16;
	  public static final float DEFAULT_MAP_LOAD_FACTOR = 0.75f;
	private Map<String, Node> localNodeMapping;
	//instance

	private MetaNode _meta;

	private transient NodeVisitorFactory _visitorFactory;

	transient int _datanodeNumber;

	transient ArrayList<EndPoint> _endPoints;

	transient RequestFactory requestFactory;
	transient ResponseClassFactory responseClassFactory;
	transient Rule rule;
	private transient boolean ready = false;
	//normal HDFS
	transient SafeModeInfo safeMode;
	private transient boolean isPermissionEnabled;
	private Host2NodesMap host2DataNodeMap = new Host2NodesMap();
	BlocksMap blocksMap = new BlocksMap(DEFAULT_INITIAL_MAP_CAPACITY,
            					DEFAULT_MAP_LOAD_FACTOR);
	  private UserGroupInformation fsOwner;
	  private String supergroup;
	  private PermissionStatus defaultPermission;

	  /*protected transient HostsFileReader hostsReader;
	  protected transient DNSToSwitchMapping dnsToSwitchMapping;*/
	/**
	   * The global generation stamp for this file system.
	   */
	  private transient final GenerationStamp generationStamp = new GenerationStamp();

	  transient long systemStart = 0;

	//  The maximum number of replicates we should allow for a single block
	  public transient int maxReplication;
	  public transient int minReplication;
	// allow appending to hdfs files
	  private transient boolean supportAppends = true;

	// for block replicas placement
	  transient ReplicationTargetChooser replicator;
	  //
	  // Stores the correct file name hierarchy
	  //
	  public transient FSDirectory dir;
	  public transient static FBTDirectory fbtDirectoryObject;
	  protected transient static InetSocketAddress nameNodeAddress;
	  public transient PendingReplicationBlocks pendingReplications;
	  /*Daemon hbthread = null;   // HeartbeatMonitor thread
	  public Daemon lmthread = null;   // LeaseMonitor thread
	  Daemon smmthread = null;  // SafeModeMonitor thread
	  public Daemon replthread = null;  // Replication thread
*/
	  private String _owner;
	  private AtomicInteger _nodeSequence = new AtomicInteger(0);
	  /**
	   * Map the range of src to the node that will get WOL blocks
	   * Example: end13, [lowSrc, highSrc]
	   * */
	  transient Map<String, String[]> WriteOffLoadingRangeMap =
		  					new ConcurrentHashMap<String, String[]>();

	  //constructor

	  public FBTDirectory() {}

	  public FBTDirectory(FSImage fsImage, Configuration conf) throws IOException {
		  super(fsImage, conf);
		  this.setConfigurationParameters(conf);
		  this.dir = new FSDirectory(fsImage, this, conf);
	}

	  public FBTDirectory(NameNodeFBT nameNodeFBT, Configuration conf,
							String owner) throws Exception {
			super(nameNodeFBT, conf);
			_owner = owner;
			DEFAULT_NAME = "/directory."+owner;
			initialize(nameNodeFBT, conf, owner);

	}

	public String getOwner() {
		return _owner;
	}
	public AtomicInteger getNodeSequence() {
		return _nodeSequence;
	}

	public void initialize(NameNodeFBT nameNodeFBT, Configuration conf,
								String owner) throws Exception {
		StringUtility.debugSpace("FBTDirectory.initialize");
		this.systemStart = now();
		 setDatanodeRangeMapping(conf);
		 setGear(conf);
		 setWriteOffLoadingTarget(conf);
		 setBackUpMapping(conf);
		this.setConfigurationParameters(conf);
		intializeFBTDirectoryReplicationMapping(conf);
		nameNodeAddress = nameNodeFBT.getNameNodeAddress();
		this.registerMBean(conf); // register the MBean for the FSNamesystemStutus
		this.dir = new FSDirectory(this, conf, owner);
		StartupOption startOpt = NameNode.getStartupOption(conf);
		Collection<File> namespaceDirs = getNamespaceDirs(conf, owner);
		Collection<File> namespaceEditsDirs = getNamespaceEditsDirs(conf, owner);

		this.dir.loadFSImage(namespaceDirs, namespaceEditsDirs, startOpt);
		long timeTakenToLoadFSImage = now() - systemStart;
			LOG.info("Finished loading FSImage in " + timeTakenToLoadFSImage + " msecs");
		/*NameNode.getNameNodeMetrics().fsImageLoadTime.set(
	                              (int) timeTakenToLoadFSImage);*/

	    //this.safeMode = new SafeModeInfo(conf);
	    //setBlockTotal();
	    pendingReplications = new PendingReplicationBlocks(
	                            conf.getInt("dfs.replication.pending.timeout.sec",
	                                        -1) * 1000L);

	    hostsReader = new HostsFileReader(conf.get("dfs.hosts",""),
	      	    		conf.get("dfs.hosts.exclude",""));

	    dnsToSwitchMapping = ReflectionUtils.newInstance(
		   		conf.getClass("topology.node.switch.mapping.impl", ScriptBasedMapping.class,
		            DNSToSwitchMapping.class), conf);

		    /* If the dns to swith mapping supports cache, resolve network
		     * locations of those hosts in the include list,
		     * and store the mapping in the cache; so future calls to resolve
		     * will be fast.
		     */
		 if (dnsToSwitchMapping instanceof CachedDNSToSwitchMapping) {
			 dnsToSwitchMapping.resolve(new ArrayList<String>(hostsReader.getHosts()));
		 }
		_endPoints = createEndPointList(conf, owner);
		if (_meta != null) {
			_meta.initOwner(this);

		} else {
			try{
				localNodeMapping = new TreeMap<String, Node>();
				createRootNode(_endPoints, conf);
				System.out.println("Root pointer "+_meta.getRootPointer());
				IndexNode node = (IndexNode) getNode(_meta.getRootPointer());
				System.out.println("rootNode "+node.getNodeIdentifier());
				node.key2String();
			}catch (Exception e)
			{
				LOG.info("initialize exception " + e);
			}
		}
		init(conf);
		 /* //append
		  clusterMap = (getOwner().equals(nameNodeAddress.getHostName()))?
				  		clusterMap :
				  		((FBTDirectory) NameNodeFBTProcessor.lookup("/directory."+
				  			nameNodeAddress.getHostName())).getClusterMap();
		  System.out.println("clusterMap: "+clusterMap.toString());
		  //appended
		  this.replicator = new ReplicationTargetChooser(
                conf.getBoolean("dfs.replication.considerLoad", true),
                this,
                clusterMap);*/
	}

	public FSImage getFSImage() {
	    return dir.getFSImage();
  }

	public void setFSImage(FSImage image) {
	  dir.fsImage = image;
	}

	public static Collection<File> getNamespaceDirs(Configuration conf,
										String owner) {
	    Collection<String> dirNames = conf.getStringCollection("dfs.name.dir");
	    if (dirNames.isEmpty())
	      dirNames.add("/tmp/hadoop/dfs/name".concat(".").concat(owner));
	    Collection<File> dirs = new ArrayList<File>(dirNames.size());
	    for(String name : dirNames) {
	      dirs.add(new File(name.concat(".").concat(owner)));
	    }
	    return dirs;
	  }

	  public static Collection<File> getNamespaceEditsDirs(Configuration conf,
			  								String owner) {
	    Collection<String> editsDirNames =
	            conf.getStringCollection("dfs.name.edits.dir");
	    if (editsDirNames.isEmpty())
	      editsDirNames.add("/tmp/hadoop/dfs/name".concat(".").concat(owner));
	    Collection<File> dirs = new ArrayList<File>(editsDirNames.size());
	    for(String name : editsDirNames) {
	      dirs.add(new File(name.concat(".").concat(owner)));
	    }
	    return dirs;
	  }

	/** Return the FSNamesystem object
	   *
	   */
	  public static FBTDirectory getFBTDirectory() {
	    return fbtDirectoryObject;
	  }
	  /**
	   * Initializes some of the members from configuration
	   * get clusterMap from primaryFBTDirectory
	   */
	  public void setConfigurationParameters(Configuration conf)
	                                          throws IOException {
		  StringUtility.debugSpace("FBTDirectory.setConfigurationParameters");
		  fbtDirectoryObject = this;
		  super.setConfigurationParameters(conf);
			//this.defaultReplication = conf.getInt("dfs.replication", 3);
			this.maxReplication = conf.getInt("dfs.replication.max", 512);
			this.minReplication = conf.getInt("dfs.replication.min", 1);
			if (minReplication <= 0)
			throw new IOException(
                     "Unexpected configuration parameters: dfs.replication.min = "
                     + minReplication
                     + " must be greater than 0");
			if (maxReplication >= (int)Short.MAX_VALUE)
				throw new IOException(
                     "Unexpected configuration parameters: dfs.replication.max = "
                     + maxReplication + " must be less than " + (Short.MAX_VALUE));
			if (maxReplication < minReplication)
			throw new IOException(
                     "Unexpected configuration parameters: dfs.replication.min = "
                     + minReplication
                     + " must be less than dfs.replication.max = "
                     + maxReplication);

	  }

	  public NetworkTopology getClusterMap() {
		  return clusterMap;
	  }
	private ArrayList<EndPoint> createEndPointList(Configuration conf,
										String owner)
								throws UnknownHostException {

		_datanodeNumber = conf.getInt("dfs.datanode.fbt.datanodeNumber",
									NameNodeFBT.DEFAULT_TOTAL_DATANODES);
		ArrayList<EndPoint> endPoints = new ArrayList<EndPoint>();
		for (int i=1; i<=_datanodeNumber; i++) {
			EndPoint ep = new EndPoint(conf.get("endPoint["+i+"]"),
										NameNodeFBT.FBT_MESSAGE_PORT);
			endPoints.add(ep);
		}
		for (EndPoint ep:endPoints) {
			int partID = 0xff & ep.getInetAddress().getAddress()[3];
		    NameNodeFBTProcessor.bind("/mapping/"+partID, ep);
		}
		return endPoints;
	}
	protected void createRootNode(List<EndPoint> endPoints, Configuration conf)
    		throws Exception {
		int selfPartID =getSelfPartID();
		//getPartIDAndRegisterMapping(endPoints);
		//System.err.println("WARNING: meta page not found");
        //System.err.println("WARNING: creating empty directory ...");
		System.out.println("selfPartID "+selfPartID);
        _meta = (MetaNode)createMetaNode(
        		selfPartID,
        		100+Integer.parseInt(getOwner().substring(getOwner().length()-2)),
                Integer.parseInt(conf.get("dfs.datanode.fbt.fanout")),
                Integer.parseInt(conf.get("dfs.datanode.fbt.leaffanout")));
        System.out.println("_meta "+_meta.getNodeIdentifier());
        synchronized (localNodeMapping) {
        	localNodeMapping.put(_meta.getNodeIdentifier().toString(), _meta);
        }

        System.out.println("Meta "+_meta.getNodeIdentifier());
        FBTInitializeVisitor visitor = createInitializeVisitor();
        visitor.initialize(endPoints, conf);
        visitor.run();
        System.out.println("FBTInitializeVisitor.run() end");
        //List<String> partList = PropertyArranger.getPartitions(endPoints);
    	//setTreeLockerPartID(partList.get(0));

		int height = 1;
		int fanout = Integer.parseInt(conf.get("dfs.datanode.fbt.fanout"));

		while (endPoints.size() > Math.pow(fanout, height)) {
		    height++;
		}
		_meta.setTreeHeight(height + 1);
		System.out.println("FBTDirectory.createRootNode end");

	}

	public static InetSocketAddress getNameNodeAddress() {
		return nameNodeAddress;
	}

	public int getSelfPartID() throws UnknownHostException {
		return InetAddress.getLocalHost().getAddress()[3];
	}
	public boolean synchronizeRootNodes () throws ServiceException, MessageException, IOException {
		StringUtility.debugSpace("FBTDirectory.synchronizeRootNode");
		return synchronizeRootNodes(_endPoints);
	}

	private boolean synchronizeRootNodes (List<EndPoint> endpoints) throws ServiceException, MessageException, IOException {
		boolean ready = false;
		StringUtility.debugSpace("FBTDirectory.synchronizeRootnodes");
		for (EndPoint ep:endpoints) {
			System.out.println("endpoint: "+ep.getHostName());
		}
		IndexNode root = null;
		synchronized (localNodeMapping) {
			root = (IndexNode) localNodeMapping.get(
										FBTDirectory.DEFAULT_ROOT_NODEID);
		}
		Messenger messenger = (Messenger) NameNodeFBTProcessor.
											lookup("/messenger");
		int partID = _meta.getPartitionID();
		//ip of edn19 is 192.168.0.119
		EndPoint source = (EndPoint) NameNodeFBTProcessor.lookup(
							"/messenger/endpoint/edn"+String.valueOf(partID-100));

		//System.out.println("Synchronize from endpoint "+source.toString());
		for (int j=0; j<endpoints.size(); j++) {
			if (!source.equals(endpoints.get(j))) {
				String destination = endpoints.get(j).getHostName();
				int destinationPartID = Integer.parseInt(destination.substring(3));
				System.out.println("Synchronize at endpoint partID "+
									destinationPartID);
				Request request = requestFactory.createSynchronizeRootRequest
								(DEFAULT_NAME,
								destinationPartID+100,
								root.get_children().get(partID-117),
								(partID-117));
				Call call = new Call(messenger);
				Class responseClass = responseClassFactory.
											createSynchronizeRootResponseClass();
				call.setDestination(endpoints.get(j));
				call.setRequest(request);
				call.setResponseClass(responseClass);

				Response response = (Response) call.invoke();
				//System.out.println("*********Response " + response.toString()+"*********");
			}
		}

		//createRootNodes
		//System.out.println(DEFAULT_ROOT_DIRECTORY);
		//System.out.println(FBTInitializeVisitor._experimentDir);
		mkdirs(FBTInitializeVisitor._experimentDir,
				this.createFsOwnerPermissions(
				new FsPermission ((short) 0755)),
				true, now(), true);
		return true;

	}

	public boolean createRootDirectory() throws MessageException {
		StringUtility.debugSpace("FBTDirectory.createRootDirectory");
		//Add new
		Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
											("/messenger");
		Call call2 = new Call(messenger);

		FsPermission permission = new FsPermission((short) 0755);
		PermissionStatus ps = new PermissionStatus(
				fsOwner.getUserName(),
				supergroup,
				permission);
		Request request = requestFactory.createInsertRequest
						//(key, inodeDirectory);
						(FBTDirectory.DEFAULT_ROOT_DIRECTORY,
						ps, true);


		Class responseClass = responseClassFactory.
									createInsertResponseClass();

		call2.setDestination(messenger.getEndPoint());
		call2.setRequest(request);
		call2.setResponseClass(responseClass);

		InsertResponse response = (InsertResponse) call2.invoke();
		System.out.println("response "+response.toString());
		return true;
	}

	protected int getPartID(Configuration conf) throws UnknownHostException {

		boolean found = false;
		int PartIDDatanodeMappingSize = _datanodeNumber;

		String machineName = DNS.getDefaultHost(
					conf.get("dfs.datanode.dns.interface","default"),
					conf.get("dfs.datanode.dns.nameserver", "default"));

		int dataNodeID=1;
		while ((dataNodeID<PartIDDatanodeMappingSize) && (!found)) {
			String temp = conf.get("dfs.datanode.fbt.partIDDatanode.["+dataNodeID+"]");
			if (temp.equals(machineName)) {
				found=true;
			} else {
				dataNodeID++;
			}
		}
		return dataNodeID;

	}
	/**
	 * <p>���ꤵ�줿�ץ�ѥƥ��ˤ�ä� Directory ����ޤ���
	 * ���ѤǤ�����ץ�ѥƥ��ϰʲ����̤�Ǥ���</p>
	 *
	 * <ul>
	 *   <li>partition-root-page[partition-id] : partition-id�ˤ�����
	 *                                           root index node�Υڡ����ֹ�
	 *   <li>NodeVisitorFactory : NodeVisitorFactory�μ������饹̾
	 *   <li>partition : root index node �ν��ʬ�䶭����
	 *                   partition-id�ȶ����ͤ�"-"�Ƕ��ڤäƸ�ߤ��¤٤롣
	 *   <li>key-length : ������Ĺ����
	 * </ul>
	 *
	 * @param prop ���ץ�ѥƥ�
	 * @throws Exception
	 */
    public void init(Configuration conf) {
		System.err.println("Initialize in Directory ");

		try {
			setRequestFactory (
					"org.apache.hadoop.hdfs.server.namenodeFBT." +
					"request.FBTLCFBRequestFactory");
			setResponseClassFactory(
					"org.apache.hadoop.hdfs.server.namenodeFBT." +
					"response.FBTLCFBResponseClassFactory");
					//"blink.rule.FBLTBLinkResponseClassFactory");
			setNodeVisitorFactory(
						//"org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitorFactory");
						"org.apache.hadoop.hdfs.server.namenodeFBT.FBTMarkOptNoCouplingNodeVisitorFactory");
						//"org.apache.hadoop.hdfs.server.namenodeFBT." +
						//"blink.FBLTLcfblNodeVisitorFactory");


		} catch (Exception e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
				//"org.apache.hadoop.hdfs.server.namenodeFBT." +
				//"blink.rule.FBLTBLinkRequestFactory");

	}




    protected void getPartIDAndRegisterMapping(List<EndPoint> endpoints)
    			throws Exception {
    	InetAddress address = InetAddress.getLocalHost();

    	Iterator<EndPoint> eIter = endpoints.iterator();

    	while (eIter.hasNext()) {
		    EndPoint ep = eIter.next();

		    int partID = 0xff & ep.getInetAddress().getAddress()[3];
		    NameNodeFBTProcessor.bind("/mapping/"+ partID, ep);
    	}
    }
    public void setNodeVisitorFactory(String visitorFactoryName) throws Exception {

    	Class<?> visitorFactoryClass = Class.forName(visitorFactoryName);
    	Constructor<?> constructor =
    			visitorFactoryClass.getConstructor(new Class[]{ FBTDirectory.class });
    	_visitorFactory =
        (NodeVisitorFactory) constructor.newInstance(new Object[]{ this });
    }



    public int getPartitionID() {
		return _meta.getPartitionID();
	}

    public int getOwnerID() {
    	return _meta.getOwnerID();
    }

	public int getFanout() {
		return _meta.getFanout();
	}

	public boolean isLocal(VPointer vPointer) {
		//StringUtility.debugSpace("FBTDirectory.isLocal? vp: "+vPointer);
		return vPointer != null &&
				vPointer.getPointer(_meta.getPartitionID()) != null;

	}

	public boolean isLocalDirectory(VPointer vPointer) {
		//StringUtility.debugSpace("FBTDirectory.isLocalDirectory? vp: "+vPointer);
		String owner = vPointer.getPointer().getNodeID().substring(0, 5);
		return vPointer!=null &&
				(owner.equals(getOwner()));

	}

	public void incrementEntryCount() {
		_meta.incrementEntryCount();
	}


	public Node getNode(VPointer vPointer) {
		//StringUtility.debugSpace("FBTDirectory.getNode vp: "+vPointer);
		Pointer p = vPointer.getPointer(_meta.getPartitionID());
		if (p!=null) {
			Map<String, Node> nodeMapping = getLocalNodeMapping(p);
			synchronized (nodeMapping) {
				Node node = (Node) nodeMapping.get(p.getNodeID());
				System.out.println("got node: "+node.getNodeID());
				if (node!=null) {
					node.initOwner(this);
					return node;
				}
			}
		}
		return null;
	}

	public Map<String, Node> getLocalNodeMapping(Pointer p) {
		//StringUtility.debugSpace("FBTDirectory.getLocalNodeMapping p "+p);

		String owner = p.getNodeID().split("_")[0];
		return (Map<String, Node>)
				((100+Integer.parseInt(owner.substring(owner.length()-2)) == getPartitionID()) ?
				getLocalNodeMapping() :
				(((FBTDirectory) NameNodeFBTProcessor.lookup(
									"/directory."+owner)).
					getLocalNodeMapping()));
	}

	public Map<String, Node> getLocalNodeMapping() {
		return localNodeMapping;
	}

	public void setLocalNodeMapping(Map<String, Node> localNodeMapping) {
		this.localNodeMapping = localNodeMapping;
	}
	public EndPoint getMapping(int partitionID) {
		String mappingURL = "/mapping/"+partitionID;
        return (EndPoint) NameNodeFBTProcessor.lookup(mappingURL);
    }


	public int getLeafFanout() {
		return _meta.get_leafFanout();
	}


	public void decrementEntryCount() {
		_meta.decrementEntryCount();

	}

	public void setDummyLeaf(VPointer dummyLeaf) {
		((MetaNode) _meta).set_dummyLeaf(dummyLeaf);
	}

	public void incrementRestartCount() {
	    ((MetaNode) _meta).incrementRestartCount();
	}

	public void incrementMoreRestartCount() {
	    (_meta).incrementMoreRestartCount();
	}

    public void incrementRedirectCount() {
        ( _meta).incrementRedirectCount();
    }

	public void resetRestartCount() {
	    (_meta).resetRestartCount();
	}

	public void resetMoreRestartCount() {
	    (_meta).resetMoreRestartCount();
	}

    public void resetRedirectCount() {
        (_meta).resetRedirectCount();
    }

	public int getRestartCount() {
	    return (_meta).getRestartCount();
	}

	public int getMoreRestartCount() {
	    return (_meta).getMoreRestartCount();
	}

    public int getRedirectCount() {
        return (_meta).getRedirectCount();
    }

	public String getVisitorFactoryName() {
	    return _visitorFactory.getClass().getName();
	}

	public void incrementLockCount(int count) {
	    (_meta).incrementLockCount(count);
	}

	public void resetLockCount() {
	    (_meta).resetLockCount();
	}

	public void logLockCount() {
	    (_meta).logLockCount();
	}

    public void incrementCorrectingCount() {
        (_meta).incrementCorrectingCount();
    }


    public void incrementChaseCount() {
        (_meta).incrementChaseCount();
    }

    public int getEntryCount() {
        return _meta.getEntryCount();
    }
    public int getMaxFilePerDirectory() {
    	return DEFAULT_MAX_FILES_PER_DIRECTORY;
    }
    public boolean getAccessCountFlg() {
	    return ((MetaNode) _meta).getAccessCountFlg();
	}


    public int getAccesccCount() {
	    return ((MetaNode) _meta).getAccessCount();
	}

	public void setAccessCountFlg(boolean accessCountFlg) {
	    ((MetaNode) _meta).setAccessCountFlg(accessCountFlg);
	}

	public void incrementAccessCount() {
	    ((MetaNode) _meta).incrementAccessCount();
	}

	public void incrementAccessCount(int count) {
	    ((MetaNode) _meta).incrementAccessCount(count);
	}

	public void decrementAccessCount(int count) {
	    ((MetaNode) _meta).decrementAccessCount(count);
	}

	public void resetAccessCount() {
	    ((MetaNode) _meta).resetAccessCount();
	}

	public NodeVisitorFactory getNodeVisitorFactory() {
        return _visitorFactory;
    }

	public void setTreeLockerPartID(String treeLockerPartID) {
        (_meta).setTreeLockerPartID(treeLockerPartID);
    }

	public int getTreeHeight() {
        return ( _meta).getTreeHeight();
    }

    public void setTreeHeight(int treeHeight) {
        (_meta).setTreeHeight(treeHeight);
    }

    public void incrementTreeHeight() {
        (_meta).getTreeHeight();
    }

    public int getLeftPartitionID() {
	    return (_meta).getLeftPartitionID();
	}

	public int getRightPartitionID() {
	    return (_meta).getRightPartitionID();
	}

	  public int getNonLoopLeftPartitionID() {
	        return (_meta).getNonLoopLeftPartitionID();
	    }

	    public int getNonLoopRightPartitionID() {
	        return (_meta).getNonLoopRightPartitionID();
	    }

    public List<Pointer> getDummies() {
        return (_meta).getDummies();
    }

    public void addDummy(Pointer dummy) {
        (_meta).addDummy(dummy);
    }

    public void replaceDummy(Pointer oldDummmy, Pointer newDummy) {
        (_meta).replaceDummy(oldDummmy, newDummy);
    }
	public Node createMetaNode(int partitionID, int ownerID, int fanout, int leafFanout) {
        return new MetaNode(this, partitionID, ownerID, fanout, leafFanout);
    }

    public Node createIndexNode() {
        return new IndexNode(this);
    }

    public Node createLeafNode() {
        return new LeafNode(this);
    }

    public Node createPointerNode() {
        return new PointerNode(this);
    }

	protected FBTInitializeVisitor createInitializeVisitor() {
        return new FBTInitializeVisitor(this);
    }
	public String normalizePath(String src) {
		if (src.length() > 1 && src.endsWith("/")) {
		      src = src.substring(0, src.length() - 1);
		    }
	    return src;
	}
	//Directory operation
    //key = directory's path
    private String getKeyFromDirectorySrc (String src) {
    	return normalizePath(src);
    }
    //File operations
    private String getKeyFromFileSrc (String src) {
    	if (src == null) {
			return null;
		}
		if (!src.startsWith("/")) {
			src = "/".concat(src);
		}
		int idx = src.lastIndexOf("/");
		return src.substring(0, idx+1);
    }

    //File operations
    private String getFileNameFromFileSrc (String src) {
    	if (src == null) {
    		return null;
    	}
    	int idx = src.lastIndexOf("/");
    	return src.substring(idx+1, src.length());
    }


	public void startFile(String src, PermissionStatus permissions,
            String holder, String clientMachine,
            boolean overwrite, short replication, long blockSize) throws
            IOException, MessageException, ServiceException {
		startFileInternal(src, permissions, holder, clientMachine, overwrite, false,
                replication, blockSize);
		//dir.fsImage.getEditLog().logSync();
		if (auditLog.isInfoEnabled()) {
			final FileStatus stat = getFileInfo(src);
			logAuditEvent(UserGroupInformation.getCurrentUGI(),
              Server.getRemoteIp(),
              "create", src, null, stat);
		}
	}

	private
	//synchronized
	void startFileInternal(String src, PermissionStatus permissions,
			String holder, String clientMachine, boolean overwrite, boolean append,
			short replication, long blockSize)
		//throws IOException, MessageException, ServiceException {
	{
		StringUtility.debugSpace("startFileInternal "+src);

		if (NameNode.stateChangeLog.isDebugEnabled()) {
		      NameNode.stateChangeLog.debug("DIR* NameSystem.startFile: src=" + src
		          + ", holder=" + holder
		          + ", clientMachine=" + clientMachine
		          + ", replication=" + replication
		          + ", overwrite=" + overwrite
		          + ", append=" + append);
		}
		if (isInSafeMode())
			try {
				throw new SafeModeException("Cannot create file" + src, safeMode);
			} catch (SafeModeException e1) {
				// TODO ��ư�������줿 catch �֥�å�
				e1.printStackTrace();
			}
		if (!DFSUtil.isValidName(src)) {
			try {
				throw new IOException("Invalid file name: " + src);
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
		}
		SearchResponse searchResponse;
		try {

			searchResponse = searchResponse(src);

			// Verify that the destination does not exist as a directory already.
			boolean pathExists = existed(searchResponse, src);
			if (pathExists && isDir(searchResponse, src)) {
				try {
					throw new IOException("Cannot create file "+ src + "; already exists as a directory.");
				} catch (IOException e) {
					// TODO ��ư�������줿 catch �֥�å�
					e.printStackTrace();
				}
			}

		if (isPermissionEnabled) {
			if (append || (overwrite && pathExists)) {
				try {
					checkPathAccess(src, FsAction.WRITE);
				} catch (AccessControlException e) {
					// TODO ��ư�������줿 catch �֥�å�
					e.printStackTrace();
				}
			}
			else {
				try {
					checkAncestorAccess(src, FsAction.WRITE);
				} catch (AccessControlException e) {
					// TODO ��ư�������줿 catch �֥�å�
					e.printStackTrace();
				}
			}
		}


		INode myFile = getFileINode(searchResponse, src);

		if (myFile != null && myFile.isUnderConstruction()) {
			System.out.println("myFile "+myFile.toString());
		}

		try {
			verifyReplication(src, replication, clientMachine);
		} catch(IOException e) {
			try {
				e.printStackTrace();
				throw new IOException("failed to create "+e.getMessage());
			} catch (IOException e1) {
				// TODO ��ư�������줿 catch �֥�å�
				e1.printStackTrace();
			}
		}

		if (append) {
			if (myFile == null) {
		          throw new FileNotFoundException("failed to append to non-existent file "
		              + src + " on client " + clientMachine);
			} else if (myFile.isDirectory()) {
				throw new IOException("failed to append to directory " + src
		                                +" on client " + clientMachine);
			}
		} else if (!isValidToCreate(searchResponse, src)) {
			if (overwrite) {
				delete(searchResponse, src, true);
			} else {
				throw new IOException("failed to create file " + src
		                                +" on client " + clientMachine
		                                +" either because the filename is invalid or the file exists");
			}
		}
		//System.out.println("Line 884 clientMachine "+clientMachine);
		//System.out.println(host2DataNodeMap);
		DatanodeDescriptor clientNode =host2DataNodeMap.getDatanodeByHost(clientMachine);
		//System.out.println("Line 885 clientNode "+clientNode.name);
		if (append) {
			//
			// Replace current node with a INodeUnderConstruction.
			// Recreate in-memory lease record.
			//
			INodeFile node = (INodeFile) myFile;

			INodeFileUnderConstruction cons = new INodeFileUnderConstruction(
		                                        node.getLocalNameBytes(),
		                                        node.getReplication(),
		                                        node.getModificationTime(),
		                                        node.getPreferredBlockSize(),
		                                        node.getBlocks(),
		                                        node.getPermissionStatus(),
		                                        holder,
		                                        clientMachine,
		                                        clientNode);

			//TODO replaceNode
		        replaceNode(src, node, cons);
		        //leaseManager.addLease(cons.clientName, src);

		} else {
			System.out.println("Line 745");
		    // Now we can add the name to the filesystem. This file has no
		    // blocks associated with it.
		    //
			//checkFsObjectLimit();

		        // increment global generation stamp
		    long genstamp = nextGenerationStamp();
		    INodeFileUnderConstruction newNode = addFile(src, permissions,
		           replication, blockSize, holder, clientMachine,
		           clientNode, genstamp,
		           false);
		    if (newNode == null) {
		          throw new IOException("DIR* NameSystem.startFile: " +
		                                "Unable to add file to namespace.");
		    }
		    //leaseManager.addLease(newNode.clientName, src);
		    if (NameNode.stateChangeLog.isDebugEnabled()) {
		          NameNode.stateChangeLog.debug("DIR* NameSystem.startFile: "
		                                     +"add "+src+" to namespace for "+holder);
		    }
		}
		} catch (MessageException e1) {
			// TODO ��ư�������줿 catch �֥�å�
			e1.printStackTrace();
		} catch (Exception e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
	}

	INodeFileUnderConstruction addFile(String src,
			PermissionStatus permissions, short replication, long preferredBlockSize,
			String clientName, String clientMachine, DatanodeDescriptor clientNode,
			long generationStamp,
			boolean isDirectory) throws MessageException
		{
		//TODO verifyQuota here, FsEditLog
		//System.out.println("clientNode "+clientNode.getHostName());
		StringUtility.debugSpace("FBTDirectory.addFile "+src);
		//waitForReady();
		// Always do an implicit mkdirs for parent directory tree.
		INode newNode = null;
		try {
			long modTime = now();
		    if (!mkdirs(new Path(src).getParent().toString(), permissions, true,
		        modTime, false)) {
		      return null;
		    }

		    String[] parentNames = INode.getParentPathNames(src);
		    INode[] parentINodes = new INode[parentNames.length];

		    for (int count=0; count<parentNames.length; count++) {
		    	parentINodes[count] = searchResponse(parentNames[count]).getINode();
		    }

		    for (int i=0; i<parentINodes.length && parentINodes[i]!=null; i++) {
				if (!parentINodes[i].isDirectory()) {
					throw new FileNotFoundException("Parent path is not a directory: "+
													parentINodes[i].getPathName());
				}
			}
			INode.DirCounts counts = new INode.DirCounts();
			updateCount(parentINodes, parentINodes.length, counts.getNsCount(),
											counts.getDsCount(), true);

			Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
										("/messenger");
			Call call2 = new Call(messenger);
			String key = normalizePath(src);
			Request request = requestFactory.createInsertRequest
												("/directory."+getNameNodeAddress().getHostName(),
												key, permissions, clientName, clientMachine,
												replication, preferredBlockSize, clientNode, false, false);


			Class responseClass = responseClassFactory.createInsertResponseClass();

			call2.setDestination(messenger.getEndPoint());
			call2.setRequest(request);
			call2.setResponseClass(responseClass);
			InsertResponse response = (InsertResponse) call2.invoke();
			newNode = response.getINode();

			if (newNode == null) {
				NameNode.stateChangeLog.info("DIR* FBTDirectory.addFile: "
		                                   +"failed to add "+src
		                                   +" to the file system");
				return null;
		    }
		    // add create file record to log, record new generation stamp
		    //dir.fsImage.getEditLog().logOpenFile(src, (INodeFileUnderConstruction) newNode);

			System.out.println("src "+src+" was successfully added at "+response.getVPointer());
		    NameNode.stateChangeLog.debug("DIR* FSDirectory.addFile: "
		                                  +src+" is added to the file system");
		} catch (Exception e) {
			e.printStackTrace();
		}

	    return (INodeFileUnderConstruction) newNode;

	}

	public static long now() {
		return System.currentTimeMillis();
	}

	 public synchronized void setTimes(String src, long mtime, long atime) throws IOException, MessageException {
		    if (!isAccessTimeSupported() && atime != -1) {
		      throw new IOException("Access time for hdfs is not configured. " +
		                            " Please set dfs.support.accessTime configuration parameter.");
		    }
		    //
		    // The caller needs to have write access to set access & modification times.
		    if (isPermissionEnabled) {
		      checkPathAccess(src, FsAction.WRITE);
		    }

		    INodeFile inode = getFileINode(searchResponse(src), src);
		    if (inode != null) {
		      dir.setTimes(src, inode, mtime, atime, true);
		      if (auditLog.isInfoEnabled()) {
		        final FileStatus stat = getFileInfo(src);
		        logAuditEvent(UserGroupInformation.getCurrentUGI(),
		                      Server.getRemoteIp(),
		                      "setTimes", src, null, stat);
		      }
		    } else {
		      throw new FileNotFoundException("File " + src + " does not exist.");
		    }
		  }

	 /**
	   * Return the number of nodes that are live and decommissioned.
	   */
	  protected NumberReplicas countNodes(Block b) {
	    return countNodes(b, blocksMap.nodeIterator(b));
	  }
	  /**
	   * Counts the number of nodes in the given list into active and
	   * decommissioned counters.
	   */
	  private NumberReplicas countNodes(Block b,
	                                    Iterator<DatanodeDescriptor> nodeIter) {
	    int count = 0;
	    int live = 0;
	    int corrupt = 0;
	    int excess = 0;
	    Collection<DatanodeDescriptor> nodesCorrupt = corruptReplicas.getNodes(b);
	    while ( nodeIter.hasNext() ) {
	      DatanodeDescriptor node = nodeIter.next();
	      if ((nodesCorrupt != null) && (nodesCorrupt.contains(node))) {
	        corrupt++;
	      }
	      else if (node.isDecommissionInProgress() || node.isDecommissioned()) {
	        count++;
	      }
	      else  {
	        Collection<Block> blocksExcess =
	          excessReplicateMap.get(node.getStorageID());
	        if (blocksExcess != null && blocksExcess.contains(b)) {
	          excess++;
	        } else {
	          live++;
	        }
	      }
	    }
	    return new NumberReplicas(live, count, corrupt, excess);
	  }
	private long nextGenerationStamp() {
		long gs = generationStamp.nextStamp();
	    //getEditLog().logGenerationStamp(gs);
	    return gs;
	}

	private void replaceNode(String path, INodeFile oldnode,
			INodeFile newnode) throws IOException, MessageException {
		replaceNode(path, oldnode, newnode, true);

	}
	/**
	   * @throws MessageException
	 * @see #replaceNode(String, INodeFile, INodeFile)
	   */
	private void replaceNode(String path, INodeFile oldnode, INodeFile newnode,
	                           boolean updateDiskspace) throws IOException,
	                           							MessageException {
		//TODO
	    //synchronized (rootDir) {
	      long dsOld = oldnode.diskspaceConsumed();

	      //
	      // Remove the node from the namespace
	      //
	      if (!deleteResponse(path).isSuccess()) {
	        NameNode.stateChangeLog.warn("DIR* FBTDirectory.replaceNode: " +
	                                     "failed to remove " + path);
	        throw new IOException("FBTDirectory.replaceNode: " +
	                              "failed to remove " + path);
	      }

	      //insertResponse(path, newnode);

	      //check if disk space needs to be updated.
	      long dsNew = 0;
	      if (updateDiskspace && (dsNew = newnode.diskspaceConsumed()) != dsOld) {
	        //try {
	          //updateSpaceConsumed(path, 0, dsNew-dsOld);
	        //} catch (QuotaExceededException e) {
	          // undo
	          //replaceNode(path, newnode, oldnode, false);
	          //throw e;
	        //}
	      }
	      System.out.println("newNode blocks "+newnode.getBlocks().length);
	      int index = 0;
	      for (Block b : newnode.getBlocks()) {
	        BlockInfo info = blocksMap.addINode(b, newnode);
	        newnode.setBlock(index, info); // inode refers to the block in BlocksMap
	        index++;
	      }
	   // }
	  }


	boolean mkdirs(String src, PermissionStatus permissions, boolean inheritPermission,
					long now, boolean isDirectory) throws ServiceException,
					MessageException, IOException {
		StringUtility.debugSpace("FBTDirectory.mkdirs with IsDirectory "+src);

		// TODO ��ư�������줿�᥽�åɡ�������
		//Check exist is implmented at getFileInfoStatus
		//
		// 1. CheckPathLength(src)
		// 3. CheckSafeMode(src)
		// 4. CheckValid(src)
		// 5. CheckAncestorAccess(src, FSAction.Write)
		// 6. CheckFsObjectLimit
		// 7. Mkdirs(src, permissionStatus, inheritPermission, now)
		// 7.1 key=GetKeyFromSrc(src)
		// 7.2 insert(key)

		if (isPermissionEnabled) {
		      //checkTraverse(src);
		}

		if (isDir(src)) {
			return true;
		}
		if (isInSafeMode())
		      throw new SafeModeException("Cannot create directory " + src, safeMode);
		if (!DFSUtil.isValidName(src)) {
			throw new IOException("Invalid directory name: " + src);
		}
		if (isPermissionEnabled) {
			checkAncestorAccess(src, FsAction.WRITE);
		}

		    // validate that we have enough inodes. This is, at best, a
		    // heuristic because the mkdirs() operation migth need to
		    // create multiple inodes.
		checkFsObjectLimit();

		//check and update metadata at parent INode
		String[] parentNames = INode.getParentPathNames(src);
		INode[] parentINodes = new INode[parentNames.length];
		for (int count = 0; count < parentNames.length; count++) {
			parentINodes[count] = searchResponse(parentNames[count]).getINode();
		}

		for (int i=0; i<parentINodes.length && parentINodes[i]!=null; i++) {
			if (!parentINodes[i].isDirectory()) {
				throw new FileNotFoundException("Parent path is not a directory: "+
												parentINodes[i].getPathName());
			}
		}
		INode.DirCounts counts = new INode.DirCounts();
		updateCount(parentINodes, parentINodes.length, counts.getNsCount(),
										counts.getDsCount(), true);
		//insert to FBT
		boolean result=false;

		Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
												("/messenger");
		Call call2 = new Call(messenger);
		String key = normalizePath(src);
		Request request = requestFactory.createInsertRequest
							(key, permissions, true);


		Class responseClass = responseClassFactory.
									createInsertResponseClass();

		call2.setDestination(messenger.getEndPoint());
		call2.setRequest(request);
		call2.setResponseClass(responseClass);
		InsertResponse response = (InsertResponse) call2.invoke();
		result=true;
		if (!result) {
			updateCount(parentINodes, parentINodes.length, -counts.getNsCount(),
					-counts.getDsCount(), true);

		}
		return result;
	}

	/**
	   * Remove the indicated filename from namespace. If the filename
	   * is a directory (non empty) and recursive is set to false then throw exception.
	   */
	    public boolean delete(SearchResponse searchResponse,
	    							String src, boolean recursive) throws IOException {
	      if ((!recursive) && (isDirEmpty(searchResponse, src))) {
	        throw new IOException(src + " is non empty");
	      }
	      boolean status = deleteInternal(src, true);
	      //getEditLog().logSync();
	      //if (status && auditLog.isInfoEnabled()) {
	       // logAuditEvent(UserGroupInformation.getCurrentUGI(),
	       //               Server.getRemoteIp(),
	       //               "delete", src, null, null);
	      //}
	      return status;
	    }

	private boolean isDirEmpty(SearchResponse searchResponse, String src) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	private boolean deleteInternal(String src, boolean b) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}

	public SearchResponse searchResponse(String src)
				throws MessageException {
		//StringUtility.debugSpace("FBTDirectory.searchResponse src "+src);

		Messenger messenger = (Messenger) NameNodeFBTProcessor.
							lookup("/messenger");

		Call call = new Call(messenger);

	    Request checkExistRequest = requestFactory.createSearchRequest(
	    		"/directory."+getNameNodeAddress().getHostName(),
				normalizePath(src));

		Class checkExistResponseClass = responseClassFactory.
									createSearchResponseClass();
		call.setDestination(messenger.getEndPoint());
		call.setRequest(checkExistRequest);
		call.setResponseClass(checkExistResponseClass);
		SearchResponse results = (SearchResponse) call.invoke();

		return results;
	}

	public RangeSearchResponse rangeSearchResponse(String lowSrc, String highSrc) {
		Messenger messenger = (Messenger) NameNodeFBTProcessor.
							lookup("/messenger");
		Call call = new Call(messenger);
		Request rangeSearchRequest = requestFactory.createRangeSearchRequest(
								"/directory."+getNameNodeAddress().getHostName(),
								lowSrc, highSrc);
		Class rangeSearchResponseClass = responseClassFactory.
									createRangeSearchResponseClass();
		call.setDestination(messenger.getEndPoint());
		call.setRequest(rangeSearchRequest);
		call.setResponseClass(rangeSearchResponseClass);
		RangeSearchResponse results = (RangeSearchResponse) call.invoke();

		return results;
	}
	public DeleteResponse deleteResponse(String src) throws MessageException {
		StringUtility.debugSpace("FBTDirectory.deleteResponse src "+src);
		normalizePath(src);

	    Request deleteRequest = requestFactory.createDeleteRequest(
				normalizePath(src));
	    		//src,
	    		//null);
		Messenger messenger = (Messenger) NameNodeFBTProcessor.
										lookup("/messenger");
		Call call = new Call(messenger);
		Class deleteResponseClass = responseClassFactory.createDeleteResponseClass();
		call.setDestination(messenger.getEndPoint());
		call.setRequest(deleteRequest);
		call.setResponseClass(deleteResponseClass);

		return (DeleteResponse) call.invoke();

	}

	private SearchResponse searchResponseDirectoryOperation(String src) throws MessageException {
		StringUtility.debugSpace("FBTDirectory.searchResponse src "+src);
		src = normalizePath(getKeyFromDirectorySrc(src));

	    Request checkExistRequest = requestFactory.createSearchRequest(
				src,
				null);
		Messenger messenger = (Messenger) NameNodeFBTProcessor.
										lookup("/messenger");
		Call call = new Call(messenger);
		Class checkExistResponseClass = responseClassFactory.createSearchResponseClass();
		call.setDestination(messenger.getEndPoint());
		call.setRequest(checkExistRequest);
		call.setResponseClass(checkExistResponseClass);

		return (SearchResponse) call.invoke();

	}


	private InsertResponse insertResponse(String src, INodeFile inode)
										throws MessageException {
		src = normalizePath(src);
		Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
	    Call call = new Call(messenger);
	    //LeafEntry le = new LeafEntry(getFileNameFromFileSrc(src));
		Request request = requestFactory.createInsertRequest(
						src,
						inode,
						true);
		Class responseClass = responseClassFactory.createInsertResponseClass();
		call.setDestination(messenger.getEndPoint());
		call.setRequest(request);
		call.setResponseClass(responseClass);
		return (InsertResponse) call.invoke();

	}
	private boolean existed(SearchResponse searchResponse, String src) {
		StringUtility.debugSpace("FBTDirectory.existed");
		if (searchResponse.getINode()==null) {
			return false;
		}
		return true;
	}

	private boolean existedDirectory(String src) throws MessageException {
		StringUtility.debugSpace("FBTDirectory.existedDirectory");
		SearchResponse searchResponse = searchResponseDirectoryOperation(src);
		if (searchResponse.getValue()==null){
			return false;
		}
		return true;

	}
	private boolean existedDirectory(SearchResponse searchResponse)
									throws MessageException {
		if (searchResponse.getValue()==null){
			return false;
		}
		return true;

	}

	private boolean isDir(SearchResponse searchResponse, String src) {
		if (searchResponse.getINode()==null) {
			return false;
		}
		return (searchResponse.getINode().isDirectory());
	}

	private void setRequestFactory(String name) throws Exception {
		Class<?> requestFactoryClass = Class.forName(name);
		requestFactory = (RequestFactory)
							requestFactoryClass.newInstance();
	}

	private void setResponseClassFactory(String name) throws Exception {
		Class<?> responseClassFactoryClass = Class.forName(name);
		responseClassFactory = (ResponseClassFactory)
							responseClassFactoryClass.newInstance();
	}
	public void accept(NodeVisitor visitor) throws
						NotReplicatedYetException,
						MessageException,
						IOException {
		if (visitor instanceof FBTModifyVisitor) {
			VPointer self = new Pointer(getSelfPartID(),
										_meta.getNodeIdentifier().toString());
			_meta.accept(visitor, self);
		} else {
			_meta.accept(visitor, _meta.getPointer());
		}
    }

	boolean exists(String src)  {
	    return true;
	}

	/**
	   * Check whether the path specifies a directory
	 * @throws MessageException
	   */
	  boolean isDir(String src) throws MessageException {
		  INode inode = searchResponse(normalizePath(src)).getINode();
		  return inode != null && inode.isDirectory();
	  }

	  /**
	   * Get {@link INode} associated with the file.
	   */
	  INodeFile getFileINode(SearchResponse searchResponse, String src) {
		  StringUtility.debugSpace("FBTDirectory.getFileINode "+src);
		  INode inode = searchResponse.getINode();
		  if (inode == null) {
			  return null;
		  }
		  return (INodeFile) inode;
	  }

	  /**
	   * Block until the object is ready to be used.
	   */
	  void waitForReady() {
	    if (!ready) {
	      synchronized (this) {
	        while (!ready) {
	          try {
	            this.wait(5000);
	          } catch (InterruptedException ie) {
	        	  ie.printStackTrace();
	        	  NameNode.LOG.info("FBTDirectory.waitForReady exception()");
	          }
	        }
	      }
	    }
	  }

	  /**
	   * Get the blocks associated with the file.
	   */
	  Block[] getFileBlocks(SearchResponse searchResponse, String src) {
	    waitForReady();
		  INode targetNode = searchResponse.getINode();
		  if (targetNode == null || targetNode.isDirectory()) {
			  return null;
		  }
		  return (((INodeFile) targetNode).getBlocks());
	  }

	  /**
	   * Check that the indicated file's blocks are present and
	   * replicated.  If not, return false. If checkall is true, then check
	   * all blocks, otherwise check only penultimate block.
	   */
	  protected
	  //synchronized
	  boolean checkFileProgress(INodeFile v, boolean checkall) {
		  NameNode.LOG.info("FBTDirectory.checkFileProgress by "+v+ "with "+v.getBlocks().length
				  				+" blocks");
		  synchronized (blocksMap) {
			  if (checkall) {
		      //
		      // check all blocks of the file.
		      //

			      for (Block block: v.getBlocks()) {
			    	  //NameNode.LOG.info(blocksMap.numNodes(block) + " blocks");
			        if (blocksMap.numNodes(block) < this.minReplication) {
			          return false;
			        }
			      }
		    } else {
			      //
			      // check the penultimate block of this file
			      //
			      Block b = v.getPenultimateBlock();
			      if (b != null) {
			        if (blocksMap.numNodes(b) < this.minReplication) {
			          return false;
			        }
		      }
		    }
		  }
	    NameNode.LOG.info("FBTDirectory.checkFileProgress by "+v +" ended");
	    return true;
	  }

	  /**
	   * Check whether the replication parameter is within the range
	   * determined by system configuration.
	   */
	  private void verifyReplication(String src,
	                                 short replication,
	                                 String clientName
	                                 ) throws IOException {
	    String text = "file " + src
	      + ((clientName != null) ? " on client " + clientName : "")
	      + ".\n"
	      + "Requested replication " + replication;

	    if (replication > maxReplication)
	      throw new IOException(text + " exceeds maximum " + maxReplication);

	    if (replication < minReplication)
	      throw new IOException(
	                            text + " is less than the required minimum " + minReplication);
	  }

	  /**
	   * Check whether the filepath could be created
	   */
	  boolean isValidToCreate(SearchResponse searchResponse, String src) {
		  INode inode = searchResponse.getINode();
		  if (src.startsWith("/") &&
		          !src.endsWith("/") && inode == null) {
			  return true;
		  } else {
			  return false;
		  }
	  }
	  /**
	   * Check to see if we have exceeded the limit on the number
	   * of inodes.
	   */
	  void checkFsObjectLimit() throws IOException {
		  //TODO

	    /*if (maxFsObjects != 0 &&
	        maxFsObjects <= dir.totalInodes() + getBlocksTotal()) {
	      throw new IOException("Exceeded the configured number of objects " +
	                             maxFsObjects + " in the filesystem.");
	    }*/
	  }


	/**
	   * Check whether the name node is in safe mode.
	   * @return true if safe mode is ON, false otherwise
	   */
	  boolean isInSafeMode() {
	    if (safeMode == null)
	      return false;
	    return safeMode.isOn();
	  }

	public LocatedBlock appendFile(String src, String holder,
			String clientMachine) throws MessageException,
										IOException,
										ServiceException {
		StringUtility.debugSpace("FBTDirectory.appendFile");
		if (supportAppends == false) {
		      throw new IOException("Append to hdfs not supported." +
		                            " Please refer to dfs.support.append configuration parameter.");
		    }
		    startFileInternal(src, null, holder, clientMachine, false, true,
		                      (short)maxReplication, (long)0);
		    //getEditLog().logSync();

		    //
		    // Create a LocatedBlock object for the last block of the file
		    // to be returned to the client. Return null if the file does not
		    // have a partial block at the end.
		    //
		    LocatedBlock lb = null;

		    //synchronized (this) {
		    SearchResponse searchResponse = searchResponse(src);
		      INodeFileUnderConstruction file = (INodeFileUnderConstruction)
		      							getFileINode(searchResponse, src);

		      Block[] blocks = file.getBlocks();
		      if (blocks != null && blocks.length > 0) {
		        Block last = blocks[blocks.length-1];
		        BlockInfo storedBlock = blocksMap.getStoredBlock(last);
		        if (file.getPreferredBlockSize() > storedBlock.getNumBytes()) {
		          long fileLength = file.computeContentSummary().getLength();
		          DatanodeDescriptor[] targets = new DatanodeDescriptor
		          									[blocksMap.numNodes(last)];
		          Iterator<DatanodeDescriptor> it = blocksMap.nodeIterator(last);
		          for (int i = 0; it != null && it.hasNext(); i++) {
		            targets[i] = it.next();
		          }
		          // remove the replica locations of this block from the blocksMap
		          for (int i = 0; i < targets.length; i++) {
		            targets[i].removeBlock(storedBlock);
		          }
		          // set the locations of the last block in the lease record
		          file.setLastBlock(storedBlock, targets);

		          lb = new LocatedBlock(last, targets, fileLength-storedBlock.getNumBytes());

		          // Remove block from replication queue.
		          //updateNeededReplications(last, 0, 0);

		          // remove this block from the list of pending blocks to be deleted.
		          // This reduces the possibility of triggering HADOOP-1349.
		          //
		          //for(Collection<Block> v : recentInvalidateSets.values()) {
		          //  if (v.remove(last)) {
		          //    pendingDeletionBlocksCount--;
		          //}
		          }
		        }
	//}
			//}
		    if (lb != null) {
		      if (NameNode.stateChangeLog.isDebugEnabled()) {
		        NameNode.stateChangeLog.debug("DIR* NameSystem.appendFile: file "
		            +src+" for "+holder+" at "+clientMachine
		            +" block " + lb.getBlock()
		            +" block size " + lb.getBlock().getNumBytes());
		      }
		    }
		    /*
		    if (auditLog.isInfoEnabled()) {
		      logAuditEvent(UserGroupInformation.getCurrentUGI(),
		                    Server.getRemoteIp(),
		                    "append", src, null, null);
		    }*/
		    return lb;
	}

	public LocatedBlock getAdditionalBlock(String src, String clientName) throws
							IOException, MessageException {
		StringUtility.debugSpace("FBTDirectory.getAdditionalBlock for "+src);

		return getAdditionalBlockResponse(src, clientName).getLocatedBlock();
	}

	public GetAdditionalBlockResponse getAdditionalBlockResponse(String src,
										String clientName) throws MessageException {


		Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
														("/messenger");
		Call call2 = new Call(messenger);
		String key = normalizePath(src);
		Request request = requestFactory.createGetAdditionalBlockRequest
											("/directory."+getNameNodeAddress().getHostName(),
											key, clientName);
		Class responseClass = responseClassFactory.
								createGetAdditionalBlockResponseClass();
		call2.setDestination(messenger.getEndPoint());
		call2.setRequest(request);
		call2.setResponseClass(responseClass);
		GetAdditionalBlockResponse response =
				(GetAdditionalBlockResponse) call2.invoke();
		System.out.println("GetAdditionalBlockResponse "+response.toString());
		return response;

	}
		  /**
	   * The given node is reporting that it received a certain block.
	   */
	  public
	  synchronized
	  void blockReceived(DatanodeID nodeID,
	                                         Block block,
	                                         String delHint
	                                         ) throws IOException {
		  StringUtility.debugSpace("FBTDirectory.blockReceived receive block "+block+" at" +
		  		" datanodeID "+nodeID.name);
		  // NameNode.LOG.info("datanodeMap :"+datanodeMap.values());
		  // NameNode.LOG.info("nodeID :"+nodeID);
	    DatanodeDescriptor node = getDatanode(nodeID);
	    if (node == null) {
	      NameNode.stateChangeLog.warn("BLOCK* FBTDirectory.blockReceived: "
	                                   + block + " is received from an unrecorded node "
	                                   + nodeID.getName());
	      throw new IllegalArgumentException(
	                                         "Unexpected exception.  Got blockReceived message from node "
	                                         + block + ", but there is no info for it");
	    }

	    if (NameNode.stateChangeLog.isDebugEnabled()) {
	      NameNode.stateChangeLog.debug("BLOCK* FBTDirectory.blockReceived: "
	                                    +block+" is received from " + nodeID.getName());
	    }

	    // Check if this datanode should actually be shutdown instead.
	    if (shouldNodeShutdown(node)) {
	      setDatanodeDead(node);
	      throw new DisallowedDatanodeException(node);
	    }
	    // decrement number of blocks scheduled to this datanode.
	    node.decBlocksScheduled();
	    // get the deletion hint node
	    DatanodeDescriptor delHintNode = null;
	    if(delHint!=null && delHint.length()!=0) {
	      delHintNode = datanodeMap.get(delHint);

	      if(delHintNode == null) {
	        NameNode.stateChangeLog.warn("BLOCK* FBTDirectory.blockReceived: "
	            + block
	            + " is expected to be removed from an unrecorded node "
	            + delHint);
	      }
	    }
	    //
	    // Modify the blocks->datanode map and node's map.
	    //
	    pendingReplications.remove(block);
	    addStoredBlock(block, node, delHintNode);
	  }

	  /**
	   * Get data node by storage ID.
	   *
	   * @param nodeID
	   * @return DatanodeDescriptor or null if the node is not found.
	   * @throws IOException
	   */
	  public DatanodeDescriptor getDatanode(DatanodeID nodeID) throws IOException {
		  //NameNode.LOG.info("FBTDirectory.getDatanode "+nodeID.getName());
	    UnregisteredDatanodeException e = null;
	    //NameNode.LOG.info("FBTDirectory.getDatanode datanodeMap "+datanodeMap.values());
	    DatanodeDescriptor node = datanodeMap.get(nodeID.getStorageID());
	    if (node == null)
	      return null;
	    if (!node.getName().equals(nodeID.getName())) {
	      /*e = new UnregisteredDatanodeException(nodeID, node);
	      NameNode.stateChangeLog.fatal("BLOCK* FBTDirectory.getDatanode: "
	                                    + e.getLocalizedMessage());
	      throw e;*/
	    }
	    return node;
	  }
	  /**
	   * Modify (block-->datanode) map.  Remove block from set of
	   * needed replications if this takes care of the problem.
	   * @return the block that is stored in blockMap.
	   */
	  synchronized
	  Block addStoredBlock(Block block,
	                        final DatanodeDescriptor node,
	                        final DatanodeDescriptor delNodeHint) {
	    synchronized (blocksMap) {
	    	BlockInfo storedBlock = blocksMap.getStoredBlock(block);

	    	if(storedBlock == null || storedBlock.getINode() == null) {
		      // If this block does not belong to any file, then we are done.
		      NameNode.stateChangeLog.info("BLOCK* FBTDirectory.addStoredBlock: "
		                                   + "addStoredBlock request received for "
		                                   + block + " on " + node.getName()
		                                   + " size " + block.getNumBytes()
		                                   + " But it does not belong to any file.");
		      // we could add this block to invalidate set of this datanode.
		      // it will happen in next block report otherwise.


		      //NameNode.LOG.info(" line 1757 addStoredBlock finished for "+block);
		      return block;
		    }

		    // add block to the data-node
		    boolean added = node.addBlock(storedBlock);

		    assert storedBlock != null : "Block must be stored by now";

		    if (block != storedBlock) {
		      if (block.getNumBytes() >= 0) {
		        long cursize = storedBlock.getNumBytes();
		        if (cursize == 0) {
		          storedBlock.setNumBytes(block.getNumBytes());
		        } else if (cursize != block.getNumBytes()) {
		          LOG.warn("Inconsistent size for block " + block +
		                   " reported from " + node.getName() +
		                   " current size is " + cursize +
		                   " reported size is " + block.getNumBytes());
		          try {
		            if (cursize > block.getNumBytes()) {
		              // new replica is smaller in size than existing block.
		              // Mark the new replica as corrupt.
		              LOG.warn("Mark new replica " + block + " from " + node.getName() +
		                  "as corrupt because its length is shorter than existing ones");
		              markBlockAsCorrupt(block, node);
		            } else {
		              // new replica is larger in size than existing block.
		              // Mark pre-existing replicas as corrupt.
		              int numNodes = blocksMap.numNodes(block);
		              int count = 0;
		              DatanodeDescriptor nodes[] = new DatanodeDescriptor[numNodes];
		              Iterator<DatanodeDescriptor> it = blocksMap.nodeIterator(block);
		              for (; it != null && it.hasNext(); ) {
		                DatanodeDescriptor dd = it.next();
		                if (!dd.equals(node)) {
		                  nodes[count++] = dd;
		                }
		              }
		              for (int j = 0; j < count; j++) {
		                LOG.warn("Mark existing replica " + block + " from " + node.getName() +
		                " as corrupt because its length is shorter than the new one");
		                markBlockAsCorrupt(block, nodes[j]);
		              }
		              //
		              // change the size of block in blocksMap
		              //
		              storedBlock = blocksMap.getStoredBlock(block); //extra look up!
		              if (storedBlock == null) {
		                LOG.warn("Block " + block +
		                   " reported from " + node.getName() +
		                   " does not exist in blockMap. Surprise! Surprise!");
		              } else {
		                storedBlock.setNumBytes(block.getNumBytes());
		              }
		            }
		          } catch (IOException e) {
		        	  e.printStackTrace();
		            LOG.warn("Error in deleting bad block " + block + e);
		            LOG.info("Error in deleting bad block " + block + e);
		          }
		        }

		        //Updated space consumed if required.
		        INodeFile file = (storedBlock != null) ? storedBlock.getINode() : null;
		        long diff = (file == null) ? 0 :
		                    (file.getPreferredBlockSize() - storedBlock.getNumBytes());

		        if (diff > 0 && file.isUnderConstruction() &&
		            cursize < storedBlock.getNumBytes()) {
		          /*try {
		            String path =  For finding parents
		             leaseManager.findPath((INodeFileUnderConstruction)file);
		            //TODO
		            dir.updateSpaceConsumed(path, 0, -diff*file.getReplication());
		          } catch (IOException e) {
		            LOG.warn("Unexpected exception while updating disk space : " +
		                     e.getMessage());
		          }*/
		        }
		      }
		      block = storedBlock;
		    }
		    assert storedBlock == block : "Block must be stored by now";

		    int curReplicaDelta = 0;

		    if (added) {
		      curReplicaDelta = 1;
		      //
		      // At startup time, because too many new blocks come in
		      // they take up lots of space in the log file.
		      // So, we log only when namenode is out of safemode.
		      //
		      if (!isInSafeMode()) {
		        NameNode.stateChangeLog.info("BLOCK* FBTDirectory.addStoredBlock: "
		                                      +"blockMap updated: "+node.getName()+" is added to "+block+" size "+block.getNumBytes());
		      }
		    } else {
		      NameNode.stateChangeLog.warn("BLOCK* FBTDirectorym.addStoredBlock: "
		                                   + "Redundant addStoredBlock request received for "
		                                   + block + " on " + node.getName()
		                                   + " size " + block.getNumBytes());
		    }

		    // filter out containingNodes that are marked for decommission.
		    //NumberReplicas num = countNodes(storedBlock);
		    //int numLiveReplicas = num.liveReplicas();
		    //int numCurrentReplica = numLiveReplicas
		    //  + pendingReplications.getNumReplicas(block);

		    // check whether safe replication is reached for the block
		    //incrementSafeBlockCount(numCurrentReplica);

		    //
		    // if file is being actively written to, then do not check
		    // replication-factor here. It will be checked when the file is closed.
		    //
		    INodeFile fileINode = null;
		    fileINode = storedBlock.getINode();
		    if (fileINode.isUnderConstruction()) {
		      return block;
		    }

		    // do not handle mis-replicated blocks during startup
		    if(isInSafeMode()) {
		    	NameNode.LOG.info(" line 1882 addStoredBlock finished for "+block);
		      return block;
		    }

		    // handle underReplication/overReplication
		    /*short fileReplication = fileINode.getReplication();
		    if (numCurrentReplica >= fileReplication) {
		      neededReplications.remove(block, numCurrentReplica,
		                                num.decommissionedReplicas, fileReplication);
		    } else {
		      updateNeededReplications(block, curReplicaDelta, 0);
		    }
		    if (numCurrentReplica > fileReplication) {
		      processOverReplicatedBlock(block, fileReplication, node, delNodeHint);
		    }
		    // If the file replication has reached desired value
		    // we can remove any corrupt replicas the block may have
		    int corruptReplicasCount = corruptReplicas.numCorruptReplicas(block);
		    int numCorruptNodes = num.corruptReplicas();
		    if ( numCorruptNodes != corruptReplicasCount) {
		      LOG.warn("Inconsistent number of corrupt replicas for " +
		          block + "blockMap has " + numCorruptNodes +
		          " but corrupt replicas map has " + corruptReplicasCount);
		    }
		    if ((corruptReplicasCount > 0) && (numLiveReplicas >= fileReplication))
		      invalidateCorruptReplicas(block);
	*/
	    }
	    NameNode.LOG.info(" line 1904 addStoredBlock finished for "+block);

	return block;

	  }
	  public CompleteFileStatus completeFile(String src, String holder) {
		  CompleteFileStatus status = null;
		  try {
			  status = completeFileInternal(src, holder);
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return status;
	  }

	  //TODO: should be recoded. It should be done at suitable namenode that have
	  //correct blocksMap information
	  private
	  //synchronized
	  CompleteFileStatus completeFileInternal(String src,
		                                        String holder) throws IOException, MessageException {
		  NameNode.stateChangeLog.info("DIR* FBTDirectory.completeFile: " +
		    								src + " for " + holder +" start");
		  Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
			("/messenger");
		  Call call2 = new Call(messenger);
		  Request request = requestFactory.createCompleteFileRequest(
				  "/directory."+getNameNodeAddress().getHostName(),
				  src, holder);
			Class responseClass = responseClassFactory.
								createCompleteFileResponseClass();
			call2.setDestination(messenger.getEndPoint());
			call2.setRequest(request);
			call2.setResponseClass(responseClass);
			CompleteFileResponse response =
				(CompleteFileResponse) call2.invoke();
			System.out.println("CompleteFileResponse "+response.toString());
			  return response.getCompleteFileStatus();
	  }

	    		  void finalizeINodeFileUnderConstruction(String src,
			  INodeFileUnderConstruction pendingFile)
	  				throws IOException, MessageException {
		  //TODO replaceNode
		  //leaseManager.removeLease(pendingFile.clientName, src);

		  // The file is no longer pending.
		  // Create permanent INode, update blockmap
		  INodeFile newFile = pendingFile.convertToInodeFile();
		  //replaceNode(src, pendingFile, newFile);

		  // close file and persist block allocations for this file
		  //closeFile(src, newFile);

		  checkReplicationFactor(newFile);
	  }

	  /**
	   * Close file.
	   */
	  void closeFile(String path, INodeFile file) throws IOException {
	    //waitForReady();
	    //synchronized (rootDir) {
	      // file is closed
	      //dir.getFSImage().getEditLog().logCloseFile(path, file);
	      if (NameNode.stateChangeLog.isDebugEnabled()) {
	        NameNode.stateChangeLog.debug("DIR* FSDirectory.closeFile: "
	                                    +path+" with "+ file.getBlocks().length
	                                    +" blocks is persisted to the file system");
	      //}
	    }
	  }

	  LocatedBlocks getBlockLocations(String clientMachine, String src,
		      long offset, long length) throws IOException, MessageException {

		  //StringUtility.debugSpace("FBTDirectory.getBlockLocations");
		    if (isPermissionEnabled) {
		      //checkPathAccess(src, FsAction.READ);
		    }

		    LocatedBlocks blocks = getBlockLocations(src, offset, length, true);
		    if (blocks != null) {
		      //sort the blocks
		      DatanodeDescriptor client = host2DataNodeMap.getDatanodeByHost(
		          clientMachine);
		      /*for (LocatedBlock b : blocks.getLocatedBlocks()) {
		        clusterMap.pseudoSortByDistance(client, b.getLocations());
		      }*/
		      int lbSize = blocks.getLocatedBlocks().size();

		      System.out.println("getBlockLocations end");

		      for (int i=0; i<lbSize; i++) {
		    	  System.out.println("blocks "+i+"-th");
		    	  clusterMap.pseudoSortByDistance(client, blocks.getLocatedBlocks().get(i).getLocations());
		      }
		    }
		    System.out.println("sort by distance end");
		    return blocks;
		  }

	  public LocatedBlocks getBlockLocations(String src, long offset, long length,
		      boolean doAccessTime) throws IOException, MessageException {

		    if (offset < 0) {
		      throw new IOException("Negative offset is not supported. File: " + src );
		    }
		    if (length < 0) {
		      throw new IOException("Negative length is not supported. File: " + src );
		    }
		    final LocatedBlocks ret = getBlockLocationsInternal(src,
		        offset, length, Integer.MAX_VALUE, doAccessTime);
		    if (auditLog.isInfoEnabled()) {
		    			logAuditEvent(UserGroupInformation.getCurrentUGI(),
		    					Server.getRemoteIp(),
		    					"open", src, null, null);
		    }
		    return ret;
		  }

	  private LocatedBlocks getBlockLocationsInternal(String src,
              long offset,
              long length,
              int nrBlocksToReturn,
              boolean doAccessTime)
              throws IOException, MessageException {
		  StringUtility.debugSpace("FBTDirectory.getBlockLocationsInternal "+src);
		  Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
		  										("/messenger");
			Call call2 = new Call(messenger);
			String key = normalizePath(src);
			Request request = requestFactory.createGetBlockLocationsRequest
					("/directory."+getNameNodeAddress().getHostName(),
					key, offset, length, nrBlocksToReturn, doAccessTime);
			Class responseClass = responseClassFactory.
						createGetBlockLocationsResponseClass();

			call2.setDestination(messenger.getEndPoint());
			call2.setRequest(request);
			call2.setResponseClass(responseClass);
			GetBlockLocationsResponse response =
					(GetBlockLocationsResponse) call2.invoke();
			System.out.println("GetBlockLocationsResponse "+response.toString());
			return response.getLocatedBlocks();
	  }

	/**
	   * Returns whether the given block is one pointed-to by a file.
	   */
	  public boolean isValidBlock(Block b) {
	    return (blocksMap.getINode(b) != null);
	  }
	  /**
	   * Gets the generation stamp for this filesystem
	   */
	  public long getGenerationStamp() {
	    return generationStamp.getStamp();
	  }

	public FileStatus getFileInfo(String src) throws MessageException {
		StringUtility.debugSpace("FBTDirectory.getFileInfo "+src);
		SearchResponse searchResponse = searchResponse(src);
		System.out.println("Inode : "+ searchResponse.getINode());
		if (searchResponse == null || searchResponse.getINode() == null) {
			return null;
		}
		INode inode = searchResponse.getINode();
		return createFileStatus(src, inode);
	}
	public FileStatus getFileInfo(String src, boolean isDirectory) throws
								MessageException {
		return getFileInfo(src);
	}
	/**
	   * Create FileStatus by file INode
	   */
	   private static FileStatus createFileStatus(String path, INode node) {
	    // length is zero for directories
		   StringUtility.debugSpace("FBTDirectory.createFileStatus");
	    return new FileStatus(node.isDirectory() ? 0 : node.computeContentSummary().getLength(),
	        node.isDirectory(),
	        node.isDirectory() ? 0 : ((INodeFile)node).getReplication(),
	        node.isDirectory() ? 0 : ((INodeFile)node).getPreferredBlockSize(),
	        node.getModificationTime(),
	        node.getAccessTime(),
	        node.getFsPermission(),
	        node.getUserName(),
	        node.getGroupName(),
	        new Path(path));
	  }



	   /** update count of each inode with quota
	   *
	   * @param inodes an array of inodes on a path
	   * @param numOfINodes the number of inodes to update starting from index 0
	   * @param nsDelta the delta change of namespace
	   * @param dsDelta the delta change of diskspace
	   * @param checkQuota if true then check if quota is exceeded
	   * @throws QuotaExceededException if the new count violates any quota limit
	   */
	  private void updateCount(INode[] inodes, int numOfINodes,
	                           long nsDelta, long dsDelta, boolean checkQuota)
	                           throws QuotaExceededException {
	    if (!ready) {
	      //still intializing. do not check or update quotas.
	      return;
	    }
	    if (numOfINodes>inodes.length) {
	      numOfINodes = inodes.length;
	    }
	    if (checkQuota) {
	      verifyQuota(inodes, numOfINodes, nsDelta, dsDelta, null);
	    }
	    for(int i = 0; i < numOfINodes; i++) {
	      if (inodes[i].isQuotaSet()) { // a directory with quota
	        INodeDirectoryWithQuota node =(INodeDirectoryWithQuota)inodes[i];
	        node.updateNumItemsInTree(nsDelta, dsDelta);
	      }
	    }
	  }
	  /**
	   * Verify quota for adding or moving a new INode with required
	   * namespace and diskspace to a given position.
	   *
	   * @param inodes INodes corresponding to a path
	   * @param pos position where a new INode will be added
	   * @param nsDelta needed namespace
	   * @param dsDelta needed diskspace
	   * @param commonAncestor Last node in inodes array that is a common ancestor
	   *          for a INode that is being moved from one location to the other.
	   *          Pass null if a node is not being moved.
	   * @throws QuotaExceededException if quota limit is exceeded.
	   */
	  private void verifyQuota(INode[] inodes, int pos, long nsDelta, long dsDelta,
	      INode commonAncestor) throws QuotaExceededException {
	    if (!ready) {
	      // Do not check quota if edits log is still being processed
	      return;
	    }
	    if (pos>inodes.length) {
	      pos = inodes.length;
	    }
	    int i = pos - 1;
	    try {
	      // check existing components in the path
	      for(; i >= 0; i--) {
	        if (commonAncestor == inodes[i]) {
	          // Moving an existing node. Stop checking for quota when common
	          // ancestor is reached
	          return;
	        }
	        if (inodes[i].isQuotaSet()) { // a directory with quota
	          INodeDirectoryWithQuota node =(INodeDirectoryWithQuota)inodes[i];
	          node.verifyQuota(nsDelta, dsDelta);
	        }
	      }
	    } catch (QuotaExceededException e) {
	    	e.setPathName(inodes[i].getPathName());
	      throw e;
	    }
	  }

	  //DatanodeProtocol

	  public NamespaceInfo getNamespaceInfo() {
		    return new NamespaceInfo(dir.getFSImage().getNamespaceID(),
		                             dir.getFSImage().getCTime(),
		                             getDistributedUpgradeVersion());
	  }

	  /**
	   * Get registrationID for datanodes based on the namespaceID.
	   *
	   * @see #registerDatanode(DatanodeRegistration)
	   * @see FSImage#newNamespaceID()
	   * @return registration ID
	   */
	  public String getRegistrationID() {
	    return Storage.getRegistrationID(dir.getFSImage());
	  }

	  public MetaNode getMetaNode() {
		  return _meta;
	  }
	public boolean hasBackup() {
		// TODO ��ư�������줿�᥽�åɡ�������
		return false;
	}
	/**
	 * name: fbt.location.edn13
	 * value:edn13,edn14
	 * partitionID=100+machine's name
	 * */
	private static final TreeMap<String, String[]> FBTDirectoryReplicationMapping =
				new TreeMap<String, String[]>();



	public TreeMap<String, String[]> getFBTDirectoryReplicationMapping() {
		return FBTDirectoryReplicationMapping;
	}

	public void intializeFBTDirectoryReplicationMapping (Configuration conf) {

		for (String fsNameNodeFBT : NameNodeFBT.getFsNameNodeFBTs()) {
			String[] locations = conf.getStrings("fbt.location."+fsNameNodeFBT);
			FBTDirectoryReplicationMapping.put(fsNameNodeFBT, locations);
		}
	}

	public void resetPointerAtRoot(Configuration conf) {
		StringUtility.debugSpace("FBTDirectory.resetPointerAtRoot");
		IndexNode root = (IndexNode) getNode(_meta.getRootPointer());

		for (VPointer vp:root.get_children()) {
			String fsNameNodeFBT = "edn"+(vp.getPointer().getPartitionID()-100);
			System.out.println("vp "+vp);
			System.out.println("fsNameNodeFBT: "+fsNameNodeFBT);
			String[] locations = FBTDirectoryReplicationMapping.get(fsNameNodeFBT);
			try {
				if (!locations[1].equals("null")) {//no backup, only primary
					vp.getPointer().setPartitionID(
							100+Integer.parseInt(locations[1].substring(
									locations[1].length()-2)));
					System.out.println("new vp "+vp);
				}
			} catch (NullPointerException ne){
				System.out.println(ne.getMessage() + " no backup location identified");
			}
		}

		System.out.println("after reset pointers at root of "+getOwner());
		root.key2String();
	}

	// Modify FBTDirectory

	public void modify(FBTDirectory directory) throws UnknownHostException {
		StringUtility.debugSpace("FBTDirectory.modify()");
		modify(_meta);
		System.out.println("meta node "+_meta);
		FBTModifyVisitor visitor = new FBTModifyVisitor(directory);
		visitor.run();
	}

	private void modify(MetaNode meta) throws UnknownHostException {
		meta.setPartitionID(getSelfPartID());
		meta.getRootPointer().getPointer().setPartitionID(getSelfPartID());
		meta.getPointerEntry().getPointer().setPartitionID(getSelfPartID());
		meta.get_dummyLeaf().getPointer().setPartitionID(getSelfPartID());
	}
	/**
	 * After transfer FBTDirectory of targetName, modify pointers refer to
	 * that FBTDirectory.
	 * */
	public void modifyAfterTransfer(String targetName) {
		//StringUtility.debugSpace("FBTDirectory.modifyAfterTransfer "+targetName);
		FBTModifyVisitor visitor = new FBTModifyVisitor(this);
		visitor.run();
	}

	public BlocksMap getBlocksMap() {
		return blocksMap;
	}

	public void setNodeVisitorFactory(NodeVisitorFactory nvf) {
		_visitorFactory = nvf;
	}

	public void setFBTDirectory(FBTDirectory fbt) {
		this.fbtDirectoryObject = fbt;
	}

	public FSDirectory getFSDirectory() {
		return this.dir;
	}

	public void setFSDirectory(FSDirectory dir) {
		this.dir = dir;
	}

	public Map<String, String[]> getWriteOffLoadingRangeMap() {
		return this.WriteOffLoadingRangeMap;
	}

	public void setWriteOffLoadingRangeMap(Map<String, String[]> wolrm) {
		this.WriteOffLoadingRangeMap = wolrm;
	}
	public void write(DataOutput out) throws IOException {
		StringUtility.debugSpace("FBTDirectory.write");
		_meta.write(out);

	}
	public void readFields(DataInput in) throws IOException {
		StringUtility.debugSpace("FBTDirectory.read");
		_meta = new MetaNode();
		_meta.readFields(in);
	}

}
