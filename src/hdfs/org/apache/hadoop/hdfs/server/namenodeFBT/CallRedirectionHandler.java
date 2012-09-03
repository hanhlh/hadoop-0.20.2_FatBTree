/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.ExceptionalHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class CallRedirectionHandler implements ExceptionHandler {

	private final Call _call;

    public CallRedirectionHandler(Call call) {
        super();
        _call = call;
    }
	/* (�� Javadoc)
	 * @see org.apache.hadoop.hdfs.server.namenodeFBT.rule.ExceptionalHandler#handleException(java.lang.Exception)
	 * e.getURL = /mapping/partID
	 * output /directory.edn(partID-100)
	 * Example: e.getURL = /mapping/114
	 * output /directory.edn14
	 */
	public void handleException(Exception e) {
		// TODO ��ư�������줿�᥽�åɡ�������
		//StringUtility.debugSpace("CallRedirectionHandler");
		if (e instanceof CallRedirectionException) {
            try {
            	/*FBTDirectory directory =
                    (FBTDirectory) NameNodeFBTProcessor.
                    		lookup("/directory");
                directory.incrementRedirectCount();*/
                _call.redirect(((CallRedirectionException) e).getURL());
            } catch (MessageException e2) {
                e2.printStackTrace();
            }
        } else {
        	System.out.println("not CallRedirectionException");
            e.printStackTrace();
        }
	}

}
