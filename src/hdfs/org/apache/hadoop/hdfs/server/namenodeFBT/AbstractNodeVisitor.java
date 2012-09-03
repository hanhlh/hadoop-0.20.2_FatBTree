/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;

/**
 * @author hanhlh
 *
 */
public abstract class AbstractNodeVisitor implements NodeVisitor {

	// instance attributes ////////////////////////////////////////////////////

    protected Request _request;

    protected Response _response;

    protected FBTDirectory _directory;


    // constructors ///////////////////////////////////////////////////////////

    public AbstractNodeVisitor(FBTDirectory directory) {
        _directory = directory;
        _request = null;
        _response = null;
    }

    public void setRequest(Request request) {
        //_request = (TraverseRequest)request;
        _request = request;
        _response = null;
    }
    public Response getResponse() {

        return _response;
    }

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor#visit(org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visit(MetaNode meta, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		// TODO 自動生成されたメソッド・スタブ

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor#visit(org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		// TODO 自動生成されたメソッド・スタブ

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor#visit(org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visit(LeafNode leaf, VPointer self) throws
					QuotaExceededException, MessageException, NotReplicatedYetException, IOException {
		// TODO 自動生成されたメソッド・スタブ

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor#visit(org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode, org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public void visit(PointerNode leaf, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		// TODO 自動生成されたメソッド・スタブ

	}

}
