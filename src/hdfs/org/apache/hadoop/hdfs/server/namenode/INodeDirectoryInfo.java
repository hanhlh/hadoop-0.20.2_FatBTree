package org.apache.hadoop.hdfs.server.namenode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Writable;

public class INodeDirectoryInfo extends INodeInfo implements Writable {

	public INodeDirectoryInfo() {};
	
	public INodeDirectoryInfo(INodeDirectory inode) {
		super(inode);
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
	}
	
}
