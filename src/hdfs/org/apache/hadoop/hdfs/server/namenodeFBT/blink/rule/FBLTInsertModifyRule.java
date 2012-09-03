/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;

import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Locker;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.AbstractRule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertModifyRule extends AbstractRule{

	public FBLTInsertModifyRule(RuleManager manager) {
        super(manager);
    }

    protected Class[] events() {
        return new Class[] { FBLTInsertModifyRequest.class };
    }

    protected void action(RuleEvent event) {
    	System.out.println("FBLTInsertModify actions");

    	FBLTInsertModifyRequest request = (FBLTInsertModifyRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());

        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        System.out.println("NodeVisitorFactory "+visitorFactory.toString());
        NodeVisitor visitor = visitorFactory.createInsertVisitor();
        System.out.println("NodeVisitor "+visitor.toString());

        System.out.println("Target "+request.getTarget());
        visitor.setRequest(request);
        visitor.run();
        _manager.dispatch(visitor.getResponse());

        /*
        VPointer target = request.getTarget();
        System.out.println("target "+target.toString());
        IndexNode index = (IndexNode) directory.getNode(target);
        String key = request.getKey();
        VPointer leftNode = request.getLeftNode();
        VPointer rightNode = request.getRightNode();

        int position = request.getPosition();
        if (position == Integer.MIN_VALUE) {
            position = index.binaryLocate(key);
            index.addEntry(position + 1, key, rightNode);
            if (index.isLeafIndex()) {
                index.addPointer(position + 1, rightNode);
            } else {
                PointerNode pNode = new PointerNode(directory);
                directory.localNodeMapping.put(pNode.getNodeID(), pNode);

                pNode.addEntry(rightNode.getPointer());
                index.addPointer(position + 1, pNode.getPointer());
            }
        } else {
            if (index.isLeafIndex()) {
                index.addEntry(position + 1, key, rightNode);
                index.addPointer(position + 1, rightNode);
            } else {
                String transactionID = request.getTransactionID();
                Locker locker = (Locker) NameNodeFBTProcessor.
                								lookup("/locker");

                int partitionID = directory.getPartitionID();

                VPointer vPointer = index.getPointer(position);
                locker.lock(transactionID, vPointer, Lock.X + 1000);
                PointerNode leftPointerNode =
                    (PointerNode) directory.getNode(vPointer);
                leftPointerNode.clearEntry();
                for (Iterator iter = leftNode.iterator(); iter.hasNext();) {
                    leftPointerNode.addEntry((Pointer) iter.next());
                }

                Pointer leftPointer = leftNode.getPointer(partitionID);
                if (leftPointer == null) {
                    PointerSet pointerSet = leftPointerNode.getEntry();
                    leftPointer =
                        pointerSet.get(partitionID % pointerSet.size());

                    index.replaceEntry(position, leftNode.getPointer());
                }

                locker.unlock(transactionID, vPointer);

                PointerNode newRightPointer = new PointerNode(directory);
                directory.localNodeMapping.put(newRightPointer.getNodeID(),
                									newRightPointer);
                for (Iterator iter = rightNode.iterator(); iter.hasNext();) {
                    newRightPointer.addEntry((Pointer) iter.next());
                }
                index.addPointer(position + 1, newRightPointer.getPointer());

                Pointer rightPointer = rightNode.getPointer(partitionID);
                if (rightPointer == null) {
                    PointerSet pointerSet = newRightPointer.getEntry();
                    rightPointer =
                        pointerSet.get(partitionID % pointerSet.size());
                }
                index.addEntry(position + 1, key, rightPointer);
            }
        }

        if (index.isOverEntry()) {
            VPointer leftPointer = null;
            VPointer rightPointer = null;

            IndexNode rightIndex = (IndexNode) index.split();
            String boundKey = rightIndex.getKey();
            if (index.isRootIndex()) {
                IndexNode leftIndex = (IndexNode) index.entryCopy();
                index.setLeafIndex(false);
                index.setSideLink(rightIndex.getSideLink());

                if (leftIndex.hasLocal()) {
                    leftPointer = leftIndex.getPointer();
                } else {
                    leftIndex.free();
                }

                IndexNode dummy = new IndexNode(directory);
                directory.localNodeMapping.put(dummy.getNodeID(), dummy);

                VPointer dummyPointer = dummy.getPointer();
                dummy.setDummy(true);
                dummy.setLeafIndex(rightIndex.isLeafIndex());
                if (rightIndex.hasLocal()) {
                    rightIndex.setSideLink(dummyPointer);
                    rightPointer = rightIndex.getPointer();
                } else {
                    leftIndex.setSideLink(dummyPointer);
                    rightIndex.free();
                }

                _manager.dispatch(new FBLTInsertModifyResponse(request,
                        leftPointer, rightPointer, boundKey, dummyPointer));
            } else {
                if (index.hasLocal()) {
                    leftPointer = target;

                    if (rightIndex.hasLocal()) {
                        rightPointer = rightIndex.getPointer();
                    } else {
                        index.setSideLink(rightIndex.getSideLink());
                        rightIndex.free();
                    }
                } else {
                    rightPointer = target;

                    index.clear();
                    for (int i = 0; i < rightIndex.size(); i++) {
                        index.addEntry(i, rightIndex.getKey(i),
                                rightIndex.getEntry(i));
                        index.addPointer(i, rightIndex.getPointer(i));
                    }
                    index.set_nextKeyOnParent(rightIndex.get_nextKeyOnParent());
                    index.set_isLeftest(rightIndex.is_isLeftest());
                    index.set_isRightest(rightIndex.is_isRightest());
                    index.setSideLink(rightIndex.getSideLink());

                    while (rightIndex.size() > 0) {
                        rightIndex.removeEntry(0);
                    }
                    rightIndex.free();
                }

                _manager.dispatch(new FBLTInsertModifyResponse(
                        request, leftPointer, rightPointer, boundKey, null));
            }
        } else {
            _manager.dispatch(new FBLTInsertModifyResponse(
                    request, null, null, null, null));
        }*/
    }

}
