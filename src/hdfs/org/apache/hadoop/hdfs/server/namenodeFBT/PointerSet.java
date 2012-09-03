/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

import org.apache.hadoop.io.Writable;

/**
 * @author hanhlh
 *
 */
public class PointerSet extends Vector<Pointer> implements VPointer,
						Writable, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public PointerSet() {
		super();
	}

	// instance methods ///////////////////////////////////////////////////////

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < size(); i++) {
            buf.append(get(i)).append(", ");
        }
        return buf.toString();
    }


    // VPointer Interface
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#getPointer()
	 */
	public Pointer getPointer() {
		return firstElement();
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#getPointer(int)
	 */
	public Pointer getPointer(int partitionID) {
		Pointer p;
		for (int i = size() - 1; i >= 0; i--) {
            p = get(i);
            if (p.getPartitionID() == partitionID) {
                return p;
            }
        }
		return null;
	}

	public Pointer getPointer(int partitionID, String fbtOwner) {
		Pointer p;
		for (int i = size()-1; i>=0; i--) {
			p = get(i);
			System.out.println("p: "+p);
			System.out.println("p.owner: "+p.getNodeID().substring(0,5));
			if (p.getPartitionID()==partitionID &&
					p.getNodeID().substring(0,5).equals(fbtOwner)) {
				return p;
			}
		}
		return null;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#add(org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public VPointer add(VPointer vp) {

		return (vp == null) ? this : vp.addPointerSet(this);
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#addPointer(org.apache.hadoop.hdfs.server.namenodeFBT.Pointer)
	 */
	public VPointer addPointer(Pointer p) {
		if (!contains(p)) {
            addElement(p);
        }
        return this;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#addPointerSet(org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet)
	 */
	public VPointer addPointerSet(PointerSet pset) {
		addAll(pset);
        return this;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#remove(org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public VPointer remove(VPointer vp) {
		return (vp == null) ? this : vp.removeMeFrom(this);
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#removeMeFrom(org.apache.hadoop.hdfs.server.namenodeFBT.Pointer)
	 */
	public VPointer removeMeFrom(Pointer p) {
		return contains(p) ? null : p;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#removeMeFrom(org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet)
	 */
	public VPointer removeMeFrom(PointerSet pset) {
		pset.removeAll(this);
        return pset.simplify();
	}
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#simplify()
	 */
	public VPointer simplify() {
		switch(size()) {
        case 0 :
            return null;
        case 1 :
            return firstElement();
        default :
            return this;
        }
	}

	@Override
	public void write(DataOutput out) throws IOException {
		System.out.println("PS write size "+size());
		out.writeInt(size());
		for (int i=0; i<size(); i++) {
			get(i).write(out);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		System.out.println("read size "+size);
		PointerSet ps = new PointerSet();
		for (int i=0; i<size; i++) {
			Pointer p = new Pointer();
			p.readFields(in);
			ps.add(p);
		}
	}
}
