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
public class GetBlockLocationsRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * �׵���Ф������Ԥ��� Directory ��̾��.
     * "/service/directory/" �ޤ��� "/service/directory/backup".
     */
    protected String _directoryName;

    /**
     * �ǥ��쥯�ȥ긡������
     */
    protected final String _key;


    /**
     * �׵���Ф������Ԥ���Ǿ�̤� node ��ؤ��ݥ���.
     * null �ΤȤ��� MetaNode ��������Ԥ���.
     */
    protected VPointer _target;

    protected long _offset;
    protected long _length;
    protected int _nrBlocksToReturn;
    protected boolean _doAccessTime;

 // constructors //////////////////////////////////////////////////////

    /**
     * �������֥�å���Ǽ����׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key �����ե�����ѥ�
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public GetBlockLocationsRequest(String directoryName, String key,
			VPointer target, long offset, long length, int nrBlocksToReturn,
			boolean doAccessTime) {
    	super();
    	_directoryName = directoryName;
    	_key = key;
    	_target = target;
    	_offset = offset;
    	_length = length;
    	_nrBlocksToReturn = nrBlocksToReturn;
    	_doAccessTime = doAccessTime;
    }
     /**
     * �������֥�å��ɲ��׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key �����ե�����ѥ�
	 */
    public GetBlockLocationsRequest(String directoryName, String key,
    							long offset, long length, int nrBlocksToReturn,
    							boolean doAccessTime) {
        this(directoryName, key, null, offset, length, nrBlocksToReturn,
        		doAccessTime);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key �ե�����ѥ�
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public GetBlockLocationsRequest(String key, VPointer target,
    			long offset, long length, int nrBlocksToReturn,
    			boolean doAccessTime) {
        this(FBTDirectory.DEFAULT_NAME, key, target, offset, length,
        		nrBlocksToReturn, doAccessTime);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key �����ե�����ѥ�
	 */
    public GetBlockLocationsRequest(String key, long offset, long length, int nrBlocksToReturn,
			boolean doAccessTime) {
        this(FBTDirectory.DEFAULT_NAME, key, null, offset, length,
        		nrBlocksToReturn, doAccessTime);
    }

    	/**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     */
    public GetBlockLocationsRequest(long offset, long length, int nrBlocksToReturn,
			boolean doAccessTime) {
        this(FBTDirectory.DEFAULT_NAME, null, null, offset, length,
        		nrBlocksToReturn, doAccessTime);
    }
    public GetBlockLocationsRequest() {
    	this (FBTDirectory.DEFAULT_NAME, null, null, 0L, 0L, 0, false);
    }

    // accessors /////////////////////////////////////////////////////////

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

    public long getOffset() {
    	return _offset;
    }
    public long getLength() {
    	return _length;
    }
    public int getNrBlocksToReturn() {
    	return _nrBlocksToReturn;
    }
    public boolean isDoAccessTime() {
    	return _doAccessTime;
    }
    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }
	@Override
	public String toString() {
		return "GetBlockLocationsRequest [_directoryName=" + _directoryName
				+ ", _key=" + _key + ", _target=" + _target + ", _offset="
				+ _offset + ", _length=" + _length + ", _nrBlocksToReturn="
				+ _nrBlocksToReturn + ", _doAccessTime=" + _doAccessTime + "]";
	}


}
