/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.EventListener;

import org.apache.hadoop.conf.Configuration;


/**
 * @author hanhlh
 *
 */
public interface Rule extends EventListener {

	public void initialize(Configuration conf);

    /**
     * <p>イベントの通知を受けるためのメソッドです。登録しているイベントが
     * 発生したときに RuleManager から呼び出されます。</p>
     *
     * @pattern-name Observer パターン
     * @pattern-role Notify オペレーション
     *
     * @param event 通知されるイベント
     */
    public void dispatch(RuleEvent event);

    /**
     * <p>このルールを有効にします。</p>
     */
    public void enable();

    /**
     * <p>このルールを無効にします。</p>
     */
    public void disable();

}
