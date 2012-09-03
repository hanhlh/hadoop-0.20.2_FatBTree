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
public class FBTInsertMarkOptRule extends AbstractRule{

	/**
     * @param manager
     */
    public FBTInsertMarkOptRule(RuleManager manager) {
        super(manager);
    }

    protected Class[] events() {
        return new Class[] { FBTInsertMarkOptRequest.class };
    }

    protected void action(RuleEvent event) {
    	StringUtility.debugSpace("FBTInsertMarkOptRule.action");
        FBTInsertMarkOptRequest request = (FBTInsertMarkOptRequest) event;
        System.out.println("directory name: "+request.getDirectoryName());
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(
            		request.getDirectoryName());
        System.out.println("fbtdirectory "+directory);
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createInsertVisitor();
        visitor.setRequest(request);
        /*System.out.println("FBTInsertMarkOptRequest visitor.run()"+
				visitor.toString());
        */visitor.run();
        /*System.out.println("FBTInsertMarkOptRequest getResponse "+
        				visitor.getResponse());
*/
        _manager.dispatch(visitor.getResponse());
        System.out.println("FBTInsertMarkOptRequest dispatch done");
    }

}
