package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Node;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Locker;

public class FBTInsertModifyRule extends AbstractRule{

	public FBTInsertModifyRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { FBTInsertModifyRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		System.out.println("NameNodeFBT.FBTInsertModifyRule action");
		FBTInsertModifyRequest request = (FBTInsertModifyRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        System.out.println("fbtdirectoryName: "+request.getDirectoryName());
		VPointer target = request.getTarget();
		System.out.println("target "+target);
        IndexNode index = (IndexNode) directory.getNode(target);
        System.out.println("at index node "+index.getNameNodeID());
		String key = request.getKey();
		VPointer leftNode = request.getLeftNode();
		VPointer rightNode = request.getRightNode();

		System.out.println("leftNode: "+leftNode);
		System.out.println("rightNode: "+rightNode);
		int position = request.getPosition();
		System.out.println("****FBTInsertModify position "+position);
        if (position == -1) {
            position = index.binaryLocate(key);
        }
        System.out.println("****FBTInsertModify position "+position);
		String transactionID = request.getTransactionID();
		//System.out.println("leftNode "+leftNode.toString());

		Locker locker = (Locker) NameNodeFBTProcessor.lookup("/locker");

		try {
			if (index.isLeafIndex()) {
				System.out.println("FBTInsertModify index is LeafIndex");
				index.addEntry(position + 1, key, rightNode);
				index.addPointer(position + 1, rightNode);

			} else {
				System.out.println("FBTInsertModify index is not LeafIndex");
				int partitionID = directory.getPartitionID();
				System.out.println("FBTInsertModifyRule position "+position);
				VPointer vPointer = index.getPointer(position);
				System.out.println("FBTInsertModifyRule vPointer "+
									vPointer.toString());
				locker.lock(transactionID, vPointer, Lock.X);
				PointerNode leftPointerNode =
				    (PointerNode) directory.getNode(vPointer);
				leftPointerNode.clearEntry();
				for (Iterator iter = leftNode.iterator(); iter.hasNext();) {
				    leftPointerNode.addEntry((Pointer) iter.next());
				}

				Pointer leftPointer = leftNode.getPointer(partitionID,
											directory.getOwner());
				if (leftPointer == null) {
					System.out.println("FBTInsertModify index leftPointer null");
				    PointerSet pointerSet = leftPointerNode.getEntry();
				    System.out.println("FBTInsertModify pointerSet "+pointerSet);
				    System.out.println("FBTInsertModify owner "+directory.getOwner());
					leftPointer =
					    (Pointer) pointerSet.get(
					    		//partitionID % pointerSet.size());
					    		(100+Integer.parseInt(directory.getOwner().substring(3,5)))
					    					% pointerSet.size());
					System.out.println("leftPointer: "+leftPointer);
					/*VPointer vp = index.getEntry(position);
					System.out.println("vp: "+vp);
                    if(directory.isLocal(vp)) {
                    	if (directory.isLocalDirectory(vp)) {
                    		Node node = directory.getNode(vp);
                    		node.free();
                    	} else {
                    		FBTDirectory targetDirectory =
                    			(FBTDirectory) NameNodeFBTProcessor.lookup("/directory."+vp.getPointer().getFBTOwner());
                    		Node node = targetDirectory.getNode(vp);
                    		node.free();
                    	}
                    }*/
				    index.replaceEntry(position, leftNode.getPointer());
				}

				locker.unlock(transactionID, vPointer);


				PointerNode newRightPointer = new PointerNode(directory);
				synchronized (directory.getLocalNodeMapping()) {
					directory.getLocalNodeMapping().put(newRightPointer.getNodeIdentifier().toString(),
										newRightPointer);
				}
				for (Iterator iter = rightNode.iterator(); iter.hasNext();) {
				    newRightPointer.addEntry((Pointer) iter.next());
				}
				index.addPointer(position + 1, newRightPointer.getPointer());

				Pointer rightPointer = rightNode.getPointer(partitionID,
													directory.getOwner());
				if (rightPointer == null) {
					System.out.println("FBTInsertModify index rightPointer null");
				    PointerSet pointerSet = newRightPointer.getEntry();
				    System.out.println("FBTInsertModify pointerSet: "+pointerSet);
				    System.out.println("FBTInsertModify owner: "+directory.getOwner());
				    rightPointer =
				        (Pointer) pointerSet.get(
				        		//partitionID
					    		(100+Integer.parseInt(directory.getOwner().substring(3,5)))
				        		% pointerSet.size());
				}
				System.out.println("rightPointer: "+rightPointer);
				index.addEntry(position + 1, key, rightPointer);
			}

			if (index.isOverEntry()) {
				System.out.println("FBTInsertModify index overEntry");
			    VPointer leftPointer = null;
			    VPointer rightPointer = null;

			    IndexNode rightIndex = index.split();
			    rightIndex.key2String();
			    index.key2String();
			    if (index.isRootIndex()) {
			    	System.out.println("FBTInsertModify index isRootIndex");
					IndexNode leftIndex = index.entryCopy();
					leftIndex.key2String();
					index.setLeafIndex(false);

					if (leftIndex.hasLocalDirectory()) {
						System.out.println("leftIndex "+leftIndex.getNameNodeID()+" has Local");
						leftPointer = leftIndex.getPointer();
					} else {
						System.out.println("leftIndex "+leftIndex.getNameNodeID()+" doesnt have Local");
						leftIndex.free();
					}

					if (rightIndex.hasLocalDirectory()) {
						System.out.println("rightIndex "+rightIndex.getNameNodeID()+" has Local");
						rightPointer = rightIndex.getPointer();
					} else {
						System.out.println("rightIndex "+rightIndex.getNameNodeID()+" doesnt have Local");
						rightIndex.free();
					}
			    } else {
			    	System.out.println("FBTInsertModify index is not RootIndex");
				    if (index.hasLocalDirectory()) {
				    	System.out.println("index "+index.getNameNodeID()+" has Local");
				        leftPointer = target;
				        System.out.println("leftPointer: "+leftPointer);
				    } else {
				    	System.out.println("index "+index.getNameNodeID()+" doesnt have Local");
				        index.free();
				    }

				    if (rightIndex.hasLocalDirectory()) {
				    	System.out.println("rightIndex "+rightIndex.getNameNodeID()+" has Local");
				        rightPointer = rightIndex.getPointer();
				        System.out.println("rightPointer: "+rightPointer);
				    } else {
				    	System.out.println("rightIndex "+rightIndex.getNameNodeID()+" doesnt have Local");
				        rightIndex.free();
				    }
			    }

	            _manager.dispatch(new FBTInsertModifyResponse(
	                    request, leftPointer, rightPointer, rightIndex.getKey()));
			} else {
				System.out.println("FBTInsertModifyRule manager.dispatch");
				_manager.dispatch(
				        new FBTInsertModifyResponse(request, null, null, null));
				System.out.println("FBTInsertModifyRule manager.dispatch done");
			}
			System.out.println("FBTInsertModifyRule finished. Current index "+index.getNameNodeID());
			index.key2String();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(key);
            System.out.println(leftNode);
            System.out.println(rightNode);
            System.out.println(request);
            System.out.println(index);
        }
    }

}

