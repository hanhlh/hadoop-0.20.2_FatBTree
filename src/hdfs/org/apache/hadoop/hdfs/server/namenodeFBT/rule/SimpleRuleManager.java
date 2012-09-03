/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.ExceptionHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;

/**
 * @author hanhlh
 *
 */
public class SimpleRuleManager implements RuleManager {

	/**
     * {@link #dispatch(RuleEvent)}で使用される、デフォルトの例外ハンドラです。
     */
    private static final ExceptionHandler _nullHandler =
        new ExceptionHandler() {
        public void handleException(Exception e) {
            e.printStackTrace();
        }
    };

    /** トランザクションID用のシーケンス番号. */
    private static AtomicInteger _sequence = new AtomicInteger(0);

    /** トランザクションID用のホスト名. */
    private static String _hostName;

    static {
        try {
            _hostName = InetAddress.getLocalHost().getHostName();
            _hostName = _hostName.split("\\.")[0];
        } catch (UnknownHostException e) {
        	NameNode.LOG.info("SimpleRuleManager.UnknownHostException exit");
        	System.out.println("SimpleRuleManager.UnknownHostException exit");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // instance attributes ////////////////////////////////////////////////////

    /** イベントとルールの対応関係を保持するテーブル */
    private final ConcurrentMap<Class, Rule[]> _eventXruleMap;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * <p>新しい単一スレッド版の RuleManager を作成します。作成後に
     * initialize メソッドで初期化する必要があります。</p>
     */
    public SimpleRuleManager() {
        _eventXruleMap = new ConcurrentHashMap<Class, Rule[]>();
    }

    public void initialize(Configuration conf) throws ServiceException {
        _eventXruleMap.clear();
    }

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager#register(java.lang.Class, org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule)
	 */
	public synchronized void register(Class type, Rule rule) {
		Rule[] rules = _eventXruleMap.get(type);
        if (rules == null) {
            _eventXruleMap.put(type, new Rule[] { rule });
        } else {
            Rule[] temp = new Rule[rules.length + 1];
            System.arraycopy(rules, 0, temp, 0, rules.length);
            temp[rules.length] = rule;
            _eventXruleMap.put(type, temp);
        }
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager#unregister(java.lang.Class, org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule)
	 */
	public synchronized void unregister(Class type, Rule rule) {
		// TODO 自動生成されたメソッド・スタブ
		Rule[] rules = _eventXruleMap.get(type);
        NameNode.LOG.debug("start class= " + type + ", rule= " + rule
                 + ", ruleCount= " + rules.length);
        if (rules != null) {
            List<Rule> list = new ArrayList<Rule>(Arrays.asList(rules));
            if (! list.remove(rule)) {
                return;
            }
            _eventXruleMap.put(type, list.toArray(new Rule[list.size()]));
        }
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager#flush()
	 */
	public void flush() {
		_eventXruleMap.clear();
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager#dispatch(org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent)
	 */
	public void dispatch(RuleEvent event) {
		Rule[] rules;
		synchronized (_eventXruleMap) {
			rules = _eventXruleMap.get(event.getEventClass());
		}
        if (rules == null) {
            return;
        }

        for (int i = 0; i < rules.length; i++) {
            try {
                rules[i].dispatch(event);
            } catch (Exception e) {
            	e.printStackTrace();
                _nullHandler.handleException(e);
            }
        }
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager#dispatch(org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent, org.apache.hadoop.hdfs.server.namenodeFBT.ExceptionHandler)
	 */
	public void dispatch(RuleEvent event, ExceptionHandler exHandler) {
		Rule[] rules = getRules(event);
        if (rules == null) {
            return;
        }

        for (int i = 0; i < rules.length; i++) {
            try {
                rules[i].dispatch(event);
            } catch (Exception e) {
            	e.printStackTrace();
                exHandler.handleException(e);
            }
        }
	}

	/**
     * ネットワーク上のすべての RuleManager においてユニークな Transaction-ID
     * 文字列を生成して返します.
     *
     * @return ユニークな Transaction-ID
     */
    private String generateTransactionID() {
        return _sequence.incrementAndGet() + "@" + _hostName;
    }

	protected Rule[] getRules(RuleEvent event) {
        if (event.getTransactionID() == null) {
            event.setTransactionID(generateTransactionID());
        }
        return _eventXruleMap.get(event.getEventClass());
    }
}
