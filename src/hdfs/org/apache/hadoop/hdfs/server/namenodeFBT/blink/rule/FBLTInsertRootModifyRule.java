/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;

import java.util.Iterator;


import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.AbstractRule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertRootModifyRule extends AbstractRule {

	/**
     * @param manager
     */
    public FBLTInsertRootModifyRule(RuleManager manager) {
        super(manager);
    }

    // interface AbstractRule /////////////////////////////////////////////////

    protected Class[] events() {
        return new Class[] { FBLTInsertRootModifyRequest.class };
    }

    protected void action(RuleEvent event) {
        /*FBLTInsertRootModifyRequest request =
            (FBLTInsertRootModifyRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        VPointer target = request.getTarget();
        String key = request.getKey();
        VPointer leftNode = request.getLeftNode();
        VPointer rightNode = request.getRightNode();
        VPointer dummies = request.getDummies();

        IndexNode index = (IndexNode) directory.getNode(target);

        int partitionID = directory.getPartitionID();
        PointerNode newLeftPointer = new PointerNode(directory);
        synchronized (directory.localNodeMapping) {
        	directory.localNodeMapping.put(newLeftPointer
        						.getNodeIdentifier().toString(), newLeftPointer);
        }

        for (Iterator iter = leftNode.iterator(); iter.hasNext();) {
            newLeftPointer.addEntry((Pointer) iter.next());
        }
        index.addPointer(0, newLeftPointer.getPointer());

        Pointer leftPointer = leftNode.getPointer(partitionID);

        if (leftPointer == null) {
            PointerSet pointerSet = newLeftPointer.getEntry();
            leftPointer = pointerSet.get(partitionID % pointerSet.size());
        }
        index.addEntry(0, "", leftPointer);

        PointerNode newRightPointer = new PointerNode(directory);
        synchronized (directory.localNodeMapping) {
        	directory.localNodeMapping.put(newRightPointer.
        			getNodeIdentifier().toString(), newRightPointer);
        }
        for (Iterator iter = rightNode.iterator(); iter.hasNext();) {
            newRightPointer.addEntry((Pointer) iter.next());
        }
        index.addPointer(1, newRightPointer.getPointer());

        Pointer rightPointer = rightNode.getPointer(partitionID);
        if (rightPointer == null) {
            PointerSet pointerSet = newRightPointer.getEntry();
            rightPointer = pointerSet.get(partitionID % pointerSet.size());
        }
        index.addEntry(1, key, rightPointer);

        char[] max = new char[] {Character.MAX_VALUE, Character.MAX_VALUE};
        index.set_nextKeyOnParent(String.valueOf(max));
        index.set_isLeftest(true);
        index.set_isRightest(true);

        directory.incrementTreeHeight();

        IndexNode child;
        leftPointer = leftNode.getPointer(partitionID);
        rightPointer = rightNode.getPointer(partitionID);
        if (leftPointer != null) {
            child = (IndexNode) directory.getNode(leftPointer);
        } else {
            child = (IndexNode) directory.getNode(rightPointer);
        }
        int leftPartID = directory.getNonLoopLeftPartitionID();
        if (leftPartID != Integer.MIN_VALUE) {
            VPointer dummyLink = dummies.getPointer(leftPartID);
            child.setDummyLink(dummyLink);
        }

        if (leftPointer != null && rightPointer != null) {
            child = (IndexNode) directory.getNode(rightPointer);
        }

        int rightPartID = directory.getNonLoopRightPartitionID();
        if (rightPartID != Integer.MIN_VALUE) {
            IndexNode dummy =
                (IndexNode) directory.getNode(child.getSideLink());

            leftPointer = leftNode.getPointer(rightPartID);
            if (leftPointer != null) {
                dummy.setSideLink(leftPointer);
            } else {
                dummy.setSideLink(rightNode.getPointer(rightPartID));
            }
        }

        _manager.dispatch(new FBLTInsertRootModifyResponse(request));
*/    }

}
