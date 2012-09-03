/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public class LockObject {
	private Object _obj;

    private LinkedList<Lock> _list;

    private int _mode;

    private final ReentrantLock _lock;

    private final Condition _condition;

    private ConcurrentLinkedQueue<Lock> _waitList;

    private AtomicInteger _waiterCount;

    private final ReentrantLock _lockForQueue;

    private final Condition _conditionForQueue;

    public LockObject(Object obj) {
        _obj = obj;
        _list = new LinkedList<Lock>();
        _mode = Lock.NONE;
        _lock = new ReentrantLock(true);
        _condition = _lock.newCondition();
        _waitList = new ConcurrentLinkedQueue<Lock>();
        _waiterCount = new AtomicInteger(0);
        _lockForQueue = new ReentrantLock(true);
        _conditionForQueue = _lockForQueue.newCondition();
    }

    public LockObject() {
        this(null);
    }

    public synchronized int getMode() {
        return _mode;
    }

    public LinkedList<Lock> getList() {
        return _list;
    }

    public String toString() {
        return _obj + Integer.toString(_mode) + _list;
    }

    public String dump() {
        StringBuffer buf = new StringBuffer();
        if (_mode > 0) {
            buf.append("pageID = ")
            .append(_obj)
            .append("\tmode = ")
            .append(_mode)
            .append("\t")
            .append(_list)
            .append("\twait =")
            .append(_waitList)
            .append("\n");
            return buf.toString();
        }

        return "";
    }

    public synchronized void addLock(LockKey key, int request,
            				Object keyid, Object objid, Locker locker) {
        int newMode;
        Lock lock = new Lock(key, this, request);

//    	((Logger) AutoDisk.lookup("/logger")).
//    	severe("locks " + keyid + objid + request + "\n");

        try {
            while ((newMode = Lock.matrix[_mode][request]) == Lock.CONF) {
                wait();
            }

            _mode = newMode;
            _list.add(lock);
            key.addLock(lock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (request == Lock.X) {
            locker.incrementCount();
        } else if (request == Lock.IX) {
            locker.incrementIxCount();
        }

//    	((Logger) AutoDisk.lookup("/logger")).
//    	severe("lockf " + keyid + objid + request + "\n");
    }

    public void add(LockKey key, int request, Locker locker) {
        int newMode;
        Lock lock = new Lock(key, this, request);
        boolean doWait = false;
        _waitList.add(lock);
        _lockForQueue.lock();
        _lock.lock();
        try {
            if (_waiterCount.get() > 0) {
            	System.out.println("line 124 waiterCount "+_waiterCount);
                doWait = true;
                _waiterCount.decrementAndGet();
            }

            //System.out.println("_mode, request %10 "+_mode +", "+request%10);
            while ((newMode = Lock.matrix[_mode][request % 10]) == Lock.CONF) {
            	System.out.println("Lock Conflick");
                doWait = true;
                System.out.println("LockObject line 132");
                _condition.await();
                System.out.println("LockObject line 134");
                _waiterCount.set(_lockForQueue.getQueueLength());
                System.out.println("LockObject line 136");
                System.out.println("line 138 LockObject waiterCount "+_waiterCount);
            }
            //System.out.println("Out from Lock conflick");
            _mode = newMode;
            _list.add(lock);
            key.addLock(lock);

            _waitList.remove(lock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            _lock.unlock();
            _lockForQueue.unlock();
        }

        if ((request % 10) == Lock.X) {
            locker.incrementCount();

//            locker.incrementXCountPagePerHeight(request / 10);
        } else if ((request % 10) == Lock.IX) {
            locker.incrementIxCount();
        }

        if (doWait) {
            locker.incrementCollisionCount();
        }
    }

    public void add(LockKey key, int request, Locker locker, String log) {
        int newMode;
        Lock lock = new Lock(key, this, request);
        lock.setLog(log);

//        _waitList.add(lock);

        _lock.lock();
        try {
            while ((newMode = Lock.matrix[_mode][request % 10]) == Lock.CONF) {
                _condition.await();
            }

            _mode = newMode;
            _list.add(lock);
            key.addLock(lock);

//            _waitList.remove(lock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            _lock.unlock();
        }

        if ((request % 10) == Lock.X) {
            locker.incrementCount();
        } else if ((request % 10) == Lock.IX) {
            locker.incrementIxCount();
        }
    }

    public synchronized void convertLock(LockKey key, int request) {
        // Ì¤¼ÂÁõ
    }

    public void convert(LockKey key, int request) {
        int newMode;
        Lock lock = new Lock(key, this, request);

        _lock.lock();
        try {
            Lock oldLock = null;
            ListIterator<Lock> iter = _list.listIterator(0);
            _mode = Lock.NONE;
            while (iter.hasNext()) {
                oldLock = iter.next();
                if (key.equals(oldLock._key)) {
                    key.removeLock(oldLock);
                    iter.remove();
                } else {
                    _mode = Lock.matrix[_mode][lock._mode % 10];
                }
            }

            newMode = Lock.matrix[_mode][request % 10];
            if (newMode == Lock.CONF) {
                throw new DeadLockException();
            }
            _mode = newMode;
            _list.add(lock);
            key.addLock(lock);

            _condition.signal();
        } finally {
            _lock.unlock();
        }
    }

    public synchronized void removeLock(
            LockKey key, Object keyid, Object objid) {
//    	((Logger) AutoDisk.lookup("/logger")).
//    	severe("unlocks " + keyid + objid+ " " + _list + "\n");

        Lock lock = null;
        ListIterator iter = _list.listIterator(0);
        _mode = Lock.NONE;
        while (iter.hasNext()) {
            lock = (Lock) iter.next();
            if (key.equals(lock._key)) {
                key.removeLock(lock);
                iter.remove();
            } else {
                _mode = Lock.matrix[_mode][lock._mode];
            }
        }
        notifyAll();

//    	((Logger) AutoDisk.lookup("/logger")).
//    	severe("unlockf " + keyid + objid+ " " + _list + "\n");
    }

    public void remove(LockKey key) {
        //StringUtility.debugSpace("LockObject.remove key "+key.toString());
        _lock.lock();

        try {
            Lock lock = null;
            ListIterator<Lock> iter = _list.listIterator(0);
            _mode = Lock.NONE;
            while (iter.hasNext()) {
                lock = iter.next();
                if (key.equals(lock._key)) {
                    key.removeLock(lock);
                    iter.remove();
                } else {
                    _mode = Lock.matrix[_mode][lock._mode % 10];
                }
            }

            _condition.signal();
        } finally {
            _lock.unlock();
        }
    }

    public synchronized void addInstantLock(int request) {
        try {
            while (Lock.matrix[_mode][request] == Lock.CONF) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
