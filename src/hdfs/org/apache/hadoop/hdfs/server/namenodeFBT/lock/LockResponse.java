package org.apache.hadoop.hdfs.server.namenodeFBT.lock;


import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

public final class LockResponse extends Response {

	// Constructors ///////////////////////////////////////////////////////////

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * LockRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (LockRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (LockRequest)
     */
    public LockResponse(LockRequest request) {
        super(request);
    }

}
