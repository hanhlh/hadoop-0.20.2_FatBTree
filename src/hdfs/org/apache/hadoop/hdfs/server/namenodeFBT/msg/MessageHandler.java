/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

/**
 * @author hanhlh
 *
 */
public interface MessageHandler {

	public void handle(Message message);
}
