/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import java.util.Iterator;
import java.util.LinkedList;
import org.apache.hadoop.hdfs.server.namenodeFBT.Call;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDeleteModifyResponser;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyIncOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyIncOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTModifyIncOptRequest;
/**
 * @author hanhlh
 *
 */
public class FBTDeleteModifyIncOptVisitor extends FBTModifyIncOptVisitor{
	private VPointer _deleteNode;

    private boolean _isSuccess;

    private boolean _needRestart;

    private LinkedList _lockList;

	public FBTDeleteModifyIncOptVisitor(FBTDirectory directory) {
		super(directory);
	}
	public void setRequest(Request request) {
        super.setRequest(request);
        setRequest((FBTDeleteModifyIncOptRequest) request);
    }

    public void setRequest(FBTDeleteModifyIncOptRequest request) {
        super.setRequest((FBTModifyIncOptRequest) request);
        _deleteNode = null;
        _isSuccess = false;
        _needRestart = false;
        _lockList = new LinkedList();

        if (_lockList != null) {
            for (Iterator iter = _lockList.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                IndexNode index = (IndexNode) _directory.getNode(vp);
                setIsSafe(index, vp);
            }
        }
    }
    protected void modifyRequest(VPointer target, int position) {
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        FBTDeleteModifyResponser responser = new FBTDeleteModifyResponser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                Request request =
                    new FBTDeleteModifyRequest(FBTDirectory.DEFAULT_NAME,
                            vp, _deleteNode, position);
    		    request.setTransactionID(_transactionID);

                Call call = new Call(messenger, responser);
                call.setRequest(request);
                call.setResponseClass(FBTDeleteModifyResponse.class);

                EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
                        "/mapping" +
                        "/" + vp.getPointer().getPartitionID());
                call.setDestination(destination);
                call.invokeOneWay();
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();
        _deleteNode = responser.getDeleteNode();
    }
    protected Response generateResponse() {
        return new FBTDeleteModifyIncOptResponse(
                (FBTDeleteModifyIncOptRequest) _request,
                _isSuccess, _deleteNode, _needRestart);
    }

    protected void setIsSafe(IndexNode index, VPointer self) {
        if (index.numberOfLocalEntry() > 1 || index.isRootIndex()) {
            _isSafe = true;
        }

        _lockList.add(self);
    }
    protected void noEntry(VPointer self) {
        // NOP (_isSuccess = false;)
    }

    protected void callRedirect() {
	    /*
	     * ポインタ vp の指しているノードは現在の PE にはないため，
	     * 適当な PE を選択して要求を転送
	     */
        try {
            FBTDeleteModifyIncOptRequest request =
                new FBTDeleteModifyIncOptRequest(_key, _child, _lockList);
            request.setIsSafe(_isSafe);
            request.setTransactionID(_transactionID);
            FBTDeleteModifyIncOptResponse response =
                (FBTDeleteModifyIncOptResponse) request(
                    _child, request, FBTDeleteModifyIncOptResponse.class);

            _deleteNode = response.getDeleteNode();
            _isSuccess = response.isSuccess();
            _needRestart = response.needRestart();
        } catch (MessageException e) {
            e.printStackTrace();
        }
    }

    protected void modify(IndexNode index, VPointer self, int position) {
        if (_deleteNode != null) {
            modifyRequest(self, position);

            if (index.isRootIndex() && index.size() == 0) {
                index.setLeafIndex(true);
            }
        }
    }

    protected void operateWhenSameKeyExist(LeafNode leaf, int position) {
        if (leaf.size() > 1) {
            _isSafe = true;
        }

        if (_isSafe) {
            leaf.removeINode(position - 1);

            if (leaf.size() == 0) {
                _deleteNode = leaf.getPointer();
                leaf.free();
            }

            _isSuccess = true;
        } else {
            _needRestart = true;
        }
    }

    protected void operateWhenSameKeyNotExist(LeafNode leaf, int position) {
        // NOP (_isSuccess = false;)
    }

    protected void callRedirect(VPointer self) {
        try {
            FBTDeleteModifyIncOptRequest request =
                new FBTDeleteModifyIncOptRequest(_key, self, _lockList);
            request.setIsSafe(_isSafe);
            request.setTransactionID(_transactionID);
            FBTDeleteModifyIncOptResponse response =
                (FBTDeleteModifyIncOptResponse) request(
                        _child, request, FBTDeleteModifyIncOptResponse.class);

            _deleteNode = response.getDeleteNode();
            _isSuccess = response.isSuccess();
            _needRestart = response.needRestart();
        } catch (MessageException e) {
            e.printStackTrace();
        }
    }

    protected int locate(IndexNode index) {
        return index.binaryLocate(_key);
    }

    protected void beforeLockLeaf() {
        // NOP
    }

}
