package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;

public class INodeFilePointer extends INodeFile {

	  INodeFilePointer(PermissionStatus permissions,
	            int nrBlocks, short replication, long modificationTime,
	            long atime, long preferredBlockSize) {
	    this(permissions, new BlockInfo[nrBlocks], replication,
	        modificationTime, atime, preferredBlockSize);
	  }

	  protected INodeFilePointer() {
	    super();
	  }

	  protected INodeFilePointer(PermissionStatus permissions, BlockInfo[] blklist,
	                      short replication, long modificationTime,
	                      long atime, long preferredBlockSize) {
	    super(permissions, blklist, replication, modificationTime, atime, preferredBlockSize);
	  }
	  
	  public boolean isHere() {
		  return false;
	  }
}
