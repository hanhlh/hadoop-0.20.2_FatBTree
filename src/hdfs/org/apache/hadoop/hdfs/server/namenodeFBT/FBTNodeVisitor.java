/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.EndLockRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.EndLockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.LockRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.LockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Locker;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.UnlockRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.UnlockResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Messenger;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTInsertMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.InsertRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public abstract class FBTNodeVisitor extends AbstractNodeVisitor{

// instance attributes ////////////////////////////////////////////////////

	/**
	 * 他PEへの要求のための message (call)
	 */
	protected final Call _call;

	/**
	 * ノードへの lock、unlock を行う Locker
	 */
	protected final Locker _locker;

	/**
	 * このトランザクションの Transaction-ID
	 */
	protected String _transactionID;

	public static String hostName = null;
	public static int ip = 0;
	public static String temp_ep = null;
	public static	EndPoint ep = null;
	public static Request request = null;
	public static Class responseClass = null;

    protected ConcurrencyControl _protocol;

	public FBTNodeVisitor(FBTDirectory directory) {

		super(directory);
		_call = new Call((Messenger) NameNodeFBTProcessor.lookup(Messenger.NAME));
		_locker = (Locker) NameNodeFBTProcessor.lookup(Locker.NAME);
	}

	/*
	public void setRequest(TraverseRequest request) {
		super.setRequest(request);
		_transactionID = request.getTransactionID();
        _protocol = request.getProtocol();
	}*/

	public void setRequest(Request request) {
		super.setRequest(request);
		_transactionID = request.getTransactionID();

	}

	public void setRequest(InsertRequest request) {
		super.setRequest(request);
		_transactionID = request.getTransactionID();
	}

	public void setRequest(FBTInsertMarkOptRequest request) {
		super.setRequest(request);
		_transactionID = request.getTransactionID();
	}

    protected FBTDirectory getDirectory() {
        return (FBTDirectory) _directory;
    }

// instance methods ///////////////////////////////////////////////////////

	/**
	 * target に対して request を送信し、応答 (Response) を受ける.
	 *
	 * @param target 要求の処理が行われる Node を指すポインタ
	 * @param request 送信するリクエスト
	 * @param responseClass 返信されるべき応答クラス
	 * @throws MessageException sender のキューが閉じていた場合
	 * @return Response リクエスト送信に対する応答
	 */
	protected Response request(VPointer target, Request request,
	        Class responseClass) throws MessageException {
        try {
        	request.setDirectoryName("/directory."+target.getPointer().getFBTOwner());
        	_call.setRequest(request);
            _call.setResponseClass(responseClass);
            _call.setDestination(_directory.
            		getMapping(target.getPointer().getPartitionID()));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return _call.invoke();
	}

	protected Response request(int partID, Request request,
	        Class responseClass) throws MessageException {
        request.setTransactionID(_transactionID);
	    _call.setRequest(request);
	    _call.setResponseClass(responseClass);

	    EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
	            "/mapping/"+ partID);
	    _call.setDestination(destination);

	    return _call.invoke();
	}

	/**
	 * target の存在する PE に LockRequest を送信する.
	 *
	 * @param target ロックを解除する Node を指すポインタ
	 * @param mode 要求するロックモード
	 */
	protected void lockRequest(VPointer target, int mode) {
		try {
		    LockRequest request = new LockRequest(target, mode);
		    request.setTransactionID(_transactionID);
		    request(target, request, LockResponse.class);
		} catch (MessageException e) {
			e.printStackTrace();
		}
	}

	/**
	 * target をロックする.
	 * target が他 PE の場合はその PE に lock 要求を送信する.
	 *
	 * @param target ロックする Node を指すポインタ
	 * @param mode 要求するロックモード
	 */
	public void lock(VPointer target, int mode) {
		if (_directory.isLocal(target)) {
			_locker.lock(_transactionID, target, mode);
		} else {
		    lockRequest(target, mode);
		}
	}

	/**
	 * target の存在する PE に UnlockRequest を送信する.
	 *
	 * @param target ロックを解除する Node を指すポインタ
	 */
	protected void unlockRequest(VPointer target) {
		try {
		    UnlockRequest request = new UnlockRequest(target);
		    request.setTransactionID(_transactionID);
		    request(target, request, UnlockResponse.class);
		} catch (MessageException e) {
			e.printStackTrace();
		}
	}

	/**
	 * target のロックを解除する.
	 * target が他 PE の場合はその PE に unlock 要求を送信する.
	 *
	 * @param target ロックを解除する Node を指すポインタ
	 */
	public void unlock(VPointer target) {
		if (_directory.isLocal(target)) {
			_locker.unlock(_transactionID, target);
		} else {
		    unlockRequest(target);
		}
	}


	/**
	 * target の存在する PE に EndLockRequest を送信する.
	 *
	 * @param target ロックを解除する Node を指すポインタ
	 */
	protected void endLockRequest(VPointer target) {
		try {
		    EndLockRequest request = new EndLockRequest(target);
		    request.setTransactionID(_transactionID);
		    request(target, request, EndLockResponse.class);
		} catch (MessageException e) {
			e.printStackTrace();
		}
	}

	/**
	 * target のロックを解除し、Locker から TransactionID を除く.
	 * target が他 PE の場合はその PE に endLock 要求を送信する.
	 *
	 * @param target ロックを解除する Node を指すポインタ
	 */
	protected void endLock(VPointer target) {
		//StringUtility.debugSpace("FBTNodeVisitor.endLock "+target.toString());
		if (_directory.isLocal(target)) {
			_locker.unlock(_transactionID, target);
			_locker.removeKey(_transactionID);
		} else {
		    endLockRequest(target);
		}
	}

	protected void callRedirectionException(int partID) {
		throw new CallRedirectionException(
		        "/mapping/" + partID);
	}

	protected void unlockRequestConcurrently(VPointer target) {
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        Responser responser = new Responser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();

                if (_directory.isLocal(vp)) {
                	//System.out.println("FBTNodeVisitor.unLockRequestConcurrently Local");
                    _locker.unlock(_transactionID, vp);
                } else {
                	//System.out.println("FBTNodeVisitor.unLockRequestConcurrently not Local");
        		    UnlockRequest request = new UnlockRequest(vp);
        		    request.setTransactionID(_transactionID);

        	        Call call = new Call(messenger, responser);
        	        call.setRequest(request);
        	        call.setResponseClass(UnlockResponse.class);

        	        EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
        	                "/mapping/"+ vp.getPointer().getPartitionID());
        	        call.setDestination(destination);
        	        call.invokeOneWay();
                }
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();
	}

	protected void endLockRequestConcurrently(VPointer target) {
        Messenger messenger =
            (Messenger) NameNodeFBTProcessor.lookup("/messenger");
        Responser responser = new Responser();

        try {
            for (Iterator iter = target.iterator(); iter.hasNext();) {
                VPointer vp = (VPointer) iter.next();

                if (_directory.isLocal(vp)) {
            		_locker.unlock(_transactionID, vp);
            		_locker.removeKey(_transactionID);
                } else {
        		    EndLockRequest request = new EndLockRequest(vp);
        		    request.setTransactionID(_transactionID);

        	        Call call = new Call(messenger, responser);
        	        call.setRequest(request);
        	        call.setResponseClass(EndLockResponse.class);

        	        EndPoint destination = (EndPoint) NameNodeFBTProcessor.lookup(
        	                "/mapping/"
        	                + vp.getPointer().getPartitionID());
        	        call.setDestination(destination);
        	        call.invokeOneWay();
                }
            }
        } catch (MessageException e) {
            e.printStackTrace();
        }

        responser.isFinished();
	}


// instance methods ///////////////////////////////////////////////////////

    /**
     * Fat-Btree のメタページにアクセスしたときの動作を実装します.
     *
     * @param meta visitor が訪れる Fat-Btree のメタページ
     * @param self meta を指すポインタ
     * @throws MessageException
     * @throws IOException
     * @throws NotReplicatedYetException
     */
    public abstract void visit(MetaNode meta, VPointer self) throws MessageException, NotReplicatedYetException, IOException;

    /**
     * Fat-Btree のインデックスページにアクセスしたときの動作を実装します.
     *
     * @param index visitor が訪れる Fat-Btree のインデックスページ
     * @param self index を指すポインタ
     * @throws MessageException
     * @throws IOException
     * @throws NotReplicatedYetException
     */
    public abstract void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException;

    /**
     * Fat-Btree の葉ページにアクセスしたときの動作を実装します.
     *
     * @param leaf visitor が訪れる Fat-Btree のリーフページ
     * @param self leaf を指すポインタ
     * @throws QuotaExceededException
     * @throws MessageException
     * @throws NotReplicatedYetException
     * @throws IOException
     */
    public abstract void visit(LeafNode leaf, VPointer self) throws QuotaExceededException, MessageException, NotReplicatedYetException, IOException;

    /**
     * Fat-Btree のポインタページにアクセスしたときの動作を実装します.
     *
     * @param pointer visitor が訪れる Fat-Btree のポインタページ
     * @param self pointer を指すポインタ
     * @throws IOException
     * @throws MessageException
     * @throws NotReplicatedYetException
     */
	public abstract void visit(PointerNode pointer, VPointer self) throws NotReplicatedYetException, MessageException, IOException;
}
