/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.response;

import org.apache.hadoop.hdfs.server.namenodeFBT.rule.DeleteResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.DumpResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;


/**
 * @author hanhlh
 *
 */
public class DefaultResponseClassFactory implements ResponseClassFactory{

	//	constructors ///////////////////////////////////////////////////////////

    /**
     * 標準の ResponseClassFactory クラスを生成します.
     */
    public DefaultResponseClassFactory() {
        // NOP
    }
	public Class<?> createDumpResponseClass() {
		return DumpResponse.class;
	}

	public Class<?> createSearchResponseClass() {
		return SearchResponse.class;
	}

	public Class<?> createInsertResponseClass() {
		return InsertResponse.class;
	}

	public Class<?> createDeleteResponseClass() {
		return DeleteResponse.class;
	}

	public Class<?> createMigrateResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createSynchronizeRootResponseClass() {
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
	public Class<?> createCompleteFileResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public Class<?> createRangeSearchResponseClass() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
