/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.utils;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author hanhlh
 *
 */
public class ThreadPoolExecutorObserver implements Runnable {

private final ThreadPoolExecutor _executor;

    private long _interval;

    private int[] _poolSizeHistory;

    public ThreadPoolExecutorObserver(ThreadPoolExecutor executor) {
        this(executor, 1L, TimeUnit.MILLISECONDS);
    }

    public ThreadPoolExecutorObserver(
            ThreadPoolExecutor executor, long interval, TimeUnit unit) {
        _executor = executor;
        _interval = unit.toNanos(interval);
        _poolSizeHistory = new int[600];
        for (int i = 0; i < _poolSizeHistory.length; i++) {
            _poolSizeHistory[i] = _executor.getCorePoolSize();
        }
    }

    public void run() {
        int index = 0;
        int count = 0;
        _poolSizeHistory[index] = 0;
        while (!_executor.isShutdown()) {
            LockSupport.parkNanos(_interval);

            if (_poolSizeHistory[index] < _executor.getPoolSize()) {
                _poolSizeHistory[index] = _executor.getPoolSize();
            }

            int max = 0;
            for (int i = 0; i < _poolSizeHistory.length; i++) {
                if (max < _poolSizeHistory[i]) {
                    max = _poolSizeHistory[i];
                }
            }
            _executor.setCorePoolSize((int) (0.9 * max));

            count = (count + 1) % 100;
            if (count == 0) {
                index = (index + 1) % _poolSizeHistory.length;
                _poolSizeHistory[index] = 0;
            }
        }
    }

}
