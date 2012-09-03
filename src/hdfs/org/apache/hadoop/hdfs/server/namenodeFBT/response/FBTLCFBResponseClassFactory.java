/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.response;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.CompleteFileResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTDeleteIncOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetAdditionalBlockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.GetBlockLocationsResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RangeSearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SynchronizeRootResponse;


/**
 * @author hanhlh
 *
 */
public class FBTLCFBResponseClassFactory implements ResponseClassFactory {

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createDumpResponseClass()
	 */
	public Class<?> createDumpResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createSearchResponseClass()
	 */
	public Class<?> createSearchResponseClass() {
		return SearchResponse.class;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createInsertResponseClass()
	 */
	public Class<?> createInsertResponseClass() {
		return FBTInsertMarkOptResponse.class;	}

	public Class<?> createSynchronizeRootResponseClass() {
		return SynchronizeRootResponse.class;
	}
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createDeleteResponseClass()
	 */
	public Class<?> createDeleteResponseClass() {
		return FBTDeleteIncOptResponse.class;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createMigrateResponseClass()
	 */
	public Class<?> createMigrateResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public Class<?> createGetAdditionalBlockResponseClass() {
		return GetAdditionalBlockResponse.class;
	}

	public Class<?> createGetBlockLocationsResponseClass() {
		return GetBlockLocationsResponse.class;
	}

	public Class<?> createCompleteFileResponseClass() {
		return CompleteFileResponse.class;
	}

	public Class<?> createRangeSearchResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return RangeSearchResponse.class;
	}

}
