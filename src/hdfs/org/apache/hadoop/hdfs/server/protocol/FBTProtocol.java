/**
 *
 */
package org.apache.hadoop.hdfs.server.protocol;

import org.apache.hadoop.ipc.VersionedProtocol;

/**
 *
 * Interface for communication between FBTs at nodes
 * @author hanhlh
 *
 */
public interface FBTProtocol extends VersionedProtocol {

	public static final long versionID = 1L;


}
