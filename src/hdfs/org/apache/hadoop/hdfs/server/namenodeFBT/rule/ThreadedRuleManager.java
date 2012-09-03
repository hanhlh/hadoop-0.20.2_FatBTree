/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.ExceptionHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.GroupedThreadFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.ThreadPoolExecutorObserver;

/**
 * @author hanhlh
 *
 */
public class ThreadedRuleManager extends SimpleRuleManager {

	// instance attributes ////////////////////////////////////////////////////

    /** イベント処理に使用するスレッドプール */
    private ThreadPoolExecutor _executor;

    private ThreadPoolExecutorObserver _observer;

    private Thread _observerThread;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * <p>新しいスレッドプール版の RuleManager を作成します。作成後に
     * initialize メソッドで初期化する必要があります。</p>
     */
    public ThreadedRuleManager() {
        super();
    }

    // interface Service //////////////////////////////////////////////////////

    /**
     * <p>指定されたプロパティによってスレッドプール版 RuleManager を初期化
     * します。利用できる初期化プロパティは以下の通りです。</p>
     *
     * <ul>
     *   <li>max-threads : 作成可能なスレッド数の上限値</li>
     * </ul>
     *
     * @param prop 初期化プロパティ
     * @throws ServiceException
     */

    public void initialize(Configuration conf) throws ServiceException {
    	super.initialize(conf);
        try {
            _executor = new ThreadPoolExecutor(0,
            									Integer.MAX_VALUE,
            									60L,
            									TimeUnit.SECONDS,
            									new SynchronousQueue<Runnable>(),
            									new GroupedThreadFactory("RuleManager"));
            _observer = new ThreadPoolExecutorObserver(_executor);
            _observerThread = new Thread(_observer, "RuleManagerObserver");
            _observerThread.setDaemon(true);
            _observerThread.start();

        } catch (Exception e) {
        	e.printStackTrace();
            throw new ServiceException(e);

        }
    }

    public void disable() {
        // TODO
    	NameNode.LOG.info("ThreadedRuleManager.disable()");
        _executor.shutdown();
    }

    // interface RuleManager //////////////////////////////////////////////////

    public void dispatch(RuleEvent event, ExceptionHandler exHandler) {
    	Rule[] rules = getRules(event);
        if (rules == null) {
            return;
        }

        for(int i=0; i<rules.length; i++){
        	DispatchTask dt = new DispatchTask(rules[i], event, exHandler);
            _executor.execute(dt);
        }
    }

    // inner classes //////////////////////////////////////////////////////////

    /**
     * <p>スレッドプールの実行待ちキューに登録するために、ルールの実行要求を
     * Runnable インターフェースを持つタスクとして表現したクラスです。</p>
     *
     * $Id: ThreadedRuleManager.java,v 1.9.2.3 2005/12/02 06:55:58 yoshihara Exp $
     *
     * @author Hiroshi Kazato <kazato@de.cs.titech.ac.jp>
     * @version $Revision: 1.9.2.3 $
     */
    private class DispatchTask implements Runnable {

        // instance attributes ////////////////////////////////////////////////

        /** 実行するルール */
        private final Rule _rule;

        /** ルールの適用対象となるイベント */
        private final RuleEvent _event;

        private final ExceptionHandler _exHandler;

        // constructors ///////////////////////////////////////////////////////

        /**
         * <p>新しいルールの実行要求 (タスク) を作成します。</p>
         *
         * @param rule 実行するルール
         * @param event ルールの適用対象となるイベント
         */
        public DispatchTask (Rule rule, RuleEvent event,
                ExceptionHandler exHandler) {
            _rule = rule;
            _event = event;
            _exHandler = exHandler;
        }

        // interface runnable /////////////////////////////////////////////////

        public void run() {
            try {
                _rule.dispatch(_event);
            } catch (Exception e) {
                //e.printStackTrace();
            	_exHandler.handleException(e);
            }
        }
    }

}
