/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertModifyResponse extends Response{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final String _boundKey;

    private final VPointer _dummy;

    public FBLTInsertModifyResponse(FBLTInsertModifyRequest request,
            VPointer leftNode, VPointer rightNode, String boundKey,
            VPointer dummy) {
        super(request);
        _leftNode = leftNode;
        _rightNode = rightNode;
        _boundKey = boundKey;
        _dummy = dummy;
    }

    private FBLTInsertModifyResponse(VPointer leftNode, VPointer rightNode,
            String boundKey, VPointer dummy) {
        super();
        _leftNode = leftNode;
        _rightNode = rightNode;
        _boundKey = boundKey;
        _dummy = dummy;
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

    public VPointer getDummy() {
        return _dummy;
    }

}
