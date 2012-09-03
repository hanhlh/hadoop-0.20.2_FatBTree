/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;

/**
 * @author hanhlh
 *
 */
public class LeafValue implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//private INodeDirectory _iNodeDirectory;

	private List<INode> _INodes;

	//private List<LeafEntry> _leafEntries;

	public LeafValue() {
		//_iNodeDirectory = null;
		//_leafEntries = new ArrayList<LeafEntry>(
		//	FBTDirectory.DEFAULT_MAX_FILES_PER_DIRECTORY);
		_INodes = new ArrayList<INode>();
	}
	public List<INode> getINodeList() {
		return _INodes;
	}

	public void getINode(int position) {
		_INodes.get(position);
	}

	public void addINode(int position, INode inode) {
		_INodes.add(position, inode);
	}

	public void replaceINode(int position, INode inode) {
		_INodes.set(position, inode);
	}

	public void removeINode(int position) {
		_INodes.remove(position);
	}

	public int getSize() {
		return _INodes.size();
	}
	public void clearAll() {
		_INodes.clear();
	}

	public int binaryLocate(String src) {
		int bottom = 0;
        int top = _INodes.size() - 1;
        int middle = 0;

	    while (bottom <= top) {
	    	middle = (bottom + top + 1) / 2;
	        int difference = src.compareTo(
	        		(String) _INodes.get(middle).getSrcName());

	        if (difference < 0) {
	            top = middle - 1;
	        } else if (difference == 0) {
	            return middle + 1;
	        } else {
	            bottom = middle + 1;
	        }

	    }
	    return -bottom;

	}
	/*
	public LeafValue(INodeDirectory iNodeDirectory) {
		_iNodeDirectory = iNodeDirectory;
		_leafEntries = new ArrayList<LeafEntry>(
				FBTDirectory.DEFAULT_MAX_FILES_PER_DIRECTORY);
	}

	public LeafValue(INodeDirectory iNodeDirectory, List<LeafEntry> leafEntries) {
		_iNodeDirectory = iNodeDirectory;
		_leafEntries = leafEntries;
	}

	public INodeDirectory getINodeDirectory() {
		return _iNodeDirectory;
	}

	public List<LeafEntry> getLeafEntries() {
		return _leafEntries;
	}

	@Override
	public String toString() {
		return "LeafValue [_iNodeDirectory=" + _iNodeDirectory
				+ ", _leafEntries=" + _leafEntries + "]";
	}

	public LeafEntry getLeafEntry(int position) {
		return _leafEntries.get(position);
	}


	public void addLeafEntry(int position, LeafEntry leafEntry) {
		_leafEntries.add(position, leafEntry);
	}

	public void replaceLeafEntry(int position, LeafEntry leafEntry) {
		_leafEntries.set(position, leafEntry);
	}
	public void removeLeafEntry(int position) {
		_leafEntries.remove(position);
	}

	public int binaryLocateFileName(String fileName) {
		int bottom = 0;
        int top = _leafEntries.size() - 1;
        int middle = 0;

	    while (bottom <= top) {
	    	middle = (bottom + top + 1) / 2;
	        int difference = fileName.compareTo(
	        		(String) _leafEntries.get(middle).getFilename());

	        if (difference < 0) {
	            top = middle - 1;
	        } else if (difference == 0) {
	            return middle + 1;
	        } else {
	            bottom = middle + 1;
	        }

	    }
	    return -bottom;

	}

	public int getSize() {
		return _leafEntries.size();
	}

	public void clearAllEntries() {
		_leafEntries.clear();
	}
	*/

}
