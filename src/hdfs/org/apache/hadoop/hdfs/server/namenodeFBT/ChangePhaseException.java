/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class ChangePhaseException extends RuntimeException {


/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final Request _request;

    public ChangePhaseException(Request request) {
        super();
        StringUtility.debugSpace("ChangePhaseException");
        _request = request;
    }

    public Request getRequest() {
        return _request;
    }
}
