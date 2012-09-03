package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

public class GetBlockLocationsRule extends AbstractRule{

	public GetBlockLocationsRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] {GetBlockLocationsRequest.class};
	}

	@Override
	protected void action(RuleEvent event) {
		StringUtility.debugSpace("GetBlockLocationsRule.action");

		GetBlockLocationsRequest request = (GetBlockLocationsRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createGetBlockLocationsVisitor();
        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());
	}

}

