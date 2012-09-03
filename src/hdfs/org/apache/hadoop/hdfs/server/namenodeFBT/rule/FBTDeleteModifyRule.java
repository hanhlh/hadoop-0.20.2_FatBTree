/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;

/**
 * @author hanhlh
 *
 */
public class FBTDeleteModifyRule extends AbstractRule {
	public FBTDeleteModifyRule(RuleManager manager) {
        super(manager);
    }

    protected Class[] events() {
        return new Class[] { FBTDeleteModifyRequest.class };
    }

    protected void action(RuleEvent event) {
        FBTDeleteModifyRequest request = (FBTDeleteModifyRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        VPointer target = request.getTarget();
        VPointer deleteNode = request.getDeleteNode();
        int position = request.getPosition();

        IndexNode index = (IndexNode) directory.getNode(target);
        if (index.isLeafIndex()) {
            index.removeEntry(position);
        } else {
            VPointer vPointer = index.getPointer(position);
            PointerNode pointer = (PointerNode) directory.getNode(vPointer);

            if (pointer.size() == 1) {
                pointer.free();
                index.removeEntry(position);
            } else {
                pointer.removeEntry((Pointer) deleteNode);

                VPointer child = index.getEntry(position);
                if (child.equals(deleteNode)) {
                    PointerSet pointerSet = pointer.getEntry();
                    Pointer newChild = (Pointer) pointerSet.get(
                            directory.getPartitionID() % pointerSet.size());
                    index.replaceEntry(position, newChild);
                }
            }
        }

        if (index.numberOfLocalEntry() == 0 && !index.isRootIndex()) {
            index.free();

            _manager.dispatch(new FBTDeleteModifyResponse(request, target));
        } else {
            _manager.dispatch(new FBTDeleteModifyResponse(request, null));
        }
    }
}
