/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.LinkedList;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class FBTDeleteModifyIncOptRequest extends DeleteRequest
							implements FBTModifyIncOptRequest {

	// instance attributes ////////////////////////////////////////////////////

    /**
     * 現在の排他ロック範囲で足りているかどうか
     */
    private boolean _isSafe;

    private final LinkedList _lockList;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * @param directoryName
     * @param key
     * @param target
     */
    public FBTDeleteModifyIncOptRequest(String directoryName,
            String key, VPointer target, LinkedList lockList) {
        super(directoryName, key, target);
        _isSafe = false;
        _lockList = lockList;
    }

    public FBTDeleteModifyIncOptRequest(String key,
            VPointer target, LinkedList lockList) {
        this(FBTDirectory.DEFAULT_NAME, key, target, lockList);
    }

    public FBTDeleteModifyIncOptRequest(String directoryName,
            String key, LinkedList lockList) {
        this(directoryName, key, null, lockList);
    }

    /**
     * @param key
     */
    public FBTDeleteModifyIncOptRequest(String key, LinkedList lockList) {
        this(FBTDirectory.DEFAULT_NAME, key, null, lockList);
    }

	public boolean getIsSafe() {
		return _isSafe;
	}

	public void setIsSafe(boolean isSafe) {
		_isSafe = isSafe;
	}
	public LinkedList getLockList() {
        return _lockList;
    }


}
