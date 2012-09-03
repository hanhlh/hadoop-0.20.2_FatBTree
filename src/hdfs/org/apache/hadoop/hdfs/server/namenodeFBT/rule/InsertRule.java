/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;


/**
 * @author hanhlh
 *
 */
public class InsertRule extends AbstractRule {

	public InsertRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { InsertRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		System.out.println("InsertRule called");
		InsertRequest request = (InsertRequest) event;

        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());

        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createInsertVisitor();
        visitor.setRequest(request);
        visitor.run();
        _manager.dispatch(visitor.getResponse());
	}

}
