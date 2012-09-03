/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class SearchResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// instance attributes ///////////////////////////////////////////////

    /**
     * 検索結果 _value がエントリーされていた LeafNode を指すポインタ
     */
    protected final VPointer _vp;

    /**
     * B-Tree 検索に使用したキー
     */
    protected final String _key;

    protected final String _fileName;
    /**
     * 検索結果であるデータ
     */

    protected final LeafValue _value;
    protected final INode _inode;
    /**
     * 添付されたメッセージ
     */
    protected final String _message;

    // Constructors //////////////////////////////////////////////////////

    /**
     * SearchRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (SearchRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (SearchRequest)
     * @param vp value がエントリーされていた LeafNode を指すポインタ
     * @param key B-Tree 検索キー
     * @param value 検索結果データ値
     * @param message 添付されたメッセージ
     */
    public SearchResponse(SearchRequest request, VPointer vp,
            				String key, String fileName,
            				LeafValue value, String message) {
        super(request);
        _vp = vp;
        _key = key;
        _fileName = fileName;
        _value = value;
        _message = message;
        _inode = null;
    }
    /*
    public SearchResponse(SearchRequest request, VPointer vp,
								String key,
								LeafValue value, String message) {
		super(request);
		_vp = vp;
		_key = key;
		_fileName = null;
		_inode = null;
		_value = value;
		_message = message;
    }*/

    public SearchResponse(SearchRequest request, VPointer vp,
            				String key, LeafValue value) {
        super(request);
        _vp = vp;
        _key = key;
        _fileName = null;
        _inode = null;
        _value = value;
        _message = null;
    }

    public SearchResponse(SearchRequest request, VPointer vp,
			String key, INode inode) {
		super(request);
		_vp = vp;
		_key = key;
		_fileName = null;
		_inode = inode;
		_value = null;
		_message = null;
    }

    public SearchResponse(SearchRequest request, VPointer vp,
			String key, INode inode, String message) {
		super(request);
		_vp = vp;
		_key = key;
		_fileName = null;
		_value = null;
		_message = message;
		_inode = inode;
    }

    /**
     * SearchRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (SearchRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (SearchRequest)
     * @param key B-Tree 検索キー
     */
    public SearchResponse(SearchRequest request, String key, INode inode) {
        this(request, null, key, inode, null);
    }

    public SearchResponse(SearchRequest request, String key, String fileName) {
        this(request, null, key, fileName, null, null);
    }

    public SearchResponse(SearchRequest request, String key) {
    	this(request, null, key, null, null);
    }
    /**
     * SearchRequest に対する新しい応答オブジェクトを生成します.
     * 応答を発生させた原因となる
     * 要求オブジェクト (SearchRequest) を与える必要があります.
     *
     * @param request 応答に起因する要求オブジェクト (SearchRequest)
     */
    public SearchResponse(SearchRequest request) {
        this(request, request.getKey());
    }

    // accessors /////////////////////////////////////////////////////////

    public VPointer getVPointer() {
        return _vp;
    }

    public String getKey() {
        return _key;
    }

    public LeafValue getValue() {
        return _value;
    }
    public INode getINode() {
    	return _inode;
    }
    public String getMessage() {
    	return _message;
    }

	@Override
	public String toString() {
		return "SearchResponse [_vp=" + _vp + ", _key=" + _key + ", _inode="
				+ _inode + ", _message=" + _message + "]";
	}



    // instance methods //////////////////////////////////////////////////


}
