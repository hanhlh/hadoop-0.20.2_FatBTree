/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitor;
import org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory;

/**
 * @author hanhlh
 *
 */
public class FBLTLcfblNodeVisitorFactory implements NodeVisitorFactory {

	private final FBTDirectory _directory;

	public FBLTLcfblNodeVisitorFactory(FBTDirectory directory) {
        _directory = directory;
    }
	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createSearchVisitor()
	 */
	public NodeVisitor createSearchVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createInsertVisitor()
	 */
	public NodeVisitor createInsertVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		System.out.println("FBLTLcfblNodeVisitorFactory.createInsertLcfblVisitor");
		return new FBLTInsertLcfblVisitor(_directory);
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDeleteVisitor()
	 */
	public NodeVisitor createDeleteVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDumpVisitor()
	 */
	public NodeVisitor createDumpVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createInsertModifyVisitor()
	 */
	public NodeVisitor createInsertModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.NodeVisitorFactory#createDeleteModifyVisitor()
	 */
	public NodeVisitor createDeleteModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
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
		return null;
	}
	public NodeVisitor createGetAdditionalBlockVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	public NodeVisitor createGetBlockLocationsVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createCompleteFileVisitor() {
		return null;
	}
}
