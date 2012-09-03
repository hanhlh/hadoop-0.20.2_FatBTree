/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

/**
 * @author hanhlh
 *
 */
public interface ExceptionalHandler {
	public void handleException(Exception e);
}
