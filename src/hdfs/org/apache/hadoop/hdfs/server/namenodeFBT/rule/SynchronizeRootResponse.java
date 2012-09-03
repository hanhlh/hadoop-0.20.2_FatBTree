/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public final class SynchronizeRootResponse extends Response {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SynchronizeRootResponse (SynchronizeRootRequest request) {
		super(request);
	}
}
