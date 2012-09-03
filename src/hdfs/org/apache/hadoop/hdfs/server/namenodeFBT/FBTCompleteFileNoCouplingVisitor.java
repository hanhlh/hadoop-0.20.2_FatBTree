/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.hdfs.server.protocol.WriteAtLowGear;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.INodeFileUnderConstruction;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenode.SafeModeException;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.apache.hadoop.hdfs.server.namenode.writeOffLoading.WriteOffLoading;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.CompleteFileRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.CompleteFileResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTCompleteFileNoCouplingVisitor extends FBTNodeVisitor
					implements WriteAtLowGear {

	/**
     * �ǥ��쥯�ȥ긡������
     */
    private String _key;
    private String _clientName;
    private FBTDirectory _directory;

	public FBTCompleteFileNoCouplingVisitor(FBTDirectory directory) {
		super(directory);
		_directory = directory;
	}

	public void setRequest(Request request) {
        setRequest((CompleteFileRequest) request);
    }

    public void setRequest(CompleteFileRequest request) {
        super.setRequest(request);
        _key = request.getKey();
        _clientName = request.getClientName();
    }

	public void run() {
		StringUtility.debugSpace("FBTCompleteFileNoCoupling.run");
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
              _response = new CompleteFileResponse((CompleteFileRequest) _request);
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
		StringUtility.debugSpace("CompleteFileNoCoupling visit leafnode ");
		lock(self, Lock.S);
		Block[] fileBlocks = null;
      if (leaf.get_isDummy()) {
          VPointer vp = leaf.get_sideLink();
          endLock(self);
          _request.setTarget(vp);
          callRedirectionException(vp.getPointer().getPartitionID());
      } else {
          if (_key.compareTo(leaf.get_highKey()) >= 0 || leaf.get_deleteBit()) {
              unlock(self);
          } else {
        	  int position = leaf.binaryLocate(_key);
              if (position <= 0) {
            	  System.out.println("key "+_key+" doesnt exist");
                  endLock(self);
                  _response = new CompleteFileResponse((CompleteFileRequest) _request);
              } else {
                  INode iFile = leaf.getINode(position - 1);
                  if (_directory.isInSafeMode()) {
                	  endLock(self);
          			throw new SafeModeException("Cannot complete file " + _key,
          									_directory.safeMode);
              }
          		INodeFileUnderConstruction pendingFile = null;


          		System.out.println("IFile "+_key+" is underconstruction? "+
          							iFile.isUnderConstruction());
          		if (iFile != null && iFile.isUnderConstruction()) {
          			pendingFile = (INodeFileUnderConstruction) iFile;
          			fileBlocks = pendingFile.getBlocks();
          		}
          		if (fileBlocks == null ) {
	          		NameNode.stateChangeLog.info("DIR* FBTDirectory.completeFile: "
	          		+ "failed to complete " + _key
	          		+ " because dir.getFileBlocks() is null " +
	          		" and pendingFile is " +
	          		((pendingFile == null) ? "null" :
	          		 ("from " + pendingFile.getClientMachine()))
	          		);
	          		endLock(self);
	          		_response = new CompleteFileResponse((CompleteFileRequest) _request,
          							_key,
          							CompleteFileStatus.OPERATION_FAILED);
          		}
          		if (!_directory.checkFileProgress(pendingFile, true)) {
          			endLock(self);
          			_response = new CompleteFileResponse((CompleteFileRequest) _request,
          							_key,
          							CompleteFileStatus.STILL_WAITING);
          		}
          		System.out.println("checkFileProgress "+_key+ " done");
          		finalizeINodeFileUnderConstruction(_key, pendingFile, leaf, position);
          		NameNode.stateChangeLog.info("DIR* FBTDirectory.completeFile: file " + _key
          		+ " is closed by " + _clientName);
                  endLock(self);
                //Perform WriteOffloading
                  //TODO getGearInformation from Gearm Manager
        			//do WriteOffLoading writting if _directory.getOwner is in list
                    System.out.println("Implement write wol blocks");
        			String wolTarget = self.getPointer().getFBTOwner();
        			String wolSource = "192.168.0."+_directory.getPartitionID();
        	    	System.out.println("owner "+wolTarget);
        	    	/*System.out.println("gearActivateNode at Gear 1 "+
        	    			_directory.getGearActivateNodes().get(1).toString());*/
        	    	if (!_directory.getGearActivateNodes().get(1).contains(wolTarget)) {
        	    		writeOffloading(wolTarget, wolSource, _key, fileBlocks);
        	    	}
                  _response = new CompleteFileResponse(
                          (CompleteFileRequest) _request, self, _key,
                          CompleteFileStatus.COMPLETE_SUCCESS);
              }
              //endLock(self);
     	    	}
          }
      }


	/**
	 * wolTargetName: the IPAddress of node that transfers temporal transfered blocks to original node
	 * */
	public void writeOffloading(String wolTarget, String wolSource,
						String src, Block[] transferedBlocks)
					throws UnknownHostException, MessageException {
		Date startTime = new Date();
		String wolDestination = _directory._WOLMapping.get(wolTarget);
		//System.out.println("wolDestination: "+wolDestination);
		DatanodeDescriptor WOLSource = (DatanodeDescriptor) _directory.
				getClusterMap().getNode("/default-rack/"+wolSource+":50010");
		DatanodeDescriptor WOLDestination = (DatanodeDescriptor) _directory.
				getClusterMap().getNode("/default-rack/"+wolDestination+":50010");

		writeOffloadingLogging(src, wolDestination, wolTarget);
		for (Block bi : transferedBlocks) {
			WOLSource.addBlockToBeReplicated(
					bi, new DatanodeDescriptor[] {WOLDestination});
		}
		modifyWriteOffLoadingRange(wolTarget, src);
		Date endTime = new Date();
		StringUtility.debugSpace("src, wolTarget, wolSource, wolDestination, response: "+src+","+wolTarget+","+wolSource
							+","+wolDestination+
							","+(endTime.getTime()-startTime.getTime())/1000.0);
	}

	private void modifyWriteOffLoadingRange(String wolTarget, String src) {
		StringUtility.debugSpace("modifyWriteOffLoadingRange,"+wolTarget+","+src);
		ConcurrentHashMap<String, String[]> WriteOffLoadingRangeMap =
			(ConcurrentHashMap<String, String[]>) _directory.getWriteOffLoadingRangeMap();
		if (WriteOffLoadingRangeMap.get(wolTarget)==null) {
			String[] range = {src, src};
			WriteOffLoadingRangeMap.put(wolTarget, range);
		}
		String[] range = WriteOffLoadingRangeMap.get(wolTarget);
		//System.out.println("lowSrc,"+range[0]);
		//System.out.println("highSrc,"+range[1]);
		range[0] = (range[0].compareTo(src) <0)? range[0]:src; //lowSrc
		range[1] = (range[1].compareTo(src) >0)? range[1]:src; //highSrc
		WriteOffLoadingRangeMap.replace(wolTarget, range);
		System.out.println("WriteOffLoadingRangeMap,"+wolTarget+","+Arrays.toString(range));
	}

	private void writeOffloadingLogging(String src, String WOLDestination,
			String destination) {
		WriteOffLoading wol = new WriteOffLoading(
									getWOLCounting(),
									src,
									WOLDestination,
									destination);
		String record = wol.createWriteOffLoadingRecord();

		wol.outputRecord(record, _directory.getNameNodeAddress().getHostName());
	}
	public int getWOLCounting() {
		return _directory.getWOLSequence().getAndIncrement();
	}
	synchronized
	void finalizeINodeFileUnderConstruction(String src,
		      INodeFileUnderConstruction pendingFile,
		      LeafNode leaf,
		      int position
		      ) throws IOException {
		//TODO closeFile, checkReplicationFactor
		NameNode.LOG.info("FBTCompleteFileNoCoupling.finalizeINodeFileConstruction start");

		    // The file is no longer pending.
		    // Create permanent INode, update blockmap
		    INodeFile newFile = pendingFile.convertToInodeFile();
		    leaf.replaceINode(position-1, src, newFile);

		    // close file and persist block allocations for this file
		    //dir.closeFile(src, newFile);

		    //_directory.checkReplicationFactor(newFile);
		    NameNode.LOG.info("FBTCompleteFileNoCoupling.finalizeINodeFileConstruction " +
		    		"ended");
		  }

	@Override
	public void visit(PointerNode pointer, VPointer self)
			throws NotReplicatedYetException, MessageException, IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
