/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.Collections;
import java.util.List;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.namenodeFBT.INode;

/**
 * @author hanhlh
 *
 */
public class INodeDirectory extends INode {

	protected static final int DEFAULT_FILES_PER_DIRECTORY = 5;
	  final static String ROOT_NAME = "";

	  private List<INode> children;

	  INodeDirectory(String name) {

		  super(name);
	  }
	  INodeDirectory(String name, PermissionStatus permissions) {
	    super(name, permissions);
	    this.children = null;
	  }

	  public INodeDirectory(PermissionStatus permissions, long mTime) {
	    super(permissions, mTime, 0);
	    this.children = null;
	  }

	  /** constructor */
	  INodeDirectory(byte[] localName, PermissionStatus permissions, long mTime) {
	    this(permissions, mTime);
	    this.name = localName;
	  }

	  /** copy constructor
	   *
	   * @param other
	   */
	  INodeDirectory(INodeDirectory other) {
	    super(other);
	    this.children = other.getChildren();
	  }


	  private List<INode> getChildren() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/**
	   * Check whether it's a directory
	   */
	  public boolean isDirectory() {
	    return true;
	  }
	public void removeChild(INode iNode) {
		// TODO 自動生成されたメソッド・スタブ

	}

	void replaceChild(INode newChild) {
	    //TODO
	  }
	INode getChild(String child) {
		//TODO
		return null;
	}

	INode getNode(String path) {
		//TODO
		return null;
	}

	@Override
	int collectSubtreeBlocksAndClear(List<Block> v) {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}

	@Override
	long[] computeContentSummary(long[] summary) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	DirCounts spaceConsumedInTree(DirCounts counts) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}


}
