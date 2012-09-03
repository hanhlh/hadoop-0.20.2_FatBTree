package org.apache.hadoop.hdfs.server.namenode;

import java.util.List;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class INodeInfo implements Writable {

	// fields of INode
	// INodeDirectory parent
	private String name;
	private List<Integer> iNodeLocations;
	private int namenodeID;
	private long modificationTime;

	private long accessTime;
	// int namenodeID
	private long permission;


	public INodeInfo() {
		iNodeLocations = new ArrayList<Integer>();
	};

	public INodeInfo(INode inode) {
		name = inode.getLocalName();
		namenodeID = inode.getNamenodeID();
		iNodeLocations = new ArrayList<Integer>(inode.iNodeLocations);
		modificationTime = inode.getModificationTime();
		accessTime = inode.getAccessTime();
		permission = inode.getPermission();
	}

	public String getName() {
		return name;
	}

	public int getNamenodeID() {
		return namenodeID;
	}

	public List<Integer> getINodeLocations() {
		return iNodeLocations;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public long getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(long accessTime) {
		this.accessTime = accessTime;
	}

	public long getPermission() {
		return permission;
	}

	public void setPermission(long permission) {
		this.permission = permission;
	}

	public INode makeINode() {
		if(this instanceof INodeDirectoryInfo)
			return new INodeDirectory((INodeDirectoryInfo) this);
		if(this instanceof INodeFileInfo)
			return new INodeFile((INodeFileInfo) this);
		if(this instanceof INodeDirectoryWithQuotaInfo)
			return new INodeDirectoryWithQuota((INodeDirectoryWithQuotaInfo) this);
		if(this instanceof INodeFileUnderConstructionInfo)
			return new INodeFileUnderConstruction((INodeFileUnderConstructionInfo) this);
		return null;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("[iNodeLocations]" + iNodeLocations.toString());
		Text.writeString(out, name);
		out.writeInt(namenodeID);
		out.writeInt(iNodeLocations.size());
		for(Iterator<Integer> it = iNodeLocations.iterator();it.hasNext();) {
			out.writeInt(it.next().intValue());
		}
		out.writeLong(modificationTime);
		out.writeLong(accessTime);
		out.writeLong(permission);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		name = Text.readString(in);
		namenodeID = in.readInt();
		int l = in.readInt();
		for(int i = 0;i < l;i++) {
			iNodeLocations.add(new Integer(in.readInt()));
		}
		modificationTime = in.readLong();
		accessTime = in.readLong();
		permission = in.readLong();
	}


}
