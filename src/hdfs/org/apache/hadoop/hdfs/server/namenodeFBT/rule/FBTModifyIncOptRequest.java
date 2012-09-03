/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public interface FBTModifyIncOptRequest {
	public String getKey();

    public boolean getIsSafe();

    public VPointer getTarget();

    public void setIsSafe(boolean isSafe);

    public void setTarget(VPointer target);

}
