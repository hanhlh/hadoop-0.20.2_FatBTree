package org.apache.hadoop.hdfs.server.namenode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class INodeFileUnderConstructionInfo extends INodeFileInfo implements
		Writable {
	String clientName; // lease holder
	private String clientMachine;
	private DatanodeDescriptor clientNode; // if client is a cluster node too.

	private int primaryNodeIndex = -1; // the node working on lease recovery
	private DatanodeDescriptor[] targets = null; // locations for last block
	private long lastRecoveryTime = 0;

	public INodeFileUnderConstructionInfo() {};

	public INodeFileUnderConstructionInfo(INodeFileUnderConstruction inode) {
		super(inode);
		this.clientName = inode.getClientName();
		this.clientMachine = inode.getClientMachine();
		this.clientNode = inode.getClientNode();
		this.primaryNodeIndex = inode.getPrimaryNodeIndex();
		this.lastRecoveryTime = inode.getLastRecoveryTime();
	}

	public String getClientName() {
		return clientName;
	}
	public String getClientMachine() {
		return clientMachine;
	}
	public DatanodeDescriptor getClientNode() {
		return clientNode;
	}
	public int getPrimaryNodeIndex() {
		return primaryNodeIndex;
	}
	public DatanodeDescriptor[] getTargets() {
		return targets;
	}
	public long getLastRecoveryTime() {
		return lastRecoveryTime;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		super.write(out);
		Text.writeString(out, clientName);
		Text.writeString(out, clientMachine);
		clientNode.write(out);
		out.writeInt(primaryNodeIndex);
		out.writeInt(targets.length);
		for(int i = 0;i < targets.length; i++) {
			targets[i].write(out);
		}
		out.writeLong(lastRecoveryTime);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		super.readFields(in);
		clientName = Text.readString(in);
		clientMachine = Text.readString(in);
		clientNode.readFields(in);
		primaryNodeIndex = in.readInt();
		targets = new DatanodeDescriptor[in.readInt()];
		for(int i = 0;i < targets.length; i++) {
			targets[i].readFields(in);
		}
		lastRecoveryTime = in.readLong();
	}


}
