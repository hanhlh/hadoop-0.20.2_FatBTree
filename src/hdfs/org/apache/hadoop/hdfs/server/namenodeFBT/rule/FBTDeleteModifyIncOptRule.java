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
public class FBTDeleteModifyIncOptRule extends AbstractRule{

	public FBTDeleteModifyIncOptRule(RuleManager manager) {
		super(manager);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	@Override
	protected Class[] events() {
		return new Class[] { FBTDeleteModifyIncOptRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		FBTDeleteModifyIncOptRequest request =
            (FBTDeleteModifyIncOptRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createDeleteModifyVisitor();

        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());
    }
}
