package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.ResponseQueue;

public class MultiCastCallResultHandler implements MessageHandler {

private final ResponseQueue _responses;

    private final CountDownLatch _latch;

    public MultiCastCallResultHandler(Request request,
            List<EndPoint> _destinations) {
        _responses = new ResponseQueue(request);
        _latch = new CountDownLatch(_destinations.size());
    }

	public void handle(Message message) {
		System.out.println("MultiCastCallResult.handler");
		CallResult result = (CallResult) message;
        _responses.addResponse(result.getResponse());

        _latch.countDown();
	}
	public Response getResponse() {
        try {
            _latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return _responses;
    }
}
