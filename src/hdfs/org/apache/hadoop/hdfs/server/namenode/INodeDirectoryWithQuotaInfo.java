package org.apache.hadoop.hdfs.server.namenode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class INodeDirectoryWithQuotaInfo extends INodeDirectoryInfo implements
		Writable {
	  private long nsQuota; /// NameSpace quota
	  private long nsCount;
	  private long dsQuota; /// disk space quota
	  private long diskspace;

	  public INodeDirectoryWithQuotaInfo() {};

	  public INodeDirectoryWithQuotaInfo(INodeDirectoryWithQuota inode) {
		  super(inode);
		  nsQuota = inode.getNsQuota();
		  nsCount = inode.getNsCount();
		  dsQuota = inode.getDsQuota();
		  diskspace = inode.getDiskspace();
	  }

	public long getNsQuota() {
		return nsQuota;
	}

	public long getNsCount() {
		return nsCount;
	}

	public long getDsQuota() {
		return dsQuota;
	}

	public long getDiskspace() {
		return diskspace;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		super.write(out);
		out.writeLong(nsQuota);
		out.writeLong(nsCount);
		out.writeLong(dsQuota);
		out.writeLong(diskspace);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		super.readFields(in);
		nsQuota = in.readLong();
		nsCount = in.readLong();
		dsQuota = in.readLong();
		diskspace = in.readLong();
	}


}
