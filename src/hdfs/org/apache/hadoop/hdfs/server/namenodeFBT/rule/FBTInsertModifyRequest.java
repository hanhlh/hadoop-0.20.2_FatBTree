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
public final class FBTInsertModifyRequest extends Request {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final String _directoryName;

    private final String _key;

    private final String _fileName;

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final int _position;

    /**
     * @param target
     */
    public FBTInsertModifyRequest(String directoryName, VPointer target,
            String key, String fileName, VPointer leftNode, VPointer rightNode,
            int position) {
        super(target);
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _position = position;
    }

    public FBTInsertModifyRequest(String directoryName, VPointer target,
            String key, String fileName, VPointer leftNode, VPointer rightNode) {
        super(target);
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _position = -1;
    }

    public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }

    public String getFileName() {
        return _fileName;
    }

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public int getPosition() {
        return _position;
    }

}
