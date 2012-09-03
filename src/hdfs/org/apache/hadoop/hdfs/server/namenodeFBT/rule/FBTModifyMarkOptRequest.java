/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public interface FBTModifyMarkOptRequest {
	public String getKey();

	public String getFileName();

    public boolean getIsSafe();

    public VPointer getTarget();

    public int getHeight();

    public int getMark();

    public VPointer getLockList();

    public int getLockRange();

    public void setIsSafe(boolean isSafe);

    public void setTarget(VPointer target);

    public void setHeight(int height);

    public void setMark(int mark);

    public void setLockList(VPointer lockList);

    public void setLockRange(int lockRange);

    public PermissionStatus getPermissionStatus();

	public String getHolder();

	public String getClientMachine();

	//public boolean getOverwrite();

	//public boolean getAppend();

	public short getReplication();

	public long getBlockSize();

	public boolean isDirectory();

	public DatanodeDescriptor getClientNode();
}
