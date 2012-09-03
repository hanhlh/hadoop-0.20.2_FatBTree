/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public final class FBTDeleteIncOptRequest extends DeleteRequest
									implements FBTIncOptRequest {

	// instance attributes ////////////////////////////////////////////////////

    /**
     * INC-OPT 変数 l
     */
    private int _length;

    /**
     * INC-OPT 変数 h
     */
    private int _height;


    public FBTDeleteIncOptRequest(
            String directoryName, String key, VPointer target) {
        super(directoryName, key, target);
        _length = Integer.MAX_VALUE;
        _height = 0;
    }

    /**
     * @param key
     */
    public FBTDeleteIncOptRequest(String key) {
        this(FBTDirectory.DEFAULT_NAME, key, null);
    }
// accessors //////////////////////////////////////////////////////////////

    public int getLength() {
        return _length;
    }

    public int getHeight() {
        return _height;
    }

    public void setLength(int length) {
        _length = length;
    }

    public void setHeight(int height) {
        _height = height;
    }

}
