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
public class SearchRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	// instance attributes ////////////////////////////////////////////////////

    /**
     * �׵���Ф������Ԥ��� Directory ��̾��.
     * "/service/directory/" �ޤ��� "/service/directory/backup".
     */
    protected String _directoryName;

    /**
     * �ǥ��쥯�ȥ긡������
     */
    protected final String _key;

    protected final String _fileName;

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
     * @param key ��������
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public SearchRequest(String directoryName, String key, String fileName,
    							VPointer target) {
        super();
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _target = target;
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param directoryName �����׵�ν���Ԥ��� Directory ��̾��
     * @param key ��������
	 */
    public SearchRequest(String directoryName, String key, String fileName) {
        this(directoryName, key, fileName, null);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key ��������
     * @param target �����׵�ν���ǽ�˹Ԥ��� node ��ؤ��ݥ���
	 */
    public SearchRequest(String key, String fileName, VPointer target) {
        this(FBTDirectory.DEFAULT_NAME, key, fileName, target);
    }

    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     *
     * @param key ��������
	 */
    public SearchRequest(String key) {
        this(FBTDirectory.DEFAULT_NAME, key, null, null);
    }

    public SearchRequest(String directoryName, String key) {
        this(directoryName, key, null, null);
    }

    public SearchRequest(String[] src) {
    	//src[0] = parent Directory path
    	//src[1] = fileName;
        this(FBTDirectory.DEFAULT_NAME, src[0], src[1], null);
    }
    /**
     * �����������׵ᥪ�֥������Ȥ��������ޤ�.
     */
    public SearchRequest() {
        this(FBTDirectory.DEFAULT_NAME, null, null, null);
    }

    // accessors /////////////////////////////////////////////////////////

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
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
    public VPointer getTarget() {
        return _target;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

	// instance methods //////////////////////////////////////////////////

	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.append("directoryName= ");
		buf.append(_directoryName);
		buf.append(", key= ");
		buf.append(_key);
		buf.append(", fileName= ");
		buf.append(_fileName);
		buf.append(", target= ");
		buf.append(_target);
		return buf.toString();
	}

}
