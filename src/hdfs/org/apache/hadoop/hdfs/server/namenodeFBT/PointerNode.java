/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.io.Serializable;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;


/**
 * @author hanhlh
 *
 */
public class PointerNode extends AbstractNode
		implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 6836058365578677184L;
	private String _nodeType;
	private final String _key;
	private NodeIdentifier _nodeIdentifier;
	private  PointerSet _entries;

	//constructor

	public PointerNode(FBTDirectory directory) {
		super(directory);
		_nodeType = "pointer_";
        _nodeIdentifier = new NodeIdentifier(directory.getOwner(),
				directory.getNodeSequence().getAndIncrement());
		_key = null;
		_entries = new PointerSet();
 	}

	// instance methods ///////////////////////////////////////////////////////

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString()).append("\n");

        buf.append("ptr = ");
        for (int i = 0; i < _entries.size(); i++) {
            buf.append((VPointer) _entries.get(i)).append(", ");
        }
        buf.append("\n");

        return buf.toString();
    }

    public PointerSet getEntry() {
        return _entries;
    }

    /**
     * �Х��ʥ꡼���������Ѥ��� _entries �ꥹ�Ȥ� newPartID �ΰ��֤򸡺��򤹤�.
     * ��Ӥ���Τϡ�����ȥ�Υݥ��󥿤Υѡ��ƥ������ID.
     *
     * @param newPartID �������륭��
     * @return ������̤Ǥ��� newPartID �ΰ���
     */
    private int binaryLocate(int partID) {
        int bottom = 0;
        int top = _entries.size() - 1;
        int middle = 0;

        while (bottom <= top) {
           middle = (bottom + top + 1) / 2;

           if (partID == (100+ Integer.parseInt(((Pointer) _entries.get(middle)).
        		   								getNodeID().substring(3,5)))) {
               return middle;
           }
           if (partID < (100+ Integer.parseInt(((Pointer) _entries.get(middle)).
												getNodeID().substring(3,5)))) {
              top = middle - 1;
           } else {
               bottom = middle + 1;
           }
        }

        return bottom;
    }

    /**
     * �ݥ��󥿥ڡ����˿����˥���ȥ��ä���.
     * ���ΤȤ�������ȥ꤬�ѡ��ƥ�������ˤʤ�褦�˥���ȥꤹ��.
     *
     * @param newEntry �����˥���ȥ꡼����ݥ���
     */
    public void addEntry(Pointer newEntry) {
        int position = binaryLocate(100+Integer.parseInt(newEntry.getNodeID().substring(3,5)));
        _entries.add(position, newEntry);

        //touch();
    }

    public void removeEntry(Pointer pointer) {
        _entries.removeElement(pointer);
        //touch();
    }

    public void setEntry(PointerSet entries) {
    	_entries = entries;
    }

    public void clearEntry() {
        _entries.clear();
        //touch();
    }

    public int size() {
        return _entries.size();
    }

    public String getNodeType() {
    	return _nodeType;
    }
    public int getLeftPartitionID() {
        int position = binaryLocate(getPointer().getPointer().getPartitionID());
        position = (position - 1 + _entries.size()) % _entries.size();
        return ((Pointer) _entries.get(position)).getPartitionID();
    }

    public int getRightPartitionID() {
        int position = binaryLocate(getPointer().getPointer().getPartitionID());
        position = (position + 1) % _entries.size();
        return ((Pointer) _entries.get(position)).getPartitionID();
    }

    public int getNonLoopLeftPartitionID() {
        int position = binaryLocate(getPointer().getPointer().getPartitionID());
        if (position == 0) {
           return Integer.MIN_VALUE;
        }
        return _entries.get(position - 1).getPartitionID();
    }

    public int getNonLoopRightPartitionID() {
        int position = binaryLocate(getPointer().getPointer().getPartitionID());
        if (position == _entries.size() - 1) {
           return Integer.MIN_VALUE;
        }
        return _entries.get(position + 1).getPartitionID();
    }


	public void accept(NodeVisitor visitor, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		((NodeVisitor) visitor).visit(this, self);
	}

	public String getKey() {
	        return _key;
	}

	public String getNameNodeID() {
		return _nodeType + getNodeIdentifier().toString();
	}
	public void free() {
		//TODO
        /*((Locker) AutoDisk.lookup("/service/locker"))
                .removeObject(getPointer());
	    super.free();*/
	}

	public void setNodeID(long nodeID) {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

	@Override
	public NodeIdentifier getNodeIdentifier() {
		return _nodeIdentifier;
	}

}
