/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class InsertResponse extends Response {

// instance attributes ///////////////////////////////////////////////

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
     * キーとデータを挿入した LeafNode を指す仮想ポインタ
     */
    protected final VPointer _vp;

    protected INode _inode;

    // constructors //////////////////////////////////////////////////////

    /**
     * InsertRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (InsertRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (InsertRequest)
     * @param vp キーとデータを挿入した LeafNode を指すポインタ
     */
    public InsertResponse(InsertRequest request, VPointer vp, INode inode) {
        super(request);
        _vp = vp;
        _inode = inode;
    }

    public InsertResponse(InsertRequest request, VPointer vp) {
        super(request);
        _vp = vp;
        _inode = null;
    }
    public InsertResponse(InsertRequest request, LeafNode leaf, INode inode) {
        this(request, leaf.getPointer(), inode);
    }
    // accessors /////////////////////////////////////////////////////////

    public VPointer getVPointer() {
        return _vp;
    }

    public INode getINode() {
    	return _inode;
    }
    public void setINode(INode inode) {
    	_inode = inode;
    }
	@Override
	public String toString() {
		return "InsertResponse [_vp=" + _vp + ", _inode=" + _inode + "]";
	}

    // instance methods //////////////////////////////////////////////////



}
