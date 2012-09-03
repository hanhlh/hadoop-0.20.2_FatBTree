/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public class DeleteResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


// instance attributes ///////////////////////////////////////////////

    /** B-Tree のデバッグダンプを格納するバッファ */
    private final boolean _isSuccess;

    // constructors //////////////////////////////////////////////////////

    /**
     * AbstractDeleteRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (AbstractDeleteRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (AbstractDeleteRequest)
     * @param isSuccess 削除が成功したかどうか
     */
    public DeleteResponse(DeleteRequest request, boolean success){
    	super(request);
	_isSuccess = success;
    }

    public DeleteResponse(DeleteRequest request) {
        this(request, true);
    }

    // accessors /////////////////////////////////////////////////////////

    public boolean isSuccess() {
        return _isSuccess;
    }

}
