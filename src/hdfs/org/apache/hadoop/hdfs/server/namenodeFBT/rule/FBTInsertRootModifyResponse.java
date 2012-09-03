/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public final class FBTInsertRootModifyResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// Constructors ///////////////////////////////////////////////////////////

    /**
     * @param request
     */
    public FBTInsertRootModifyResponse(FBTInsertRootModifyRequest request) {
        super(request);
    }

}
