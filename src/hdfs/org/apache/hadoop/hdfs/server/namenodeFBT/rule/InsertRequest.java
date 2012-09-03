/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class InsertRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// instance attributes ///////////////////////////////////////////////

    /**
     * �׵���Ф������Ԥ��� Directory ��̾��.
     * "/service/directory/" �ޤ��� "/service/directory/backup".
     */
    protected String _directoryName;

    /**
     * �������륭����
     * Directory name
     */
    protected final String _key;

    /**
     * _key+_fileName = src
     * Example: src = /home/user/hanhlh/FBT.txt
     * _key = /home/user/hanhlh
     * _fileName = FBT.txt
     * */
    protected final String _fileName;

    /**
     * ��������ǡ���
     */
    protected final LeafValue _value;
    protected final INode _inode;
    protected final boolean _isDirectory;
    protected final LeafEntry _entry;
    protected final PermissionStatus _ps;
    protected final String _holder;
    protected final String _clientMachine;
    //protected final boolean _overwrite;
    //protected final boolean _append;
    protected final short _replication;
    protected final long _blockSize;
    protected final boolean _inheritPermission;
    protected final DatanodeDescriptor _clientNode;
    /**
     * �׵���Ф������Ԥ���Ǿ�̤� node ��ؤ��ݥ���.
     * null �ΤȤ��� MetaNode ��������Ԥ���.
     */
    protected VPointer _target;

    // constructors //////////////////////////////////////////////////////

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key �������륭����
     * @param value ��������ǡ���
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
     */
    public InsertRequest(String directoryName, String key, String fileName,
            LeafValue value, LeafEntry entry, VPointer target) {
        super();
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _value = value;
        _entry = entry;
        _target = target;
        _inode = null;
        _ps = null;
        _isDirectory = false;
        _holder = null;
        _clientMachine = null;
        //_overwrite = false;
        //_append = false;
        _replication = 0;
        _blockSize = 0;
        _clientNode = null;
        _inheritPermission = false;
    }

    public InsertRequest(String directoryName, String key,
            INode inode, VPointer target) {
        super();
        _directoryName = directoryName;
        _key = key;
        _inode = inode;
        _target = target;
        _value = null;
        _fileName = null;
        _entry = null;
        _ps = inode.getPermissionStatus();
        _isDirectory = false;
        _holder = null;
        _clientMachine = null;
        //_overwrite = false;
        //_append = false;
        _replication = 0;
        _blockSize = 0;
        _clientNode = null;
        _inheritPermission = false;
    }

    public InsertRequest(String directoryName, String key,
            PermissionStatus ps, VPointer target, boolean isDirectory) {
        super();
        _directoryName = directoryName;
        _key = key;
        _inode = null;
        _ps = ps;
        _isDirectory = isDirectory;
        _target = target;
        _value = null;
        _fileName = null;
        _entry = null;
        _holder = null;
        _clientMachine = null;
        //_overwrite = false;
        //_append = false;
        _replication = 0;
        _blockSize = 0;
        _clientNode = null;
        _inheritPermission = false;
    }
    public InsertRequest(String directoryName, String key,
            PermissionStatus ps, String holder, String clientMachine,
            //boolean overwrite,
            //boolean append,
            short replication,
            long blockSize, VPointer target, boolean isDirectory,
            DatanodeDescriptor clientNode, boolean inheritPermission) {
        super();
        _directoryName = directoryName;
        _key = key;
        _inode = null;
        _ps = ps;
        _isDirectory = isDirectory;
        _target = target;
        _value = null;
        _fileName = null;
        _entry = null;
        _holder = holder;
        _clientMachine = clientMachine;
        //_overwrite = overwrite;
        //_append = append;
        _replication = replication;
        _blockSize = blockSize;
        _clientNode = clientNode;
        _inheritPermission = inheritPermission;
    }
    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key �������륭����
     * @param value ��������ǡ���
     */
    /*public InsertRequest(String directoryName, String key,
    		String fileName, LeafValue value) {
        this(directoryName, key, fileName, value, null, null);
    }

    public InsertRequest(String directoryName, String key,
    		String fileName, LeafEntry entry) {
        this(directoryName, key, fileName, null, entry, null);
    }
*/
    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key �������륭����
     * @param value ��������ǡ���
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
     */
    /*public InsertRequest(String key, String fileName,
    						LeafValue value, VPointer target) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, value, null, target);
    }

    public InsertRequest(String key, String fileName,
			LeafEntry entry, VPointer target) {
    	this(FBTDirectory.DEFAULT_NAME, key, fileName, null, entry, target);
    }
*/    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key �������륭����
     * @param value ��������ǡ���
     */
    public InsertRequest(String key, String fileName, LeafValue value) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, value, null, null);
    }

    public InsertRequest(String key, String fileName, LeafEntry entry) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, null, entry, null);
    }
    public InsertRequest(String directoryName, String key, INode inode) {
    	this(directoryName, key, inode, null);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     */
    public InsertRequest() {
        this(FBTDirectory.DEFAULT_NAME, null, null, null, null, null);
    }

    // accessors /////////////////////////////////////////////////////////


    public InsertRequest(String src, PermissionStatus permissions,
			String holder, String clientMachine,
			//boolean overwrite,
			//boolean append,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {
    	this (FBTDirectory.DEFAULT_NAME, src, permissions, holder, clientMachine,
    			replication,
    			blockSize,
    			null, isDirectory,
    			clientNode,
    			inheritPermission);
    		System.out.println("InsertRequest fbtname "+FBTDirectory.DEFAULT_NAME);
	}

    public InsertRequest(String directoryName, String src, PermissionStatus permissions,
			String holder, String clientMachine,
			//boolean overwrite,
			//boolean append,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {
    	this (directoryName, src, permissions, holder, clientMachine,
    			replication,
    			blockSize,
    			null, isDirectory,
    			clientNode,
    			inheritPermission);
    	System.out.println("****InsertRequest fbtname "+directoryName);
	}

	public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }

    public String getFileName() {
    	return _fileName;
    }

    public LeafValue getValue() {
        return _value;
    }

    public LeafEntry getLeafEntry() {
    	return _entry;
    }
    public INode getINode() {
    	return _inode;
    }
    public PermissionStatus getPermissionStatus() {
    	return _ps;
    }

    public VPointer getTarget() {
        return _target;
    }

	public String getHolder() {
		return _holder;
	}

	public String getClientMachine() {
		return _clientMachine;
	}
/*
    public boolean getOverwrite() {
    	return _overwrite;
    }

    public boolean getAppend() {
    	return _append;
    }

*/    public short getReplication() {
    	return _replication;
    }

    public long getBlockSize() {
    	return _blockSize;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

    public boolean isDirectory() {
    	return _isDirectory;
    }
    public boolean getInheritPermission() {
    	return _inheritPermission;
    }

    public DatanodeDescriptor getClientNode() {
    	return _clientNode;
    }
    // instance methods //////////////////////////////////////////////////

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }


    public Class getResponseClass() {
        return InsertResponse.class;
    }

	@Override
	public String toString() {
		return "InsertRequest [_directoryName=" + _directoryName + ", _key="
				+ _key + ", _fileName=" + _fileName + ", _value=" + _value
				+ ", _inode=" + _inode + ", _isDirectory=" + _isDirectory
				+ ", _entry=" + _entry + ", _ps=" + _ps + ", _holder="
				+ _holder + ", _clientMachine=" + _clientMachine
				+ ", _replication=" + _replication + ", _blockSize="
				+ _blockSize + ", _inheritPermission=" + _inheritPermission
				+ ", _clientNode=" + _clientNode + ", _target=" + _target + "]";
	}


}
