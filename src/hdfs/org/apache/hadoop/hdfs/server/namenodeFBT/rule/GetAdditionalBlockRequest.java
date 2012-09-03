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
public class GetAdditionalBlockRequest extends Request {

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

    protected int _currentGear;


    /**
     * �׵���Ф������Ԥ���Ǿ�̤� node ��ؤ��ݥ���.
     * null �ΤȤ��� MetaNode ��������Ԥ���.
     */
    protected VPointer _target;

    // constructors //////////////////////////////////////////////////////

    /**
     * �������֥�å��ɲ��׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key ��������
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public GetAdditionalBlockRequest(String directoryName, String key,
    							VPointer target, int currentGear) {
        super();
        _directoryName = directoryName;
        _key = key;
        _target = target;
        _currentGear = currentGear;
    }

    /**
     * �������֥�å��ɲ��׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key ��������
	 */
    public GetAdditionalBlockRequest(String directoryName, String key) {
        this(directoryName, key, null, 1);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key ��������
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public GetAdditionalBlockRequest(String key, VPointer target) {
        this(FBTDirectory.DEFAULT_NAME, key, target, 1);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key ��������
	 */
    public GetAdditionalBlockRequest(String key) {
        this(FBTDirectory.DEFAULT_NAME, key, null, 1);
    }
    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     */
    public GetAdditionalBlockRequest() {
        this(FBTDirectory.DEFAULT_NAME, null, null, 1);
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

	public int getCurrentGear() {
		return _currentGear;
	}
    public void setTarget(VPointer target) {
        _target = target;
    }

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }

    public void setCurrentGear(int currentGear) {
    	_currentGear = currentGear;
    }

	@Override
	public String toString() {
		return "GetAdditionalBlockRequest [_directoryName=" + _directoryName
				+ ", _key=" + _key + ", _currentGear=" + _currentGear
				+ ", _target=" + _target + "]";
	}


}
