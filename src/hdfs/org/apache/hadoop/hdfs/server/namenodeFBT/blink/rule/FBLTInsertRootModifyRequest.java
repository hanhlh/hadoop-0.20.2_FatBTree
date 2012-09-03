/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.TargetNodeRequest;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertRootModifyRequest extends TargetNodeRequest {

	private final String _directoryName;

    private final String _key;

    private final String _fileName;

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    private final VPointer _dummies;

    public FBLTInsertRootModifyRequest(String directoryName, VPointer target,
            String key, String fileName, VPointer leftNode, VPointer rightNode,
            VPointer dummies) {
        super(target);
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _dummies = dummies;
    }

    public FBLTInsertRootModifyRequest(String key, String fileName, LeafValue value) {
        super();
        _directoryName = FBTDirectory.DEFAULT_NAME;
        _key = key;
        _fileName = fileName;
        _leftNode = null;
        _rightNode = null;
        _dummies = null;
    }


    private FBLTInsertRootModifyRequest(String directoryName, String key,
            String fileName, VPointer leftNode, VPointer rightNode, VPointer dummies) {
        super();
        _directoryName = directoryName;
        _key = key;
        _fileName = fileName;
        _leftNode = leftNode;
        _rightNode = rightNode;
        _dummies = dummies;
    }

    public String getDirectoryName() {
        return _directoryName;
    }

    public String getKey() {
        return _key;
    }

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public VPointer getDummies() {
        return _dummies;
    }

}
