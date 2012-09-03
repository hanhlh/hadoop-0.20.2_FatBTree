/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class CompleteFileRule extends AbstractRule{

	public CompleteFileRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { CompleteFileRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		StringUtility.debugSpace("CompleteFileRule action");
		CompleteFileRequest request = (CompleteFileRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.
            		lookup(request.getDirectoryName());

        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createCompleteFileVisitor();

        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());

	}

}
