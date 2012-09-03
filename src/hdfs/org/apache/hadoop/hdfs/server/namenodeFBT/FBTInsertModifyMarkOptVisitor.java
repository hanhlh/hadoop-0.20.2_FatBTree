/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.common.GenerationStamp;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryWithQuota;
import org.apache.hadoop.hdfs.server.namenode.INodeFileUnderConstruction;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertRootModifyRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertRootModifyResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTModifyMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTInsertModifyMarkOptVisitor extends FBTModifyMarkOptVisitor{

// instance attributes ////////////////////////////////////////////////////

    /**
     * 挿入するデータ
     */
    protected LeafValue _value;

    protected LeafEntry _entry;

    private INode _inode;

    private VPointer _leftNode;

    private VPointer _rightNode;

    private String _boundKey;

    private PermissionStatus _ps;
    private String _holder;
    private String _clientMachine;
    private short _replication;
    private long _blockSize;
    private boolean _isDirectory;
    private DatanodeDescriptor _clientNode;
    private boolean _inheritPermission;


    protected GenerationStamp generationStamp = new GenerationStamp();

    // constructors ///////////////////////////////////////////////////////////

    /**
     * @param directory
     */
    public FBTInsertModifyMarkOptVisitor(FBTDirectory directory) {
        super(directory);
    }

	// accessors //////////////////////////////////////////////////////////////

    public void setRequest(Request request) {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptRequest request "+request);
        super.setRequest(request);
        setRequest((FBTInsertModifyMarkOptRequest) request);
    }

	public void setRequest(FBTInsertModifyMarkOptRequest request) {
		super.setRequest((FBTModifyMarkOptRequest) request);
        _value = request.getValue();
        _entry = request.getLeafEntry();
        _inode = request.getINode();
        _leftNode = null;
        _rightNode = null;
        _boundKey = null;
        _ps = request.getPermissionStatus();
        _clientMachine = request.getClientMachine();
        //_overwrite = request.getOverwrite();
        //_append = request.getAppend();
        _holder = request.getHolder();
        _replication = request.getReplication();
        _blockSize = request.getBlockSize();
        _isDirectory = request.isDirectory();
        _clientNode = request.getClientNode();
        _inheritPermission = request.getInheritPermission();
	}

    protected void modifyRequest(VPointer target, int position) {
    	StringUtility.debugSpace("****FBTInsertModifyMarkOptVisitor " +
    			"ModifyRequest target: "+target);
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        FBTInsertModifyResponser responser = new FBTInsertModifyResponser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                System.out.println("vp: "+vp);
    		    Request request =
    		        new FBTInsertModifyRequest
    		        ("/directory."+vp.getPointer().getFBTOwner(),
    		        		vp, _boundKey, null, _leftNode, _rightNode,
    		        		position);
    		    request.setTransactionID(_transactionID);

    	        Call call = new Call(messenger, responser);
    	        call.setRequest(request);
    	        call.setResponseClass(FBTInsertModifyResponse.class);

    	        EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
    	                "/mapping/"  + vp.getPointer().getPartitionID());
    	        call.setDestination(destination);
    	        call.invokeOneWay();
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();
        _leftNode = responser.getLeftNode();
        _rightNode = responser.getRightNode();
        _boundKey = responser.getBoundKey();
    }

    private void rootModifyRequest(VPointer target) {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor rootModifyRequest");
    	System.out.println("target "+target.toString());
    	System.out.println("boundKey "+_boundKey);
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        Responser responser = new Responser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                System.out.println("vp: "+vp);
    		    Request request =
    		        new FBTInsertRootModifyRequest("/directory."+vp.getPointer().getFBTOwner(),
    		                			vp, _boundKey, _leftNode, _rightNode);
    		    request.setTransactionID(_transactionID);

    	        Call call = new Call(messenger, responser);
    	        call.setRequest(request);
    	        call.setResponseClass(FBTInsertRootModifyResponse.class);

    	        EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
    	                "/mapping/"+ vp.getPointer().getPartitionID());
    	        call.setDestination(destination);
    	        call.invokeOneWay();
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();
    }

    protected void requestBackup() {
    	//DO nothing
        //if (_directory.hasBackup()) {
        //}
    }

    protected Response generateResponse() {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor.generateResponse");
    	FBTInsertModifyMarkOptResponse response = new FBTInsertModifyMarkOptResponse(
                (FBTInsertModifyMarkOptRequest) _request, _child,
                _leftNode, _rightNode, _boundKey, _mark, _lockList, _lockRange,
                this._inode);
    	return response;
    }

    protected void setIsSafe(IndexNode index, VPointer self) {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor setIsSafeIndex");
    	//System.out.println(index.toString());
        if (!index.isFullEntry() || index.isRootIndex()) {
        	System.out.println("****FBTInsertModifyMarkOptVisitor unlockRequest");
            _mark = _height;
            _lockRange = _height;
            _isSafe = true;

//            for (Iterator iter = _lockList.iterator(); iter.hasNext();) {
//                VPointer vPointer = (VPointer) iter.next();
//                unlock(vPointer);
//            }
            unlockRequestConcurrently(_lockList);

            _lockList = new PointerSet();
            _lockList.addPointerSet((PointerSet) self);
            System.out.println("lockList 156 "+_lockList.toString());
        } else {
        	System.out.println("****FBTInsertModifyMarkOptVisitor addToLockList");
            _lockList.addPointerSet((PointerSet) self);
        }
    }

    protected void noEntry(VPointer self) {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor.noEntry");
        LeafNode newLeaf =
        	new LeafNode(_directory, _key, _inode);
        synchronized (_directory.getLocalNodeMapping()) {
        	_directory.getLocalNodeMapping().put(newLeaf.
        						getNodeIdentifier().toString(), newLeaf);
        }
        /*System.out.println("*****FBTInserModifyMarkOptVisitor." +
        		"			Create LeafNode "+newLeaf.getNodeNameID());
        */
        _rightNode = newLeaf.getPointer();
        _boundKey = _key;
        _child = newLeaf.getPointer();
        modifyRequest(self, -1);
        /*
        LeafNode dummyLeaf = new LeafNode(_directory);
        System.out.println("*****FBTInserModifyMarkOptVisitor." +
        		"			Create DummyLeafNode "+dummyLeaf.getNodeNameID());
        _directory.localNodeMapping.put(dummyLeaf.getNodeID(), dummyLeaf);
    	dummyLeaf.set_highKey("/home/user/h");
        dummyLeaf.set_sideLink(null);
        dummyLeaf.set_isDummy(true);
    	newLeaf.set_highKey("/home/user/h");
    	newLeaf.set_sideLink(dummyLeaf.getPointer().getPointer());
    	_directory.setDummyLeaf(dummyLeaf.getPointer());*/
    }

    protected void callRedirect() {
    	System.out.println(FBTDirectory.SPACE);
    	System.out.println("FBTInsertModifyMarkOptVisitor.callRedirect");
	    /*
	     * ポインタ vp の指しているノードは現在の PE にはないため，
	     * 適当な PE を選択して要求を転送
	     */
        try {
            FBTInsertModifyMarkOptRequest request =
                new FBTInsertModifyMarkOptRequest(
                        "/directory."+_directory.getOwner(),
                		_key, _child, _height, _mark, _lockList, _lockRange,
                        _ps, _holder, _clientMachine, _replication,
                        _blockSize, _isDirectory, _clientNode, _inheritPermission);

            request.setIsSafe(_isSafe);
            request.setTransactionID(_transactionID);
            System.out.println("******callRedirect"+ request.toString());
            FBTInsertModifyMarkOptResponse response =
                (FBTInsertModifyMarkOptResponse) request(
                    _child, request, FBTInsertModifyMarkOptResponse.class);

            _leftNode = response.getLeftNode();
            _rightNode = response.getRightNode();
            _boundKey = response.getBoundKey();
            _child = response.getVPointer();
            _mark = response.getMark();
            _lockList = response.getLockList();
            _lockRange = response.getLockRange();
        } catch (MessageException e) {
            e.printStackTrace();
        }
    }

    protected void modify(
            IndexNode index, VPointer self, int position, int height) {
    	System.out.println(FBTDirectory.SPACE);
    	System.out.println("FBTInsertModifyMarkOptVisitor.modify "+index.getNameNodeID());
    	System.out.println("FBTInsertModifyMarkOptVisitor.modify boundKey "
    						+_boundKey);
    	System.out.println("FBTInsertModifyMarkOptVisitor.modify position "
							+position);
        if (_boundKey != null) {
            modifyRequest(self, position);

            if (index.size() == 0) {
                /*
                 * ルートがスプリットすると、
                 * 2つの新子ノードにエントリーを全て渡すため、
                 * 自身のエントリーはなくなる.
                 */
                rootModifyRequest(self);
            }

            if (_lockRange == height) {
                endLockRequestConcurrently(self);
            } else {
                unlockRequestConcurrently(self);
            }
        }
    }

    protected void operateWhenSameKeyExist(
            LeafNode leaf, int position) {
    	System.out.println("FBTInsertModifyMarkOptVisitor.operateWhenSameKeyExist");
//        for (Iterator iter = _lockList.iterator(); iter.hasNext();) {
//            VPointer vPointer = (VPointer) iter.next();
//            unlock(vPointer);
//        }
        unlockRequestConcurrently(_lockList);

//        _lockList = leaf.getPointer();

        //TODO leaf.replaceEntry
        leaf.replaceLeafValue(position - 1, null, _value);
    }

    protected void operateWhenSameKeyNotExist( LeafNode leaf, int position) {
    	System.out.println(FBTDirectory.SPACE);
    	System.out.println("FBTInsertModifyMarkOptVisitor.operateWhenSameKeyNotExist");
    	System.out.println("is full of entry per node?" +leaf.isFullLeafEntriesPerNode());
    	System.out.println("position" +position);
        if (!leaf.isFullLeafEntriesPerNode()) {
            _isSafe = true;

//            for (Iterator iter = _lockList.iterator(); iter.hasNext();) {
//                VPointer vPointer = (VPointer) iter.next();
//                unlock(vPointer);
//            }
            unlockRequestConcurrently(_lockList);

//            _lockList = leaf.getPointer();
        } else {
//            _lockList.addPointer((Pointer) leaf.getPointer());
        }

        if (_isSafe) {
            leaf.addINode(-position, _key, _inode);
            if (leaf.isOverLeafEntriesPerNode()) {
                LeafNode newLeaf = leaf.split();
                _rightNode = newLeaf.getPointer();
                _boundKey = newLeaf.getKey();
            }
        } else {
            /* エントリーがいっぱいなので、ルートからやり直す */
            _child = null;
            unlockRequestConcurrently(_lockList);
        }
    }

    protected void callRedirect(VPointer self) {
    	StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor.callRedirect");
        try {
            FBTInsertModifyMarkOptRequest request =
                new FBTInsertModifyMarkOptRequest(
                		"/directory."+self.getPointer().getFBTOwner(),
                        _key, null, _value, self,
                        _height, _mark, _lockList, _lockRange);
            request.setIsSafe(_isSafe);
            request.setTransactionID(_transactionID);

            FBTInsertModifyMarkOptResponse response =
                (FBTInsertModifyMarkOptResponse) request(
                    _child, request, FBTInsertModifyMarkOptResponse.class);

            _leftNode = response.getLeftNode();
            _rightNode = response.getRightNode();
            _boundKey = response.getBoundKey();
            _child = response.getVPointer();
            _mark = response.getMark();
            _lockList = response.getLockList();
            _lockRange = response.getLockRange();

        } catch (MessageException e) {
            e.printStackTrace();
        }
    }

    protected boolean correctPath(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	//System.out.println(FBTDirectory.SPACE);
		//System.out.println("FBLTInsertLcfblVisitor correctPath starts ");
		/*
		if (index.isRootIndex()) {
            return false;
        }
		 */
        if (index.isInRange(_key)) {
            return false;
        }
        //System.out.println("!index.isInRange");
        VPointer vp = index.getSideLink();
        if ((vp == null) || index.isUnderRange(_key)) {
        	//System.out.println("self "+self.toString());
        	if (self instanceof Pointer) {
        		unlock(self);

        	}
        	else if (self instanceof PointerSet) {
        		int pointerIndex = 0;
        		while (pointerIndex < self.size()) {
        			unlock(self.getPointer(pointerIndex));
        			pointerIndex++;
        		}
        	}
        	getDirectory().incrementCorrectingCount();
        } else {
            unlock(self);

            getDirectory().incrementChaseCount();
            visit((IndexNode) _directory.getNode(vp), vp);
        }

        return true;

    }
    protected void visitUnsafeIndex(
            IndexNode index, VPointer self, int height) throws NotReplicatedYetException, MessageException, IOException {
        continueThisPhase(index, self, height);
    }

    protected synchronized int locate(IndexNode index) {
        return index.binaryLocate(_key);
    }

    protected void beforeLockLeaf() {
        // NOP
    }

	@Override
	protected void operateWhenSameKeyExist(LeafNode leaf, VPointer self,
			int position, String key, PermissionStatus ps, String holder,
			String clientMachine, short replication, long _blockSize,
			boolean isDirectory, DatanodeDescriptor clientNode)
			throws QuotaExceededException, MessageException {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	protected void operateWhenSameKeyNotExist(LeafNode leaf, VPointer self,
			int position, String key, PermissionStatus ps, String holder,
			String clientMachine, short replication, long preferredBlockSize,
			boolean isDirectory, DatanodeDescriptor clientNode)
			throws QuotaExceededException, MessageException,
			NotReplicatedYetException, IOException {

		StringUtility.debugSpace("FBTInsertModifyMarkOptVisitor.operateWhenSameKeyNotExist " +
				"438");
        if (!leaf.isFullLeafEntriesPerNode()) {
            _isSafe = true;
            unlockRequestConcurrently(_lockList);
        } else {
        }
        if (_isSafe) {
        	if (isDirectory) {
        		if (_key.equals(FBTDirectory.DEFAULT_ROOT_DIRECTORY)) {
        			INodeDirectoryWithQuota rootDir = new
        						INodeDirectoryWithQuota(_key,ps,
        						Integer.MAX_VALUE, -1);
        			leaf.addINode(-position, _key, rootDir);
        			_inode = rootDir;
        			endLock(self);
        			//_response = new FBTInsertMarkOptResponse(
        			//		(FBTInsertMarkOptRequest) _request, self, rootDir);
        		} else {
	        		long genstamp = nextGenerationStamp();
	        		INodeDirectory inode = new INodeDirectory(_key, ps, genstamp);
	        		leaf.addINode(-position, _key, inode);
	        		_inode = inode;
	        		endLock(self);
	        		//_response = new FBTInsertMarkOptResponse(
	            	//		(FBTInsertMarkOptRequest) _request, self, inode);
	        	}

        	} else {
        		long genstamp = nextGenerationStamp();
        		INodeFileUnderConstruction inode = new
        						INodeFileUnderConstruction(
        						ps,replication,
        						preferredBlockSize, genstamp, holder,
        						clientMachine, clientNode);

        		//if (inheritPermission) {
        		if (true) {
        		      FsPermission p = inode.getFsPermission();
        		      //make sure the  permission has wx for the user
        		      if (!p.getUserAction().implies(FsAction.WRITE_EXECUTE)) {
        		        p = new FsPermission(p.getUserAction().or(FsAction.WRITE_EXECUTE),
        		            p.getGroupAction(), p.getOtherAction());
        		      }
        		      inode.setPermission(p);
        		}

        		inode.setLocalName(_key);
        		inode.setModificationTime(inode.getModificationTime());
        		if (inode.getGroupName() == null) {
        			inode.setGroup(inode.getGroupName());
        		}

        		leaf.addINode(-position, _key, inode);
        		_inode = inode;
        		endLock(self);

        		//_response = new FBTInsertMarkOptResponse(
            	//				(FBTInsertMarkOptRequest) _request, self, inode);
        	}


        	if (leaf.isOverLeafEntriesPerNode()) {
        		LeafNode newLeaf = leaf.split();
        		_rightNode = newLeaf.getPointer();
        		_boundKey = newLeaf.getKey();
        	}
        } else {
            /* エントリーがいっぱいなので、ルートからやり直す */
		_child = null;
         unlockRequestConcurrently(_lockList);
	}

	}
	/**
	 * Increments, logs and then returns the stamp
	 */
	private long nextGenerationStamp() {
	  long gs = generationStamp.nextStamp();
	  return gs;
}
	private <T extends INode> T addChild(INode[] parents, INode child,
			int pos, long childDiskspace, boolean inheritPermission)
				throws QuotaExceededException {
		INode.DirCounts counts = new INode.DirCounts();
	    child.spaceConsumedInTree(counts);
	    if (childDiskspace < 0) {
	      childDiskspace = counts.getDsCount();
	    }
	    updateCount(parents, parents.length, counts.getNsCount(), childDiskspace,
	        true);
	    INode addedNode = ((INodeDirectory) parents[pos]).addChild(
	    				child, inheritPermission);
	    if (addedNode == null) {
	      updateCount(parents, pos, -counts.getNsCount(),
	          -childDiskspace, true);
	    }
	    return (T) addedNode;
	}

	private void updateCount(INode[] inodes, int numOfINodes,
            long nsDelta, long dsDelta, boolean checkQuota)
    				throws QuotaExceededException {
		//if (!ready) {
			//still intializing. do not check or update quotas.
		//	return;
	    //}
	    if (numOfINodes>inodes.length) {
	    	numOfINodes = inodes.length;
	    }
		if (checkQuota) {
			verifyQuota(inodes, numOfINodes, nsDelta, dsDelta, null);
		}

		for(int i = 0; i < numOfINodes; i++) {
			//System.out.println("inode["+i+"]: "+inodes[i].toString());
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
	    //if (!ready) {
	      // Do not check quota if edits log is still being processed
	    //  return;
	    //}
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

	  public void lock(VPointer target, int mode, int h, int c, int r) {
	    	//System.out.println("FBTMarkOptNoCouplingVisitor.lock line 291");
	        lock(target, mode);
	        //_locker.incrementXCount(h, c, r);
	    }

}
