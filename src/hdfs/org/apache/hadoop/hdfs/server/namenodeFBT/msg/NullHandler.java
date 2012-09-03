/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

/**
 * @author hanhlh
 *
 */
public class NullHandler implements MessageHandler {

	// class attributes ///////////////////////////////////////////////////////

    /** NullHandler オブジェクトの唯一のインスタンス */
    private static NullHandler _instance;

 // class methods //////////////////////////////////////////////////////////

    /**
     * <p>NullHandler オブジェクトの唯一のインスタンスを取得します。</p>
     *
     * @pattern-name Singleton
     * @pattern-role Instance オペレーション
     *
     */
    public static synchronized NullHandler getInstance() {
        if (_instance == null) {
            _instance = new NullHandler();
        }
        return _instance;
    }

    // constructors ///////////////////////////////////////////////////////////

    /**
     * <p>プライベートコンストラクタです。このクラスのインスタンスを直接
     * 生成することを禁止しています。</p>
     */
    private NullHandler() {
        // NOP
    }


	/* (非 Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler#handle(org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message)
	 */
	public void handle(Message message) {
		// NOP
		System.out.println("NullHandler.handle");
	}

}
