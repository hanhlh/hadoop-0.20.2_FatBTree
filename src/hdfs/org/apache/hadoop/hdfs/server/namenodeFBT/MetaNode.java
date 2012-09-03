/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.io.Writable;

/**
 * @author hanhlh
 *
 */
public class MetaNode extends AbstractNode implements Writable, Serializable {
	private String _nodeType;
	private int _partitionID;

	private int _ownerID;

    protected int _fanout;

    protected String _key;

    private VPointer _root;

    /** Number of leaf nodes inside this partition */

    private int _entryCount;

    private VPointer _newest;

    private VPointer _oldest;

    private int _keyLength;

    private int _dataSize;

    protected int _leafFanout;

    private boolean _accessCountFlg;

    private int _accessCount;
    /**
     * 全 RootIndex へのリンクがエントリされているポインタページを指すポインタ
     */
    private VPointer _pointerEntry;

    private AtomicInteger _restartCount;

    private AtomicInteger _moreRestartCount;

    private AtomicInteger _redirectCount;

	private AtomicIntegerArray _lockCount;

    private AtomicInteger _correctingCount;

    private AtomicInteger _chaseCount;

    private VPointer _dummyLeaf;

    private AtomicInteger _treeHeight;

    private String _treeLockerPartID;

    private List<Pointer> _dummies;

    private NodeIdentifier _nodeIdentifier;


    //constructor

    public MetaNode(FBTDirectory directory, int partitionID,
    		int ownerID, int fanout, int leafFanout,
    		boolean accessCountFlg) {

        super(directory);
        _nodeType = "meta_";
        _nodeIdentifier = new NodeIdentifier(directory.getOwner(),
				directory.getNodeSequence().getAndIncrement());
        _partitionID = partitionID;
        _ownerID = ownerID;
        _fanout = fanout;
        _key = null;
        _root = new PointerSet();
        _entryCount = 0;
        _newest = null;
        _oldest = null;
        _leafFanout = leafFanout;
        _accessCountFlg = accessCountFlg;
        _accessCount = 0;
        _restartCount = new AtomicInteger(0);
        _moreRestartCount = new AtomicInteger(0);
        _redirectCount = new AtomicInteger(0);
        _lockCount = new AtomicIntegerArray(128);
        for (int i = 0; i < 128; i++) {
            _lockCount.set(i, 0);
        }
        _correctingCount = new AtomicInteger(0);
        _chaseCount = new AtomicInteger(0);

        _treeHeight = new AtomicInteger(0);
        _dummies = new ArrayList<Pointer>();
    }

    public MetaNode(FBTDirectory directory, int partitionID,
            				int ownerID, int fanout, int leafFanout) {

        this(directory, partitionID, ownerID, fanout, leafFanout, true);
    }

    public MetaNode(FBTDirectory directory, int partitionID, int ownerID, int fanout) {
        this(directory, partitionID, ownerID, fanout, 0, true);
    }

    public MetaNode() {}
 // accessors //////////////////////////////////////////////////////////////

    public VPointer getPointerEntry() {
        return _pointerEntry;
    }

    public void setPointerEntry(VPointer pointerEntry) {
        _pointerEntry = pointerEntry;
    }
    public int getPartitionID() {
        return _partitionID;
    }
    public int getOwnerID() {
    	return _ownerID;
    }

    public int getFanout() {
        return _fanout;
    }

    public VPointer getRootPointer() {
        return _root;
    }

    public void setRootPointer(VPointer vp) {
        _root = vp;
        //touch();
    }

    public void addRootPointer(VPointer vp) {
        _root.add(vp);
    }

    public void setKey(String key) {
        _key = key;
        //touch();
    }

    public synchronized int getEntryCount() {
        return _entryCount;
    }

    public synchronized void incrementEntryCount() {
        _entryCount++;
        //touch();
    }

    public synchronized void decrementEntryCount() {
        _entryCount--;
        //touch();
    }

    public VPointer getNewestLeaf() {
        return _newest;
    }

    public void setNewestLeaf(VPointer vp) {
        _newest = vp;
        //touch();
    }

    public VPointer getOldestLeaf() {
        return _oldest;
    }

    public void setOldestLeaf(VPointer vp) {
        _oldest = vp;
        //touch();
    }

 // instance attributes ////////////////////////////////////////////////////

    public String get_key() {
		return _key;
	}

	public void set_key(String _key) {
		this._key = _key;
	}

	public VPointer get_root() {
		return _root;
	}

	public void set_root(VPointer _root) {
		this._root = _root;
	}

	public int get_entryCount() {
		return _entryCount;
	}

	public void set_entryCount(int _entryCount) {
		this._entryCount = _entryCount;
	}

	public VPointer get_newest() {
		return _newest;
	}

	public void set_newest(VPointer _newest) {
		this._newest = _newest;
	}

	public VPointer get_oldest() {
		return _oldest;
	}

	public void set_oldest(VPointer _oldest) {
		this._oldest = _oldest;
	}

	public int get_keyLength() {
		return _keyLength;
	}

	public void set_keyLength(int _keyLength) {
		this._keyLength = _keyLength;
	}

	public int get_dataSize() {
		return _dataSize;
	}

	public void set_dataSize(int _dataSize) {
		this._dataSize = _dataSize;
	}

	public int get_leafFanout() {
		return _leafFanout;
	}

	public void set_leafFanout(int _leafFanout) {
		this._leafFanout = _leafFanout;
	}

	public boolean is_accessCountFlg() {
		return _accessCountFlg;
	}

	public void set_accessCountFlg(boolean _accessCountFlg) {
		this._accessCountFlg = _accessCountFlg;
	}

	public int get_accessCount() {
		return _accessCount;
	}

	public void set_accessCount(int _accessCount) {
		this._accessCount = _accessCount;
	}

	public VPointer get_pointerEntry() {
		return _pointerEntry;
	}

	public void set_pointerEntry(VPointer _pointerEntry) {
		this._pointerEntry = _pointerEntry;
	}

	public AtomicInteger get_restartCount() {
		return _restartCount;
	}

	public void set_restartCount(AtomicInteger _restartCount) {
		this._restartCount = _restartCount;
	}

	public AtomicInteger get_moreRestartCount() {
		return _moreRestartCount;
	}

	public void set_moreRestartCount(AtomicInteger _moreRestartCount) {
		this._moreRestartCount = _moreRestartCount;
	}

	public AtomicInteger get_redirectCount() {
		return _redirectCount;
	}

	public void set_redirectCount(AtomicInteger _redirectCount) {
		this._redirectCount = _redirectCount;
	}

	public AtomicIntegerArray get_lockCount() {
		return _lockCount;
	}

	public void set_lockCount(AtomicIntegerArray _lockCount) {
		this._lockCount = _lockCount;
	}

	public AtomicInteger get_correctingCount() {
		return _correctingCount;
	}

	public void set_correctingCount(AtomicInteger _correctingCount) {
		this._correctingCount = _correctingCount;
	}

	public AtomicInteger get_chaseCount() {
		return _chaseCount;
	}

	public void set_chaseCount(AtomicInteger _chaseCount) {
		this._chaseCount = _chaseCount;
	}

	public VPointer get_dummyLeaf() {
		return _dummyLeaf;
	}

	public void set_dummyLeaf(VPointer _dummyLeaf) {
		this._dummyLeaf = _dummyLeaf;
	}

	public AtomicInteger get_treeHeight() {
		return _treeHeight;
	}

	public void set_treeHeight(AtomicInteger _treeHeight) {
		this._treeHeight = _treeHeight;
	}

	public int get_partitionID() {
		return _partitionID;
	}

	public int get_fanout() {
		return _fanout;
	}

	public String getNameNodeID() {
		return getNodeType() + getNodeIdentifier().toString();
	}

	public String getTreeLockerPartID() {
        return _treeLockerPartID;
    }

    public void setTreeLockerPartID(String treeLockerPartID) {
        _treeLockerPartID = treeLockerPartID;
    }
    public List<Pointer> getDummies() {
        return _dummies;
    }

    public void addDummy(Pointer dummy) {
        _dummies.add(dummy);
    }
    public String getNodeType() {
    	return _nodeType;
    }

    public void replaceDummy(Pointer oldDummy, Pointer newDummy) {
        _dummies.set(_dummies.indexOf(oldDummy), newDummy);

    }
    public int getTreeHeight() {
        return _treeHeight.get();
    }

    public void setTreeHeight(int treeHeight) {
        _treeHeight.set(treeHeight);
    }

    public void incrementTreeHeight() {
        _treeHeight.incrementAndGet();
    }
    // instance methods ///////////////////////////////////////////////////////


    public int getLeftPartitionID() {
        PointerNode pNode = (PointerNode) _directory.getNode(_pointerEntry);
        return pNode.getLeftPartitionID();
    }




	@Override
	public String toString() {
		return "MetaNode [_partitionID=" + _partitionID + ", _ownerID="
				+ _ownerID + ", _fanout=" + _fanout + ", _key=" + _key
				+ ", _root=" + _root + ", _entryCount=" + _entryCount
				+ ", _newest=" + _newest + ", _oldest=" + _oldest
				+ ", _keyLength=" + _keyLength + ", _dataSize=" + _dataSize
				+ ", _leafFanout=" + _leafFanout + ", _accessCountFlg="
				+ _accessCountFlg + ", _accessCount=" + _accessCount
				+ ", _pointerEntry=" + _pointerEntry + ", _restartCount="
				+ _restartCount + ", _moreRestartCount=" + _moreRestartCount
				+ ", _redirectCount=" + _redirectCount + ", _lockCount="
				+ _lockCount + ", _correctingCount=" + _correctingCount
				+ ", _chaseCount=" + _chaseCount + ", _dummyLeaf=" + _dummyLeaf
				+ ", _treeHeight=" + _treeHeight + ", _treeLockerPartID="
				+ _treeLockerPartID + ", _dummies=" + _dummies
				+ ", _nodeIdentifier=" + _nodeIdentifier + "]";
	}

	public int getRightPartitionID() {
        PointerNode pNode = (PointerNode) _directory.getNode(_pointerEntry);
        return pNode.getRightPartitionID();
    }

    public int getNonLoopLeftPartitionID() {
        PointerNode pNode = (PointerNode) _directory.getNode(_pointerEntry);
        return pNode.getNonLoopLeftPartitionID();
    }

    public int getNonLoopRightPartitionID() {
        PointerNode pNode = (PointerNode) _directory.getNode(_pointerEntry);
        return pNode.getNonLoopRightPartitionID();
    }

    public void incrementRestartCount() {
        _restartCount.incrementAndGet();
        //touch();
    }

    public void incrementMoreRestartCount() {
        _moreRestartCount.incrementAndGet();
        //touch();
    }

    public void incrementRedirectCount() {
        _redirectCount.incrementAndGet();
        //touch();
    }

    public void resetRestartCount() {
        _restartCount.set(0);
        //touch();
    }

    public void resetMoreRestartCount() {
        _moreRestartCount.set(0);
        //touch();
    }

    public void resetRedirectCount() {
        _redirectCount.set(0);
        //touch();
    }

    public int getRestartCount() {
        return _restartCount.get();
    }

    public int getMoreRestartCount() {
        return _moreRestartCount.get();
    }

    public int getRedirectCount() {
        return _redirectCount.get();
    }

    public void incrementLockCount(int count) {
        _lockCount.incrementAndGet(count);
    }

    public void resetLockCount() {
        for (int i = 0; i < 128; i++) {
            _lockCount.set(i, 0);
        }
    }

    public void incrementCorrectingCount() {
        _correctingCount.incrementAndGet();
    }

    public void incrementChaseCount() {
        _chaseCount.incrementAndGet();
    }

    public void resetCorrectingCount() {
        _correctingCount.set(0);
    }

    public void resetChaseCount() {
        _chaseCount.set(0);
    }

    public int getCorrectingCount() {
        return _correctingCount.get();
    }

    public int getChaseCount() {
        return _chaseCount.get();
    }


	public String getKey() {
        return _key;
        }

    public int getKeyLength() {
        return _keyLength;
    }

    public int getDataSize() {
        return _dataSize;
    }

    public void setKeyLength(int keyLength) {
        _keyLength = keyLength;
    }

    public void setDataSize(int dataSize) {
        _dataSize = dataSize;
    }

    public boolean getAccessCountFlg() {
        return _accessCountFlg;
    }

    public int getAccessCount() {
        return _accessCount;
    }

    public void setAccessCountFlg(boolean accessCountFlg) {
        _accessCountFlg = accessCountFlg;
    }

    public synchronized void incrementAccessCount() {
        _accessCount++;
    }

    public synchronized void incrementAccessCount(int count) {
        _accessCount = _accessCount + count;
    }

    public synchronized void decrementAccessCount(int count) {
        _accessCount = _accessCount - count;
    }

    public synchronized void resetAccessCount() {
        _accessCount = 0;
    }


    public void logLockCount() {
    	StringUtility.debugSpace("MetaNode.logLockCount");
        Logger logger = Logger.getLogger("AutoDisk2-lock");
        try {
            // 設定ファイルの読み込み
            FileInputStream inputStream = new FileInputStream(
                    "etc/default/logging.properties");
            LogManager.getLogManager().readConfiguration(inputStream);

            // FileHandler を生成
            String host =
                InetAddress.getLocalHost().getHostName().split("\\.")[0];
            FileHandler fh = new FileHandler("log/"+ host + "lock.log", true);

            // Level を設定します
            fh.setLevel(Level.ALL);

            // Formatter を設定します
            fh.setFormatter(new LogFormatter());

            // ログの出力先を追加
            logger.addHandler(fh);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            NameNode.LOG.info("MetaNode exit exception");
            System.out.println("MetaNode exit exception");
            System.exit(1);
        }

        for (int i = 0; i < 128; i++) {
         //   logger.severe(_lockCount[i] + "\t");
        }
        logger.severe("\n");
    }


    public void accept(NodeVisitor visitor, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		visitor.visit(this, self);
	}

    public NodeIdentifier getNodeIdentifier() {
    	return _nodeIdentifier;
    }
	public void setNodeID(long nodeID) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public void setPartitionID(int partitionID) {
		_partitionID = partitionID;
	}
	@Override
	public void write(DataOutput out) throws IOException {
		StringUtility.debugSpace("MetaNode.write");
		out.writeInt(_partitionID);
		out.writeInt(_ownerID);
		_root.write(out);
		out.writeInt(_entryCount);
		_pointerEntry.write(out);
		out.writeInt(_accessCount);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		StringUtility.debugSpace("MetaNode.read");
		_partitionID = in.readInt();
		_ownerID = in.readInt();
		_root = new PointerSet();
		_root.readFields(in);
		_entryCount = in.readInt();
		_pointerEntry = new Pointer();
		_pointerEntry.readFields(in);
		_accessCount = in.readInt();
	}

	}
