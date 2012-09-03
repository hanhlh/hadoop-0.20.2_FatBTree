package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

public final class FBTInsertModifyResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final String _boundKey;
	// Constructors ///////////////////////////////////////////////////////////

    /**
     * @param request
     */
    public FBTInsertModifyResponse(FBTInsertModifyRequest request,
    		VPointer leftNode, VPointer rightNode, String boundKey) {
        super(request);
        _leftNode = leftNode;
        _rightNode = rightNode;
        _boundKey = boundKey;
    }

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public String getBoundKey() {
        return _boundKey;
    }
}
