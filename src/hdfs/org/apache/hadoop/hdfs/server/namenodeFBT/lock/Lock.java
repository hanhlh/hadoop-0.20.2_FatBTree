/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;

/**
 * @author hanhlh
 *
 */
public class Lock {
// class attribute ////////////////////////////////////////////////////////

    /* ロックモードの定義 */
    public static final int	NONE = 0;
    public static final int	IS   = 1;
    public static final int	IX   = 2;
    public static final int	S    = 3;
    public static final int	SIX  = 4;
    public static final int	X    = 5;
    public static final int	CONF = -1; // lock confliction

    /** ロックマトリクス */
    public static final int[][] matrix = new int[][] {
        /*           NONE  IS    IX    S     SIX   X   */
        /* NONE */ { NONE, IS,   IX,   S,    SIX,  X    },
        /* IS   */ { IS,   IS,   IX,   S,    SIX,  CONF },
        /* IX   */ { IX,   IX,   IX,   CONF, CONF, CONF },
        /* S    */ { S,    S,    CONF, S,    CONF, CONF },
        /* SIX  */ { SIX,  SIX,  CONF, CONF, CONF, CONF },
        /* X    */ { X,    CONF, CONF, CONF, CONF, CONF }
    };

    /** ロックモードの文字列表現 */
    public static final String[] modeString = new String[] {
        "NONE", "IS", "IX", "S", "SIX", "X"
    };

    // instance attribute /////////////////////////////////////////////////////

    /** ロックを保持しているキー */
    protected LockKey _key;

    /** ロック対象となるオブジェクト */
    protected LockObject _obj;

    /** ロックモード */
    protected int _mode;

    protected String _log;

    // constructors ///////////////////////////////////////////////////////////

    public Lock(LockKey key, LockObject obj, int mode) {
        _key = key;
        _obj = obj;
        _mode = mode;
    }

    public String toString() {
        return _key.toString() + "(" + _mode + ")[" + _log + "]";
    }

    public void setLog(String log) {
        _log = log;
    }

}
