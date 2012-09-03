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
public interface FBTMarkOptRequest {


	public String getKey();

	public String getFileName();

    public int getLength();

    public int getHeight();

    public int getMark();

    public VPointer getTarget();

    public void setLength(int length);

    public void setHeight(int height);

    public void setMark(int mark);

    public void setTarget(VPointer target);

	public PermissionStatus getPermissionStatus();

	public String getHolder();

	public String getClientMachine();

	//public boolean getOverwrite();

	//public boolean getAppend();

	public short getReplication();

	public long getBlockSize();

	public boolean isDirectory();

	public DatanodeDescriptor getClientNode();

	public boolean getInheritPermission();
}
