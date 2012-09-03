/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;


import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public final class EndLockRequest extends Request{

	// constructors ///////////////////////////////////////////////////////////

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * ノードロック解除と Transaction-ID 除去の要求オブジェクトを生成します.
     *
     * @param target この要求の処理が最初に行われる node を指すポインタ
     */
    public EndLockRequest(VPointer target) {
        super(target);
    }

}
