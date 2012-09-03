/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenode.INodeFile;
/**
 * @author hanhlh
 *
 */
public class LeafEntry {

	private String _fileName;

	private INodeFile _iNodeFile;

	public LeafEntry() {
		_fileName = null;
		_iNodeFile = null;
	}
	public LeafEntry(String fileName) {
		_fileName = fileName;
		_iNodeFile = null;
	}

	public LeafEntry(String fileName, INodeFile iNodeFile) {
		_fileName = fileName;
		_iNodeFile = iNodeFile;
	}

/*
	public LeafEntry(String fsdirectory,
			List<String> fileNames) {
		this(FBTDirectory.DEFAULT_NAME, fsdirectory, fileNames);
	}

	public LeafEntry(String fsdirectory) {
		this(FBTDirectory.DEFAULT_NAME, fsdirectory, null);
	}

	public LeafEntry(String fbtdirectory, String fsdirectory) {
		this(fbtdirectory, fsdirectory, null);
		_fileNames = new ArrayList<String>();
	}

	public INodeDirectory getINodeDirectory() {
		return _iNodeDirectory;
	}

	private INodeDirectory setINodeDirectory(String fsdirectory) {
		//TODO
		return new INodeDirectory(fsdirectory, null);
	}
*/
	//instance
	public String getFilename() {
		return _fileName;
	}

	public INodeFile getINodeFile() {
		return _iNodeFile;
	}
/*
	public INodeFile getINodeFile(int position) {
		return _iNodeFiles.get(position);
	}

	public String getFilename(String fileName) {
		return _fileNames.get(binaryLocateFileName(fileName));
	}

	public INodeFile getINodeFile(String fileName) {
		return getINodeFile(binaryLocateFileName(fileName));
	}
	void addFileName(int position, String fileName) {
		_fileNames.add(position, fileName);
		_iNodeFiles.add(position, setINodeFile(fileName));
	}

	void replaceFileName(int position, String fileName) {
		_fileNames.set(position, fileName);
		replaceINodeFile(position, fileName);
	}

	void removeFileName(int position) {
		_fileNames.remove(position);
		removeINodeFile(position);
	}

	void addINodeFile(int position, String fileName) {

		_iNodeFiles.add(position, setINodeFile(fileName));
	}

	void replaceINodeFile(int position, String fileName) {
		_iNodeFiles.set(position, setINodeFile(fileName));
	}

	void removeINodeFile(int position) {
		_iNodeFiles.remove(position);
	}

	public int binaryLocateFileName(String fileName) {
		int bottom = 0;
        int top = _fileNames.size() - 1;
        int middle = 0;

        while (bottom <= top) {
           middle = (bottom + top + 1) / 2;
           int difference = fileName.compareTo((String) _fileNames.get(middle));

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
	public int size() {
		return _fileNames.size();
	}
*/
	@Override
	public String toString() {
		return "LeafEntry [_fileName=" + _fileName + ", _iNodeFile="
				+ _iNodeFile + "]";
	}

}
