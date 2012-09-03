/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SynchronizeRootResponse;

/**
 * @author hanhlh
 *
 */
public class FBLTBLinkResponseClassFactory implements ResponseClassFactory {

	public FBLTBLinkResponseClassFactory() {
        // NOP
    }
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
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createInsertResponseClass()
	 */
	public Class<?> createInsertResponseClass() {
		return FBTInsertMarkOptResponse.class;
	}
	public Class createInsertResponseClass2() {
        return FBLTInsertModifyResponse.class;
    }

	public Class<?> createSynchronizeRootResponseClass() {
		return SynchronizeRootResponse.class;
	}
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createDeleteResponseClass()
	 */
	public Class<?> createDeleteResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory#createMigrateResponseClass()
	 */
	public Class<?> createMigrateResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createGetAdditionalBlockResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createGetBlockLocationsResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createRangeSearchResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createCompleteFileResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
