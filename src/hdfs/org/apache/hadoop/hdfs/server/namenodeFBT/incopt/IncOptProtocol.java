/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class IncOptProtocol implements ConcurrencyControl {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
     * INC-OPT 変数 l
     */
    private int _length;

    /**
     * INC-OPT 変数 h
     */
    private int _height;

    /**
     * 現在の排他ロック範囲で足りているかどうか
     */
    private boolean _isSafeFirst;

    private boolean _isSafeSecond;

    private VPointer _parent;

    private LinkedList<VPointer> _lockedNodeList;

    private IncOptPhase _phase;

    private transient FBTNodeVisitor _visitor;

    public IncOptProtocol() {
        this(Integer.MAX_VALUE, 1);
    }

    public IncOptProtocol(int length, int height) {
        _length = length;
        _height = height;
        _isSafeFirst = true;
        _isSafeSecond = true;
        _lockedNodeList = new LinkedList<VPointer>();
        _phase = IncOptOptimisticPhase.getInstance();
    }

    public int getLength() {
        return _length;
    }

    protected void setLength(int length) {
        _length = length;
    }

    public int getHeight() {
        return _height;
    }

    protected void setHeight(int height) {
        _height = height;
    }

    protected void incrementHeight() {
        _height++;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#isSafe()
	 */
	public boolean isSafe() {
		return _isSafeFirst && _isSafeSecond;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#addSafeFirst(boolean)
	 */
	public void addSafeFirst(boolean isSafe) {
		_phase.setSafeFirst(isSafe, this);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#addSafeSecond(boolean)
	 */
	public void addSafeSecond(boolean isSafe) {
		_phase.setSafeSecond(isSafe, this);
	}

	protected void setSafeFirst(boolean isSafe) {
        _isSafeFirst = isSafe;
    }
	protected void setSafeSecond(boolean isSafe) {
        _isSafeFirst = isSafe;
    }

	public VPointer getParent() {
        return _parent;
    }

    public void setParent(VPointer parent) {
        _parent = parent;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#getLockedNodeList()
	 */
	public LinkedList<VPointer> getLockedNodeList() {
		return _lockedNodeList;
	}
	protected void addLockedNodeList(VPointer vp) {
        _lockedNodeList.add(vp);
    }

    public void setPhase(IncOptPhase phase) {
        _phase = phase;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#setVisitor(org.apache.hadoop.hdfs.server.namenodeFBT.FBTNodeVisitor)
	 */
	public void setVisitor(FBTNodeVisitor visitor) {
		_visitor = visitor;
	}

	protected void lock(VPointer target, int mode) {
        Iterator<Pointer> pIter = target.iterator();
        while (pIter.hasNext()) {
            _visitor.lock(pIter.next(), mode);
        }
    }

    protected void unlock(VPointer target) {
        Iterator<Pointer> pIter = target.iterator();
        while (pIter.hasNext()) {
            // TODO 並列に実行可能
            _visitor.unlock(pIter.next());
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(", length = ");
        buf.append(_length);
        buf.append(", height = ");
        buf.append(_height);
        buf.append(", isSafe = ");
        buf.append(_isSafeFirst);

        return buf.toString();
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#visitMetaNode(org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visitMetaNode(MetaNode meta, VPointer self) {
		_phase.visitMetaNode(meta, self, this);
        _parent = self;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#visitIndexNode(org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visitIndexNode(IndexNode index, VPointer self) {
		// TODO 自動生成されたメソッド・スタブ
		_phase.visitIndexNode(index, self, this);
        _parent = self;
	}

	public void visitLeafNode(LeafNode index, VPointer self) {
		// TODO 自動生成されたメソッド・スタブ
		_phase.visitLeafNode(index, self, this);
        _parent = self;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#visitPointerNode(org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visitPointerNode(PointerNode pointer, VPointer self) {
		// TODO 自動生成されたメソッド・スタブ
		_phase.visitPointerNode(pointer, self, this);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode)
	 */
	public VPointer getNextNode(MetaNode meta) {
		return _phase.getNextNode(meta, this);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode, int)
	 */
	public VPointer getNextNode(IndexNode index, int pos) {
		return _phase.getNextNode(index, pos, this);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode)
	 */
	public VPointer getNextNode(PointerNode pointer) {
		// TODO 自動生成されたメソッド・スタブ
		return _phase.getNextNode(pointer, this);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.ConcurrencyControl#afterTraverse(org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void afterTraverse(VPointer child) {
		// TODO 自動生成されたメソッド・スタブ
		_phase.afterTraverse(child, this);
	}

}
