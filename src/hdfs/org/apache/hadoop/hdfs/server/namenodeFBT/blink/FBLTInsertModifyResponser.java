/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink;

import org.apache.hadoop.hdfs.server.namenodeFBT.CallResult;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.Responser;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule.FBLTInsertModifyResponse;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertModifyResponser extends Responser {
// instance attributes ////////////////////////////////////////////////////

    private final VPointer _leftNode;

    private final VPointer _rightNode;

    private String _boundKey;

    private final VPointer _dummies;

    /**
     *
     */
    public FBLTInsertModifyResponser() {
        super();
        _leftNode = new PointerSet();
        _rightNode = new PointerSet();
        _boundKey = null;
        _dummies = new PointerSet();
    }

    // accessors //////////////////////////////////////////////////////////////

    public VPointer getLeftNode() {
        return _leftNode;
    }

    public VPointer getRightNode() {
        return _rightNode;
    }

    public String getBoundKey() {
        return _boundKey;
    }

    public VPointer getDummies() {
        return _dummies;
    }

    public synchronized void handleResult(CallResult result) {
        FBLTInsertModifyResponse response =
            (FBLTInsertModifyResponse) result.getResponse();

        if (response.getLeftNode() != null) {
            _leftNode.add(response.getLeftNode());
        }
        if (response.getRightNode() != null) {
            _rightNode.add(response.getRightNode());
        }
        _boundKey = response.getBoundKey();
        if (response.getDummy() != null) {
            _dummies.add(response.getDummy());
        }

        super.handleResult(result);
    }

}
