/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.TargetNodeRequest;
/**
 * @author hanhlh
 *
 */
//public final class FBLTInsertModifyRequest extends TargetNodeRequest {
public final class FBLTInsertModifyRequest extends InsertRequest {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final String _directoryName;

    private final String _key;
    private final String _fileName;

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final int _position;

    public FBLTInsertModifyRequest(String directoryName, VPointer target,
            String key, String fileName, VPointer leftNode, VPointer rightNode, int position) {
        super(key, fileName, null, target);
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _position = position;
    }


    public FBLTInsertModifyRequest(String key, String fileName, LeafValue value) {
        super();
        _directoryName = FBTDirectory.DEFAULT_NAME;
        _key = key;
        _fileName=fileName;
        _leftNode = null;
        _rightNode = null;
        _position = 0;
    }

    public FBLTInsertModifyRequest(VPointer target,
    						String key, String fileName, LeafValue value) {
        super(key, fileName, value);
        _directoryName = FBTDirectory.DEFAULT_NAME;
        _key = key;
        _fileName=fileName;
        _leftNode = null;
        _rightNode = null;
        _position = 0;
    }


    /*
    private FBLTInsertModifyRequest(String directoryName, String key,
            String fileName, VPointer leftNode, VPointer rightNode, int position) {
        super();
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _position = position;
    }
    */


    public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }
    public String getFileName() {
    	return _fileName;
    }
    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public int getPosition() {
        return _position;
    }


}
