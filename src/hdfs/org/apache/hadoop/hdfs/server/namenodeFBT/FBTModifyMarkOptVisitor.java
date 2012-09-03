/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertModifyMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTModifyMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;



/**
 * @author hanhlh
 *
 */
public abstract class FBTModifyMarkOptVisitor extends FBTNodeVisitor{

// instance attributes ////////////////////////////////////////////////////

    protected String _key;

    protected String _fileName;

    /**
     * ���ߤ���¾��å��ϰϤ�­��Ƥ��뤫�ɤ���
     */
    protected boolean _isSafe;

    /**
     * ���ߤΥΡ��ɤλҥΡ��ɤؤΥݥ���
     */
    protected VPointer _child;

    protected int _height;

    protected int _mark;

    protected VPointer _lockList;

    protected int _lockRange;

    protected PermissionStatus _ps;

    protected String _holder;
    protected String _clientMachine;
    protected short _replication;
    protected long _blockSize;
    protected boolean _isDirectory;
    protected DatanodeDescriptor _clientNode;

	public FBTModifyMarkOptVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(FBTModifyMarkOptRequest request) {
		System.out.println("******setRequest called "+request);
        _key = request.getKey();
        _fileName = request.getFileName();
        _isSafe = request.getIsSafe();
        _child = request.getTarget();
        _height = request.getHeight();
        _mark = request.getMark();
        _lockList = request.getLockList();
        _lockRange = request.getLockRange();
        _ps = request.getPermissionStatus();
        _holder = request.getHolder();
        _clientMachine = request.getClientMachine();
        _replication = request.getReplication();
        _blockSize = request.getBlockSize();
        _isDirectory = request.isDirectory();
        _clientNode = request.getClientNode();

    }

    // interface Runnable /////////////////////////////////////////////////////

    public void run() {
    	StringUtility.debugSpace("****FBTModifyMarkOptVisitor run()");
        try {
        	//System.out.println("child "+_child.toString());
        	Node node = _directory.getNode(_child);
        	//System.out.println("node "+node.getNodeID());
        	node.accept(this, _child);
        } catch (NullPointerException e) {
            System.out.println("key "+_key);
            System.out.println("fileName "+_fileName);
            System.out.println(_isSafe);
            System.out.println(_child);
            System.out.println(_height);
            System.out.println(_mark);
            System.out.println(_lockList);
            System.out.println(_lockRange);
            System.out.println(_request);
            System.out.println(_request.getTransactionID());
            e.printStackTrace();
        } catch (NotReplicatedYetException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		} catch (MessageException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		} catch (IOException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
		_response = generateResponse();
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        messenger.removeHandler(_transactionID);
    }

    // interface FBTNodeVisitor ///////////////////////////////////////////////

    public void visit(MetaNode meta, VPointer self) {
        // NOP
    }

    public void visit(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {

    	//StringUtility.debugSpace("****FBTModifyMarkOptVisitor visit Index");
    	//System.out.println("index is rootIndex? "+index.is_isRootIndex());
    	//System.out.println("index is leafIndex? "+index.is_isLeafIndex());
    	//System.out.println("height old "+_height);
    	_height++;
        int height = _height;
        //System.out.println("height new "+height);

        setIsSafe(index, self);
        //System.out.println("setIsSafe "+_isSafe);
        if (_isSafe) {
            continueThisPhase(index, self, height);
        } else {
            visitUnsafeIndex(index, self, height);
        }
    }

    public void visit(LeafNode leaf, VPointer self) throws QuotaExceededException, NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBTModifyMarkOptVisitor visit leaf");
        /* ���Ρ��ɤǤθ��������ΰ��� */
        int position = leaf.binaryLocate(_key);

        if (position <=0) {
            operateWhenSameKeyNotExist(leaf, self, position, _key, _ps,
            		_holder, _clientMachine, _replication, _blockSize,
            		_isDirectory, _clientNode);
        } else {
            operateWhenSameKeyExist(leaf, position);
        }
    }

    public void visit(PointerNode pointer, VPointer self) throws
    				NotReplicatedYetException, MessageException, IOException {
    	//System.out.println("****FBTModifyMarkOptVisitor visit pointer");
        int height = _height + 1;

        VPointer pointerSet = pointer.getEntry();

        /* �ҥڡ����򥰥?�Х���¾��å� */
        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
            VPointer vPointer = (VPointer) iter.next();
            //System.out.println("FBTModifyMarkOptVisitor line 122");
            lock(vPointer, Lock.X);
        }
        ((FBTDirectory) _directory).incrementLockCount(pointerSet.size() - 1);

        if (_directory.isLocal(_child)) {
            Node node = _directory.getNode(_child);
            node.accept(this, pointerSet);
        } else {
        	//System.out.println("Call Redirect");
            callRedirect(pointerSet);
        }

	    if (_lockRange <= height) {
	        /* �ҥڡ����Υ�å����� */
//	        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
//	            VPointer vPointer = (VPointer) iter.next();
//	            unlock(vPointer);
//	        }
//            unlockRequestConcurrently(pointerSet);
	    }
    }

    protected void continueThisPhase(
            IndexNode index, VPointer self, int height) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBTModifyMarkOptVisitor.continueThisPhase key "+_key);
        /* ���Ρ��ɤǤθ��������ΰ��� */
        int position = locate(index);
        System.out.println("ContinueThisPhase position "+position);
        if (index.isLeafIndex()) {
            if (index.size() > 0) {
                /* �ҥΡ��ɤ�ؤ��ݥ��� */
                VPointer child = index.getEntry(position);
                _child = child;

                beforeLockLeaf();
                //System.out.println("continue this phase");
                lock(_child, Lock.X);
                //System.out.println("child "+_child.toString());
                if (_directory.isLocal(_child)) {
                	if (_directory.isLocalDirectory(_child)) {
	        			Node node = _directory.getNode(_child);
	                    node.accept(this, _child);
                	} else {
                		FBTDirectory targetDirectory = (FBTDirectory) NameNodeFBTProcessor.lookup(
                				"/directory."+_child.getPointer().getFBTOwner());
                		Node node = targetDirectory.getNode(_child);
                		node.accept(this, _child);
                	}
                } else {
                    callRedirect();
                }

    		    unlock(child);
            } else {
                noEntry(self);
            }
        } else {
        	System.out.println("doesnt continue this phase");
            VPointer vPointer = index.getPointer(position);
            _child = index.getEntry(position);

            lock(vPointer, Lock.X);
            _lockList.add(vPointer);

            Node node = _directory.getNode(vPointer);
            node.accept(this, vPointer);

		    if (_lockRange <= height) {
		        if (_request instanceof FBTInsertModifyMarkOptRequest) {
			        unlock(vPointer);
		        }
		    }
        }

        modify(index, self, position, height);
    }

    protected abstract Response generateResponse();

    protected abstract void setIsSafe(IndexNode index, VPointer self);

    protected abstract void noEntry(VPointer self);

    protected abstract void callRedirect();

    protected abstract void modify(
            IndexNode index, VPointer self, int position, int height);

    protected abstract void operateWhenSameKeyExist(
            LeafNode leaf, int position);

    protected abstract void operateWhenSameKeyNotExist(
            LeafNode leaf, int position);

    protected abstract void callRedirect(VPointer self);

    protected abstract void visitUnsafeIndex(
            IndexNode index, VPointer self, int height) throws NotReplicatedYetException, MessageException, IOException;

    protected abstract int locate(IndexNode index);

    protected abstract void beforeLockLeaf();

    protected abstract void operateWhenSameKeyExist(
            LeafNode leaf, VPointer self, int position, String key,
            PermissionStatus ps,
            String holder,
            String clientMachine,
            //boolean overwrite,
            //boolean append,
            short replication,
            long _blockSize,
            boolean isDirectory,
            DatanodeDescriptor clientNode) throws QuotaExceededException, MessageException;

    protected abstract void operateWhenSameKeyNotExist(
            LeafNode leaf, VPointer self, int position, String key,
            PermissionStatus ps,
            String holder,
            String clientMachine,
            //boolean overwrite,
            //boolean append,
            short replication,
            long _blockSize,
            boolean isDirectory,
            DatanodeDescriptor clientNode) throws QuotaExceededException, MessageException, NotReplicatedYetException, IOException;

}
