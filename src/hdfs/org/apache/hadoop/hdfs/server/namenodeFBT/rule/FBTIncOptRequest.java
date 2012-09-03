package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;


public interface FBTIncOptRequest {

	public String getKey();

    public int getLength();

    public int getHeight();

    public VPointer getTarget();

    public void setLength(int length);

    public void setHeight(int height);

    public void setTarget(VPointer target);

}
