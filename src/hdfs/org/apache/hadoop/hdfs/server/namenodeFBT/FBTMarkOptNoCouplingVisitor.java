/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;


import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.FBTMarkOptRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;



/**
 * @author hanhlh
 *
 */
public abstract class FBTMarkOptNoCouplingVisitor extends FBTNodeVisitor{

	// instance attributes ////////////////////////////////////////////////////

	protected boolean _isSafe;
    /**
     * �������륭����
     */
    protected String _key;

    protected String _fileName;

    protected PermissionStatus _ps;

    protected String _holder;
    protected String _clientMachine;
    //protected boolean _overwrite;
    //protected boolean _append;
    protected short _replication;
    protected long _blockSize;
    protected boolean _isDirectory;
    protected DatanodeDescriptor _clientNode;
    /**
     * INC-OPT �ѿ� l
     */
    protected int _length;

    /**
     * INC-OPT �ѿ� h
     */
    protected int _height;

    protected int _mark;

    /**
     * ���ߤΥΡ��ɤλҥΡ��ɤؤΥݥ���
     */
    protected VPointer _child;

    protected VPointer _parent;

    protected boolean _restart;

    protected int _modifiedRange;
    protected boolean _isOptimistic;
    protected PointerSet _lockSet;

	public FBTMarkOptNoCouplingVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(FBTMarkOptRequest request) {
        _key = request.getKey();
        _fileName = request.getFileName();
        _ps = request.getPermissionStatus();
        _length = request.getLength();
        _height = request.getHeight();
        _mark = request.getMark();
        _child = request.getTarget();
        _holder = request.getHolder();
        _clientMachine = request.getClientMachine();
        //_overwrite = request.getOverwrite();
        //_append = request.getAppend();
        _replication = request.getReplication();
        _blockSize = request.getBlockSize();
        _isDirectory = request.isDirectory();
        _clientNode = request.getClientNode();
        _parent = null;
        _restart = false;
        _modifiedRange = Integer.MAX_VALUE;
        _isOptimistic = true;
        _isSafe = false;
        _lockSet = new PointerSet();
    }

	 public void run() {
		 //StringUtility.debugSpace("FBTMarkOptNoCoupling.run");
	        try {
	            if (_child == null) {
	                /* �롼�Ȥ������򳫻� */
	                _directory.accept(this);
	            } else {
	                /*
	                 * ¾�� PE ����ž������Ƥ����׵�ξ��� _child != null �Ȥʤ�.
	                 * _child �ǻ��ꤵ�줿�Ρ��ɤ� Visitor ���Ϥ�.
	                 */
	                Node node = _directory.getNode(_child);
	                node.accept(this, _child);
	            }

	            while (_response == null) {
	                ((FBTDirectory) _directory).incrementCorrectingCount();

	                _directory.accept(this);
	            }
	        } catch (ChangePhaseException e) {
	            changePhase(e);
	        }catch (NullPointerException e) {
	        	System.out.println("NullPointerException line 93");
	            System.out.println(_key);
	            System.out.println(_child);
	            System.out.println(_height);
	            System.out.println(_mark);
	            System.out.println(_request);
	            System.out.println(_request.getTransactionID());
	            e.printStackTrace();
	        } catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (NotReplicatedYetException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
	    }

	public void visit(MetaNode meta, VPointer self) throws NotReplicatedYetException, IOException {
		StringUtility.debugSpace("FBTMarkOptNoCouplingVisitor.visit metanode "+
							meta.getNameNodeID());
    	//System.out.println("isOptimistic "+_isOptimistic);
    	//System.out.println("self "+self);

    	lock(self, Lock.IX);

        System.out.println("height "+_height);
        System.out.println("length "+_length);
        System.out.println("modifiedRange "+_modifiedRange);
        if ((_height == _length)){
        		//|| (_height == _modifiedRange)) {

        	VPointer vPointer = meta.getPointerEntry();
            _child = meta.getRootPointer();
            lock(vPointer, Lock.IX);
            _parent = self;
            Node node = _directory.getNode(vPointer);
            try {
				node.accept(this, vPointer);
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
        } else {  //height < length
        	_child = meta.getRootPointer();

            unlock(self);
            /* _child �ǻ��ꤵ�줿�Ρ��ɤ� Visitor ���Ϥ� */
            Node node = _directory.getNode(_child);
            try {
				node.accept(this, _child);
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
        }
    }

	 public void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		 //System.out.println(FBTDirectory.SPACE);
		 //System.out.println("FBTMarkOptNoCouplingVisitor visit 130 index "
		//		 						+index.getNameNodeID());
		 StringUtility.debugSpace("FBTMarkOptNoCoupling.visit index "+
				 							index.getNameNodeID());
		 index.key2String();
		 System.out.println("Lock IX");
		 lock(self, Lock.IX);
	        /*if (_parent != null) {
	            unlock(_parent);
	            _parent = null;
	        }*/


	        if (!index.isInRange(_key) || index.is_deleteBit()) {
	        	//index is out range
	        //if (index.isDeleted()) {
	            unlock(self);
	            System.out.println("unlock IX");
	            _height = 0;
	            _length = 0;
	            _mark = 0;
	        } else {
	            /* ���Ρ��ɤǤθ��������ΰ��� */
	        	int position = locate(index);
	        	//System.out.println("line 146 position "+position);
	            markSafe(index, self);
	            checkDestinationDiskNode(index, position);
	            _height++;
	            if (_height == _length) {
	            	//INC-OPT
	                _child = index.getEntry(position);
	                VPointer vPointer = index.getPointer(position);
	                //System.out.println("FBTMarkOptNoCoupling lock "+vPointer.toString());
	                lock(vPointer, Lock.IX);

	                _parent = self;

	                Node node = _directory.getNode(vPointer);
	                try {
						node.accept(this, vPointer);
					} catch (MessageException e) {
						// TODO ��ư�������줿 catch �֥�å�
						e.printStackTrace();
					}
	            } else if (index.isLeafIndex()) {
	                if (index.size() > 0) {
	                    visitLeafIndexWithEntry(index, self, position);
	                } else {
	                    visitLeafIndexWithoutEntry(self);
	                }
	            } else {
	                goNextNode(index, self, position);
	            }
	        }
	    }

	    public void visit(LeafNode leaf, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
	    	StringUtility.debugSpace("FBTMarkOptNoCouplingVisitor visit leaf "+leaf.getNodeNameID());
	    	System.out.println("lock X");
	        lock(self, Lock.X, _height + 1, 1, 1);
	        if (leaf.get_isDummy()) {
	            VPointer vp = leaf.get_sideLink();
	            endLock(self);
	            System.out.println("unlock X");
	            _request.setTarget(vp);
	            callRedirectionException(vp.getPointer().getPartitionID());
	        } else {
	        	//System.out.println(
	        	//		"line190"+
	        	//		(_key.compareTo(leaf.get_highKey()) >= 0 || leaf.get_deleteBit()));
	        	//System.out.println("HighKey "+leaf.get_highKey());
	            if (_key.compareTo(leaf.get_highKey()) >= 0 || leaf.get_deleteBit()) {
	                ((FBTDirectory) _directory).incrementChaseCount();

	                VPointer vp = leaf.get_sideLink();
	                unlock(self);
	                System.out.println("unlock X");
	                _parent = self;
	                Node node = _directory.getNode(vp);
	                node.accept(this, vp);

	                unlock(self);
	                _height = 0;
	                _mark = 0;
	            } else {
	                /* ���Ρ��ɤǤθ��������ΰ��� */

	            	int position = leaf.binaryLocate(_key);
	            	//System.out.println("position "+position);
	                if (position <= 0) {
	                    operateWhenSameKeyNotExist(
	                    		leaf, self, position, _key, _ps,
	                    		_holder,
	                    	    _clientMachine,
	                    	    //_overwrite,
	                    	    //_append,
	                    	    _replication,
	                    	    _blockSize,
	                    	    _isDirectory,
	                    	    _clientNode);
	                } else {
	                    operateWhenSameKeyExist(leaf, self, position, _key, _ps,
	                    		_holder,
	                	    	_clientMachine,
	                	    	//_overwrite,
	                	    	//_append,
	                	    	_replication,
	                	    	_blockSize,
	                	    	_isDirectory,
	                	    	_clientNode);
	                }
	            }

	        }
	        unlock(self);
	    }

	    public void visit(PointerNode pointer, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
	    	StringUtility.debugSpace("FBTMarkOptNoCouplingVisitor.visitPointerNode "+
	    							pointer.getNameNodeID());
	        PointerSet pointerSet = pointer.getEntry();
	        System.out.println("pointerSet: "+pointerSet.toString());
	         // �ҥڡ����򥰥?�Х���¾��å�
	        for (Iterator iter = pointerSet.iterator(); iter.hasNext();) {
	            VPointer vPointer = (VPointer) iter.next();
	            lock(vPointer, Lock.X);
	            //lock(vPointer, Lock.IX);
	        }
//	        _locker.incrementXCount(_height + 1, pointer.size(),
//	                ((FBLTDirectory) _directory).getTreeHeight() - _height);
	        ((FBTDirectory) _directory).incrementLockCount(pointer.size() - 1);

	        if (_parent != null) {
	            unlock(_parent);
	            _parent = null;
	        }

	        unlock(self);

	        Request request = generateModifyRequest(pointerSet);
	        if (_directory.isLocal(_child)) {
	            NodeVisitor visitor = generateNodeVisitor();
	            System.out.println("FBTMarkOptNoCouplingVisitor visitor "+
	            						visitor.toString());
	            visitor.setRequest(request);
	            //System.out.println("FBTMarkOptNoCouplingVisitor.visitPointerNode " +
	            //		"request "+request.toString());
	            visitor.run();
	            //unlock(self);

	            Response response = visitor.getResponse();
	            //System.out.println("330 response "+response);

	            handle(response, pointerSet);
	        } else {
	            unlock(self);

	             // �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
	             // Ŭ���� PE �����򤷤��׵��ž��

	            try {
	                Response response = callModifyRequest(request);

	                judgeResponse(response, pointerSet);
	            } catch (MessageException e) {
	                e.printStackTrace();
	            }

	        }



	    }

	    protected void goNextNode(IndexNode index, VPointer self, int position) throws MessageException, NotReplicatedYetException, IOException {
	    	//StringUtility.debugSpace("FBTMarkOptNoCoupling.goNextNode");

	        _child = index.getEntry(position);
	        if (_directory.isLocal(_child)) {
	            unlock(self);
	            System.out.println("unlock IX");
	            try {
	                Node node = _directory.getNode(_child);
	                node.accept(this, _child);
	            } catch (NullPointerException e) {
	                e.printStackTrace();
	            }
	        } else {
	            /*
	             * �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
	             * Ŭ���� PE �����򤷤��׵��ž��
	             */
	            endLock(self);
	            System.out.println("unlock IX");
	            callRedirect();
	        }
	    }

	    protected void restart() throws NotReplicatedYetException, MessageException, IOException {
	        _length = _mark;
	        _height = 0;
	        _mark = 0;
	        _directory.accept(this);
	    }

	    public void lock(VPointer target, int mode, int h, int c, int r) {
	    	//System.out.println("FBTMarkOptNoCouplingVisitor.lock line 291");
	        lock(target, mode);
	        //_locker.incrementXCount(h, c, r);
	    }

	    protected abstract void noEntry(VPointer self) throws NotReplicatedYetException, MessageException, IOException;

	    protected void callRedirect() {
	    	StringUtility.debugSpace("FBTMarkOptNoCoulingVisitor.calLRedirect "+_child);
			FBTMarkOptRequest request = (FBTMarkOptRequest) _request;
			request.setTarget(_child);
			request.setLength(_length);
			request.setHeight(_height);
			request.setMark(_mark);
			callRedirectionException(_child.getPointer().getPartitionID());
	    }

	   // protected abstract void callRedirect();

	    protected abstract void operateWhenSameKeyExist(
	            LeafNode leaf, VPointer self, int position, String key,
	            PermissionStatus ps,
	            String holder,
	            String clientMachine,
	            //boolean overwrite,
	            //boolean append,
	            short replication,
	            long _blockSize,
	            boolean isDirectory,
	            DatanodeDescriptor clientNode) throws QuotaExceededException, MessageException;

	    protected abstract void operateWhenSameKeyNotExist(
	            LeafNode leaf, VPointer self, int position, String key,
	            PermissionStatus ps,
	            String holder,
	            String clientMachine,
	            //boolean overwrite,
	            //boolean append,
	            short replication,
	            long _blockSize,
	            boolean isDirectory,
	            DatanodeDescriptor clientNode) throws QuotaExceededException, MessageException, NotReplicatedYetException, IOException;

	    protected abstract Response callModifyRequest(Request request)
	                                                throws MessageException;

	    protected abstract void judgeResponse(Response response, VPointer child);

	    protected abstract Request generateModifyRequest(
	            PointerSet pointerSet);

	    //protected abstract TraverseRequest generateModifyRequest(
	    //        PointerSet pointerSet);

	    protected abstract NodeVisitor generateNodeVisitor();

	    protected abstract void handle(Response response, VPointer child) throws NotReplicatedYetException, MessageException, IOException;

	    protected abstract void markSafe(IndexNode index, VPointer self);

	    protected abstract void changePhase(ChangePhaseException e);

	    protected abstract int locate(IndexNode index);

	    protected abstract void visitLeafIndexWithEntry(
	            IndexNode index, VPointer self, int position) throws MessageException, NotReplicatedYetException, IOException;

	    protected abstract void visitLeafIndexWithoutEntry(VPointer self) throws NotReplicatedYetException, MessageException, IOException;

	    protected abstract void checkDestinationDiskNode(
	            IndexNode index, int position);
	    /**
	     * ��ϩ��꤬���뤫�ɤ���Ĵ�١����ä������н褹��
	     * @param index
	     * @param self
	     * @return ��ϩ��꤬���ꡢ�����н��Ԥä���true��
	     */
	    protected abstract boolean correctPath(IndexNode index, VPointer self);

}
