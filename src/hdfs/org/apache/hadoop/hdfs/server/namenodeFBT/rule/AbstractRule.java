/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.LinkedList;

import org.apache.hadoop.conf.Configuration;

/**
 * @author hanhlh
 *
 */
public abstract class AbstractRule implements Rule {


	// instance attributes ////////////////////////////////////////////////////

    /** このルールを登録する RuleManager */
    protected final RuleManager _manager;

    protected static LinkedList<String> stack = new LinkedList<String>();
	protected int pop_lock;


	public void push(String obj){

		stack.addFirst(obj);
		}

		//get top
		public String peek(){

		//isnull?
			if(!isEmpty()){

			return stack.getFirst();

			}else{
			return null;
			}

		}

		//pop
		public String pop(){

		//is null
			if(!isEmpty()){

			return stack.removeFirst();

			}else{
			return null;
			}

		}

		//is null
		public boolean isEmpty(){

			return stack.isEmpty();
		}

		// constructors ///////////////////////////////////////////////////////////

	    /**
	     * <p>このクラスが提供するアダプタを使用して新しい Rule を生成します。</p>
	     *
	     * @param manager このルールを登録する RuleManager
	     */
	    public AbstractRule(RuleManager manager) {
	    	//super((Messenger) NameNodeFBTProcessor.lookup("/service/messenger"));
	        _manager = manager;
	        enable();
	    }


	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule#initialize(org.apache.hadoop.conf.Configuration)
	 */
	public void initialize(Configuration conf) {
		// TODO 自動生成されたメソッド・スタブ

	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule#dispatch(org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent)
	 */
	public void dispatch(RuleEvent event) {
		if (condition(event)) {
            action(event);
        }
	}

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule#enable()
	 */
	public void enable() {
		Class[] events = events();
        for (int i = events.length - 1; i >= 0; i--) {
            _manager.register(events[i], this);
        }
	}

	// instance methods ///////////////////////////////////////////////////////

    /**
     * <p>このルールが対象としているイベントタイプを取得します。</p>
     *
     * @return 通知を受ける RuleEvent クラスの配列
     */
    protected abstract Class[] events();

	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.Rule#disable()
	 */
	public void disable() {
		Class[] events = events();
        for (int i = events.length - 1; i >= 0; i--) {
            _manager.unregister(events[i], this);
        }
	}
	/**
     * <p>このメソッドには ECA ルールの発火条件を定義します。条件判断において
     * 副作用を伴う操作を行うべきではありません。このメソッドをオーバーライド
     * する場合は、メソッドの中で AutoDisk の内部状態を変化させないように注意
     * してください。</p>
     *
     * @pattern-name Template Method パターン
     * @pattern-role Primitive Operation
     *
     * @param event 通知されたイベント
     * @return アクションを発火させる場合のみ true
     */
    protected boolean condition(RuleEvent event) {
        /* デフォルトではイベントに対して常に発火する */
        return true;
    }

    /**
     * <p>このメソッドには ECA ルールにおけるアクションを定義します。</p>
     *
     * @pattern-name Template Method パターン
     * @pattern-role Primitive Operation
     *
     * @param event 通知されたイベント
     */
    protected abstract void action(RuleEvent event);

}
