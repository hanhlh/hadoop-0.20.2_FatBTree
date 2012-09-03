/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;


/**
 * @author hanhlh
 *
 */
public interface Node {

	/**
     * <p>Initialize directory that contains this node.</p>
     * @param directory
     */
    public void initOwner(FBTDirectory directory);

    public void accept(NodeVisitor visitor, VPointer self) throws MessageException, NotReplicatedYetException, IOException;

    /**
     * <p>Get the key of this node </p>
     *
     * @return this node's key
     */
    public String getKey();

    /**
     * <p>���ΥΡ��ɤ򻲾Ȥ��� Pointer ���֥������Ȥ��֤��ޤ���</p>
     *
     * @returns ���ΥΡ��ɤ򻲾Ȥ��� Pointer
     */
    public VPointer getPointer();

    public long getNodeID();

    public void setNodeID(long nodeID);

	public void free();

	public NodeIdentifier getNodeIdentifier();

}
