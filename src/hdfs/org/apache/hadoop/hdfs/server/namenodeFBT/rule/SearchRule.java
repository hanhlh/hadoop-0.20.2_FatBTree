/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Date;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public class SearchRule extends AbstractRule {

	public SearchRule(RuleManager manager) {
		super(manager);
	}

	@Override
	protected Class[] events() {
		return new Class[] { SearchRequest.class };
	}

	@Override
	protected void action(RuleEvent event) {
		//StringUtility.debugSpace("SearchRule action");
		SearchRequest request = (SearchRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        //System.out.println("directory name: "+request.getDirectoryName());
        //System.out.println ("FBTDirectory "+directory);
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createSearchVisitor();
        visitor.setRequest(request);
        Date start = new Date();
        visitor.run();
        _manager.dispatch(visitor.getResponse());
        NameNode.LOG.info("searchCore,"+(new Date().getTime()-start.getTime())/1000.0);
	}

}
