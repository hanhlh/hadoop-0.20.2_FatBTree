/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class DeleteRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	// instance attributes ///////////////////////////////////////////////

    /**
     * 要求に対する処理が行われる Directory の名前.
     * "/service/directory/" または "/service/directory/backup".
     */
    protected final String _directoryName;

    /**
     * 削除するキー値
     */
    protected final String _key;

    /**
     * 要求に対する処理が行われる最上位の node を指すポインタ.
     * null のときは MetaNode から処理を行われる.
     */
    protected VPointer _target;

    // constructors //////////////////////////////////////////////////////

    /**
     * 新しい削除要求オブジェクトを生成します.
     *
     * @param directoryName この要求の処理が行われる Directory の名前
     * @param key 削除するキー値
     * @param target この要求の処理が最初に行われる node を指すポインタ
     */
    public DeleteRequest(
            String directoryName, String key, VPointer target) {
        super();
        _directoryName = directoryName;
        _key = key;
        _target = target;
    }

    /**
     * 新しい削除要求オブジェクトを生成します.
     *
     * @param directoryName この要求の処理が行われる Directory の名前
     * @param key 削除するキー値
     */
    public DeleteRequest(String directoryName, String key) {
        this(directoryName, key, null);
    }

    /**
     * 新しい削除要求オブジェクトを生成します.
     *
     * @param key 削除するキー値
     * @param target この要求の処理が最初に行われる node を指すポインタ
     */
    public DeleteRequest(VPointer target, String key) {
        this(FBTDirectory.DEFAULT_NAME, key, target);
    }

    /**
     * 新しい削除要求オブジェクトを生成します.
     *
     * @param key 削除するキー値
     */
    public DeleteRequest(String key) {
        this(FBTDirectory.DEFAULT_NAME, key, null);
    }

    /**
     * 新しい挿入要求オブジェクトを生成します.
     */
    public DeleteRequest() {
        this(FBTDirectory.DEFAULT_NAME, null, null);
    }

    // accessors //////////////////////////////////////////////////////////////

    public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }

    public VPointer getTarget() {
        return _target;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

	// instance methods ///////////////////////////////////////////////////////

	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.append("directoryName= ");
		buf.append(_directoryName);
		buf.append(", key= ");
		buf.append(_key);
		buf.append(", target= ");
		buf.append(_target);
		return buf.toString();
	}

}
