/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeStaticPartition;

/**
 * @author hanhlh
 *
 */
public class TargetNamespace {

	private Integer _namenodeID;
	private String _namespaceOwner;

	public TargetNamespace (Integer namenodeID, String namespaceOwner) {
		_namenodeID = namenodeID;
		_namespaceOwner = namespaceOwner;
	}

	public Integer getNamenodeID() {
		return _namenodeID;
	}

	public void setNamenodeID(Integer _namenodeID) {
		this._namenodeID = _namenodeID;
	}

	public String getNamespaceOwner() {
		return _namespaceOwner;
	}

	public void setNamespaceOwner(String _namespaceOwner) {
		this._namespaceOwner = _namespaceOwner;
	}

	@Override
	public String toString() {
		return "Target [_namenodeID=" + _namenodeID + ", _namespaceOwner="
				+ _namespaceOwner + "]";
	}




}
