/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class RangeSearchRequest extends Request{
	protected  String _directoryName;

    protected final String _minKey;

    protected final String _maxKey;

    protected VPointer _target;

    public RangeSearchRequest(
            String directoryName, String minKey, String maxKey) {
        super();
        _directoryName = directoryName;
        _minKey = minKey;
        _maxKey = maxKey;
    }

    public RangeSearchRequest(String minKey, String maxKey) {
        this(FBTDirectory.DEFAULT_NAME, minKey, maxKey);
    }

    public String getDirectoryName() {
        return _directoryName;
    }

    public String getMinKey() {
        return _minKey;
    }

    public String getMaxKey() {
        return _maxKey;
    }

    public VPointer getTarget() {
        return _target;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

    public void setDirectoryName(String directoryName) {
    	_directoryName = directoryName;
    }
    public Class getResponseClass() {
        return RangeSearchResponse.class;
    }

}
