/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.ExceptionHandler;




/**
 * @author hanhlh
 *
 */
public interface RuleManager {

	/**
     * <p>指定されたタイプのイベントが発生したときに実行するルールをこの
     * RuleManager に登録します。</p>
     *
     * @param type 適用対象のイベントクラス
     * @param rule ルール
     */
    public void register(Class type, Rule rule);

    /**
     * <p>指定されたタイプのイベントが発生したときに実行するように登録
     * されているルールをこの RuleManager から削除します。</p>
     *
     * @param type 適用対象のイベントクラス
     * @param rule ルール
     */
    public void unregister(Class type, Rule rule);

    /**
     * <p>この RuleManager に登録されたすべてのルールを削除します。</p>
     */
    public void flush();

    /**
     * <p>イベントの発生を通知します。引数 event の実行時のクラスに応じて
     * ルールがトリガーされます。</p>
     *
     * @param event 発生したイベント
     */
    public void dispatch(RuleEvent event);

    /**
     * <p>イベントの発生を通知します。引数 event の実行時のクラスに応じて
     * ルールがトリガーされます。ルールの実行中に例外が発生した場合には、
     * その例外が指定されたハンドラに渡されます。</p>
     *
     * @param event 発生したイベント
     * @param exHandler 例外を処理するハンドラ
     */
    public void dispatch(RuleEvent event, ExceptionHandler exHandler);

}
