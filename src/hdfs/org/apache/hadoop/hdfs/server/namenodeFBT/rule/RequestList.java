/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.hdfs.server.namenodeFBT.Request;

/**
 * @author hanhlh
 *
 */
public class RequestList extends Request {
/**
	 *
	 */
	private static final long serialVersionUID = 1L;
private List<Request> _requests;

    public RequestList() {
        _requests = new LinkedList<Request>();
    }

    public void addRequest(Request request) {
        _requests.add(request);
    }

    public Iterator<Request> Iterator() {
        return _requests.iterator();
    }
}
