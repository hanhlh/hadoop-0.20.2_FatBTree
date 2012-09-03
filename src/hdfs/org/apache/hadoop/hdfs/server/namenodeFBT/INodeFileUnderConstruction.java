/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;

/**
 * @author hanhlh
 *
 */
public class INodeFileUnderConstruction extends INodeFile {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	final String clientName;         // lease holder
	private final String clientMachine;
	private final DatanodeDescriptor clientNode; // if client is a cluster node too.

	  private int primaryNodeIndex = -1; //the node working on lease recovery
	  private DatanodeDescriptor[] targets = null;   //locations for last block
	  private long lastRecoveryTime = 0;

	  public INodeFileUnderConstruction(byte[] name,
						              short blockReplication,
						              long modificationTime,
						              long preferredBlockSize,
						              BlockInfo[] blockInfos,
						              PermissionStatus perm,
						              String clientName,
						              String clientMachine,
						              DatanodeDescriptor clientNode) {
		super(perm, blockInfos, blockReplication, modificationTime, modificationTime,
		preferredBlockSize);
		setLocalName(name);
		this.clientName = clientName;
		this.clientMachine = clientMachine;
		this.clientNode = clientNode;
	  }
		INodeFileUnderConstruction(PermissionStatus permissions,
            short replication,
            long preferredBlockSize,
            long modTime,
            String clientName,
            String clientMachine,
            DatanodeDescriptor clientNode) {
		super(permissions.applyUMask(UMASK), 0, replication, modTime, modTime,
						preferredBlockSize);
		this.clientName = clientName;
		this.clientMachine = clientMachine;
		this.clientNode = clientNode;
		}

		public
		//synchronized
		void setLastBlock(BlockInfo newblock, DatanodeDescriptor[] newtargets
	      ) throws IOException {
	    if (blocks == null) {
	      throw new IOException("Trying to update non-existant block (newblock="
	          + newblock + ")");
	    }
	    blocks[blocks.length - 1] = newblock;
	    setTargets(newtargets);
	    lastRecoveryTime = 0;
	  }
		void setTargets(DatanodeDescriptor[] targets) {
		    this.targets = targets;
		    this.primaryNodeIndex = -1;
		  }

		DatanodeDescriptor getClientNode() {
		    return clientNode;
		  }
		String getClientMachine() {
			return clientMachine;
		}
		String getClientName() {
			return clientName;
		}
}
