/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.AbstractRule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;

/**
 * @author hanhlh
 *
 */
public class ReplyRule extends AbstractRule {
	private Messenger _messenger;

    /**
     * @param manager
     */
    public ReplyRule(RuleManager manager) {
        super(manager);
        _messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
    }

    protected Class[] events() {
        return new Class[] { Response.class };
    }

    protected void action(RuleEvent event) {
    	System.out.println("ReplyRule action");
        Response response = (Response) event;
        _messenger = (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        //System.out.println("response "+response);
        Call call = _messenger.getReplyCall(response);
        try {
            CallResult result = new CallResult(response);
            result.setDestination(call.getSource());
            result.setHandlerID(call.getMessageID());
            _messenger.send(result);
        } catch (MessageException e) {
            e.printStackTrace();
        } finally {
            _messenger.removeReplyCall(call);
        }
    }

}
