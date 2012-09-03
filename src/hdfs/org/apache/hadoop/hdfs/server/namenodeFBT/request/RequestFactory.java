/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.request;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


/**
 * @author hanhlh
 *
 */
public interface RequestFactory {

	public Request createDumpRequest(String directoryName);

    public Request createDumpRequest();

    public Request createSearchRequest(String key);

    public Request createRangeSearchRequest(String directoryName,
    						String minKey, String maxKey);

    public Request createSearchRequest(String directoryName, String key);

    public Request createInsertRequest(String key, INode inode,
    									boolean isDirectory);

    public Request createInsertRequest(String key, PermissionStatus ps,
    									boolean isDirectory);

    public Request createInsertRequest(String key, String fileName, LeafValue value);

    public Request createInsertRequest(String key, String fileName, LeafEntry entry);

    public Request createSynchronizeRootRequest(String directoryName,
    											int partID, VPointer updateChild,
													int position);

    public Request createDeleteRequest(String key);

    public Request createMigrateRequest(boolean side);

	public Request createInsertRequest(String src,
			PermissionStatus permissions, String holder, String clientMachine,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission);

	public Request createInsertRequest(String directoryName, String src,
			PermissionStatus permissions, String holder, String clientMachine,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission);

	public Request createGetAdditionalBlockRequest(String src, String clientName);

	public Request createGetAdditionalBlockRequest(String directoryName, String src, String clientName);

	public Request createGetBlockLocationsRequest(String src,
											long offset,
            								long length,
            								int nrBlocksToReturn,
            								boolean doAccessTime);

	public Request createGetBlockLocationsRequest(String directoryName,
			String src,
			long offset,
			long length,
			int nrBlocksToReturn,
			boolean doAccessTime);

	public Request createCompleteFileRequest(String src, String clientName);
	public Request createCompleteFileRequest(String directoryName, String src, String clientName);

}
