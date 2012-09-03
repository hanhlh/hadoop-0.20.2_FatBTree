/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.Serializable;

/**
 * @author hanhlh
 *
 */
public class NodeIdentifier implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1004100926294279247L;
	private long _nodeID;
	private String _owner;
	public NodeIdentifier (String owner, long nodeID) {
		_owner = owner;
		_nodeID = nodeID;
	}
	public long getNodeID() {
		return _nodeID;
	}
	public String getOwner() {
		return _owner;
	}
	public void setNodeID(long nodeID) {
		_nodeID = nodeID;
	}
	public void setOwner(String owner) {
		_owner = owner;
	}
	public String toString() {
		return _owner.concat("_").concat(String.valueOf(_nodeID));
	}
}
