package org.apache.hadoop.hdfs.server.namenode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.BlocksMap.BlockInfo;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

public class INodeFileInfo extends INodeInfo implements Writable {

	protected long header;

	protected ArrayWritable blocks;
	
	public INodeFileInfo() {
		blocks = new ArrayWritable(BlockInfo.class, null);
	};

	public INodeFileInfo(INodeFile inode) {
		super(inode);
		header = inode.getHeader();
		setBlocks(inode.getBlocks());
	}
	  
	public long getHeader() {
		return header;
	}

	public void setHeader(long header) {
		this.header = header;
	}
	
	public BlockInfo[] getBlocks() {
		return (BlockInfo[]) blocks.toArray();
	}
	
	public void setBlocks(BlockInfo[] blks) {
		blocks = new ArrayWritable(BlockInfo.class, blks);
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		blocks.write(out);
		out.writeLong(header);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		blocks.readFields(in);
		header = in.readLong();
	}
	  
}
