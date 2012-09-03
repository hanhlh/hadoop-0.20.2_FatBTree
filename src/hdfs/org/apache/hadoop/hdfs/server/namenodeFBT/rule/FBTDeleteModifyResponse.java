/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class FBTDeleteModifyResponse extends Response{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final VPointer _deleteNode;

    /**
     * @param source
     */
    public FBTDeleteModifyResponse(FBTDeleteModifyRequest request,
            									VPointer deleteNode) {
        super(request);
        _deleteNode = deleteNode;
    }

    public VPointer getDeleteNode() {
        return _deleteNode;
    }

}
