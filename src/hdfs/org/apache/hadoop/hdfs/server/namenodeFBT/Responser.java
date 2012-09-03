/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.ArrayList;
import java.util.List;


/**
 * @author hanhlh
 *
 */
public class Responser {

	// instance attributes ////////////////////////////////////////////////////

    private final List _waiters;

    /**
     *
     */
    public Responser() {
        super();
    	_waiters = new ArrayList();
    }

    public synchronized void addMessageID(String messageID) {
        _waiters.add(messageID);
    }

    public synchronized void isFinished() {
        try {
            while (!_waiters.isEmpty()) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void handleResult(CallResult result) {
	    _waiters.remove(result.getHandlerID());

	    if (_waiters.isEmpty()) {
	        notifyAll();
	    }
    }

}
