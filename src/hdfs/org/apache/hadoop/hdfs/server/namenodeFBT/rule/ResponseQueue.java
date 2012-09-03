/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.Response;


/**
 * @author hanhlh
 *
 */
public class ResponseQueue extends Response {

/**
	 *
	 */
	private static final long serialVersionUID = 1L	;
	private Queue<Response> _responses;

    public ResponseQueue(Request request) {
        super(request);
        _responses = new ConcurrentLinkedQueue<Response>();
    }

    public Queue<Response> getResponses() {
        return _responses;
    }

    public void addResponse(Response response) {
        _responses.add(response);
    }
}
