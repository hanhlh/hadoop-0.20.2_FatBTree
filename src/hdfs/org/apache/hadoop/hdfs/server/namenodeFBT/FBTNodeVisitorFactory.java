/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenode.NameNode;

/**
 * @author hanhlh
 *
 */
public class FBTNodeVisitorFactory implements NodeVisitorFactory {

	private final FBTDirectory _directory;

	public FBTNodeVisitorFactory(FBTDirectory directory) {
		_directory = directory;
	}
	public NodeVisitor createSearchVisitor() {
		// TODO 自動生成されたメソッド・スタブ

		return null;
	}

	public NodeVisitor createInsertVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		NameNode.LOG.info("FBTNodeVisitorFactory create InsertVisitor");
		return (new FBTInsertVisitor(_directory));
	}
	public NodeVisitor createDeleteVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createDumpVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createInsertModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createDeleteModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createMigrateVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public NodeVisitor createMigrateModifyVisitor() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

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
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}


}
