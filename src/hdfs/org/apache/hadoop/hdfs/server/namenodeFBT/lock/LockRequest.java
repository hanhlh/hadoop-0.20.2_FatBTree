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
public final class LockRequest extends Request {

// instance attributes ////////////////////////////////////////////////////

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * ロックのモード(S、Xなど)
	 */
	private final int _mode;

    // constructors ///////////////////////////////////////////////////////////

	/**
     * 新しいノードロック要求オブジェクトを生成します.
     *
     * @param target この要求の処理が最初に行われる node を指すポインタ
	 * @param mode 要求するロックのモード
	 */
	public LockRequest(VPointer target, int mode) {
		super(target);
		_mode = mode;
	}

	// accessors //////////////////////////////////////////////////////////////

	public int getMode() {
		return _mode;
	}

    // instance methods ///////////////////////////////////////////////////////

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(", mode= ");
        buf.append(_mode);

        return buf.toString();
    }

}
