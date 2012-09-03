/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteIncOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteIncOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyIncOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteModifyIncOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTIncOptRequest;

/**
 * @author hanhlh
 *
 */
public class FBTDeleteIncOptVisitor extends FBTIncOptVisitor{

	public FBTDeleteIncOptVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(Request request) {
        super.setRequest(request);
        setRequest((FBTDeleteIncOptRequest) request);
    }

	 public void setRequest(FBTDeleteIncOptRequest request) {
	        super.setRequest((FBTIncOptRequest) request);
	    }
	@Override
	protected void noEntry(VPointer self) {
		_response = new FBTDeleteIncOptResponse(
                (FBTDeleteIncOptRequest) _request, false);

        endLock(self);
	}

	@Override
	protected void operateWhenSameKeyExist(LeafNode leaf, VPointer self,
			int position) throws NotReplicatedYetException, MessageException, IOException {
	        if (leaf.size() > 1) {
	            leaf.removeINode(position-1);
	            _response = new FBTDeleteIncOptResponse(
	                    (FBTDeleteIncOptRequest) _request, true);

	            endLock(self);
	        } else {
	            /* エントリーが1つなので、ルートからやり直す */
	            unlock(self);

	            _length = _height - 2;
	            _height = 0;

	            _directory.accept(this);
	        }
	}

	@Override
	protected void operateWhenSameKeyNotExist(LeafNode leaf, VPointer self,
			int position) {
		_response = new FBTDeleteIncOptResponse(
                (FBTDeleteIncOptRequest) _request, false);

        endLock(self);
	}

	@Override
	protected Response callModifyRequest(Request request)
			throws MessageException {
		return request(_child, request, FBTDeleteModifyIncOptResponse.class);	}

	@Override
	protected void judgeResponse(Response response) {
		FBTDeleteModifyIncOptResponse deleteResponse =
            (FBTDeleteModifyIncOptResponse) response;

        if (deleteResponse.needRestart()) {
            FBTDeleteIncOptRequest deleteRequest =
                (FBTDeleteIncOptRequest) _request;
            deleteRequest.setTarget(null);
            deleteRequest.setLength(_height - 1);
            deleteRequest.setHeight(0);
        } else {
            _response = new FBTDeleteIncOptResponse(
                    (FBTDeleteIncOptRequest) _request, true);
        }
	}

	@Override
	protected Request generateModifyRequest(PointerSet pointerSet) {
		LinkedList lockList = new LinkedList();
        lockList.add(pointerSet);
        FBTDeleteModifyIncOptRequest request =
            new FBTDeleteModifyIncOptRequest(_key, pointerSet, null);
        request.setTransactionID(_transactionID);

        return request;
        }

	@Override
	protected NodeVisitor generateNodeVisitor() {
		return _directory.getNodeVisitorFactory().createDeleteModifyVisitor();
	}

	@Override
	protected void handle(Response response) throws NotReplicatedYetException, MessageException, IOException {
		FBTDeleteModifyIncOptResponse modifyResponse =
            (FBTDeleteModifyIncOptResponse) response;
        if (modifyResponse.needRestart()) {
    		((FBTDirectory) _directory).incrementMoreRestartCount();
            restart();
        } else {
            _response = new FBTDeleteIncOptResponse(
                    (FBTDeleteIncOptRequest) _request, true);
        }
	}

	@Override
	protected int locate(IndexNode index) {
		return index.binaryLocate(_key);
	}

	@Override
	protected void visitLeafIndexWithEntry(IndexNode index, VPointer self,
			int position) throws NotReplicatedYetException, MessageException, IOException {
		goNextNode(index, self, position, Lock.X);
	}

	@Override
	protected void visitLeafIndexWithoutEntry(VPointer self) {
		noEntry(self);
	}

}
