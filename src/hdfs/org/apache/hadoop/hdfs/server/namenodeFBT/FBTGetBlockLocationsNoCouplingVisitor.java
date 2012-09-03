/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetBlockLocationsRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetBlockLocationsResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTGetBlockLocationsNoCouplingVisitor extends FBTNodeVisitor{


	/**
     * �ǥ��쥯�ȥ긡������
     */
    private String _key;


    private long _offset;
    private long _length;
    private int _nrBlocksToReturn;
    private boolean _doAccessTime;

	public FBTGetBlockLocationsNoCouplingVisitor(FBTDirectory directory) {
		super(directory);
		// TODO ��ư�������줿���󥹥ȥ饯������������
	}

	// accessors //////////////////////////////////////////////////////////////

    public void setRequest(Request request) {
        setRequest((GetBlockLocationsRequest) request);
    }
    public void setRequest(GetBlockLocationsRequest request) {
        super.setRequest(request);
        _key = request.getKey();
        _offset = request.getOffset();
        _length = request.getLength();
        _nrBlocksToReturn = request.getNrBlocksToReturn();
        _doAccessTime = request.isDoAccessTime();
    }

	public void run() {
		StringUtility.debugSpace("FBTGetBlockLocationsNoCoupling.run "+_key);
		VPointer vp = _request.getTarget();
        if (vp == null) {
            /* �롼�Ȥ������򳫻� */
            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				e.printStackTrace();
			} catch (MessageException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (NotReplicatedYetException e) {
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
	public void visit(MetaNode meta, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
		lock(self, Lock.IS);

        VPointer root = meta.getRootPointer();

        unlock(self);

        Node node = _directory.getNode(root);
        node.accept(this, root);


	}

	@Override
	public void visit(IndexNode index, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
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
              _response = new GetBlockLocationsResponse((
            		  			GetBlockLocationsRequest) _request);
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
			throws QuotaExceededException, MessageException,
			NotReplicatedYetException, IOException {
		StringUtility.debugSpace("FBTGetBlockLocationNoCoupling.visit leafnode");
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
                  System.out.println(_key+" doesnt exist");
                  _response = new GetBlockLocationsResponse(
                		  			(GetBlockLocationsRequest) _request);
              } else {
                  INodeFile inode = (INodeFile) leaf.getINode(position - 1);
                  if(inode == null) {
        			  _response = new GetBlockLocationsResponse(
        					  			(GetBlockLocationsRequest) _request);
        		  }
        		  if (_doAccessTime && _directory.isAccessTimeSupported()) {
        			  //dir.setTimes(src, inode, -1, now(), false);
        			  //TODO setAccessTime to inode
        		  }
        		  Block[] blocks = inode.getBlocks();
        		  if (blocks == null) {
        			  System.out.println("blocks null");
        			  _response = new GetBlockLocationsResponse(
					  			(GetBlockLocationsRequest) _request,
					  			_key,
					  			//null);
					  			new LocatedBlocks());
        		  }
        		  if (blocks.length == 0) {
        			  System.out.println("blocks length 0");
        			  _response = new GetBlockLocationsResponse(
					  			(GetBlockLocationsRequest) _request,
					  			_key,
					  			new LocatedBlocks(_length,
					  						new ArrayList<LocatedBlock>(blocks.length),
					  						true));

        		  }
        		  ArrayList<LocatedBlock> results = new ArrayList<LocatedBlock>
        		  								(blocks.length);

        		  int curBlk = 0;
        		  long curPos = 0, blkSize = 0;
        		  int nrBlocks = (blocks[0].getNumBytes() == 0) ? 0 : blocks.length;
        		  for (curBlk = 0; curBlk < nrBlocks; curBlk++) {
        			  blkSize = blocks[curBlk].getNumBytes();
        			  assert blkSize > 0 : "Block of size 0";
        			  if (curPos + blkSize > _offset) {
        				  break;
        			  }
        			  curPos += blkSize;
        		  }

        		  if (nrBlocks > 0 && curBlk == nrBlocks)   // offset >= end of file
        			  _response = new GetBlockLocationsResponse(
        					  (GetBlockLocationsRequest) _request,
        					  _key,
        					  null);

        		  long endOff = _offset + _length;

        		  do {
        			// get block locations
        			  synchronized (_directory.blocksMap) {
        				  int numNodes = _directory.blocksMap.numNodes(blocks[curBlk]);

	        			  int numCorruptNodes = _directory.countNodes(blocks[curBlk]).corruptReplicas();
	        			  int numCorruptReplicas = _directory.corruptReplicas.numCorruptReplicas(blocks[curBlk]);
	        			  if (numCorruptNodes != numCorruptReplicas) {
	        				  NameNodeFBT.LOG.warn("Inconsistent number of corrupt replicas for " +
	        						  blocks[curBlk] + "blockMap has " + numCorruptNodes +
	        						  " but corrupt replicas map has " + numCorruptReplicas);
	        			  }
	        			  boolean blockCorrupt = (numCorruptNodes == numNodes);
	        			  int numMachineSet = blockCorrupt ? numNodes :
	        				  (numNodes - numCorruptNodes);
	        			  DatanodeDescriptor[] machineSet = new DatanodeDescriptor[numMachineSet];
	        			  if (numMachineSet > 0) {
	        				  numNodes = 0;
	        				  for(Iterator<DatanodeDescriptor> it =
	        					  _directory.blocksMap.nodeIterator(blocks[curBlk]); it.hasNext();) {
	        					  DatanodeDescriptor dn = it.next();
	        					  boolean replicaCorrupt = _directory.corruptReplicas.isReplicaCorrupt(blocks[curBlk], dn);
	        					  if (blockCorrupt || (!blockCorrupt && !replicaCorrupt))
	        						  machineSet[numNodes++] = dn;
	        					  }
	        				}

        			  results.add(new LocatedBlock(blocks[curBlk], machineSet, curPos,
        					  blockCorrupt));

        			  System.out.println("result size "+results.size());
        			  curPos += blocks[curBlk].getNumBytes();
        			  curBlk++;
        			  }
        		  } while (curPos < endOff
        				  && curBlk < blocks.length
        				  && results.size() < _nrBlocksToReturn);
	                  endLock(self);
	                  System.out.println("generate response");
	                  LocatedBlocks lbs = inode.createLocatedBlocks(results);
	                  System.out.println("lbs: "+lbs);

	                  _response = new GetBlockLocationsResponse(
	                          (GetBlockLocationsRequest) _request,
	                          self,
	                          _key,
	                          lbs);
              }

          }
      }
	}

	@Override
	public void visit(PointerNode pointer, VPointer self)
			throws NotReplicatedYetException, MessageException, IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
