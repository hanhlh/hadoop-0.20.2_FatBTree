/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;



/**
 * For B-Tree transverse
 * @author hanhlh
 *
 */
public interface NodeVisitor extends Runnable {

	public void setRequest(Request request);
	public Response getResponse();

	public void visit(MetaNode meta, VPointer self) throws MessageException, NotReplicatedYetException, IOException;

    public void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException;

    public void visit(LeafNode leaf, VPointer self) throws QuotaExceededException, MessageException, NotReplicatedYetException, IOException;

    public void visit(PointerNode leaf, VPointer self) throws NotReplicatedYetException, MessageException, IOException;

}
