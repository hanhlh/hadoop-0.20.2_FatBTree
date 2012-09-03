/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.Node;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTModifyIncOptRequest;

/**
 * @author hanhlh
 *
 */
public abstract class FBTModifyIncOptVisitor extends FBTNodeVisitor{
// instance attributes ////////////////////////////////////////////////////

    protected String _key;

    /**
     * 現在の排他ロック範囲で足りているかどうか
     */
    protected boolean _isSafe;

    /**
     * 現在のノードの子ノードへのポインタ
     */
    protected VPointer _child;

	public FBTModifyIncOptVisitor(FBTDirectory directory) {
		super(directory);
	}
	public void setRequest(FBTModifyIncOptRequest request) {
        _key = request.getKey();
        _isSafe = request.getIsSafe();
        _child = request.getTarget();
    }
	// interface Runnable /////////////////////////////////////////////////////

    public void run() {
    	Node node = _directory.getNode(_child);
    	try {
			node.accept(this, _child);
		} catch (NotReplicatedYetException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (MessageException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
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
        setIsSafe(index, self);

        /* 現ノードでの検索キーの位置 */
        int position = locate(index);

        if (index.isLeafIndex()) {
            if (index.size() > 0) {
                /* 子ノードを指すポインタ */
                VPointer child = index.getEntry(position);
                _child = child;

                beforeLockLeaf();

                lock(_child, Lock.X);

                if (_directory.isLocal(_child)) {
        			Node node = _directory.getNode(_child);
                    node.accept(this, _child);
                } else {
                    callRedirect();
                }

                unlock(child);
            } else {
                noEntry(self);
            }
        } else {
            VPointer vPointer = index.getPointer(position);
            _child = index.getEntry(position);

            lock(vPointer, Lock.X);

            Node node = _directory.getNode(vPointer);
            node.accept(this, vPointer);

            unlock(vPointer);
        }

        modify(index, self, position);
    }

    public void visit(LeafNode leaf, VPointer self) {
        /* 現ノードでの検索キーの位置 */
        int position = leaf.binaryLocate(_key);

        if (position <= 0) {
            operateWhenSameKeyNotExist(leaf, position);
        } else {
            operateWhenSameKeyExist(leaf, position);
        }
    }

    public void visit(PointerNode pointer, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
        VPointer pointerSet = pointer.getEntry();

        /* 子ページをグローバル排他ロック */
        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
            VPointer vPointer = (VPointer) iter.next();
            lock(vPointer, Lock.X);
        }

        ((FBTDirectory) _directory).incrementLockCount(pointerSet.size() - 1);

        if (_directory.isLocal(_child)) {
            Node node = _directory.getNode(_child);
            node.accept(this, pointerSet);
        } else {
            callRedirect(pointerSet);
        }

        /* 子ページのロックを解除 */
//        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
//            VPointer vPointer = (VPointer) iter.next();
//            unlock(vPointer);
//        }
        unlockRequestConcurrently(pointerSet);
    }

    protected abstract Response generateResponse();

    protected abstract void setIsSafe(IndexNode index, VPointer self);

    protected abstract void noEntry(VPointer self);

    protected abstract void callRedirect();

    protected abstract void modify(IndexNode index,
            							VPointer self, int position);

    protected abstract void operateWhenSameKeyExist(
            LeafNode leaf, int position);

    protected abstract void operateWhenSameKeyNotExist(
            LeafNode leaf, int position);

    protected abstract void callRedirect(VPointer self);

    protected abstract int locate(IndexNode index);

    protected abstract void beforeLockLeaf();

}
