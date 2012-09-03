/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTInsertRootModifyRule extends AbstractRule {

	public FBTInsertRootModifyRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { FBTInsertRootModifyRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		StringUtility.debugSpace("FBTInsertRootModify action ");
		FBTInsertRootModifyRequest request =
					(FBTInsertRootModifyRequest) event;
		System.out.println("fbtdirectoryName: "+request.getDirectoryName());
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
		VPointer target = request.getTarget();
		String key = request.getKey();
		VPointer leftNode = request.getLeftNode();
		VPointer rightNode = request.getRightNode();
		System.out.println("target: "+target);
		System.out.println("leftNode: "+leftNode);
		System.out.println("rightNode: "+rightNode);

		IndexNode index = (IndexNode) directory.getNode(target);

		int partitionID = directory.getPartitionID();
		PointerNode newLeftPointer = new PointerNode(directory);
		synchronized (directory.getLocalNodeMapping()) {
			directory.getLocalNodeMapping().put(
					newLeftPointer.getNodeIdentifier().toString(),
					newLeftPointer);
		}

		for (Iterator iter = leftNode.iterator(); iter.hasNext();) {
		    newLeftPointer.addEntry((Pointer) iter.next());
		}
		index.addPointer(0, newLeftPointer.getPointer());

		System.out.println("fbtOwner: "+directory.getOwner());
		Pointer leftPointer = leftNode.getPointer(partitionID,
								directory.getOwner());
		System.out.println("leftPointer: "+leftPointer);
		if (leftPointer == null) {
		    PointerSet pointerSet = newLeftPointer.getEntry();
		    leftPointer =
		        (Pointer) pointerSet.get(partitionID % pointerSet.size());
		}
		index.addEntry(0, "/user/hanhlh/", leftPointer);
		//index.addEntry(0, key, leftPointer);

		PointerNode newRightPointer = new PointerNode(directory);
		synchronized (directory.getLocalNodeMapping()) {
			directory.getLocalNodeMapping().put(
					newRightPointer.getNodeIdentifier().toString(),
					newRightPointer);
		}
		//System.out.println("create new right pointer node "+newRightPointer.getNameNodeID());
		for (Iterator iter = rightNode.iterator(); iter.hasNext();) {
		    newRightPointer.addEntry((Pointer) iter.next());
		}
		index.addPointer(1, newRightPointer.getPointer());

		Pointer rightPointer = rightNode.getPointer(partitionID,
									directory.getOwner());
		if (rightPointer == null) {
		    PointerSet pointerSet = newRightPointer.getEntry();
		    rightPointer =
		        (Pointer) pointerSet.get(partitionID % pointerSet.size());
		}
		index.addEntry(1, key, rightPointer);

		//add by hanhlh
		index.set_isLeftest(true);
		index.set_isRightest(true);
		_manager.dispatch(new FBTInsertRootModifyResponse(request));

		System.out.println(
		        "FBTInsertRootModifyRule " + directory.getEntryCount());
    }
}


