/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @author hanhlh
 *
 */
public interface ConcurrencyControl extends Serializable {

	public boolean isSafe();

    public void addSafeFirst(boolean isSafe);

    public void addSafeSecond(boolean isSafe);

    public LinkedList<VPointer> getLockedNodeList();

    public void setVisitor(FBTNodeVisitor visitor);

    public void visitMetaNode(MetaNode meta, VPointer self);

    public void visitIndexNode(IndexNode index, VPointer self);

    public void visitLeafNode(LeafNode index, VPointer self);

    public void visitPointerNode(PointerNode pointer, VPointer self);

    public VPointer getNextNode(MetaNode meta);

    public VPointer getNextNode(IndexNode index, int pos);

    public VPointer getNextNode(PointerNode pointer);

    public void afterTraverse(VPointer child);

}
