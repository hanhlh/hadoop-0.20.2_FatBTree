/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.mortbay.log.Log;

/**
 * @author hanhlh
 *
 */
public class MultiCastCall extends Call{
/**
	 *
	 */
	private static final long serialVersionUID = 1L;

protected transient List<EndPoint> _destinations;

    protected transient MultiCastCallResultHandler _handler;

    public MultiCastCall(Messenger messenger) {
        super(messenger, null);
        _destinations = null;
        _handler = null;
    }

    public void setDestinations(List<EndPoint> destinations) {
        _destinations = destinations;
    }

    public Response getResponse() {
        return _handler.getResponse();
    }

    public void invokeOneWay() throws MessageException {
        _handler = new MultiCastCallResultHandler(_request, _destinations);

        Call call;
        try {
            Iterator<EndPoint> dIter = _destinations.iterator();
            while (dIter.hasNext()) {
                call = (Call) this.clone();
                call.setDestination(dIter.next());
                _messenger.send(call);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            Log.info("MultiCastCall exception exit");
            NameNode.LOG.info("MultiCastCall exception exit");
        	System.out.println("MultiCastCall exception exit");
            System.exit(-1);
        }
    }

    public void sendPrepare() {
        if (! _redirected) {
            _messenger.addHandler(getMessageID(), _handler);
        }
    }

}
