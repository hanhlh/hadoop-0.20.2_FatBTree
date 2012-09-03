/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.incopt;

import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public interface IncOptPhase {

	public void visitMetaNode(MetaNode meta, VPointer self, IncOptProtocol protocol);

    public void visitIndexNode(IndexNode index, VPointer self,
            IncOptProtocol protocol);

    public void visitLeafNode(LeafNode leaf, VPointer self,
            IncOptProtocol protocol);

    public void visitPointerNode(PointerNode pointer, VPointer self,
            IncOptProtocol protocol);

    public VPointer getNextNode(MetaNode meta, IncOptProtocol protocol);

    public VPointer getNextNode(IndexNode index, int pos, IncOptProtocol protocol);

    public VPointer getNextNode(PointerNode pointer, IncOptProtocol protocol);

    public void afterTraverse(VPointer child, IncOptProtocol protocol);

    public void setSafeFirst(boolean isSafe, IncOptProtocol protocol);

    public void setSafeSecond(boolean isSafe, IncOptProtocol protocol);
}
