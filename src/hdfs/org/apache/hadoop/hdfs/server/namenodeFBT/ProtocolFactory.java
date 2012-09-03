/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


/**
 * @author hanhlh
 *
 */
public interface ProtocolFactory {

	public ConcurrencyControl createModifyProtocol();
}
