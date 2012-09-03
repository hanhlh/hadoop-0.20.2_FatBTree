/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.hadoop.io.Writable;


/**
 * @author hanhlh
 *
 */
public interface VPointer extends Serializable, Writable {

	public Pointer getPointer();


	//partitionID = PE's number
	public Pointer getPointer(int partitionID);

	public Pointer getPointer(int partitionID, String fbtOwner);

    public VPointer add(VPointer vp);

    public VPointer addPointer(Pointer p);

    public VPointer addPointerSet(PointerSet pset);

    public VPointer remove(VPointer vp);

    public VPointer removeMeFrom(Pointer p);

    public VPointer removeMeFrom(PointerSet pset);

    public VPointer simplify();

    public Iterator iterator();

    public int size();
}
