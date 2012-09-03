/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Locker;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 * TODO getINodeDirectories
 */
public class IndexNode extends AbstractNode implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 1026910915370378400L;

	private String _nodeType;

	// instance attributes ////////////////////////////////////////////////////

    protected final List<String> _keys;

    //Pointers referring to this node's children
    private List<VPointer> _children;

    //Pointers referring to other nodes (index/leaf) at different datanodes.
    //Transfer request to suitable nodes that contains suitable partID
    private List<VPointer> _pointers;

    protected boolean _isLeafIndex;

    protected boolean _isRootIndex;

    /**
     * Next key on parent
     * =highkey
     * */
    protected String _nextKeyOnParent;

    protected String _highestKeyOnChildren;

    protected boolean _isLeftest;

    protected boolean _isRightest;

    protected boolean _deleteBit;

    private VPointer _pointerPage;

    private VPointer _sideLink;

    private VPointer _dummyLink;

    private boolean _isDummy;

    protected int _pageLsn;

    protected boolean _smBit;

    private NodeIdentifier _nodeIdentifier;


 // constructors ///////////////////////////////////////////////////////////

    public IndexNode(FBTDirectory directory) {
        super(directory);
        _nodeType = "index_";
        _nodeIdentifier = new NodeIdentifier(directory.getOwner(),
				directory.getNodeSequence().getAndIncrement());
        _keys = new ArrayList<String>(directory.getFanout() + 1);
        _children = new ArrayList<VPointer>(directory.getFanout() + 1);
        _pointers = new ArrayList<VPointer>(directory.getFanout() + 1);
        _isLeafIndex = false;
        _isRootIndex = false;
        _nextKeyOnParent = null;
        _highestKeyOnChildren = null;
        _isLeftest = false;
        _isRightest = false;
        _deleteBit = false;
        _pointerPage = null;
        _sideLink = null;
        _isDummy = false;
        _dummyLink = null;
        _pageLsn = 0;
        _smBit = false;

    }

    // instance methods ///////////////////////////////////////////////////////

    public VPointer get_sideLink() {
		return _sideLink;
	}
    public String getNodeType() {
    	return _nodeType;
    }

	public void set_sideLink(VPointer _sideLink) {
		this._sideLink = _sideLink;
	}


	public VPointer get_dummyLink() {
		return _dummyLink;
	}


	public void set_dummyLink(VPointer _dummyLink) {
		this._dummyLink = _dummyLink;
	}


	public boolean is_isDummy() {
		return _isDummy;
	}


	public void set_isDummy(boolean _isDummy) {
		this._isDummy = _isDummy;
	}


	public boolean is_isLeafIndex() {
		return _isLeafIndex;
	}


	public void set_isLeafIndex(boolean _isLeafIndex) {
		this._isLeafIndex = _isLeafIndex;
	}


	public boolean is_isRootIndex() {
		return _isRootIndex;
	}


	public void set_isRootIndex(boolean _isRootIndex) {
		this._isRootIndex = _isRootIndex;
	}


	public String get_highestKeyOnChildren() {
		return _highestKeyOnChildren;
	}


	public void set_highestKeyOnChildren(String _highestKeyOnChildren) {
		this._highestKeyOnChildren = _highestKeyOnChildren;
	}

	public String get_nextKeyOnParent() {
		return _nextKeyOnParent;
	}


	public void set_nextKeyOnParent(String _highKey) {
		this._nextKeyOnParent = _highKey;
	}


	public boolean is_isLeftest() {
		return _isLeftest;
	}


	public void set_isLeftest(boolean _isLeftest) {
		this._isLeftest = _isLeftest;
	}


	public boolean is_isRightest() {
		return _isRightest;
	}


	public void set_isRightest(boolean _isRightest) {
		this._isRightest = _isRightest;
	}


	public boolean is_deleteBit() {
		return _deleteBit;
	}


	public void set_deleteBit(boolean _deleteBit) {
		this._deleteBit = _deleteBit;
	}


	public List<String> get_keys() {
		return _keys;
	}

	public List<VPointer> get_children() {
		return _children;
	}

	public List<VPointer> get_pointers() {
		return _pointers;
	}

	public boolean isLeafIndex() {
    	return _isLeafIndex;
    }

    public void setLeafIndex(boolean isLeafIndex) {
        _isLeafIndex = isLeafIndex;
    }

    public VPointer getPointerPage() {
        return _pointerPage;
    }

    public VPointer getSideLink() {
        return _sideLink;
    }

    public void setPointerPage(VPointer pointerPage) {
        _pointerPage = pointerPage;
        //touch();
    }

    public void setSideLink(VPointer sideLink) {
        _sideLink = sideLink;
        //touch();
    }

    public void setChildren(List<VPointer> newChildren) {
    	_children = newChildren;
    }

    public void setPointers(List<VPointer> newPointers) {
    	_pointers = newPointers;
    }
    public boolean isDummy() {
        return _isDummy;
    }

    public void setDummy(boolean isDummy) {
        _isDummy = isDummy;
        //touch();
    }

    public VPointer getDummyLink() {
        return _dummyLink;
    }

    public void setDummyLink(VPointer dummyLink) {
        _dummyLink = dummyLink;
        //touch();
    }

    public int getPageLsn() {
        return _pageLsn;
    }

    public void incrementPageLsn() {
        _pageLsn++;
        //touch();
    }

    public boolean getSmBit() {
        return _smBit;
    }

    public void setSmBit(boolean smBit) {
        _smBit = smBit;
        //touch();
    }

    public String getNameNodeID() {
    	return getNodeType()+getNodeIdentifier().toString();
    }



	/**
     * �ݥ��󥿥ڡ����򻲾Ȥ��� VPointer �Υꥹ�Ȥ��֤��ޤ�.
     *
     * @return �ݥ��󥿥ڡ����� VPointer ����󤹤� Iterator ���֥�������
     */
    public Iterator pointerPagesEntries() {
        return new Iterator() {
            private int i = 0;

            //private final int size = _pointerPages.size();
            private int size = 0;

            public boolean hasNext() {
                return i < size;
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return _pointers.get(i++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    public void addEntry(int pos, String key, Pointer vp) {
    	System.out.println(FBTDirectory.SPACE);
    	System.out.println("addEntry Pointer");

		try {
            _keys.add(pos, key);
            _children.add(pos, (Pointer) vp);
            System.out.println("Successfully added Entry at "+getNameNodeID());
            System.out.println("position " +pos);
        	System.out.println("key "+ key);
            System.out.println("child "+ vp.toString());
            //key2String();
        } catch (ClassCastException e) {
            System.out.println(vp);
            System.out.println(this);
            e.printStackTrace();
        }
	}

	/**
     * pos �ΰ��֤� key �ȻҤؤΥݥ��� vp ���ɲä��ޤ�.
     *
     * @param pos �ɲä������
     * @param key �ɲä��륭��
     * @param vp �ɲä���ҥ���ȥ꡼
     */
    public void addEntry(int pos, String key, VPointer vp) {
    	System.out.println(FBTDirectory.SPACE);
    	//System.out.println("addEntry " +vp.toString() + "at "+getNameNodeID());
    	System.out.println("addEntry vPointer");
        try {
            _keys.add(pos, key);
            _children.add(pos, (Pointer) vp);
            System.out.println("Successfully added Entry at "+getNameNodeID());
            System.out.println("position " +pos);
        	System.out.println("key "+ key);
            System.out.println("child "+ vp.toString());
            //key2String();
        } catch (ClassCastException e) {
            System.out.println(vp);
            System.out.println(this);
            e.printStackTrace();
        }
    }



	/**
     * pos �ΰ��֤λҤΥ���ȥ꡼���֤�.
     *
     * @param pos ���륨��ȥ꡼�ΰ���
     * @return pos �ΰ��֤Υ���ȥ꡼
     */
    public VPointer getEntry(int pos) {
        return (VPointer) _children.get(pos);
    }

	public void replaceEntry(int pos, VPointer vp) {
		try {
			_children.set(pos, vp);
		} catch (ClassCastException e) {
            System.out.println(vp);
            System.out.println(this);
            e.printStackTrace();
        }
	}

	/**
     * pos �ΰ��֤Υ���ȥ꡼�����.
     *
     * @param pos ���륨��ȥ꡼�ΰ���
     */
    public void removeEntry(int pos) {
    	_keys.remove(pos);
        _children.remove(pos);
        _pointers.remove(pos);
    }
    /**
     * pos �ΰ��֤˥���ȥ꡼����Ƥ���(�ݥ��󥿥ڡ�����)�Υݥ��󥿤����.
     *
     * @param pos ����ݥ��󥿤Υ���ȥ꡼����
     * @return pos �ΰ��֤˥���ȥ꡼����Ƥ���ݥ��󥿡��ڡ����ؤΥݥ���
     */
    public VPointer getPointer(int pos) {
        return _pointers.get(pos);
    }

    /**
     * pos �ΰ��֤˥ݥ��󥿥ڡ����إݥ��� vp ���ɲä��ޤ�.
     *
     * @param pos �ɲä������
     * @param vp �ɲä���ݥ��󥿥ڡ�������ȥ�
     */
    public void addPointer(int pos, VPointer vp) {
    	StringUtility.debugSpace("addPointer");
    	System.out.println("position, vp :"+pos + ", "+vp.toString());
        _pointers.add(pos, vp);
    }

    /**
     * pos �ΰ��֤Υݥ��󥿥ڡ�������ȥ꡼�� vp ���֤�������.
     *
     * @param pos �֤����������
     * @param vp �֤������륨��ȥ꡼
     */
    public void replacePointer(int pos, VPointer vp) {
        _pointers.set(pos, vp);
    }



    /**
     * �Х��ʥ꡼���������Ѥ��� _keys �ꥹ�Ȥ� key �ΰ��֤򸡺��򤹤�.
     * ��������Τ� key �ʲ��Ǻ���Υ����ΰ���.
     * ����ȥ꡼��̵���Ȥ��� -1 ���֤�.
     *
     * @param key �������륭��
     * @return ������̤Ǥ���key�ΰ���
     */
    public int binaryLocate(String key) {
    	//System.out.println(FBTDirectory.SPACE);
    	//key2String();
    	int bottom = 1;
    	//int bottom = 0;
        int top = _keys.size() - 1;
        int middle = 1;
        //int middle = 0;

        while (bottom <= top) {
           middle = (bottom + top + 1) / 2;

           if (key.compareTo((String) _keys.get(middle)) < 0) {
               top = middle - 1;
           } else {
               bottom = middle + 1;
           }
        }
        //return bottom;
        return bottom - 1;
    }
    public void key2String() {
    	System.out.println("Number of keys at "+getNameNodeID()+": "+_keys.size());
    	for(int i=0; i<_keys.size();i++) {
    		System.out.println(getNameNodeID()+"***key["+i+"]: "+_keys.get(i));
    	}
    	System.out.println("Number of children at "+getNameNodeID()+": "+_children.size());
    	for(int i=0; i<_children.size();i++) {
    		System.out.println(getNameNodeID()+"***children["+i+"]: "+_children.get(i));
    	}
    	System.out.println("Number of pointer at "+getNameNodeID()+": "+_pointers.size());
    	for(int i=0; i<_children.size();i++) {
    		System.out.println(getNameNodeID()+"***pointers["+i+"]: "+_pointers.get(i));
    	}
    }
    public int binaryLocateLeftEdge() {
        int bottom = 0;
        int top = _keys.size() - 1;
        int middle = 0;

        int partID = getPointer().getPointer().getPartitionID();

        while (bottom <= top) {
            middle = (bottom + top + 1) / 2;

            Pointer p = (Pointer) _children.get(middle);
            if (partID <= p.getPartitionID()) {
                top = middle - 1;
            } else {
                bottom = middle + 1;
            }
        }

        if (partID == ((Pointer) _children.get(bottom)).getPartitionID()) {
            return bottom;
        } else {
            return bottom + 1;
        }
    }

    public int binaryLocateRightEdge() {
        int bottom = 0;
        int top = _keys.size() - 1;
        int middle = 0;

        int partID = getPointer().getPointer().getPartitionID();

        while (bottom <= top) {
            middle = (bottom + top + 1) / 2;

            Pointer p = (Pointer) _children.get(middle);
            if (partID < p.getPartitionID()) {
                top = middle - 1;
            } else {
                bottom = middle + 1;
            }
        }

        if (partID == ((Pointer) _children.get(bottom - 1)).getPartitionID()) {
            return bottom - 1;
        } else {
            return bottom - 2;
        }
    }

    public int locateLeftEdge() {
        int partID = getPointer().getPointer().getPartitionID();

        for (int i = 0; i < _keys.size(); i++) {
            if (partID == ((Pointer) _children.get(i)).getPartitionID()) {
                return i;
            }
        }

        return -1;
    }

    public int locateRightEdge() {
        int partID = getPointer().getPointer().getPartitionID();

        for (int i = _keys.size() - 1; i > 0; i--) {
            if (partID == ((Pointer) _children.get(i)).getPartitionID()) {
                return i;
            }
        }

        return -1;
    }

    /**
     * pos �ΰ��֤Υ������֤�.
     *
     * @param pos ���륭���ΰ���
     * @return pos �ΰ��֤Υ���
     */
    public String getKey(int pos) {
        return (String) _keys.get(pos);
    }



	public int numberOfLocalEntry() {
	    int count = 0;

	    for(int i = 0; i < _children.size(); i++) {
	        if (_directory.isLocal(_children.get(i))) {
	            count++;
	        }
	    }

	    return count;
	}
	public boolean isInRange(String key) {
		/*System.out.println("IndexNode 573 key "+ key);
		System.out.println("IndexNode 574 _key.first "+ _keys.get(0));
		System.out.println("IndexNode 575  "+ key.compareTo(_keys.get(0)));
		System.out.println("IndexNode leftest  "+ _isLeftest);
		System.out.println("IndexNode rightest  "+ _isRightest);
		*///TODO
		//if (_keys.size()>0) {
	        if (_isLeftest || key.compareTo(_keys.get(0)) >= 0) {
	        	//System.out.println("599 nextKeyOnParent "+_nextKeyOnParent);
	        	if(_nextKeyOnParent==null)
	        		_nextKeyOnParent = "/user/hanhlh/".concat(String.format("%06d",
	        													100));
	        	System.out.println(key.compareTo(_nextKeyOnParent));
	            if (_isRightest || key.compareTo(_nextKeyOnParent) < 0) {
	                return true;
	            }

	            return false;
	        }

	        return false;
		//} else return true;
    }
	public boolean isUnderRange(String key) {
		//TODO
        if (_isLeftest || key.compareTo(_keys.get(0)) >= 0) {
            return false;
        }

        return true;
    }

    public boolean isOverRange(String key) {
    	//TODO
    	if(_nextKeyOnParent==null)
    		_nextKeyOnParent = "/user/hanhlh/".concat(String.format("%06d",
					100));
        if (_isRightest || key.compareTo(_nextKeyOnParent) < 0) {
            return false;
        }

        return true;
    }

    public boolean isOverEntry() {
    	StringUtility.debugSpace("isOverEntry check");
    	return _children.size() > _directory.getFanout()+1;
    }

    public boolean isFullEntry() {
        return _children.size() >= _directory.getFanout()+1;
    }
    public String getHighestKey() {
        return (String) _keys.get(_keys.size() - 1);
    }

    public boolean isRootIndex() {
    	return _isRootIndex;
    }

    public void setRootIndex(boolean isRootIndex) {
        _isRootIndex = isRootIndex;
    }


    public boolean maxKeyIsLowerThan(String key) {
        String maxKey = (String) _keys.get(_keys.size() - 1);
        return maxKey.compareTo(key) < 0;
    }

	public void free() {
		StringUtility.debugSpace("IndexNode.free");
		//TODO Using Locker to free the object contains getPointer()
		/*Locker locker = (Locker) NameNodeFBTProcessor.lookup("/locker");
		locker.removeObject(getPointer());
	    super.free();*/

		if (!isLeafIndex()) {
		    for (int i = 0; i < _pointers.size(); i++) {
		        Node node = _directory.getNode(_pointers.get(i));
		        synchronized (_directory.getLocalNodeMapping()) {
		        	_directory.getLocalNodeMapping().remove(node.getNodeIdentifier().toString());
		        }
		    }
	    }
		((Locker) NameNodeFBTProcessor.lookup("/locker"))
        				.removeObject(getPointer());
	}



	public int size() {
        return _children.size();
    }

    public boolean hasLocal() {
        for (int i = _children.size() - 1; i >= 0; i--) {
            if (_directory.isLocal(_children.get(i))) {
                return true;
            }
        }
        return false;
    }


    public boolean hasLocalDirectory() {
        for (int i = _children.size() - 1; i >= 0; i--) {
            if (_directory.isLocalDirectory(_children.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * ���� IndexNode ��ʬ�䤷�ޤ�.
     *
     * @return ���ΥΡ��ɤα�Ⱦʬ
     */
    public IndexNode split() {
    	StringUtility.debugSpace("IndexNode split");
    	if (!_isRootIndex) {
            _smBit = true;
        }
        _pageLsn++;
        int half  = (_directory.getFanout() + 1) / 2;
        System.out.println("half "+half);
        List<String> klist = _keys.subList(half, _keys.size());
        List<VPointer> clist = _children.subList(half, _children.size());
        List<VPointer> elist = _pointers.subList(half, _pointers.size());

        IndexNode newIndex = new IndexNode(_directory);
        synchronized (_directory.getLocalNodeMapping()) {
        	_directory.getLocalNodeMapping().put(newIndex.getNodeIdentifier().toString(),
        									newIndex);
        }
        System.out.println("create new index node "+newIndex.getNameNodeID());
        newIndex._keys.addAll(klist);
        newIndex._children.addAll(clist);
        newIndex._pointers.addAll(elist);
        newIndex._isLeafIndex = _isLeafIndex;
        newIndex._nextKeyOnParent = _nextKeyOnParent;
        newIndex._isRightest = _isRightest;

        newIndex._sideLink = _sideLink;
        newIndex._isDummy = false;
        newIndex._dummyLink = null;
        //newIndex.touch();

        klist.clear();
        clist.clear();
        elist.clear();
        _nextKeyOnParent = newIndex.getKey(0);
        _isRightest = false;
        _sideLink = newIndex.getPointer();
        return newIndex;
    }

    public IndexNode entryCopy() {
        IndexNode newIndex = new IndexNode(_directory);
        synchronized (_directory.getLocalNodeMapping()) {
        	_directory.getLocalNodeMapping().put(newIndex.getNodeIdentifier().
        									toString(), newIndex);
        }
        newIndex._keys.addAll(_keys);
        newIndex._children.addAll(_children);
        newIndex._pointers.addAll(_pointers);
        newIndex._isLeafIndex = _isLeafIndex;
        newIndex._nextKeyOnParent = _nextKeyOnParent;
        newIndex._isLeftest = _isLeftest;
        newIndex._isRightest = _isRightest;
        newIndex._sideLink = _sideLink;
        newIndex._isDummy = _isDummy;
        newIndex._dummyLink = _dummyLink;
        //newIndex.touch();

        _keys.clear();
        _children.clear();
        _pointers.clear();
        //touch();

        return newIndex;
    }



    @Override
	public String toString() {
		return "IndexNode [_nodeType=" + _nodeType + "\n"+
				"_keys=" + _keys +"\n"
				+ "_children=" + _children +"\n"
				+ "_pointers=" + _pointers +"\n"
				+ "_isLeafIndex=" + _isLeafIndex + ", _isRootIndex="
				+ _isRootIndex + ", _nextKeyOnParent=" + _nextKeyOnParent
				+ ", _highestKeyOnChildren=" + _highestKeyOnChildren
				+ ", _isLeftest=" + _isLeftest + ", _isRightest=" + _isRightest
				+ ", _deleteBit=" + _deleteBit + ", _pointerPage="
				+ _pointerPage + ", _sideLink=" + _sideLink + ", _dummyLink="
				+ _dummyLink + ", _isDummy=" + _isDummy + ", _pageLsn="
				+ _pageLsn + ", _smBit=" + _smBit + ", _nodeIdentifier="
				+ _nodeIdentifier + "]";
	}

	public NodeIdentifier getNodeIdentifier() {
    	return _nodeIdentifier;
    }


	public boolean hasCopy() {
        if (!_directory.isLocal(_children.get(0))) {
            return true;
        }
        if (!_directory.isLocal(_children.get(_children.size() - 1))) {
            return true;
        }

        if (_isLeafIndex) {
            return false;
        }

        VPointer vp;
        PointerNode pointer;

        vp = _pointers.get(0);
        pointer = (PointerNode) _directory.getNode(vp);
        if (pointer.size() > 1) {
            return true;
        }

        vp = _pointers.get(_pointers.size() - 1);
        pointer = (PointerNode) _directory.getNode(vp);
        if (pointer.size() > 1) {
            return true;
        }

        return false;
    }
	public void accept(NodeVisitor visitor, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		visitor.visit(this, self);
	}

	public String getKey() {
		return (_keys.size() > 0)
        ? _keys.get(0)
        : null;
	}

	public void clear() {
        if (!isLeafIndex()) {
            for (int i = 0; i < _pointers.size(); i++) {
                Node node = _directory.getNode(_pointers.get(i));
                synchronized (_directory.getLocalNodeMapping()) {
                	_directory.getLocalNodeMapping().remove(node);
                }
            }
        }

        _keys.clear();
        _children.clear();
        _pointers.clear();

        //touch();
    }






}
