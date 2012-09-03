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
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryWithQuota;
import org.apache.hadoop.hdfs.server.namenode.INodeFileUnderConstruction;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;

/**
 * @author hanhlh
 *
 */
public class FBTInsertMarkOptNoCouplingVisitor extends
					FBTMarkOptNoCouplingVisitor{

	// instance attributes ////////////////////////////////////////////////////
    /**
     * 挿入するデータ
     */
    private LeafValue _value;

    private LeafEntry _entry;

    //private INode _inode;

    protected PermissionStatus _ps;
    protected String _holder;
    protected String _clientMachine;
    protected short _replication;
    protected long _blockSize;
    protected boolean _isDirectory;
    protected boolean _inheritPermission;
    protected DatanodeDescriptor _clientNode;

    protected GenerationStamp generationStamp = new GenerationStamp();

	public FBTInsertMarkOptNoCouplingVisitor(FBTDirectory directory) {
		super(directory);
	}

	// accessors //////////////////////////////////////////////////////////////

    public void setRequest(Request request) {
        super.setRequest(request);
        setRequest((FBTInsertMarkOptRequest) request);
    }

	public void setRequest(FBTInsertMarkOptRequest request) {
        super.setRequest((FBTMarkOptRequest) request);
        //_value = request.getValue();
        //_entry = request.getLeafEntry();
        //_inode = request.getINode();
        _ps = request.getPermissionStatus();
        _clientMachine = request.getClientMachine();
        //_overwrite = request.getOverwrite();
        //_append = request.getAppend();
        _replication = request.getReplication();
        _blockSize = request.getBlockSize();
        _isDirectory = request.isDirectory();
        _clientNode = request.getClientNode();
        _inheritPermission = request.getInheritPermission();
	}

		@Override
	protected void noEntry(VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		/*
         * 葉ノードが存在しないので、
         * このノードにも排他ロックが必要なのでリスタート
         */
        unlock(self);

		((FBTDirectory) _directory).incrementRestartCount();
        restart();
	}

	@Override
	protected void operateWhenSameKeyExist(LeafNode leaf, VPointer self,
			int position, String key, PermissionStatus ps,
			String holder,
            String clientMachine,
            //boolean overwrite,
            //boolean append,
            short replication,
            long _blockSize,
            boolean isDirectory,
            DatanodeDescriptor clientNode) throws QuotaExceededException, MessageException {
		StringUtility.debugSpace("operateWhenSameKeyExist");
		if (isDirectory) {
			//System.out.println("key "+_key);
			if (_key.equals(FBTDirectory.DEFAULT_ROOT_DIRECTORY)) {
    			//long genstamp = nextGenerationStamp();
				//System.out.println("line 118");
    			INodeDirectoryWithQuota rootDir = new
    						INodeDirectoryWithQuota(_key,ps,
    			        Integer.MAX_VALUE, -1);
    			leaf.replaceINode(position-1, key, rootDir);
    			endLock(self);
    			_response = new FBTInsertMarkOptResponse(
    					(FBTInsertMarkOptRequest) _request, self, rootDir);

    		}
			 else {
				 //System.out.println("line 129");
	        	long genstamp = nextGenerationStamp();
	        	INodeDirectory inode = new INodeDirectory
	        										(_key, ps, genstamp);
	        	leaf.replaceINode(position-1, _key, inode);
	        	endLock(self);
	        	_response = new FBTInsertMarkOptResponse(
	            				(FBTInsertMarkOptRequest) _request, self, inode);
	        	}
		} else {

		}

	}

	protected void operateWhenSameKeyNotExist(LeafNode leaf, VPointer self,
			int position,
			String key,
			PermissionStatus ps,
			String holder,
            String clientMachine,
            //boolean overwrite,
            //boolean append,
            short replication,
            long preferredBlockSize,
            boolean isDirectory,
            DatanodeDescriptor clientNode) throws MessageException, NotReplicatedYetException, IOException {
		StringUtility.debugSpace("FBTInsertMarkOptNoCouplingVisitor operate when" +
				" same key not exist");
		//TODO check update diskspace
		if (leaf.isFullLeafEntriesPerNode()) {
            /* エントリーがいっぱいなので、ルートからやり直す */
			//System.out.println("full leaf node");
            unlock(self);
            System.out.println("unlock X");
            //System.out.println("mark "+_mark);
            _length = _mark;
            _height = 0;
            _mark = 0;

            if (_restart) {
                ((FBTDirectory) _directory).incrementMoreRestartCount();
            } else {
                ((FBTDirectory) _directory).incrementRestartCount();
            }
            _restart = true;

            _directory.accept(this);
        } else {
        	if (isDirectory) {

        		//System.out.println("FBTInsertMarkOptNoCouplingVisitor add INodeDirectory");
        		if (_key.equals(FBTDirectory.DEFAULT_ROOT_DIRECTORY)) {
        			INodeDirectoryWithQuota rootDir = new
        						INodeDirectoryWithQuota(_key,ps,
        						Integer.MAX_VALUE, -1);
        			leaf.addINode(-position, _key, rootDir);
        			endLock(self);
        			System.out.println("unlock X");
        			_response = new FBTInsertMarkOptResponse(
        					(FBTInsertMarkOptRequest) _request, self, rootDir);

        		} else {
	        		long genstamp = nextGenerationStamp();
	        		INodeDirectory inode = new INodeDirectory(_key, ps, genstamp);
	        		//add to Inode with check inherit permission, data quota
	        		/*endLock(self);
	        		String[] parentNames = INode.getParentPathNames(_key);
	        		INode[] parentInodes = new INode[parentNames.length];
	        		for (int i=0; i<parentNames.length; i++) {
	        			parentInodes[i] = _directory.searchResponse(parentNames[i]).
	        										getINode();
	        		}
	        		//add current Node to Parent node
	        		addChild(parentInodes, inode, parentInodes.length-1,
	        				-1, true);
	        		if (inode==null) {
	        			_response = new FBTInsertMarkOptResponse(
	        					(FBTInsertMarkOptRequest) _request, self, null);
	        		}*/
	        		//lock(self, Lock.X, _height+1, 1, 1);
	        		//lock(self, Lock.X, _height, 1, 1);
	        		leaf.addINode(-position, _key, inode);
	        		endLock(self);
	        		//System.out.println("unlock X");
	        		_response = new FBTInsertMarkOptResponse(
	            			(FBTInsertMarkOptRequest) _request, self, inode);
	        	}

        	} else {
        		System.out.println("FBTInsertMarkOptNoCoupling addFile");
        		long genstamp = nextGenerationStamp();
        		INodeFileUnderConstruction inode = new
        						INodeFileUnderConstruction(
        						ps,replication,
        						preferredBlockSize, genstamp, holder,
        						clientMachine, clientNode);
        		inode.setLocalName(getLocalName(_key));
        		/*String[] parentNames = INode.getParentPathNames(_key);
        		INode[] parentInodes = new INode[parentNames.length];
        		endLock(self);
        		for (int i=0; i<parentNames.length; i++) {
        			parentInodes[i] = _directory.searchResponse(parentNames[i]).
        										getINode();
        		}
        		//add current Node to Parent node
        		addChild(parentInodes, inode, parentInodes.length-1,
        				-1, true);
        		System.out.println("FBTInsertMarkOptNoCoupling addChild " +
        				"inode "+inode);
        		if (inode==null) {
        			_response = new FBTInsertMarkOptResponse(
        					(FBTInsertMarkOptRequest) _request, self, null);
        		}*/
        		if (_inheritPermission) {
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
        		}

        		/*lock(self, Lock.X, _height, 1, 1);*/
        		leaf.addINode(-position, _key, inode);

        		//System.out.println("unlock X");
        		_response = new FBTInsertMarkOptResponse(
            					(FBTInsertMarkOptRequest) _request, self, inode);
        		endLock(self);
        	}

        	}

	}


	  /**
	   * Increments, logs and then returns the stamp
	   */
	  private synchronized long nextGenerationStamp() {
	    long gs = generationStamp.nextStamp();
	    return gs;
	  }

	@Override
	protected Response callModifyRequest(Request request)
			throws MessageException {
		StringUtility.debugSpace("FBTInsertMarkOptNoCoupling.callModifyRequest "+request);
		System.out.println("child: "+_child);
		return request(_child, request, FBTInsertModifyMarkOptResponse.class);	}

	@Override
	protected void judgeResponse(Response response, VPointer child) {
		//StringUtility.debugSpace("FBTInsertMarkOptNoCouplingVisitor.judgeResponse");
		FBTInsertModifyMarkOptResponse modifyResponse =
            (FBTInsertModifyMarkOptResponse) response;

        int lockRange = modifyResponse.getLockRange();
        //System.out.println("lockRange "+ lockRange);
        //System.out.println("height "+ _height);
        if (lockRange <= _height + 1) {
            for (Iterator iter = child.iterator(); iter.hasNext();) {
                VPointer vPointer = (VPointer) iter.next();
                unlock(vPointer);
            }
            //unlockRequestConcurrently(child);
            //endLockRequestConcurrently(child);
        } else {
            // TODO
        }

        if (modifyResponse.getVPointer() == null) {

            InsertRequest insertRequest = (InsertRequest) _request;
            insertRequest.setTarget(null);
        } else {
            _response = new FBTInsertMarkOptResponse(
                    (FBTInsertMarkOptRequest) _request,
                    modifyResponse.getVPointer(),
                    modifyResponse.getINode());
        }

	}

	protected Request generateModifyRequest(PointerSet pointer) {
		StringUtility.debugSpace("FBTInsertMarkOptNoCouplingVisitor.generateModifyRequest ps: "+
							pointer);
		//System.out.println("fbtOwner: "+_directory.getOwner());
		FBTInsertModifyMarkOptRequest request =
            new FBTInsertModifyMarkOptRequest(
            		"/directory."+_directory.getOwner(),
            		_key, pointer,
                    _height, _mark, new PointerSet(), 0,
                    _ps,
                    _holder,
                    _clientMachine,
                    //_overwrite,
                    //_append,
                    _replication,
                    _blockSize,
                    _isDirectory,
                    _clientNode,
                    _inheritPermission);
        request.setTransactionID(_transactionID);
        System.out.println("******FBTInsertMarkOptNoCouplingVisitor.generateModifyRequest " +
        					"request "+ request.toString()	);

        return request;

	}

	@Override
	protected NodeVisitor generateNodeVisitor() {
		return _directory.getNodeVisitorFactory().createInsertModifyVisitor();
	}
	@Override
	protected void handle(Response response, VPointer child) throws NotReplicatedYetException, MessageException, IOException {
		FBTInsertModifyMarkOptResponse modifyResponse =
            (FBTInsertModifyMarkOptResponse) response;

        _mark = modifyResponse.getMark();
//        VPointer lockList = modifyResponse.getLockList();
//        for (Iterator iter = lockList.iterator(); iter.hasNext();) {
//          VPointer vPointer = (VPointer) iter.next();
//          unlock(vPointer);
//        }
        int lockRange = modifyResponse.getLockRange();
        if (lockRange <= _height + 1) {
//            for (Iterator iter = child.iterator(); iter.hasNext();) {
//                VPointer vPointer = (VPointer) iter.next();
//                unlock(vPointer);
//            }
//            unlockRequestConcurrently(child);
//            endLockRequestConcurrently(child);
        } else {
            // TODO
        }

        if (modifyResponse.getVPointer() == null) {
            ((FBTDirectory) _directory).incrementMoreRestartCount();
            restart();
        } else {
            _response = new FBTInsertMarkOptResponse(
                    (FBTInsertMarkOptRequest) _request,
                    modifyResponse.getVPointer(),
                    modifyResponse.getINode());
        }

	}

	@Override
	protected void markSafe(IndexNode index, VPointer self) {
		if (!index.isFullEntry() || index.isRootIndex()) {
			/*System.out.println("FBTInsertMarkOptNoCouplingVisitor is root index "+
					index.isRootIndex());
			System.out.println("FBTInsertMarkOptNoCouplingVisitor is safe");*/
            _mark = _height;
        }
	}

	@Override
	protected void changePhase(ChangePhaseException e) {
		FBTInsertMarkOptRequest insertRequest = (FBTInsertMarkOptRequest)_request;
    	FBTInsertMarkOptRequest request = (FBTInsertMarkOptRequest)e.getRequest();
    	VPointer target = request.getTarget();
    	insertRequest.setTarget(target);
    	insertRequest.setLength(request.getLength());
    	insertRequest.setHeight(request.getHeight());
    	insertRequest.setMark(request.getMark());


//      ((FBTDirectoryTest) _directory).incrementRestartCount();

        if (_directory.isLocal(target)) {
            setRequest(_request);
            _restart = true;
            run();
        } else {
            callRedirectionException(target.getPointer().getPartitionID());
        }

	}

	@Override
	protected int locate(IndexNode index) {
		return index.binaryLocate(_key);
	}

	@Override
	protected void visitLeafIndexWithEntry(IndexNode index, VPointer self,
			int position) throws MessageException, NotReplicatedYetException, IOException {

		goNextNode(index, self, position);
	}

	@Override
	protected void visitLeafIndexWithoutEntry(VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		noEntry(self);
	}

	@Override
	protected void checkDestinationDiskNode(IndexNode index, int position) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	protected boolean correctPath(IndexNode index, VPointer self) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	private synchronized <T extends INode> T addChild(INode[] parents, INode child,
			int pos, long childDiskspace, boolean inheritPermission)
				throws QuotaExceededException {
		StringUtility.debugSpace("FBTInsertMarkOptNoCoupling.addChild");
		INode.DirCounts counts = new INode.DirCounts();
	    child.spaceConsumedInTree(counts);
	    if (childDiskspace < 0) {
	      childDiskspace = counts.getDsCount();
	    }
	    updateCount(parents, parents.length, counts.getNsCount(), childDiskspace,
	        true);
	    INode addedNode = ((INodeDirectory) parents[pos]).addChild(
	    				child, inheritPermission);
	    System.out.println("addedNode "+addedNode);
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

	  public String getLocalName(String src) {
		  return (src==null)? null : src.substring(src.lastIndexOf("/")+1, src.length());
	  }
}
