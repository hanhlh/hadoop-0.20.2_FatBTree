/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.Service;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;


/**
 * @author hanhlh
 *
 */
public class Locker implements Service {
	public static final String NAME = "/locker";
// instance attributes ////////////////////////////////////////////////////

    protected ConcurrentHashMap<Object, LockKey> _keyhash;

    private ConcurrentHashMap<Object, Object> _objhash;

    private AtomicInteger _count;

    private AtomicInteger _ixCount;

    private AtomicIntegerArray _xCountPagePerHeight;

    private AtomicIntegerArray _xCountNodePerHeight;

    private AtomicIntegerArray _xCountNodePerRange;

    private AtomicInteger _collisionCount;

    private int _left;

    private int _right;

    private final ReentrantLock _lock;

    private final Condition _condition;

    private StringBuffer _buf;

 // constructors ///////////////////////////////////////////////////////////

    /**
     * <p>������ Locker �Υ��󥹥��󥹤�������ޤ���������� initialize
     * �᥽�åɤǽ���ɬ�פ�����ޤ���</p>
     */
    public Locker() {
        _lock = new ReentrantLock(true);
        _condition = _lock.newCondition();
    }

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#initialize(org.apache.hadoop.conf.Configuration)
	 */
	public void initialize(Configuration conf) throws ServiceException {
		_keyhash = new ConcurrentHashMap<Object, LockKey>();
        _objhash = new ConcurrentHashMap<Object, Object>();
        _count = new AtomicInteger(0);
        _ixCount = new AtomicInteger(0);
        _xCountPagePerHeight = new AtomicIntegerArray(10);
        _xCountNodePerHeight = new AtomicIntegerArray(10);
        _xCountNodePerRange = new AtomicIntegerArray(10);
        _collisionCount = new AtomicInteger(0);
        _left = 0;
        _right = 0;
        _buf = new StringBuffer();
	}

// instance methods ///////////////////////////////////////////////////////

    public void lock(Object keyid, Object objid, int mode) {
        try {
        	//System.out.println("Locker.lock "+
        	//		keyid.toString()+", "
        	//		+objid.toString()+", "
        	//		+mode);

            getObject(objid).add(getKey(keyid), mode, this);
        } catch (ClassCastException e) {
            System.out.println(objid);
            e.printStackTrace();
        }
    }

    public void lock(Object keyid, Object objid, int mode, String logrecord) {
        getObject(objid).add(getKey(keyid), mode, this, logrecord);
    }
    public void convert(Object keyid, Object objid, int mode) {
//      getObject(objid).convertLock(getKey(keyid), mode);
      getObject(objid).convert(getKey(keyid), mode);
    }
    public void unlock(Object keyid, Object objid) {

    	//System.out.println("Locker.unlock "+keyid.toString()+
    	//					", target "+objid.toString());
        try {
//            _buf.append("unlock\t" + keyid + "\t" + objid + "\n");

            LockKey key = getKey(keyid);
            //getObject(objid).removeLock(key, keyid, objid);
            getObject(objid).remove(key);
//            getLatch(objid).unlatch((String) keyid);
            if (key.getListSize() == 0) {
                _keyhash.remove(keyid);
            }
        } catch (ClassCastException e) {
            System.out.println(objid);
            e.printStackTrace();
        }
    }

    protected LockObject getObject(Object objid) {
        String nodeID = ((Pointer) objid).getNodeID();
        LockObject obj = (LockObject) _objhash.get(nodeID);
        if (obj == null) {
            obj = new LockObject(nodeID);
            Object entry = _objhash.putIfAbsent(nodeID, obj);
            if (entry == null) {
                return obj;
            }

            return (LockObject) entry;
        }

        return obj;
    }
    protected ReentrantLatch getLatch(Object objid) {
        String nodeID = ((Pointer) objid).getNodeID();
        ReentrantLatch latch = (ReentrantLatch) _objhash.get(nodeID);
        if (latch == null) {
            latch = new ReentrantLatch(objid);
            Object entry = _objhash.putIfAbsent(nodeID, latch);
            if (entry == null) {
                return latch;
            }

            return (ReentrantLatch) entry;
        }

        return latch;
    }

    protected LockKey getKey(Object keyid) {
        LockKey key = _keyhash.get(keyid);
        if (key == null) {
            key = new LockKey(keyid);
            LockKey entry = _keyhash.putIfAbsent(keyid, key);
            if (entry == null) {
                return key;
            }

            return entry;
        }

        return key;
    }

    /**
	 * ����ν���ä�key(messageID)�򥭡��ꥹ�Ȥ����.
	 *
	 * @param keyid _keyhash������(Object)
	 */
	public void removeKey(Object keyid) {
	    _keyhash.remove(keyid);
	}

	/**
	 * delete����object(�Ρ��ɥݥ���)�򥪥֥������ȥꥹ�Ȥ����.
	 *
	 * @param objid _objhash�����ݥ���(Object)
	 */
	public void removeObject(Object objid) {
	    _objhash.remove(((Pointer) objid).getNodeID());
	}

	/**
	 * ���objid�θ��ߤΥ�å��⡼�ɤ�ʸ������֤�
	 * @param objid
	 * @return
	 */
	public String toString(Object objid) {
	    String buffer = Integer.toString((getObject(objid).getMode()));
		buffer = buffer + getObject(objid).getList();
	    return buffer;
	}

    public String toString() {
        return _keyhash.toString() + _objhash.toString();
    }

    public String dump() {
        StringBuffer buf = new StringBuffer();
        Iterator iter = _objhash.values().iterator();
        while (iter.hasNext()) {
            LockObject obj = (LockObject) iter.next();
            buf.append(obj.dump());
        }

        return buf.toString() + _buf.toString();
    }

    public void reset() {
        _keyhash = new ConcurrentHashMap<Object, LockKey>();
        _objhash = new ConcurrentHashMap<Object, Object>();
        _left = 0;
        _right = 0;
        _buf = new StringBuffer();
    }

    public int getCount() {
        return _count.get();
    }

    public void incrementCount() {
        _count.incrementAndGet();
    }

    public void decrementCount() {
        _count.decrementAndGet();
    }

    public void resetCount() {
        _count.set(0);
    }

    public int getIxCount() {
        return _ixCount.get();
    }

    public void incrementIxCount() {
        _ixCount.incrementAndGet();
    }

    public void resetIxCount() {
        _ixCount.set(0);
    }

    public int[] getXCountPagePerHeight() {
        int[] counts = new int[_xCountPagePerHeight.length()];

        for (int i = 0; i < _xCountPagePerHeight.length(); i++) {
            counts[i] = _xCountPagePerHeight.get(i);
        }

        return counts;
    }

    public void incrementXCountPagePerHeight(int height) {
        _xCountPagePerHeight.incrementAndGet(height);
    }

    public void resetXCountPagePerHeight() {
        for (int i = 0; i < _xCountPagePerHeight.length(); i++) {
            _xCountPagePerHeight.set(i, 0);
        }
    }

    public int[] getXCountNodePerHeight() {
        int[] counts = new int[_xCountNodePerHeight.length()];

        for (int i = 0; i < _xCountNodePerHeight.length(); i++) {
            counts[i] = _xCountNodePerHeight.get(i);
        }

        return counts;
    }

    public void incrementXCountNodePerHeight(int height) {
        _xCountNodePerHeight.incrementAndGet(height);
    }

    public void resetXCountNodePerHeight() {
        for (int i = 0; i < _xCountNodePerHeight.length(); i++) {
            _xCountNodePerHeight.set(i, 0);
        }
    }

    public int[] getXCountNodePerRange() {
        int[] counts = new int[_xCountNodePerRange.length()];

        for (int i = 0; i < _xCountNodePerRange.length(); i++) {
            counts[i] = _xCountNodePerRange.get(i);
        }

        return counts;
    }

    public void incrementXCountNodePerRange(int range) {
    	System.out.println("xCountNodePerRange length "+_xCountNodePerRange.length());
        _xCountNodePerRange.incrementAndGet(range);
    }

    public void resetXCountNodePerRange() {
        for (int i = 0; i < _xCountNodePerRange.length(); i++) {
            _xCountNodePerRange.set(i, 0);
        }
    }

    public void incrementXCount(int height, int count, int range) {
        for (int i = 0; i < count; i++) {
            incrementXCountPagePerHeight(height);
        }
        incrementXCountNodePerHeight(height);
        incrementXCountNodePerRange(range);
    }

    public int getCollisionCount() {
        return _collisionCount.get();
    }

    public void incrementCollisionCount() {
        _collisionCount.incrementAndGet();
    }

    public void resetCollisionCount() {
        _collisionCount.set(0);
    }

    public int getKeySize() {
        return _keyhash.size();
    }

    public void lockLeft(String tID) {
//        _buf.append("lockls\t" + tID + "\n");

        _lock.lock();
        try {
            while (_right > 0) {
                _condition.await();
            }
            _left++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            _lock.unlock();
        }

//        _buf.append("locklf\t" + tID + "\n");
    }

    public void lockRight(String tID) {
//        _buf.append("lockrs\t" + tID + "\n");

        _lock.lock();
        try {
            while (_left > 0) {
                _condition.await();
            }
            _right++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            _lock.unlock();
        }

//        _buf.append("lockrf\t" + tID + "\n");
    }

    public void unlockLeft(String tID) {
//        _buf.append("unlockls\t" + tID + "\n");

        _lock.lock();
        try {
            _left--;

            _condition.signal();
        } finally {
            _lock.unlock();
        }

//        _buf.append("unlocklf\t" + tID + "\n");
    }

    public void unlockRight(String tID) {
//        _buf.append("unlockrs\t" + tID + "\n");

        _lock.lock();
        try {
            _right--;

            _condition.signal();
        } finally {
            _lock.unlock();
        }

//        _buf.append("unlockrf\t" + tID + "\n");
    }

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.service.Service#terminate()
	 */
	public void terminate() throws ServiceException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
