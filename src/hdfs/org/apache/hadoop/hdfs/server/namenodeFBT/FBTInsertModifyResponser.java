/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyResponse;

/**
 * @author hanhlh
 *
 */
public final class FBTInsertModifyResponser extends Responser{

// instance attributes ////////////////////////////////////////////////////

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    private String _boundKey;

    /**
     *
     */
    public FBTInsertModifyResponser() {
        super();
        _leftNode = new PointerSet();
        _rightNode = new PointerSet();
        _boundKey = null;
    }

    // accessors //////////////////////////////////////////////////////////////

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public String getBoundKey() {
        return _boundKey;
    }

    public synchronized void handleResult(CallResult result) {
        FBTInsertModifyResponse response =
            (FBTInsertModifyResponse) result.getResponse();

	    if (response.getLeftNode() != null) {
	        _leftNode.add(response.getLeftNode());
	    }
	    if (response.getRightNode() != null) {
	        _rightNode.add(response.getRightNode());
	    }
	    _boundKey = response.getBoundKey();

	    super.handleResult(result);
    }

}
