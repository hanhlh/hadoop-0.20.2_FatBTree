/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public class LeafNode extends AbstractNode implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -2218464919419288812L;
	private String _nodeType;
	//instances
	/**
	 * List of Directories' paths. Key have to be normalized.
	 * Ex: /home/user/, /home/root/, /, /etc/
	 *
	 * */
	protected List<String> _keys;

	protected List<Integer> _accessCounts;

	protected List<INode> _iNodes;

    protected String _highKey;

    private NodeIdentifier _nodeIdentifier;

    /**
     * ��(�����ͤ���)�Υڡ����ؤΥ����ɥ��
     */
    protected Pointer _sideLink;

    protected boolean _isDummy;
    protected boolean _deleteBit;

    //constructor
	public LeafNode(FBTDirectory directory) {
		super(directory);
		_nodeType = "leaf_";
		_nodeIdentifier = new NodeIdentifier(directory.getOwner(),
				directory.getNodeSequence().getAndIncrement());
		_keys = new ArrayList<String>(_directory.getLeafFanout()+1);
		_iNodes = new ArrayList<INode>(_directory.getLeafFanout()+1);
		_accessCounts = new ArrayList<Integer> (_directory.getLeafFanout() + 1);
        _highKey = null;
        _sideLink = null;
        _isDummy = false;
        _deleteBit = false;
        _directory.decrementEntryCount();

	}
	/**
	 *  key=directoryPath
	 *//*
	public LeafNode(FBTDirectory directory, String key) {
		this(directory);
		_keys = new ArrayList<String>(_directory.getLeafFanout()+1);
		_values = new ArrayList<LeafValue>(_directory.getMaxFilePerDirectory());
		_accessCounts = new ArrayList(_directory.getLeafFanout() + 1);
		_keys.add(key);
		_accessCounts.add(new Integer(0));

		INodeDirectory iNodeDirectory = setINodeDirectory(key);
		LeafValue lv = new LeafValue(iNodeDirectory, null);
		_values.add(lv);
        _highKey = null;
        _sideLink = null;
        _isDummy = false;
	}
*/


	//key = directoryPath
	//fileName = fileName
	//src={key+fileName}
	// Example: src = /home/hanhlh/fbt.txt
	// key=/home/hanhlh, fileName=fbt.txt
	public LeafNode(FBTDirectory directory, String key) {
		this(directory);
		_keys = new ArrayList<String>(_directory.getLeafFanout()+1);
		_iNodes = new ArrayList<INode>(_directory.getLeafFanout()+1);
		_accessCounts = new ArrayList<Integer>(_directory.getLeafFanout() + 1);
		_keys.add(key);
		/*INode inode = new INode(key);
		_iNodes.add(inode);*/
		_accessCounts.add(new Integer(0));
		_highKey = null;
        _sideLink = null;
        _isDummy = false;
        _deleteBit = false;
	}
	//key = directoryPath
	//fileName = fileName
	//src={key+fileName}
	// Example: src = /home/hanhlh/fbt.txt
	// key=/home/hanhlh, fileName=fbt.txt
	public LeafNode(FBTDirectory directory, String key, String fileName,
					LeafValue leafValue) {
		this(directory);
		_keys = new ArrayList<String>(_directory.getLeafFanout()+1);
		_accessCounts = new ArrayList<Integer>(_directory.getLeafFanout() + 1);
		_keys.add(key);
		_accessCounts.add(new Integer(0));
		_highKey = null;
        _sideLink = null;
        _isDummy = false;
        _deleteBit = false;
	}

	public LeafNode(FBTDirectory directory, String key, INode inode) {
		this(directory);
		_keys = new ArrayList<String>(_directory.getLeafFanout()+1);
		//_values = new ArrayList<LeafValue>(_directory.getMaxFilePerDirectory());
		_accessCounts = new ArrayList<Integer>(_directory.getLeafFanout() + 1);

		_keys.add(key);
		//_values.add(leafValue);
		_iNodes = new ArrayList<INode>(_directory.getLeafFanout()+1);
		_iNodes.add(inode);
		_accessCounts.add(new Integer(0));
		_highKey = null;
		_sideLink = null;
		_isDummy = false;
		_deleteBit = false;
	}

		// instance methods ///////////////////////////////////////////////////////


	public List<String> get_keys() {
		return _keys;
	}


	@Override
	public String toString() {
		return "LeafNode [_nodeType=" + _nodeType + ", _keys=" + _keys
				+ ", _accessCounts=" + _accessCounts
				+ ", _highKey=" + _highKey + ", _nodeIdentifier="
				+ _nodeIdentifier + ", _sideLink=" + _sideLink + ", _isDummy="
				+ _isDummy + ", _deleteBit=" + _deleteBit + "]";
	}
	public NodeIdentifier getNodeIdentifier() {
		return _nodeIdentifier;
	}
	public boolean get_isDummy() {
		return _isDummy;
	}

	public void set_isDummy(boolean _isDummy) {
		this._isDummy = _isDummy;
	}

	public boolean get_deleteBit() {
		return _deleteBit;
	}

	public void set_deleteBit(boolean _deleteBit) {
		this._deleteBit = _deleteBit;
	}

	public Pointer get_sideLink() {
		return _sideLink;
	}

	public void set_sideLink(Pointer _sideLink) {
		this._sideLink = _sideLink;
	}

	public String get_highKey() {
		return _highKey;
	}

	public void set_highKey(String _highKey) {
		this._highKey = _highKey;
	}

	public String getNodeNameID() {
		return getNodeType() + getNodeIdentifier().toString();
	}

	public String getNodeType(){
		return _nodeType;
	}
	public void accept(NodeVisitor visitor, VPointer self) throws
						MessageException, NotReplicatedYetException, IOException {
		try {
			visitor.visit(this, self);
		} catch (QuotaExceededException e) {
			// TODO ��ư�������줿 catch �֥�å�
			e.printStackTrace();
		}
	}

	public String getKey() {
		if (_keys.size() > 0) {
			return (String) _keys.get(0);
		} else {
			return null;
		}
	}

	public void addLeafValue(int keyPosition, String key, LeafValue leafValue) {
		System.out.println(FBTDirectory.SPACE);
		System.out.println("LeafNode.addLeafValue at "+ getNodeNameID());
		if (_directory.getAccessCountFlg()) {
            _directory.incrementAccessCount();
            //_accessCounts.add(keyPosition, new Integer(1));
        } else {
            //_accessCounts.add(keyPosition, new Integer(0));
        }
		_keys.add(keyPosition, key);
		System.out.println("LeafNode.addLeafValue done at "+ getNodeNameID());
		/*for (int i=0;i<_keys.size();i++) {
			System.out.println("(key, value)["+i+"]: " +
					"("+_keys.get(i)+", "+_values.get(i)+"]");
		}*/

	}

	public void addINode(int position, String key, INode inode) {
		System.out.println(FBTDirectory.SPACE);
		System.out.println("LeafNode.addINode "+key +" at "+ getNodeNameID());
		if (_directory.getAccessCountFlg()) {
            _directory.incrementAccessCount();
            //_accessCounts.add(keyPosition, new Integer(1));
        } else {
            //_accessCounts.add(keyPosition, new Integer(0));
        }
		_keys.add(position, key);
		_iNodes.add(position, inode);
	}

	public void replaceINode(int position, String key, INode inode) {
		_keys.set(position, key);
		_iNodes.set(position, inode);
	}

	public void removeINode(int position) {
		_keys.remove(position);
		_iNodes.remove(position);
	}
	public INode getINode(int position) {
		return _iNodes.get(position);
	}

	public void addLeafEntry(int keyPosition, int leafEntryPosition,
			LeafEntry leafEntry) {
		//getLeafValue(keyPosition).addLeafEntry(leafEntryPosition, leafEntry);
	}

	public void replaceLeafValue(int keyPosition, String key, LeafValue leafValue) {
		/*_keys.set(keyPosition, key);
		_values.set(keyPosition, leafValue);*/
	}

	public void replaceLeafEntry(int keyPosition, int leafEntryPosition,
									LeafEntry leafEntry) {
		//getLeafValue(keyPosition).addLeafEntry(leafEntryPosition, leafEntry);
	}

	public void removeLeafValue(int keyPosition) {
	/*	_keys.remove(keyPosition);
		_values.remove(keyPosition);
	*/}

	public void removeLeafEntry(int keyPosition, int leafEntryPosition) {
		//getLeafValue(keyPosition).getLeafEntries().remove(leafEntryPosition);
	}

	public void removeLeafEntriesAll(int keyPosition) {
    	//_values.get(keyPosition).clearAllEntries();
    }
	public List<LeafEntry> getLeafEntries(int position) {
		//List<LeafEntry> list = getLeafValue(position).getLeafEntries();
		//return list;
		return null;
	}

	public LeafValue getLeafValue(int position) {
		//LeafValue lv = _values.get(position);
		//return lv;
		return null;
	}
	/*
	public int binaryLocateFileName(String key, String fileName) {
		int bottom = 0;
		List<LeafEntry> leafEntries = getLeafValue(binaryLocate(key)).
													getLeafEntries();
		int top = leafEntries.size()-1;
		int middle = 0;

		while (bottom <= top) {
			middle = (bottom + top + 1) / 2;

			int difference = fileName.compareTo(leafEntries.get(middle).getFilename());

			if (difference < 0) {
				top = middle - 1;
			} else if (difference == 0) {
				return middle + 1;
			} else {
				bottom = middle + 1;
			}
		}
		return -bottom;
	}
*/
	public int binaryLocate(String key) {

        int bottom = 0;
        int top = _keys.size() - 1;
        int middle = 0;
        while (bottom <= top) {
           middle = (bottom + top + 1) / 2;
           int difference = key.compareTo((String) _keys.get(middle));

           if (difference < 0) {
               top = middle - 1;
           } else if (difference == 0) {
               return middle + 1;
           } else {
               bottom = middle + 1;
           }
        }

        return -bottom;
    }


	public String getKey(int pos) {
		return _keys.get(pos);
	}


	public boolean isFullLeafEntriesPerNode() {
		//return _values.size() >= _directory.getLeafFanout();
		return _iNodes.size() >= _directory.getLeafFanout();
	}

	public boolean isFullLeafEntry(int keyPosition) {
		//return _values.get(keyPosition).getLeafEntries().size()
		//						>= _directory.getMaxFilePerDirectory();
		return true;
	}

	public boolean isOverLeafEntriesPerNode() {
		return _iNodes.size() > _directory.getLeafFanout();
	}

	public boolean isOverLeafEntry(int keyPosition) {
		//return _values.get(keyPosition).getLeafEntries().size()
		//						> _directory.getMaxFilePerDirectory();
		return true;
	}

	/**
     * ���� LeafNode ��ʬ�䤷�ޤ���
     * @return ���ΥΡ��ɤα�Ⱦʬ��
     */
    public LeafNode split() {
        StringUtility.debugSpace("Leaf Node splits ");
    	int half  = (_directory.getLeafFanout() + 1) / 2;
    	//System.out.println("half "+half);
        List<String> klist = _keys.subList(half, _keys.size());
        List<INode> elist = _iNodes.subList(half, _iNodes.size());

        LeafNode newLeaf = new LeafNode(_directory);
        synchronized (_directory.getLocalNodeMapping()) {
        	_directory.getLocalNodeMapping().put(newLeaf.
        						getNodeIdentifier().toString(), newLeaf);
        }
        System.out.println("Create Leaf node "+newLeaf.getNodeNameID());
        newLeaf._keys.addAll(klist);
        newLeaf._iNodes.addAll(elist);
        newLeaf._highKey = _highKey;
        newLeaf._sideLink = _sideLink;

        klist.clear();
        elist.clear();
        _highKey = newLeaf.getKey(0);
        _sideLink = newLeaf.getPointer().getPointer();
        System.out.println("_highkey "+_highKey);
        System.out.println("_sideLink "+_sideLink.toString());
        System.out.println("Curren keys at new leafNode "+newLeaf.getNodeNameID());
        for (int i = 0; i<newLeaf._keys.size(); i++) {
        	System.out.println(newLeaf.getNodeNameID()+"***key["+i+"]: "+newLeaf._keys.get(i));
        }
        System.out.println("Current keys at leaf "+getNodeNameID());
        for (int i = 0; i<_keys.size(); i++) {
        	System.out.println(this.getNodeNameID()+"***key["+i+"]: "+_keys.get(i));
        }

        return newLeaf;
    }

    public int size() {
        //return _values.size();
    	return _iNodes.size();
    }

    public void removeAll() {
        _keys.clear();
        //_values.clear();
        _iNodes.clear();
    }

    public List<INode> getINodes() {
    	return _iNodes;
    }

}
