/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.Call;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.MultiRequestCall;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.Responser;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule.FBLTInsertModifyRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule.FBLTInsertModifyResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule.FBLTInsertRootModifyRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule.FBLTInsertRootModifyResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RequestList;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.ResponseQueue;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBLTInsertLcfblVisitor extends FBLTLcfblVisitor{

	private String _key;

    private LeafValue _value;

    private VPointer _leftNode;

    private VPointer _rightNode;

    private String _boundKey;
	public FBLTInsertLcfblVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(Request request) {
        super.setRequest(request);

        InsertRequest insertRequest = (InsertRequest) request;
        //FBTInsertMarkOptRequest insertRequest = (FBTInsertMarkOptRequest) request;

        _key = insertRequest.getKey();
        _value = insertRequest.getValue();

        _leftNode = null;
        _rightNode = null;
        _boundKey = null;
    }

	@Override
	protected int locate(IndexNode index) {
		return index.binaryLocate(_key);
	}

	@Override
	protected void markSafe(IndexNode index, int position) {
		if (!index.isFullEntry() || index.isRootIndex()) {
            _mark = _height - 1;
        }
	}

	@Override
	protected void setSafe(IndexNode index, VPointer self) {
		if (!index.isFullEntry() || index.isRootIndex()) {
            _mark = _height - 1;
            _isSafe = true;
        }
	}

	@Override
	protected void goChildPage(IndexNode index, VPointer self, int position) {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	@Override
	protected boolean correctPath(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		System.out.println(FBTDirectory.SPACE);
		System.out.println("FBLTInsertLcfblVisitor correctPath starts ");
		/*
		if (index.isRootIndex()) {
            return false;
        }
		 */
        if (index.isInRange(_key)) {
            return false;
        }
        System.out.println("!index.isInRange");
        VPointer vp = index.getSideLink();
        if ((vp == null) || index.isUnderRange(_key)) {
        	//System.out.println("self "+self.toString());
        	if (self instanceof Pointer) {
        		unlock(self);

        	}
        	else if (self instanceof PointerSet) {
        		int pointerIndex = 0;
        		while (pointerIndex < self.size()) {
        			unlock(self.getPointer(pointerIndex));
        			pointerIndex++;
        		}
        	}
        	getDirectory().incrementCorrectingCount();
        } else {
            unlock(self);

            getDirectory().incrementChaseCount();
            visit((IndexNode) _directory.getNode(vp), vp);
        }

        return true;
	}

	@Override
	protected void modifyPage(IndexNode index, VPointer self) {
		// TODO NotSure
		lock(self, Lock.IX);
        if (index.is_deleteBit()) {
            if (!index.isFullEntry() || index.isRootIndex()) {//����ifʸ�ΰ�̣����
                _mark = _height - 1;
            }
            unlock(self);
            _isSafe = false;
        } else if (!index.hasCopy()) {
            unlock(self);
            lock(self, Lock.X + 500, _height, 1, 1);

            if (index.is_deleteBit()) {
                if (!index.isFullEntry() || index.isRootIndex()) {//����ifʸ�ΰ�̣����
                    _mark = _height - 1;
                }
                unlock(self);
                _isSafe = false;
            } else if (!index.hasCopy()) {// ���ԡ��ڡ������ʤ��Τ�B-link�ǹ���
                if (index.isOverRange(_boundKey)) {
                    log(20);
                    VPointer vp = index.getSideLink();
                    unlock(self);

                    getDirectory().incrementChaseCount();

                    modifyPage((IndexNode) _directory.getNode(vp), vp);
                } else if (index.isRootIndex() &&
                        (getDirectory().getTreeHeight() > _treeHeight)) {
                    // �롼�ȥ��ץ�åȤˤ���ڤι⤵���Ѥ�ä��Τǰ�Ĳ��ΥΡ��ɤ򹹿�
                    log(22);
                    int position = index.binaryLocate(_boundKey);
                    VPointer vp = index.getEntry(position);
                    unlock(self);

                    modifyPage((IndexNode) _directory.getNode(vp), vp);
                } else {
                    unlock(_leftNode);
                    unlock(_rightNode);

                    modifyNormalIndex(index, self);

                    if (index.size() == 0) {
                        modifyRootIndex(index, self);
                    }

                    if (_boundKey == null) {
                        log(23);
                        _isFinished = true;
                    }
                }
            } else {// ���ԡ��ڡ���������Τ�LCFB���ڤ��ؤ��ƥꥹ������
                if (!index.isFullEntry() || index.isRootIndex()) {//����ifʸ�ΰ�̣����
                    _mark = _height - 1;
                }
                unlock(self);
                _isSafe = false;
            }
        } else {// ���ԡ��ڡ���������Τ�LCFB���ڤ��ؤ��ƥꥹ������
            if (!index.isFullEntry() || index.isRootIndex()) {//����ifʸ�ΰ�̣����
                _mark = _height - 1;
            }
            unlock(self);
            _isSafe = false;
        }
	}

	private void modifyNormalIndex(IndexNode index, VPointer self) {
        /*int position = index.binaryLocate(_boundKey);
        index.addEntry(position + 1, _boundKey, _rightNode);
        if (index.isLeafIndex()) {
            index.addPointer(position + 1, _rightNode);
        } else {
            PointerNode pNode = new PointerNode(_directory);
            synchronized (_directory.localNodeMapping) {
            	_directory.localNodeMapping.put(pNode.getNodeIdentifier().toString(), pNode);
            }
            pNode.addEntry(_rightNode.getPointer());
            index.addPointer(position + 1, pNode.getPointer());
        }

        _leftNode = null;
        _rightNode = null;
        _boundKey = null;
        if (index.isOverEntry()) {
            log(24);
            IndexNode rightIndex = (IndexNode) index.split();
            _leftNode = self;
            _rightNode = rightIndex.getPointer();
            _boundKey = rightIndex.getKey();

            if (index.isRootIndex()) {
                IndexNode leftIndex = (IndexNode) index.entryCopy();
                index.setLeafIndex(false);

                _leftNode = leftIndex.getPointer();

                IndexNode dummy = new IndexNode(_directory);
                synchronized (_directory.localNodeMapping) {
                	_directory.localNodeMapping.put(dummy.getNodeIdentifier().toString(), dummy);
                }
                dummy.setDummy(true);
                dummy.setLeafIndex(leftIndex.isLeafIndex());
                rightIndex.setSideLink(dummy.getPointer());
            } else {
                lock(_rightNode, Lock.IS);
                _locker.convert(_transactionID, self, Lock.IS);
                _height--;
                _modifiedRange = _height - 1;
            }
        } else {
            log(25);
            endLock(self);
        }
*/    }

	private void modifyRootIndex(IndexNode index, VPointer self) {
		/*System.out.println("FBLTInsertLcfblVisitor.modifyRootIndex");
        PointerNode newLeftPointer = new PointerNode(_directory);
        synchronized (_directory.localNodeMapping) {
        	_directory.localNodeMapping.put(newLeftPointer.getNodeIdentifier().toString(), newLeftPointer);
        }
        newLeftPointer.addEntry(_leftNode.getPointer());
        index.addPointer(0, newLeftPointer.getPointer());
        index.addEntry(0, "", _leftNode);

        PointerNode newRightPointer = new PointerNode(_directory);
        synchronized (_directory.localNodeMapping) {
        	_directory.localNodeMapping.put(newRightPointer.getNodeIdentifier().toString(), newRightPointer);
        }
        newRightPointer.addEntry(_rightNode.getPointer());
        index.addPointer(1, newRightPointer.getPointer());
        index.addEntry(1, _boundKey, _rightNode);

        char[] max = new char[] {Character.MAX_VALUE, Character.MAX_VALUE};
        //index.setHighKey(String.valueOf(max));
        index.set_nextKeyOnParent(String.valueOf(max));
        index.set_isLeftest(true);
        index.set_isRightest(true);

        getDirectory().incrementTreeHeight();

        endLock(self);

        _boundKey = null;
*/    }

	@Override
	protected void modifyNode(IndexNode index, VPointer self) {
		StringUtility.debugSpace("FBLTInsertLcfblVisitor.modifyNode line 281");
		unlock(_leftNode);
        unlock(_rightNode);

        Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        MultiRequestCall call = new MultiRequestCall(messenger);

        VPointer dummies = new PointerSet();
        RequestList requestList = new RequestList();
		List<EndPoint> destinations = new ArrayList<EndPoint>();
		Iterator<Pointer> pIter = self.iterator();
		while (pIter.hasNext()) {
		    Pointer p = pIter.next();
		    System.out.println("pointer "+p.toString());
		    Request request = new FBLTInsertModifyRequest(
		            _directory.DEFAULT_NAME, p, _boundKey, null, null,
		            _rightNode, Integer.MIN_VALUE);
		    requestList.addRequest(request);
		    EndPoint destination = _directory.getMapping(p.getPartitionID());
		    destinations.add(destination);
		}
		call.setRequests(requestList);
		call.setResponseClass(FBLTInsertModifyResponse.class);
		call.setDestinations(destinations);
		ResponseQueue responses = (ResponseQueue) call.invoke();

		_leftNode = new PointerSet();
		_rightNode = new PointerSet();
		Iterator<Response> rIter = responses.getResponses().iterator();
		while (rIter.hasNext()) {
		    FBLTInsertModifyResponse response =
		        (FBLTInsertModifyResponse) rIter.next();

		    if (response.getLeftNode() != null) {
		        _leftNode.add(response.getLeftNode());
		    }
		    if (response.getRightNode() != null) {
		        _rightNode.add(response.getRightNode());
		    }
		    _boundKey = response.getBoundKey();
		    if (response.getDummy() != null) {
		        dummies.add(response.getDummy());
		    }
		}

        if (index.size() == 0) {
            modifyRootIndex(self, dummies);
        }

        if (_boundKey == null) {
            log(26);
            _isFinished = true;
        }

	}

	@Override
	protected void modifyNode(IndexNode index, VPointer self, int pos) {
		StringUtility.debugSpace("FBLTInsertLcfblVisitor.modifyNode line 343");
		System.out.println("position "+pos);
		Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        FBLTInsertModifyResponser responser = new FBLTInsertModifyResponser();

        try {
            for (Iterator iter = self.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                System.out.println("vp "+vp.toString());
                Request request =
                    new FBLTInsertModifyRequest(_directory.DEFAULT_NAME,
                            vp, _boundKey, null, _leftNode, _rightNode, pos);
                request.setTransactionID(_transactionID);

                Call call = new Call(messenger, responser);
                call.setRequest(request);
                call.setResponseClass(FBLTInsertModifyResponse.class);

                EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
                        "/mapping"+
                        "/" + vp.getPointer().getPartitionID());
                System.out.println("destination "+destination);
                call.setDestination(destination);
                call.invokeOneWay();
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }
        System.out.println("FBLTInsertLcfblVisitor send requests finished ");
        responser.isFinished();
        _leftNode = responser.getLeftNode();
        _rightNode = responser.getRightNode();
        _boundKey = responser.getBoundKey();
        VPointer dummies = responser.getDummies();

        if (index.size() == 0) {
            modifyRootIndex(self, dummies);
        }

        if (_boundKey == null) {
            log(27);
            _isFinished = true;
        }
	}
    private void modifyRootIndex(VPointer target, VPointer dummies) {
    	System.out.println("FBLTInsertLcfblVisitor.modifyRootIndex");
        Messenger messenger = (Messenger) NameNodeFBTProcessor.lookup
        											("/messenger");
        Responser responser = new Responser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();
                Request request = new FBLTInsertRootModifyRequest(
                        _directory.DEFAULT_NAME, vp, _boundKey, null, _leftNode,
                        _rightNode, dummies);
                request.setTransactionID(_transactionID);

                Call call = new Call(messenger, responser);
                call.setRequest(request);
                call.setResponseClass(FBLTInsertRootModifyResponse.class);

                EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
                        "/mapping/"+
                        vp.getPointer().getPartitionID());
                call.setDestination(destination);
                call.invokeOneWay();
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();

        _boundKey = null;
    }
	@Override
	protected void memorizeNextKey(IndexNode index, int position) {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	@Override
	public void visit(LeafNode leaf, VPointer self) {
		System.out.println("FBLTInsertLcfblVisitor visit leaf node "+leaf.getNodeNameID());
		//lock(self, Lock.S + 510, _height + 1, 1, 1); BLink
        if (leaf.get_deleteBit()) {
            VPointer vp = leaf.get_sideLink();
            endLock(self);
            _request.setTarget(vp);
            callRedirectionException(vp.getPointer().getPartitionID());
        } else if (_key.compareTo(leaf.get_highKey()) >= 0) {
            VPointer vp = leaf.get_sideLink();
            unlock(self);
            getDirectory().incrementChaseCount();
            if (_directory.isLocal(vp)) {
                visit((LeafNode) _directory.getNode(vp), vp);
            } else {
                _request.setTarget(vp);
                callRedirectionException(vp.getPointer().getPartitionID());
            }
        } else if (leaf.get_isDummy()) {
            unlock(self);
            getDirectory().incrementCorrectingCount();
        } else {
            unlock(self);
            lock(self, Lock.X + 510, _height + 1, 1, 1);

            if (leaf.get_deleteBit()) {
                unlock(self);
                getDirectory().incrementCorrectingCount();
            } else if (_key.compareTo(leaf.get_highKey()) >= 0) {
                VPointer vp = leaf.get_sideLink();
                unlock(self);
                getDirectory().incrementChaseCount();
                visit((LeafNode) _directory.getNode(vp), vp);
            } else if (leaf.get_isDummy()) {
                unlock(self);
                getDirectory().incrementCorrectingCount();
            } else {
                int position = leaf.binaryLocate(_key);

                if (position <= 0) {
                    leaf.addLeafValue(-position, _key, _value);
                    //if (leaf.isOverEntry()) {
                    if (leaf.isOverLeafEntriesPerNode()) {
                        LeafNode newLeaf = leaf.split();

                        _leftNode = self;
                        _rightNode = newLeaf.getPointer();
                        _boundKey = newLeaf.getKey();

                        lock(_rightNode, Lock.S);
                        _locker.convert(_transactionID, self, Lock.S);
                        _modifiedRange = _height - 1;
                        _isSafe = true;
                    } else {
                        endLock(self);
                        _isFinished = true;
                    }
                } else {
                    leaf.replaceLeafValue(position - 1, _key, _value);

                    endLock(self);
                    _isFinished = true;
                }
                _response = new FBTInsertMarkOptResponse((InsertRequest) _request, self, null);
                _length = _mark;
                _treeHeight = _height + 1;
            }
        }

	}

}
