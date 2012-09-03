/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class DeadLockException extends RuntimeException{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public DeadLockException() {
        super();
        StringUtility.debugSpace("DeadLockException");
    }
}
