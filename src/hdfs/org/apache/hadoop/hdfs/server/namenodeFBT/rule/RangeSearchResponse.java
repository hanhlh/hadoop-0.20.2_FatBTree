/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.List;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;
import org.apache.hadoop.hdfs.server.namenode.INode;

/**
 * @author hanhlh
 *
 */
public class RangeSearchResponse extends Response {

	private List<INode> _inodes;

	public RangeSearchResponse(RangeSearchRequest request) {
        super(request);
        _inodes = null;
    }

	public RangeSearchResponse(RangeSearchRequest request, List<INode> inodes) {
		super(request);
		_inodes = inodes;
	}

	public List<INode> getINodes() {
		return _inodes;
	}
	public void setINodes(List<INode> inodes) {
		_inodes = inodes;
	}
}

