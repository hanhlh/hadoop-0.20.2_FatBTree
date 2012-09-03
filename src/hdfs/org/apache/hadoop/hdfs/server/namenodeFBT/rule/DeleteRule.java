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
public class DeleteRule extends AbstractRule {

// constructors ///////////////////////////////////////////////////////////

    /**
     * B-Tree に対する挿入を行う DeleteRule を生成します.
     *
     * @param manager このルールを登録する RuleManager
     */
    public DeleteRule(RuleManager manager) {
	super(manager);
    }

    // interface AbstractRule /////////////////////////////////////////////////

    protected Class[] events() {
	return new Class[] { DeleteRequest.class };
    }

    protected void action(RuleEvent event) {
        DeleteRequest request = (DeleteRequest) event;
        FBTDirectory directory =
            (FBTDirectory) NameNodeFBTProcessor.lookup(request.getDirectoryName());
        NodeVisitorFactory visitorFactory = directory.getNodeVisitorFactory();
        NodeVisitor visitor = visitorFactory.createDeleteVisitor();

        visitor.setRequest(request);
        visitor.run();
        /* Revisionには無い部分。バックアップを置く操作 */
        /*if (directory.hasBackup()) {
            String backupDirectoryUrl = directory.getBackupUrl();
            Directory backupDirectory =
                (Directory) AutoDisk.lookup(backupDirectoryUrl);
            Call call = new Call(
                (Messenger) AutoDisk.lookup("/service/messenger"));
            DeleteRequest backupRequest =
                new DeleteRequest(backupDirectoryUrl, request.getKey());
            call.setRequest(backupRequest);
            call.setResponseClass(DeleteResponse.class);
            call.setDestination(
                backupDirectory.getMapping(directory.getPartitionID()));
            try {
                call.invoke();
            } catch (MessageException e) {
                e.printStackTrace();
            }
        }*/
        /* ------------ */
        _manager.dispatch(visitor.getResponse());
    }

}
