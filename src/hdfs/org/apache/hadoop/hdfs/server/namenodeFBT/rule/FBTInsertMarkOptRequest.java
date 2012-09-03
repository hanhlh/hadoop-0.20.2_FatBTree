/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


/**
 * @author hanhlh
 *
 */
public class FBTInsertMarkOptRequest extends InsertRequest
									implements FBTMarkOptRequest{


	 /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * INC-OPT �ѿ� l
     */
    private int _length;

    /**
     * INC-OPT �ѿ� h
     */
    private int _height;

    private int _mark;

    private String _directoryName;

 // constructors ///////////////////////////////////////////////////////////

    /**
     * @param directoryName
     * @param key
     * @param value
     * @param target
     */
    public FBTInsertMarkOptRequest(String directoryName, String key,
    		String fileName,
            LeafValue value, VPointer target) {
        super(directoryName, key, fileName, value, null,target);
        _length = Integer.MAX_VALUE;
        _height = 0;
        _mark = 0;
    }
    public FBTInsertMarkOptRequest(String directoryName, String key,
    		String fileName,
            LeafEntry entry, VPointer target) {
        super(directoryName, key, fileName, null, entry,target);
        _length = Integer.MAX_VALUE;
        _height = 0;
        _mark = 0;
    }

    public FBTInsertMarkOptRequest(
            String directoryName, String key, String fileName, LeafValue value) {
        this(directoryName, key, fileName, value, null);
    }

    /**
     * @param key
     * @param value
     */
    public FBTInsertMarkOptRequest(String key, String fileName,
    								LeafValue value) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, value, null);
    }
    public FBTInsertMarkOptRequest (String key, String fileName,
    								LeafEntry entry) {
    	this (FBTDirectory.DEFAULT_NAME, key, fileName, entry, null);
    }

    //key=srcName
    public FBTInsertMarkOptRequest(String directoryName, String key,
    								INode inode, VPointer target,
    								boolean isDirectory) {
    	super(directoryName, key, inode, target);
    	_length = Integer.MAX_VALUE;
        _height = 0;
        _mark = 0;
    }

    public FBTInsertMarkOptRequest(String key, INode inode,
    									boolean isDirectory) {
    	this(FBTDirectory.DEFAULT_NAME, key, inode, null, isDirectory);
    }

    public FBTInsertMarkOptRequest(String directoryName, String key,
			PermissionStatus ps, VPointer target, boolean isDirectory) {
		super(directoryName, key, ps, target, isDirectory);
		_length = Integer.MAX_VALUE;
		_height = 0;
		_mark = 0;
		System.out.println("FBTInsertMarkOptRequest fbtdirectoryName: "+directoryName);
	}
    public FBTInsertMarkOptRequest(String key, PermissionStatus ps,
    								boolean isDirectory) {
    	this(FBTDirectory.DEFAULT_NAME, key, ps, null, isDirectory);
    	System.out.println("FBTInsertMarkOptRequest fbtdirectoryName default: "+FBTDirectory.DEFAULT_NAME);
    }

	public FBTInsertMarkOptRequest(String src, PermissionStatus permissions,
			String holder, String clientMachine,
			//boolean overwrite,
			//boolean append,
			short replication, long blockSize, DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {

		super(src, permissions, holder, clientMachine,
				//overwrite,
				//append,
				replication, blockSize, clientNode,
				isDirectory,
				inheritPermission);
		_length = Integer.MAX_VALUE;
        _height = 0;
        _mark = 0;
	}

	public FBTInsertMarkOptRequest(String directoryName, String src, PermissionStatus permissions,
			String holder, String clientMachine,
			//boolean overwrite,
			//boolean append,
			short replication, long blockSize, DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {

		super(directoryName, src, permissions, holder, clientMachine,
				//overwrite,
				//append,
				replication, blockSize, clientNode,
				isDirectory,
				inheritPermission);
		_length = Integer.MAX_VALUE;
        _height = 0;
        _mark = 0;
	}

	public int getLength() {

		return _length;
	}

	public int getHeight() {
		return _height;
	}

	public int getMark() {
		return _mark;
	}

	public void setLength(int length) {
		_length = length;
	}

	public void setHeight(int height) {
		_height = height;
	}

	public void setMark(int mark) {
		_mark = mark;
	}

}
