/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;


/**
 * @author hanhlh
 *
 */
public class FBLTInsertRootModifyResponse extends Response {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public FBLTInsertRootModifyResponse(FBLTInsertRootModifyRequest request) {
        super(request);
    }

    private FBLTInsertRootModifyResponse() {
        super();
    }
}
