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
public class FBTDeleteIncOptRule extends AbstractRule{

	public FBTDeleteIncOptRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { FBTDeleteIncOptRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		// TODO 自動生成されたメソッド・スタブ
		FBTDeleteIncOptRequest request = (FBTDeleteIncOptRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createDeleteVisitor();

        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());
	}

}
