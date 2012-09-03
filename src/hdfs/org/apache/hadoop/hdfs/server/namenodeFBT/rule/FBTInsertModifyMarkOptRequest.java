package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.io.Serializable;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


/**
 * @author hanhlh
 *
 */
public final class FBTInsertModifyMarkOptRequest extends InsertRequest
							implements FBTModifyMarkOptRequest{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * ���ߤ���¾��å��ϰϤ�­��Ƥ��뤫�ɤ���
     */
    private boolean _isSafe;

    private int _height;

    private int _mark;

    private VPointer _lockList;

    private int _lockRange;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * @param directoryName
     * @param key
     * @param value
     * @param target
     */
    public FBTInsertModifyMarkOptRequest(
            String directoryName, String key, String fileName,
            LeafValue value, LeafEntry entry,
            VPointer target, int height, int mark,
            VPointer lockList, int lockRange) {
        super(directoryName, key, fileName, value, entry, target);
        _isSafe = false;
        _height = height;
        _mark = mark;
        _lockList = lockList;
        _lockRange = lockRange;
    }

    /**
     * @param key
     * @param value
     * @param entry = null
     */
    public FBTInsertModifyMarkOptRequest(String key,
    		String fileName, LeafValue value,
            VPointer target, int height, int mark,
            VPointer lockList, int lockRange) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, value, null,
                target, height, mark, lockList, lockRange);
    }

    public FBTInsertModifyMarkOptRequest(String directoryName, String key,
    		String fileName, LeafValue value,
            VPointer target, int height, int mark,
            VPointer lockList, int lockRange) {
        this(directoryName, key, fileName, value, null,
                target, height, mark, lockList, lockRange);
    }

    /**
     * @param key
     * @param value = null
     * @param entry
     */
    public FBTInsertModifyMarkOptRequest(String key,
    		String fileName, LeafValue value, LeafEntry entry,
            VPointer target, int height, int mark,
            VPointer lockList, int lockRange) {

        this(FBTDirectory.DEFAULT_NAME, key, fileName, value, entry,
                target, height, mark, lockList, lockRange);
    }

    public FBTInsertModifyMarkOptRequest(String key, INode inode,
			VPointer target, int height, int mark,
			VPointer lockList, int lockRange) {
    	super(FBTDirectory.DEFAULT_NAME, key, inode, target);
    	_isSafe = false;
        _height = height;
        _mark = mark;
        _lockList = lockList;
        _lockRange = lockRange;
	}


	public FBTInsertModifyMarkOptRequest(String key,
			VPointer target, int height, int mark,
			VPointer lockList, int lockRange,
			PermissionStatus ps, String holder,
			String clientMachine,
			//boolean overwrite, boolean append,
			short replication, long blockSize, boolean isDirectory,
			DatanodeDescriptor clientNode,
			boolean inheritPermission) {
		super(FBTDirectory.DEFAULT_NAME, key,
				ps, holder,clientMachine,
				//overwrite, append,
				replication,
				blockSize, target, isDirectory,
				clientNode,
				inheritPermission);
		_isSafe = false;
        _height = height;
        _mark = mark;
        _lockList = lockList;
        _lockRange = lockRange;
	}

	public FBTInsertModifyMarkOptRequest(String directoryName, String key,
			VPointer target, int height, int mark,
			VPointer lockList, int lockRange,
			PermissionStatus ps, String holder,
			String clientMachine,
			//boolean overwrite, boolean append,
			short replication, long blockSize, boolean isDirectory,
			DatanodeDescriptor clientNode,
			boolean inheritPermission) {
		super(directoryName, key,
				ps, holder,clientMachine,
				//overwrite, append,
				replication,
				blockSize, target, isDirectory,
				clientNode,
				inheritPermission);
		_isSafe = false;
        _height = height;
        _mark = mark;
        _lockList = lockList;
        _lockRange = lockRange;
	}
	public boolean getIsSafe() {
        return _isSafe;
    }

    public int getHeight() {
        return _height;
    }

    public int getMark() {
        return _mark;
    }

    public VPointer getLockList() {
        return _lockList;
    }

    public int getLockRange() {
        return _lockRange;
    }

    public void setIsSafe(boolean isSafe) {
        _isSafe = isSafe;
    }

    public void setHeight(int height) {
        _height = height;
    }

    public void setMark(int mark) {
        _mark = mark;
    }

    public void setLockList(VPointer lockList) {
        _lockList = lockList;
    }

    public void setLockRange(int lockRange) {
        _lockRange = lockRange;
    }

	@Override
	public String toString() {
		return super.toString();
	}

}
