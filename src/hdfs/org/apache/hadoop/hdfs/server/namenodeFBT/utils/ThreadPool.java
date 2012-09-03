/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.utils;

/**
 * @author hanhlh
 *
 */
public class ThreadPool extends Thread {
	// instance attributes ////////////////////////////////////////////////////

    /** ターゲットの Runnable オブジェクト */
    private Runnable _target;

    /** このスレッドプールに属するスレッドの ThreadGroup */
    private ThreadGroup _tgroup;

    /** このスレッドプールが使用するスレッド */
    private Thread[] _workers;

    // constructors ///////////////////////////////////////////////////////////

    public ThreadPool(Runnable target, int nthreads) {
        this("ThreadPool", target, nthreads);
    }

    public ThreadPool(String name, Runnable target, int nthreads) {
        _target = target;
        _tgroup = new ThreadGroup(name);
		_tgroup.setDaemon(true);
        _workers = new Thread[nthreads];
		for (int i = 0; i < _workers.length; i++) {
			_workers[i] = new Thread(_tgroup, _target, name + i);
			_workers[i].setDaemon(true);
		}
    }

    // interface Runnable /////////////////////////////////////////////////////

    /**
     * <p>ターゲットオブジェクトをスレッドプールによって並列に実行します。
     * 正しい実行結果を得るためには、ターゲットの run() メソッドの実装が
     * スレッドセーフである必要があります。</p>
     */
    public void run() {
        for (int i = 0; i < _workers.length; i++) {
            _workers[i].start();
        }
    }

    // instance methods ///////////////////////////////////////////////////////

    public void shutdown() {
        for (int i = 0; i < _workers.length; i++) {
            try {
                _workers[i].join();
            } catch (InterruptedException e) {
                /* ignore */
            }
        }
    }

}
