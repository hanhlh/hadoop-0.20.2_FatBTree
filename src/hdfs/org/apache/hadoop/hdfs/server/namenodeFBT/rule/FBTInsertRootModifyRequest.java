/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public final class FBTInsertRootModifyRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final String _directoryName;

    private final String _key;

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    /**
     * @param target
     */
    public FBTInsertRootModifyRequest(String directoryName, VPointer target,
            				String key, VPointer leftNode, VPointer rightNode) {
        super(target);
        _directoryName = directoryName;
        _key = key;
        _leftNode = leftNode;
        _rightNode = rightNode;
    }

    public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }


}
