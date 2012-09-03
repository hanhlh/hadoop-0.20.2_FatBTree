/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;


import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;

/**
 * @author hanhlh
 *
 */
public class IncOptPessimisticPhase implements IncOptPhase {

	private static IncOptPessimisticPhase _singleton;

    static {
        _singleton = new IncOptPessimisticPhase();
    }

    private IncOptPessimisticPhase() {
        // NOP
    }

    public static IncOptPhase getInstance() {
        return _singleton;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#visitMetaNode(org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void visitMetaNode(MetaNode meta, VPointer self,
			IncOptProtocol protocol) {

		protocol.lock(self, Lock.IX);

        protocol.incrementHeight();

        protocol.setPhase(IncOptOptimisticPhase.getInstance());

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#visitIndexNode(org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void visitIndexNode(IndexNode index, VPointer self,
			IncOptProtocol protocol) {
		protocol.lock(self, Lock.X);

        protocol.addLockedNodeList(self);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#visitLeafNode(org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void visitLeafNode(LeafNode leaf, VPointer self,
			IncOptProtocol protocol) {
		protocol.lock(self, Lock.X);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#visitPointerNode(org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void visitPointerNode(PointerNode pointer, VPointer self,
			IncOptProtocol protocol) {
		protocol.lock(self, Lock.X);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public VPointer getNextNode(MetaNode meta, IncOptProtocol protocol) {
		return meta.getPointerEntry();
		}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode, int, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public VPointer getNextNode(IndexNode index, int pos,
			IncOptProtocol protocol) {
		return index.getPointer(pos);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#getNextNode(org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public VPointer getNextNode(PointerNode pointer, IncOptProtocol protocol) {
		VPointer entry = pointer.getEntry();
        protocol.unlock(pointer.getPointer());

        return entry;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#afterTraverse(org.apache.hadoop.hdfs.server.namenodeFBT.VPointer, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void afterTraverse(VPointer child, IncOptProtocol protocol) {
		// TODO ロック解除を並行に実行
        Iterator<VPointer> vpIter = protocol.getLockedNodeList().iterator();
        while (vpIter.hasNext()) {
            protocol.unlock(vpIter.next());
        }

        protocol.unlock(child);

        protocol.setLength(protocol.getLength() - 1);
        protocol.setHeight(0);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#setSafeFirst(boolean, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void setSafeFirst(boolean isSafe, IncOptProtocol protocol) {
		protocol.setSafeFirst(isSafe || protocol.isSafe());
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptPhase#setSafeSecond(boolean, org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol)
	 */
	public void setSafeSecond(boolean isSafe, IncOptProtocol protocol) {
		protocol.setSafeSecond(isSafe || protocol.isSafe());
	}

}
