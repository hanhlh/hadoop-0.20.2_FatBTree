/**
 *
 */
package org.apache.hadoop.hdfs.server.protocol;

import org.apache.hadoop.ipc.VersionedProtocol;

/**
 * Protocol that Datanode uses to communicate with its local FBTManager.
 * It is used to upload current load information and blocks report.
 * @author hanhlh
 *
 */
public interface FBTDatanodeProtocol extends VersionedProtocol {

	public static final long versionID = 1L;


}
