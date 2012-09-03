/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RequestList;
import org.mortbay.log.Log;

public class MultiRequestCall extends MultiCastCall{
/**
	 *
	 */
	private static final long serialVersionUID = 1L;
private transient RequestList _requests;

    public MultiRequestCall(Messenger messenger) {
        super(messenger);
        _requests = null;
    }

    public void setRequests(RequestList requests) {
        _requests = requests;
    }

    public void invokeOneWay() throws MessageException {
        _handler = new MultiCastCallResultHandler(_requests, _destinations);

        Call call;
        try {
            Iterator<EndPoint> dIter = _destinations.iterator();
            Iterator<Request> rIter = _requests.Iterator();
            while (dIter.hasNext() && rIter.hasNext()) {
                call = (Call) this.clone();
                call.setDestination(dIter.next());
                call.setRequest(rIter.next());
                _messenger.send(call);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        	Log.info("MultiRequestCall exception exit");
        	NameNode.LOG.info("MultiRequestCall exception exit");
        	System.out.println("MultiRequestCall exception exit");
            System.exit(-1);
        }
    }

}
