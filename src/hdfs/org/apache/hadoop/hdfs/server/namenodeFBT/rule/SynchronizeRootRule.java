package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.List;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * For synchronize root right after being initialized
 * */
public class SynchronizeRootRule extends AbstractRule{

	public SynchronizeRootRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { SynchronizeRootRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		StringUtility.debugSpace("SynchronizeRoot action ");
		/*SynchronizeRootRequest request = (SynchronizeRootRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());

        int updateChildPosition = request.getPosition();
		int partID = request.getPartID();

		VPointer rootPointer = new Pointer(partID, 0);
        IndexNode root = (IndexNode) directory.getNode(rootPointer);

        VPointer recorrectChild = request.getUpdateChild().getPointer();

        root.replaceEntry(updateChildPosition, recorrectChild);

        root.key2String();
        _manager.dispatch(new SynchronizeRootResponse(request));
*/
	}

}
