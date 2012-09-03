/**
 *
 */
package org.apache.hadoop.hdfs.server.protocol;

import java.net.UnknownHostException;

import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;

/**
 * @author hanhlh
 *
 */
public interface WriteAtLowGear {

	void writeOffloading(String wolTarget, String wolSource,
			String src, Block[] transferedBlocks)
		throws UnknownHostException, MessageException;

}
