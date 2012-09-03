/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.EventObject;

/**
 * @author hanhlh
 *
 */
public abstract class RuleEvent extends EventObject {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	//instance
	private String _transactionID;

	public RuleEvent(Object source) {
		super(source);
	}

	@Override
	public String toString() {
		return "RuleEvent [_transactionID=" + _transactionID + "]";
	}
	//accessor
	public String getTransactionID() {
		return _transactionID;
	}

	public void setTransactionID(String _transactionID) {
		this._transactionID = _transactionID;
	}
	public abstract Class getEventClass();
}
