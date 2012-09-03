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
public class FBTDeleteModifyRequest extends Request{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final String _directoryName;

    private final VPointer _deleteNode;

    private final int _position;

    /**
     *
     */
    public FBTDeleteModifyRequest(String directoryName, VPointer target,
            						VPointer deleteNode, int position) {
        super(target);
        _directoryName = directoryName;
        _deleteNode = deleteNode;
        _position = position;
    }

    public String getDirectoryName() {
        return _directoryName;
    }

    public VPointer getDeleteNode() {
        return _deleteNode;
    }

    public int getPosition() {
        return _position;
    }

}
