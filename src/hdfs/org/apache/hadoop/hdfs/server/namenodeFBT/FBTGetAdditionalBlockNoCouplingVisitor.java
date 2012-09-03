/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;

import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.INodeFileUnderConstruction;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetAdditionalBlockRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetAdditionalBlockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTGetAdditionalBlockNoCouplingVisitor extends FBTNodeVisitor {

	/**
     * �ǥ��쥯�ȥ긡������
     */
    private String _key;

    private int _currentGear;

	public FBTGetAdditionalBlockNoCouplingVisitor(FBTDirectory directory) {
		super(directory);
	}
	// accessors //////////////////////////////////////////////////////////////

    public void setRequest(Request request) {
        setRequest((GetAdditionalBlockRequest) request);
    }

    public void setRequest(GetAdditionalBlockRequest request) {
        super.setRequest(request);
        _key = request.getKey();
        _currentGear = request.getCurrentGear();
    }

	public void run() {
		VPointer vp = _request.getTarget();

        if (vp == null) {
            /* �롼�Ȥ������򳫻� */
            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
        } else {
            /*
             * ¾�� PE ����ž������Ƥ����׵�ξ��� vp != null �Ȥʤ�
             * vp �ǻ��ꤵ�줿�Ρ��ɤ� Visitor ���Ϥ�
             */
            Node node = _directory.getNode(vp);
			try {
				node.accept(this, vp);
			} catch (NotReplicatedYetException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
		}


        while (_response == null) {
            ((FBTDirectory) _directory).incrementCorrectingCount();

            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
        }

	}

	@Override
	public void visit(MetaNode meta, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		lock(self, Lock.IS);

        VPointer root = meta.getRootPointer();

        unlock(self);

        Node node = _directory.getNode(root);
        node.accept(this, root);

	}

	@Override
	public void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		lock(self, Lock.IS);
//      if (_parent != null) {
//          unlock(_parent);
//          _parent = null;
//      }

      if (!index.isInRange(_key) || index._deleteBit){
     // if (index.isDeleted()) {
          unlock(self);
      } else {
          /* ���Ρ��ɤǤθ��������ΰ��� */
          int position = index.binaryLocate(_key);

          if (position < 0) {
              /* ���ߡ��ĥ꡼�˥���ȥ꡼��¸�ߤ��ʤ� */
              _response = new GetAdditionalBlockResponse(
            		  				(GetAdditionalBlockRequest) _request);
              endLock(self);
          } else {
              /* �ҥΡ��ɤ�ؤ��ݥ��� */
              VPointer vp = index.getEntry(position);

              if (_directory.isLocal(vp)) {
                  unlock(self);
//                  _parent = self;

                  Node node = _directory.getNode(vp);
                  node.accept(this, vp);
              } else {
                  /*
                   * �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
                   * Ŭ���� PE �����򤷤��׵��ž��
                   */
                  endLock(self);

                  _request.setTarget(vp);
                  callRedirectionException(vp.getPointer().getPartitionID());
              }
          }
      }


	}

	@Override
	public void visit(LeafNode leaf, VPointer self)
			throws MessageException, IOException {
		StringUtility.debugSpace("FBTGetAdditionalBlockVisitor visit leafnode ");
		lock(self, Lock.S);

      if (leaf.get_isDummy()) {
          VPointer vp = leaf.get_sideLink();
          endLock(self);
          _request.setTarget(vp);
          callRedirectionException(vp.getPointer().getPartitionID());
      } else {
          if (_key.compareTo(leaf.get_highKey()) >= 0 || leaf.get_deleteBit()) {
              unlock(self);
          } else {
              /* ���Ρ��ɤǤθ��������ΰ��� */

        	  int position = leaf.binaryLocate(_key);
              if (position <= 0) {
                  // ������¸�ߤ��ʤ�
                  endLock(self);
                  _response = new GetAdditionalBlockResponse(
                		  (GetAdditionalBlockRequest) _request);
              } else {
            	  long fileLength, blockSize;
            	  int replication;
            	  DatanodeDescriptor clientNode = null;
            	  Block newBlock = null;

            	  INodeFileUnderConstruction pendingFile =
              			(INodeFileUnderConstruction) leaf.getINode(position-1);
	            //
	            // If we fail this, bad things happen!
	            //
	            if (!checkFileProgress(pendingFile, false)) {
	              throw new NotReplicatedYetException("Not replicated yet:" + _key);
	            }
            	  fileLength = pendingFile.computeContentSummary().getLength();
            	  System.out.println("fileLength "+fileLength);
            	  blockSize = pendingFile.getPreferredBlockSize();
            	  clientNode = pendingFile.getClientNode();
            	  replication = (int)pendingFile.getReplication();

            	  // choose targets for the new block tobe allocated.
            	  /*DatanodeDescriptor targets[] = _directory.replicator.chooseTarget(
            													replication,
                                                               clientNode,
                                                               null,
                                                               blockSize);*/
            	  //TODO get placement
            	  DatanodeDescriptor targets[] = new DatanodeDescriptor[1];
            	  //System.out.println("clusterMap: "+_directory.getClusterMap());
            	  String targetName = "/default-rack/"+_directory.getNameNodeAddress().getAddress().
            	  					getHostAddress()+":50010";

            	  //System.out.println("targetName: "+targetName);

            	  DatanodeInfo targetNode = (DatanodeInfo) _directory.getClusterMap().getNode(targetName);


            	  targets[0] = (DatanodeDescriptor) targetNode;
	            /*for (DatanodeDescriptor dd:targets) {
	            	System.out.println("target: "+dd.toString());
	            }
*/
	            if (targets.length < _directory.minReplication) {
	            	throw new IOException("File " + _key + " could only be replicated to " +
                                targets.length + " nodes, instead of " +
                                _directory.minReplication);
	            }

	            // Allocate a new block and record it in the INode.
	            synchronized (this) {
	            	String[] parentPathNames = INode.getParentAndCurrentPathNames(_key);
	            	INode[] parentPathINodes = new INode[parentPathNames.length];
	            	/*for (int count=0; count<parentPathNames.length; count++) {
	            		parentPathINodes[count] = _directory.searchResponse(
            								parentPathNames[count]).getINode();
	            	}*/

	            	int inodesLen = parentPathINodes.length;
	            	//checkLease(src, clientName, pathINodes[inodesLen-1]);
	            	/*INodeFileUnderConstruction pendingIFile  = (INodeFileUnderConstruction)
                                               parentPathINodes[inodesLen - 1];*/

	            	//System.out.println("pendingFile "+pendingIFile.toString());
	            	//if (!_directory.checkFileProgress(pendingIFile, false)) {
	            	if (!_directory.checkFileProgress(pendingFile, false)) {
	            		throw new NotReplicatedYetException("Not replicated yet:" + _key);
	            	}

	            	// allocate new block record block locations in INode.
	            	newBlock = allocateBlock(_key, parentPathINodes, pendingFile);
	            	pendingFile.setTargets(targets);

	            	for (DatanodeDescriptor dn : targets) {
	            		dn.incBlocksScheduled();
	            	}

	            	// Create next block
	            	LocatedBlock lb = new LocatedBlock(newBlock, targets, fileLength);
	            	endLock(self);
	            	_response = new GetAdditionalBlockResponse(
                          (GetAdditionalBlockRequest) _request, self, _key, lb, _directory.getOwner());

	            }
              }
          }
      }

              //endLock(self);
	}
      //}
	//}
	/**
	* Allocate a block at the given pending filename
	*
	* @param src path to the file
	* @param inodes INode representing each of the components of src.
	*        <code>inodes[inodes.length-1]</code> is the INode for the file.
	*/
	private
	Block allocateBlock(String src, INode[] inodes, INodeFile inodeFile)
			throws	IOException {
		//StringUtility.debugSpace("FBTGetAdditionalBlockVisitor.allocateBlock for "+src);
		Block b = new Block(_directory.randBlockId.nextLong(), 0, 0);
		while(_directory.isValidBlock(b)) {
			b.setBlockId(_directory.randBlockId.nextLong());
		}
		b.setGenerationStamp(_directory.getGenerationStamp());
		b = addBlock(src, inodes, b, inodeFile);
		NameNode.stateChangeLog.info("BLOCK* FBTGetAdditionalBlock.allocateBlock: "
                         +src+ ". "+b);
		NameNode.LOG.info("BLOCK* FBTGetAdditionalBlock.allocateBlock: "
                +src+ ". "+b);
		return b;
	}
	/**
	* Add a block to the file. Returns a reference to the added block.
	*/
	Block addBlock(String path, INode[] inodes, Block block,
						INodeFile inodeFile) throws IOException {
		//waitForReady();
		//StringUtility.debugSpace("FBTGetAdditionalBlock.addBlock");
		//synchronized (rootDir) {
		//INodeFile fileINode = (INodeFile) inodes[inodes.length-1];
		INodeFile fileINode = inodeFile;
		//System.out.println("fileINode "+fileINode);
		// check quota limits and updated space consumed
		//updateCount(inodes, inodes.length-1, 0,
		//    fileNode.getPreferredBlockSize()*fileNode.getReplication(), true);

		// associate the new list of blocks with this file
		synchronized (_directory.blocksMap) {
			_directory.blocksMap.addINode(block, fileINode);
			BlockInfo blockInfo = _directory.blocksMap.getStoredBlock(block);
			fileINode.addBlock(blockInfo);
		}

		NameNode.stateChangeLog.info("DIR* FBTDirectory.addFile: "
		                            + path + " with " + block
		                            + " block is added to the in-memory "
		                            + "file system");
		//}
		return block;
	}

	/**
	   * Check that the indicated file's blocks are present and
	   * replicated.  If not, return false. If checkall is true, then check
	   * all blocks, otherwise check only penultimate block.
	   */
	  protected synchronized boolean checkFileProgress(INodeFile v, boolean checkall) {
	    if (checkall) {
	      //
	      // check all blocks of the file.
	      //
	      for (Block block: v.getBlocks()) {
	        if (_directory.blocksMap.numNodes(block) < _directory.minReplication) {
	          return false;
	        }
	      }
	    } else {
	      //
	      // check the penultimate block of this file
	      //
	      Block b = v.getPenultimateBlock();
	      if (b != null) {
	        if (_directory.blocksMap.numNodes(b) < _directory.minReplication) {
	          return false;
	        }
	      }
	    }
	    return true;
	  }
	@Override
	public void visit(PointerNode pointer, VPointer self) {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
