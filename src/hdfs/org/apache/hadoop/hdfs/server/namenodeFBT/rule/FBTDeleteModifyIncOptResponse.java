/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


/**
 * @author hanhlh
 *
 */
public class FBTDeleteModifyIncOptResponse extends DeleteResponse{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final VPointer _deleteNode;

    private final boolean _needRestart;

    public FBTDeleteModifyIncOptResponse(FBTDeleteModifyIncOptRequest request,
        	boolean isSuccess, VPointer deleteNode, boolean needRestart) {
    super(request, isSuccess);
    _deleteNode = deleteNode;
    _needRestart = needRestart;
}

    public VPointer getDeleteNode() {
    return _deleteNode;
}

    public boolean needRestart() {
    return _needRestart;
    }
}

