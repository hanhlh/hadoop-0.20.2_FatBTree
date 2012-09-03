/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.io.Writable;

/**
 * @author hanhlh
 *
 */
public final class Pointer implements VPointer, Writable, Serializable {


	// instance attributes ////////////////////////////////////////////////////

    /** Datanode's number */
    private int _partitionID;

    /** Node's Id*/
    private String _nodeID;

    // constructors ///////////////////////////////////////////////////////////

    public Pointer() {}

    public Pointer(int partitionID, String nodeID) {
        _partitionID = partitionID;
        _nodeID = nodeID;
    }

    // accessors //////////////////////////////////////////////////////////////

    public int getPartitionID() {
        return _partitionID;
    }

    public String getNodeID() {
        return _nodeID;
    }


    public void setPartitionID (int partitionID) {
    	_partitionID = partitionID;
    }
    public void setNodeID (String nodeID) {
    	_nodeID = nodeID;
    }
    // instance methods ///////////////////////////////////////////////////////
    /**
    public int hashCode() {
        return  _nodeID;
    }
    */
    public boolean equals(Object obj) {
        return (obj instanceof Pointer) && equals((Pointer) obj);
    }

    public boolean equals(Pointer p) {
        return ((p._partitionID == _partitionID) && (p._nodeID == _nodeID));
    }

    public String toString() {
        return "(partion, node): (" + _partitionID + ", "+ _nodeID +")";
    }

    //VPointer interface
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#getPointer()
	 */
	public Pointer getPointer() {
		return this;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#getPointer(int)
	 */
	public Pointer getPointer(int partitionID) {
		return (_partitionID == partitionID) ? this : null;
	}

	public Pointer getPointer(int partitionID, String fbtOwner) {
		return (_partitionID == partitionID &&
					_nodeID.substring(0,5).equals(fbtOwner)) ?
				this : null;
	}
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#add(org.apache.hadoop.hdfs.server.namenodeFBT.VPointer)
	 */
	public VPointer add(VPointer vp) {
		return (vp == null) ? this : vp.addPointer(this);
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#addPointer(org.apache.hadoop.hdfs.server.namenodeFBT.Pointer)
	 */
	public VPointer addPointer(Pointer p) {
		PointerSet pset = new PointerSet();
        pset.addPointer(this);
        pset.addPointer(p);
        return pset;
	}

	public VPointer addPointerSet(PointerSet pset) {
		return pset.addPointer(this);
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
		return equals(p) ? null : p;
	}

	public VPointer removeMeFrom(PointerSet pset) {
        pset.remove(this);
        return pset.simplify();
    }
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#simplify()
	 */
	public VPointer simplify() {
		return this;
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#iterator()
	 */
	public Iterator iterator() {
		return new Iterator() {
            private boolean done = false;
            public boolean hasNext() {
                return !done;
            }
            public Object next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                done = true;
                return Pointer.this;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.VPointer#size()
	 */
	public int size() {
		return 1;
	}


	public String getFBTOwner() {
		return getPointer().getNodeID().substring(0, 5);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		StringUtility.debugSpace("Pointer.write");
		System.out.println("partitionID "+_partitionID);
		System.out.println("nodeID "+_nodeID);
		out.writeInt(_partitionID);
		byte[] temp = _nodeID.getBytes();
		out.writeInt(temp.length);
		out.write(temp);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		StringUtility.debugSpace("Pointer.read");
		this._partitionID = in.readInt();
		int size = in.readInt();
		byte[] temp = new byte[size];
		in.readFully(temp);
		this._nodeID=temp.toString();
	}


}
