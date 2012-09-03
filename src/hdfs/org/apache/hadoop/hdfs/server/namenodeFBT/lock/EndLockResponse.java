/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public final class EndLockResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * EndLockRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (EndLockRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (EndLockRequest)
     */
    public EndLockResponse(EndLockRequest request) {
        super(request);
    }
}
