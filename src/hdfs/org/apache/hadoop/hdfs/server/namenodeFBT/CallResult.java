/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


import org.apache.hadoop.hdfs.server.namenodeFBT.msg.AbstractMessage;

/**
 * @author hanhlh
 *
 */
public final class CallResult extends AbstractMessage {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final Response _response;

	public CallResult(Response response) {
        super();
        _response = response;
    }

    // accessors //////////////////////////////////////////////////////////////

    public Response getResponse() {
        return _response;
    }

	public String toString() {
		return "CallResult [_response=" + _response + "]";
	}
}
