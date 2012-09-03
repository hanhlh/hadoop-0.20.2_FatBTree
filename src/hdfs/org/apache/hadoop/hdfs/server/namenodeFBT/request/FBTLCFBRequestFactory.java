/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.request;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.CompleteFileRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetAdditionalBlockRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetBlockLocationsRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RangeSearchRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SynchronizeRootRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteIncOptRequest;

/**
 * @author hanhlh
 *
 */
public class FBTLCFBRequestFactory implements RequestFactory {

	public FBTLCFBRequestFactory() {

	}
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createDumpRequest(java.lang.String)
	 */
	public Request createDumpRequest(String directoryName) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createDumpRequest()
	 */
	public Request createDumpRequest() {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createInsertRequest(java.lang.String, java.lang.String, org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry)
	 */
	public Request createInsertRequest(String key, String fileName,
			LeafValue value) {
		//System.out.println("FBTLCFBRequestFactory createInsertRequest");
		return new FBTInsertMarkOptRequest(key, fileName, value);

	}

	public Request createInsertRequest(String key, INode inode,
										boolean isDirectory) {
		//System.out.println("FBTLCFBRequestFactory createInsertRequest Inode");
		//return new FBTInsertMarkOptRequest(key, fileName, value);
		return new FBTInsertMarkOptRequest(key, inode, isDirectory);
	}

	public Request createSynchronizeRootRequest(String directory,
									int partID, VPointer updateChild,int position) {
		return new SynchronizeRootRequest(directory, partID, updateChild, position);
	}
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createDeleteRequest(java.lang.String, java.lang.String)
	 */
	public Request createDeleteRequest(String key, String fileName) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createMigrateRequest(boolean)
	 */
	public Request createMigrateRequest(boolean side) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}
	public Request createInsertRequest(String key, String fileName,
			LeafEntry entry) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return new FBTInsertMarkOptRequest(key, fileName, entry);
	}
	public Request createSearchRequest(String key) {
		return new SearchRequest(key);
	}

	public Request createSearchRequest(String directoryName, String key) {
		return new SearchRequest(directoryName, key);
	}
	public Request createInsertRequest(String key, PermissionStatus ps,
										boolean isDirectory) {
		return new FBTInsertMarkOptRequest(key, ps, isDirectory);
	}
	public Request createInsertRequest(String src,
			PermissionStatus permissions, String holder, String clientMachine,
			//boolean overwrite, boolean append,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {

		return new FBTInsertMarkOptRequest(src, permissions, holder, clientMachine,
				//overwrite, append,
				replication, blockSize, clientNode, isDirectory,
				inheritPermission);
	}
	public Request createDeleteRequest(String key) {
		return new FBTDeleteIncOptRequest(key);
	}
	public Request createGetAdditionalBlockRequest(String src, String clientName) {
		return new GetAdditionalBlockRequest(src);
	}
	public Request createGetBlockLocationsRequest(String src, long offset,
			long length, int nrBlocksToReturn, boolean doAccessTime) {
		return new GetBlockLocationsRequest(src, offset, length, nrBlocksToReturn,
				doAccessTime);
	}
	public Request createCompleteFileRequest(String src, String clientName) {
		return new CompleteFileRequest(src, clientName);
	}
	public Request createRangeSearchRequest(String directoryName,
									String minKey, String maxKey) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return new RangeSearchRequest(directoryName, minKey, maxKey);
	}
	@Override
	public Request createInsertRequest(String directoryName, String src,
			PermissionStatus permissions, String holder, String clientMachine,
			short replication, long blockSize, DatanodeDescriptor clientNode,
			boolean isDirectory, boolean inheritPermission) {

		return new FBTInsertMarkOptRequest(directoryName, src, permissions, holder, clientMachine,
				//overwrite, append,
				replication, blockSize, clientNode, isDirectory,
				inheritPermission);
	}
	@Override
	public Request createGetAdditionalBlockRequest(String directoryName,
			String src, String clientName) {
		// TODO 自動生成されたメソッド・スタブ
		return new GetAdditionalBlockRequest(directoryName, src);
	}
	@Override
	public Request createGetBlockLocationsRequest(String directoryName,
			String src, long offset, long length, int nrBlocksToReturn,
			boolean doAccessTime) {
		return new GetBlockLocationsRequest(directoryName,
				src, offset, length, nrBlocksToReturn,
				doAccessTime);	}
	@Override
	public Request createCompleteFileRequest(String directoryName, String src,
			String clientName) {
		return new CompleteFileRequest(directoryName, src, clientName);
	}

}
