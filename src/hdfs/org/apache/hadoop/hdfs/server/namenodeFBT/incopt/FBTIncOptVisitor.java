/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Node;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTIncOptRequest;

/**
 * @author hanhlh
 *
 */
public abstract class FBTIncOptVisitor extends FBTNodeVisitor{


// instance attributes ////////////////////////////////////////////////////

    /**
     * 挿入するキー値
     */
    protected String _key;

    /**
     * INC-OPT 変数 l
     */
    protected int _length;

    /**
     * INC-OPT 変数 h
     */
    protected int _height;

    /**
     * 現在のノードの子ノードへのポインタ
     */
    protected VPointer _child;

    protected VPointer _parent;

	public FBTIncOptVisitor(FBTDirectory directory) {
		super(directory);
	}
	public void setRequest(FBTIncOptRequest request) {
        _key = request.getKey();
        _length = request.getLength();
        _height = request.getHeight();
        _child = request.getTarget();
        _parent = null;
    }


	public void run() {
		if (_child == null) {
	    	/* ルートから処理を開始 */
	   		try {
				_directory.accept(this);
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
		} else {
	    	/*
	    	 * 他の PE から転送されてきた要求の場合は _child != null となる.
	    	 * _child で指定されたノードへ Visitor を渡す.
	    	 */
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
		}
	}

	@Override
	public void visit(MetaNode meta, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		if (_height == _length) {
		    VPointer vPointer = meta.getPointerEntry();
			_child = meta.getRootPointer();

		    lock(vPointer, Lock.IX);

		    Node node = _directory.getNode(vPointer);
		    node.accept(this, vPointer);
		} else {
		    _height++;

			_child = meta.getRootPointer();

	        lock(_child, Lock.IX);

			/* _child で指定されたノードへ Visitor を渡す */
			Node node = _directory.getNode(_child);
			node.accept(this, _child);
		}
	}

	@Override
	public void visit(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		int position = locate(index);

        if (_height == _length) {
            _child = index.getEntry(position);
            VPointer vPointer = index.getPointer(position);

		    lock(vPointer, Lock.IX);

//			unlock(self);
		    _parent = self;

            Node node = _directory.getNode(vPointer);
            node.accept(this, vPointer);
        } else if (index.isLeafIndex()) {
            if (index.size() > 0) {
                visitLeafIndexWithEntry(index, self, position);
            } else {
                visitLeafIndexWithoutEntry(self);
            }
        } else {
            goNextNode(index, self, position, Lock.IX);
        }
	}

	@Override
	public void visit(LeafNode leaf, VPointer self)
			throws MessageException, NotReplicatedYetException, IOException {
		/* 現ノードでの検索キーの位置 */
        int position = leaf.binaryLocate(_key);

        if (position <= 0) {
            operateWhenSameKeyNotExist(leaf, self, position);
        } else {
            operateWhenSameKeyExist(leaf, self, position);
        }
	}

	@Override
	public void visit(PointerNode pointer, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		// TODO 自動生成されたメソッド・スタブ
		PointerSet pointerSet = pointer.getEntry();

        /* 子ページをグローバル排他ロック */
        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
            VPointer vPointer = (VPointer) iter.next();
            lock(vPointer, Lock.X);
        }
        ((FBTDirectory) _directory).incrementLockCount(pointerSet.size() - 1);

	    if (_parent != null) {
	        unlock(_parent);
		    _parent = null;
	    }
		unlock(self);

        Request request = generateModifyRequest(pointerSet);
        if (_directory.isLocal(_child)) {
            NodeVisitor visitor = generateNodeVisitor();
            visitor.setRequest(request);
            visitor.run();

            Response response = visitor.getResponse();

            /* 子ページのロックを解除 */
//            for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
//                VPointer vPointer = (VPointer) iter.next();
//                endLock(vPointer);
//            }
            endLockRequestConcurrently(pointerSet);

            handle(response);
        } else {
    	    /*
    	     * ポインタ vp の指しているノードは現在の PE にはないため，
    	     * 適当な PE を選択して要求を転送
    	     */
            try {
                Response response = callModifyRequest(request);

                /* 子ページのロックを解除 */
//                for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
//                    VPointer vPointer = (VPointer) iter.next();
//                    endLock(vPointer);
//                }
                endLockRequestConcurrently(pointerSet);

                judgeResponse(response);
            } catch (MessageException e) {
                e.printStackTrace();
            }
        }

	}

	protected void goNextNode(IndexNode index, VPointer self,
			int position, int lockMode) throws NotReplicatedYetException, MessageException, IOException {
		_height++;

		_child = index.getEntry(position);

		if (_directory.isLocal(_child)) {
		lock(_child, lockMode);
		unlock(self);

		try {
		Node node = _directory.getNode(_child);
		node.accept(this, _child);
		} catch (NullPointerException e) {
		e.printStackTrace();
		}
		} else {
		/*
		* ポインタ vp の指しているノードは現在の PE にはないため，
		* 適当な PE を選択して要求を転送
		*/
		lockRequest(_child, lockMode);
		endLock(self);

		callRedirect();
		}
		}

		protected void restart() throws NotReplicatedYetException, MessageException, IOException {
		_length = _height - 1;
		_height = 0;

		_directory.accept(this);
		}

	protected abstract void noEntry(VPointer self);

	protected void callRedirect() {
		FBTIncOptRequest request =
		(FBTIncOptRequest) _request;
		request.setTarget(_child);
		request.setLength(_length);
		request.setHeight(_height);
		callRedirectionException(_child.getPointer().getPartitionID());
	}

	protected abstract void operateWhenSameKeyExist(
	LeafNode leaf, VPointer self, int position) throws NotReplicatedYetException, MessageException, IOException;

	protected abstract void operateWhenSameKeyNotExist(
	LeafNode leaf, VPointer self, int position);

	protected abstract Response callModifyRequest(Request request)
							throws MessageException;

	protected abstract void judgeResponse(Response response);

	protected abstract Request generateModifyRequest(PointerSet pointerSet);

	protected abstract NodeVisitor generateNodeVisitor();

	protected abstract void handle(Response response) throws NotReplicatedYetException, MessageException, IOException;

	protected abstract int locate(IndexNode index);

	protected abstract void visitLeafIndexWithEntry(
			IndexNode index, VPointer self, int position) throws NotReplicatedYetException, MessageException, IOException;

	protected abstract void visitLeafIndexWithoutEntry(VPointer self);


}
