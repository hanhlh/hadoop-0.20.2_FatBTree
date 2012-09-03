/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import java.util.LinkedList;


/**
 * @author hanhlh
 *
 */
public class LockKey {

	protected Object _keyid;

    protected LinkedList<Lock> _list;

    protected int _count;

    public LockKey(Object keyid) {
        _keyid  = keyid;
        _list = new LinkedList<Lock>();
    }

    public void addLock(Lock lock) {
        _list.add(lock);
    }

    public void removeLock(Lock lock) {
        _list.remove(lock);
    }

    public int getListSize() {
        return _list.size();
    }

    public boolean equals(LockKey key) {
        if (_keyid.equals(key._keyid)) {
            return true;
        }
        return false;
    }

    public String toString() {
        return (String) _keyid;
    }

}
