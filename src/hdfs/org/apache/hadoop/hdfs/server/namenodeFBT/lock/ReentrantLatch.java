/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author hanhlh
 *
 */
public class ReentrantLatch implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final Object _obj;

    private final Sync _sync;

    private final ConcurrentHashMap _map;

    public ReentrantLatch() {
        this(null, true);
    }

    public ReentrantLatch(Object obj) {
        this(obj, true);
    }

    public ReentrantLatch(boolean fair) {
        this(null, fair);
    }

    public ReentrantLatch(Object obj, boolean fair) {
        _obj = obj;
        if (fair) {
            _sync = new FairSync();
        } else {
            _sync = new NonfairSync();
        }
        _map = new ConcurrentHashMap();
    }

    static final int IX_SHIFT = 10;
    static final int S_SHIFT = 20;
    static final int SIX_SHIFT = 30;
    static final int X_SHIFT = 31;

    static final int UNIT = (1 << IX_SHIFT);

    static final int MASK = (1 << IX_SHIFT) - 1;

    static int ISCount(int c)  { return c & MASK; }
    static int IXCount(int c)  { return (c >>> IX_SHIFT) & MASK; }
    static int SCount(int c)   { return (c >>> S_SHIFT) & MASK; }
    static int SIXCount(int c) { return (c >>> SIX_SHIFT) & 1; }
    static int XCount(int c)   { return c >>> X_SHIFT; }

    abstract static class Sync extends AbstractQueuedSynchronizer {

        protected final int tryAcquireShared(int arg) {
            switch(arg) {
            case Lock.IS:
                return tryAcquireIS(1);

            case Lock.IX:
                return tryAcquireIX(1);

            case Lock.S:
                return tryAcquireS(1);

            case Lock.SIX:
                return tryAcquireSIX(1);

            case Lock.X:
                return tryAcquireX(1);

            default:
                return -1;
            }
        }

        abstract int tryAcquireIS(int acquires);

        abstract int tryAcquireIX(int acquires);

        abstract int tryAcquireS(int acquires);

        abstract int tryAcquireSIX(int acquires);

        abstract int tryAcquireX(int acquires);

        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int c = getState();
                int nextc = -1;
                switch(releases) {
                case Lock.IS:
                    nextc = c - 1;
                break;

                case Lock.IX:
                    nextc = c - (1 << IX_SHIFT);
                break;

                case Lock.S:
                    nextc = c - (1 << S_SHIFT);
                break;

                case Lock.SIX:
                    nextc = c - (1 << SIX_SHIFT);
                break;

                case Lock.X:
                    nextc = c - (1 << X_SHIFT);
                break;
                }
                if (nextc < 0)
                    throw new IllegalMonitorStateException();
                if (compareAndSetState(c, nextc))
                    return true;
            }
        }
    }

    /**
     * Nonfair version of Sync
     */
    final static class NonfairSync extends Sync {

        private static final long serialVersionUID = 7367102342872822567L;

        protected final int tryAcquireIS(int acquires) {
            for (;;) {
                int c = getState();
                if (XCount(c) != 0) {
                    return -1;
                }
                int nextc = c + acquires;
                if (ISCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireIX(int acquires) {
            for (;;) {
                int c = getState();
                if ((XCount(c) + SIXCount(c) + SCount(c)) != 0)
                    return -1;
                int nextc = c + (acquires << IX_SHIFT);
                if (IXCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireS(int acquires) {
            for (;;) {
                int c = getState();
                if ((XCount(c) + SIXCount(c) + IXCount(c)) != 0)
                    return -1;
                int nextc = c + (acquires << S_SHIFT);
                if (SCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireSIX(int acquires) {
            for (;;) {
                int c = getState();
                if ((c >>> IX_SHIFT) != 0)
                    return -1;
                int nextc = c + (acquires << SIX_SHIFT);
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireX(int acquires) {
            for (;;) {
                int c = getState();
                if (c != 0)
                    return -1;
                int nextc = c + (acquires << X_SHIFT);
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }
    }

    /**
     * Fair version of Sync
     */
    final static class FairSync extends Sync {

        private static final long serialVersionUID = 4639200247600295392L;

        protected final int tryAcquireIS(int acquires) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (XCount(c) != 0) {
                    return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + acquires;
                if (ISCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireIX(int acquires) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if ((XCount(c) + SIXCount(c) + SCount(c)) != 0) {
                    return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + (acquires << IX_SHIFT);
                if (IXCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireS(int acquires) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if ((XCount(c) + SIXCount(c) + IXCount(c)) != 0) {
                    return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + (acquires << S_SHIFT);
                if (SCount(nextc) >= UNIT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireSIX(int acquires) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if ((c >>> IX_SHIFT) != 0) {
                    return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + (acquires << SIX_SHIFT);
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }

        protected final int tryAcquireX(int acquires) {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (c != 0) {
                    return -1;
                } else {
                    Thread first = getFirstQueuedThread();
                    if (first != null && first != current)
                        return -1;
                }
                int nextc = c + (acquires << X_SHIFT);
                if (compareAndSetState(c, nextc))
                    return 1;
                // Recheck count if lost CAS
            }
        }
    }

    public void latch(String key, int request) {
        _sync.acquireShared(request);

        _map.put(key, new Integer(request));
//        int nextc = _sync.getC();
//        System.out.println("latch\t" + _obj + "\t" + key + "\trequest\t" + request
//                + "\t" + ISCount(nextc) + "\t"
//                + IXCount(nextc) + "\t" + SCount(nextc) + "\t"
//                + SIXCount(nextc) + "\t" + XCount(nextc));
    }

    public void unlatch(String key) {
//        int nextc = _sync.getC();
//        System.out.println("unlatch\t" + _obj + "\t" + key
//                + "\t\t\t" + ISCount(nextc) + "\t"
//                + IXCount(nextc) + "\t" + SCount(nextc) + "\t"
//                + SIXCount(nextc) + "\t" + XCount(nextc));
        int mode = ((Integer) _map.remove(key)).intValue();
        _sync.releaseShared(mode);
    }

}
