/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.Serializable;

import org.apache.hadoop.hdfs.server.namenodeFBT.incopt.FBTDeleteIncOptVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.incopt.FBTDeleteModifyIncOptVisitor;




/**
 * @author hanhlh
 *
 */
public class FBTMarkOptNoCouplingNodeVisitorFactory implements
		NodeVisitorFactory, Serializable {

	private final FBTDirectory _directory;

	public FBTMarkOptNoCouplingNodeVisitorFactory(FBTDirectory directory) {
        _directory = directory;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createSearchVisitor()
	 */
	public NodeVisitor createSearchVisitor() {
		return new FBTSearchNoCouplingVisitor(_directory);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createInsertVisitor()
	 */
	public NodeVisitor createInsertVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return new FBTInsertMarkOptNoCouplingVisitor(_directory);
	}
	public NodeVisitor createInsertModifyVisitor() {
        return new FBTInsertModifyMarkOptVisitor(_directory);
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDeleteVisitor()
	 */
	public NodeVisitor createDeleteVisitor() {
		return new FBTDeleteIncOptVisitor(_directory);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDumpVisitor()
	 */
	public NodeVisitor createDumpVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}


	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDeleteModifyVisitor()
	 */
	public NodeVisitor createDeleteModifyVisitor() {
		return new FBTDeleteModifyIncOptVisitor(_directory);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createMigrateVisitor()
	 */
	public NodeVisitor createMigrateVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createMigrateModifyVisitor()
	 */
	public NodeVisitor createMigrateModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createRangeSearchVisitor()
	 */
	public NodeVisitor createRangeSearchVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return new FBTRangeSearchBayerVisitor(_directory);
	}
	public NodeVisitor createGetAdditionalBlockVisitor() {
		return new FBTGetAdditionalBlockNoCouplingVisitor(_directory);
	}
	public NodeVisitor createGetBlockLocationsVisitor() {
		return new FBTGetBlockLocationsNoCouplingVisitor(_directory);
	}
	public NodeVisitor createCompleteFileVisitor() {
		return new FBTCompleteFileNoCouplingVisitor(_directory);
	}

}
