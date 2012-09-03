/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;


import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;

/**
 * @author hanhlh
 *
 */
public class DumpRequest extends Request{


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final String _directoryName;

    public DumpRequest(String directoryName) {
	super();
        _directoryName = directoryName;
    }

    public DumpRequest() {
        this(FBTDirectory.DEFAULT_NAME);
    }

    public String getDirectoryName() {
        return _directoryName;
    }

}
