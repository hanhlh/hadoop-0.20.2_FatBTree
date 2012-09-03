/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.lock;


import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBTProcessor;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.AbstractRule;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleManager;

/**
 * @author hanhlh
 *
 */
public class EndLockRule extends AbstractRule{

// instance attributes ////////////////////////////////////////////////////

    /**
     * 要求された unlock の処理をし、Transaction-ID が除去される Locker
     */
	private final Locker _locker;

	// Constructors ///////////////////////////////////////////////////////////

    /**
     * Node に対するロック解除と
     * Transaction-ID の除去の処理を行う LockRule を生成します.
     *
     * @param manager このルールを登録する RuleManager
     */
    public EndLockRule(RuleManager manager) {
        super(manager);
        _locker = (Locker) NameNodeFBTProcessor.lookup(Locker.NAME);
    }

    // interface AbstractRule /////////////////////////////////////////////////

    protected Class[] events() {
        return new Class[] { EndLockRequest.class };
    }

    protected void action(RuleEvent event) {
        EndLockRequest request = (EndLockRequest) event;

		String transactionID = request.getTransactionID();
		VPointer target = request.getTarget();

		_locker.unlock(transactionID, target);
//		_locker.removeKey(transactionID);

		_manager.dispatch(new EndLockResponse(request));

    }

}
