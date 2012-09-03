package org.apache.hadoop.hdfs.server.namenode;


import org.apache.hadoop.fs.permission.PermissionStatus;

public class INodeDirectoryPointer extends INodeDirectory {

	INodeDirectoryPointer(String name, PermissionStatus permissions) {
		super(name, permissions);
	}

	public INodeDirectoryPointer(PermissionStatus permissions, long mTime) {
		super(permissions, mTime);
	}

	INodeDirectoryPointer(byte[] localName, PermissionStatus permissions,long mTime) {
		super(localName, permissions, mTime);
	}

	public boolean isHere() {
		return false;
	}
}
