/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;


/**
 * @author hanhlh
 *
 */
public class FBTInsertModifyMarkOptRule extends AbstractRule {

	/**
     * @param manager
     */
    public FBTInsertModifyMarkOptRule(RuleManager manager) {
        super(manager);
    }

    protected Class[] events() {
        return new Class[] { FBTInsertModifyMarkOptRequest.class };
    }

    protected void action(RuleEvent event) {
    	System.out.println("FBTInsertModifyMarkOpt actions");
        FBTInsertModifyMarkOptRequest request =
            (FBTInsertModifyMarkOptRequest) event;

        String directoryName = request.getDirectoryName();
        System.out.println("*****request: "+request);
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(
            					directoryName);
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createInsertModifyVisitor();

        visitor.setRequest(request);
        visitor.run();
        NameNode.LOG.info("FBTInsertModifyMarkOpt manager "+_manager.toString());
        NameNode.LOG.info("FBTInsertModifyMarkOpt getResponse "+visitor.getResponse());
        _manager.dispatch(visitor.getResponse());
    }

}
