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
public class GetAdditionalBlockRule extends AbstractRule {

	public GetAdditionalBlockRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { GetAdditionalBlockRequest.class };
		}

	@Override
	protected void action(RuleEvent event) {
		GetAdditionalBlockRequest request = (GetAdditionalBlockRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createGetAdditionalBlockVisitor();
        System.out.println("GetAdditionalBlockRequest.NodeVisitor "+visitor);
        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());
	}

}


