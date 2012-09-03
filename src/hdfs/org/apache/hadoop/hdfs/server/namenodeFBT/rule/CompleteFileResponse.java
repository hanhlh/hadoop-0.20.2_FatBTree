/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem.CompleteFileStatus;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class CompleteFileResponse extends Response {

	// instance attributes ///////////////////////////////////////////////

    /**
     * ������� _value ������ȥ꡼����Ƥ��� LeafNode ��ؤ��ݥ���
     */
    protected final VPointer _vp;

    /**
     * B-Tree �����˻��Ѥ�������
     */
    protected final String _key;
    /**
     * ������̤Ǥ���ǡ���
     */

    protected final CompleteFileStatus _completeFileStatus;
    /**
     * ź�դ��줿��å�����
     */
    protected final String _message;


    public CompleteFileResponse(CompleteFileRequest request, VPointer vp,
							String key,
							CompleteFileStatus status, String message
							) {
		super(request);
		_vp = vp;
		_key = key;
		_completeFileStatus = status;
		_message = message;
    }

    public CompleteFileResponse(CompleteFileRequest request, VPointer vp,
    						String key, CompleteFileStatus status
    						) {
		super(request);
		_vp = vp;
		_key = key;
		_completeFileStatus = status;
		_message = null;
    }

    public CompleteFileResponse(CompleteFileRequest request,
    							String key,
    							CompleteFileStatus status) {
		this(request, null, key, status, null);
    }

    public CompleteFileResponse(CompleteFileRequest request,
								String key) {
    	this(request, null, key, null, null);
    }

    public CompleteFileResponse(CompleteFileRequest request) {
    	this(request, request.getKey());
    }

	public VPointer getVPointer() {
		return _vp;
	}

	public String getKey() {
		return _key;
	}

	public CompleteFileStatus getCompleteFileStatus() {
		return _completeFileStatus;
	}

	public String getMessage() {
		return _message;
	}


	@Override
	public String toString() {
		return "CompleteFileResponse [_vp=" + _vp + ", _key=" + _key
				+ ", _completeFileStatus=" + _completeFileStatus
				+ ", _message=" + _message + "]";
	}


}
