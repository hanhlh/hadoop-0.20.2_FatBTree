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
public class RangeSearchRule extends AbstractRule {
	public RangeSearchRule(RuleManager manager) {
        super(manager);
    }

    protected Class[] events() {
        return new Class[] { RangeSearchRequest.class };
    }

    protected void action(RuleEvent event) {
    	StringUtility.debugSpace("RangeSearchRule action");
        RangeSearchRequest request = (RangeSearchRequest) event;
        System.out.println("fbtDirectoryname,"+request.getDirectoryName());
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createRangeSearchVisitor();

        visitor.setRequest(request);
        visitor.run();

        _manager.dispatch(visitor.getResponse());
    }

}
