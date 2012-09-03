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
public class CompleteFileRequest extends Request{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	protected String _directoryName;

    /**
     * �ǥ��쥯�ȥ긡������
     */
    protected final String _key;

    protected VPointer _target;

    protected final String _clientName;

	public CompleteFileRequest(String directoryName, String src,
								String clientName, VPointer target) {
		super();
		_directoryName = directoryName;
		_key = src;
		_clientName = clientName;
		_target = target;
		System.out.println("CompleteFileRequest fbtdirectoryname "+directoryName);
	}

	public CompleteFileRequest(String directoryName, String src, String clientName) {
		this(directoryName, src, clientName, null);
	}

	public CompleteFileRequest(String src, String clientName, VPointer target) {
		this(FBTDirectory.DEFAULT_NAME, src, clientName, target);
	}

	public CompleteFileRequest(String src, String clientName) {
		this(FBTDirectory.DEFAULT_NAME, src, clientName, null);
	}

	public CompleteFileRequest() {
		this(FBTDirectory.DEFAULT_NAME, null, null, null);
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
	public String getClientName() {
		return _clientName;
	}
    public void setTarget(VPointer target) {
        _target = target;
    }

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }
	@Override
	public String toString() {
		return "CompleteFileRequest [_directoryName=" + _directoryName
				+ ", _key=" + _key + ", _target=" + _target + ", _clientName="
				+ _clientName + "]";
	}


}
