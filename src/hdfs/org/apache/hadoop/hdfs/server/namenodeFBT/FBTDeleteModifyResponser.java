/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyResponse;


/**
 * @author hanhlh
 *
 */
public final class FBTDeleteModifyResponser extends Responser{
	private VPointer _deleteNode;

    /**
     *
     */
    public FBTDeleteModifyResponser() {
        super();
        _deleteNode = null;
    }

    public VPointer getDeleteNode() {
        return _deleteNode;
    }

    public synchronized void handleResult(CallResult result) {
        FBTDeleteModifyResponse response =
            (FBTDeleteModifyResponse) result.getResponse();

        if (response.getDeleteNode() != null) {
            _deleteNode = response.getDeleteNode();
        }

        super.handleResult(result);
    }

}
