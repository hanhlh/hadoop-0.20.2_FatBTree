/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.response;


import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.request.AbstractMigrateRequest;

/**
 * @author hanhlh
 *
 */
public abstract class AbstractMigrateResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// instance attributes ////////////////////////////////////////////////////

    private final boolean _isSuccess;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * AbstractMigrateRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (AbstractMigrateRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (AbstractMigrateRequest)
     */
    public AbstractMigrateResponse(
            AbstractMigrateRequest request, boolean isSuccess) {
        super(request);
        _isSuccess = isSuccess;
    }

    public AbstractMigrateResponse(AbstractMigrateRequest request) {
        this(request, true);
    }

    // accessors //////////////////////////////////////////////////////////////

    public boolean isSuccess() {
        return _isSuccess;
    }

}
