/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;

/**
 * @author hanhlh
 *
 */
public class CallHandler implements MessageHandler {

	// instance attributes ////////////////////////////////////////////////////

    private final Messenger _messenger;

    private final RuleManager _manager;

    // constructors ///////////////////////////////////////////////////////////

    /**
     * �������� Message-Call �򰷤�����ݡ��ͥ�Ȥ��������ޤ�.
     *
     * @param messenger ���Υ�å�������������Ԥ�����ݡ��ͥ��
     * @param manager ������ä����׵��ȯ���������٥�Ȥ����륳��ݡ��ͥ��
     */
    public CallHandler(Messenger messenger, RuleManager manager) {
        _messenger = messenger;
        _manager = manager;
   	}

	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler#handle(org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message)
	 */
	public void handle(Message message) {
		//System.out.println("CallHandler.handler");
		Call call = (Call) message;
        call.setMessenger(_messenger);
        //System.out.println("call "+call.getMessageID());
        //call.getRequest().setFrom_To(call.getMessageID());
        //call.getRequest().setDestnation(call.getSource());
        //call.getRequest().getTransactionID();
        _messenger.addReplyCall(call);
        _manager.dispatch(call.getRequest(), new CallRedirectionHandler(call));

	}

}
