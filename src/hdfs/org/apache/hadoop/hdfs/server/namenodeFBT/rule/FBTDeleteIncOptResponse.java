/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public class FBTDeleteIncOptResponse extends DeleteResponse{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * @param request
     * @param isSuccess
     */
    public FBTDeleteIncOptResponse(FBTDeleteIncOptRequest request,
            boolean isSuccess) {
        super(request, isSuccess);
    }
}
