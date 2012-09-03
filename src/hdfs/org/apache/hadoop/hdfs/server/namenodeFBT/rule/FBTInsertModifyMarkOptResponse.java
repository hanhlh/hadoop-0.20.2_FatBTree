/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


/**
 * @author hanhlh
 *
 */
public final class FBTInsertModifyMarkOptResponse extends InsertResponse {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final String _boundKey;

    private final int _mark;

    private final VPointer _lockList;

    private final int _lockRange;


    /**
     * @param request
     * @param vp
     */
    public FBTInsertModifyMarkOptResponse(
            FBTInsertModifyMarkOptRequest request, VPointer vp,
            VPointer leftNode, VPointer rightNode, String boundKey,
            int mark, VPointer lockList, int lockRange, INode inode) {
        super(request, vp, inode);
        _leftNode = leftNode;
        _rightNode = rightNode;
        _boundKey = boundKey;
        _mark = mark;
        _lockList = lockList;
        _lockRange = lockRange;
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

    public int getMark() {
        return _mark;
    }

    public VPointer getLockList() {
        return _lockList;
    }

    public int getLockRange() {
        return _lockRange;
    }
}
