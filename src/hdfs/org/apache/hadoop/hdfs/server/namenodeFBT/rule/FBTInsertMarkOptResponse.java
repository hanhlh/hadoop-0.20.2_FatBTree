/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public final class FBTInsertMarkOptResponse extends InsertResponse{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	public FBTInsertMarkOptResponse(FBTInsertMarkOptRequest request,
								VPointer vp, INode inode) {
		super(request, vp, inode);
	}

	public FBTInsertMarkOptResponse(InsertRequest request,
			VPointer vp,
			INode inode) {
		super(request, vp, inode);
	}
	/**
     * @param request
     */
    public FBTInsertMarkOptResponse(FBTInsertMarkOptRequest request) {
        this(request, null, null);
    }
}
