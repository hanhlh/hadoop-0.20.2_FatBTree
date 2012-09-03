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
public final class UnlockRequest extends Request{

	// constructors ///////////////////////////////////////////////////////////

    /**
     * ノードロック解除要求オブジェクトを生成します.
     *
     * @param target この要求の処理が最初に行われる node を指すポインタ
	 */
	public UnlockRequest(VPointer target) {
		super(target);
	}


}
