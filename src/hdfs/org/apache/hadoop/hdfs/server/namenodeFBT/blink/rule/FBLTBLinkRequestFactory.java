/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;


import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafEntry;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SynchronizeRootRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBLTBLinkRequestFactory implements RequestFactory {

	// constructors ///////////////////////////////////////////////////////////

    /**
     * Fat-Btree �� Request ���饹���Ф��� RequestFactory ���饹���������ޤ�.
     */
    public FBLTBLinkRequestFactory() {
        // NOP
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
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createSearchRequest(java.lang.String, java.lang.String)
	 */
	public Request createSearchRequest(String key, String fileName) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory#createInsertRequest(java.lang.String, java.lang.String, org.apache.hadoop.hdfs.server.namenodeFBT.LeafValue)
	 */
	public Request createInsertRequest(String key, String fileName,
			LeafValue value) {
		// TODO ��ư�������줿�᥽�åɡ�������
       return new FBTInsertMarkOptRequest(key,fileName, value);
	}

	public Request createSynchronizeRootRequest(int partID, VPointer updateChild,
													int position) {
		return new SynchronizeRootRequest(partID, updateChild, position);
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
		return null;
	}

	public Request createSynchronizeRootRequest(String directoryName,
			int partID, VPointer updateChild, int position) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createInsertRequest(String key, INode inode,
						boolean isDirectory) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createSearchRequest(String key) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return new SearchRequest(key);
	}

	public Request createInsertRequest(String key, PermissionStatus ps,
										boolean isDirectory) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createInsertRequest(String src,
			PermissionStatus permissions, String holder, String clientMachine,
			//boolean overwrite, boolean append,
			short replication, long blockSize,
			DatanodeDescriptor clientNode,
			boolean isDirectory,
			boolean inheritPermission) {
		return null;
	}

	public Request createDeleteRequest(String key) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createGetAdditionalBlockRequest(String src, String clientName) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createGetBlockLocationsRequest(String src, long offset,
			long length, int nrBlocksToReturn, boolean doAccessTime) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createRangeSearchRequest(String directoryName,
			String minKey, String maxKey) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	public Request createCompleteFileRequest(String src, String clientName) {
		// TODO ��ư�������줿�᥽�åɡ�������
		return null;
	}

	@Override
	public Request createInsertRequest(String directoryName, String src,
			PermissionStatus permissions, String holder, String clientMachine,
			short replication, long blockSize, DatanodeDescriptor clientNode,
			boolean isDirectory, boolean inheritPermission) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Request createGetAdditionalBlockRequest(String directoryName,
			String src, String clientName) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Request createGetBlockLocationsRequest(String directoryName,
			String src, long offset, long length, int nrBlocksToReturn,
			boolean doAccessTime) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Request createCompleteFileRequest(String directoryName, String src,
			String clientName) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
