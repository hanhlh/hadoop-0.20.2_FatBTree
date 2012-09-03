/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.request;


import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public abstract class AbstractMigrateRequest extends Request{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


// instance attributes ////////////////////////////////////////////////////

    /**
     * 要求に対する処理が行われる Directory の名前．
     * "/service/directory/" または "/service/directory/backup"．
     */
    protected final String _directoryName;

    /**
     * データを移動する方向．
     * true のときは左で，false のときは右．
     */
    protected final boolean _side;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * 新しいマイグレーション要求オブジェクトを生成します．
     *
     * @param directoryName この要求の処理が行われる Directory の名前
     * @param side データを移動する方向(true のときは左で，false のときは右)
     */
    public AbstractMigrateRequest(
            String directoryName, boolean side, VPointer target) {
        super(target);
        _directoryName = directoryName;
        _side = side;
    }

    /**
     * 新しいマイグレーション要求オブジェクトを生成します．
     *
     * @param directoryName この要求の処理が行われる Directory の名前
     * @param side データを移動する方向(true のときは左で，false のときは右)
     */
    public AbstractMigrateRequest(String directoryName, boolean side) {
        this(directoryName, side, null);
    }

    /**
     * 新しいマイグレーション要求オブジェクトを生成します．
     *
     * @param side データを移動する方向(true のときは左で，false のときは右)
     */
    public AbstractMigrateRequest(boolean side) {
        this(FBTDirectory.DEFAULT_NAME, side);
    }

    /**
     * 新しいマイグレーション要求オブジェクトを生成します．
     */
    public AbstractMigrateRequest() {
        this(FBTDirectory.DEFAULT_NAME, true);
    }

    // accessors //////////////////////////////////////////////////////////////

    public String getDirectoryName() {
        return _directoryName;
    }

    public boolean getSide() {
        return _side;
    }

}
