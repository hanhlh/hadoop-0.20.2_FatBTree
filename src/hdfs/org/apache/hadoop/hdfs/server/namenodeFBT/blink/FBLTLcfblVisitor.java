/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.blink;


import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.IndexNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.LeafNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.MetaNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.Node;
import org.apache.hadoop.hdfs.server.namenodeFBT.Pointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerNode;
import org.apache.hadoop.hdfs.server.namenodeFBT.PointerSet;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public abstract class FBLTLcfblVisitor extends FBLTNodeVisitor{

	protected boolean _isSafe;

    protected int _length;

    protected int _height;

    protected int _mark;

    protected int _treeHeight;

    protected int _modifiedRange;

    protected boolean _isFinished;

    protected PointerSet _lockSet;

    protected boolean _isOptimistic;

    protected int _count;

    protected int _count2;

	public FBLTLcfblVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void setRequest(Request request) {
        super.setRequest(request);
        _isSafe = false;

      /*  MarkOptProtocol protocol = (MarkOptProtocol) request.getProtocol();
        if (protocol != null) {
            _length = protocol.getLength();
            _height = protocol.getHeight();
            _mark = protocol.getMark();
        } else {
            _length = Integer.MAX_VALUE;
            _height = 0;
            _mark = 0;
        }
        */
        _treeHeight = Integer.MAX_VALUE;
        //_modifiedRange = Integer.MAX_VALUE;
        _height=1;
        _length=1;
        _mark=1;
        _modifiedRange = 10;
        _isFinished = false;
        _isOptimistic = true;
        System.out.println("FBLTLcfblVisitor.setRequest isOptimistic "+_isOptimistic);
    }
	public void run() {
		StringUtility.debugSpace("FBLTLcfblVisitor.run()");
        super.run();

        boolean isRestart = false;
        System.out.println("FBLTLcfblVisitor.run() isFinished "+_isFinished);
        while (!_isFinished) {
            if (_count > 1000 || _count2 >1000) {
                System.out.println(_request);
                System.out.println(_transactionID);
                System.out.println(_length);
                System.out.println(_height);
                System.out.println(_mark);
                System.out.println(_modifiedRange);
                System.out.println(_treeHeight);
                VPointer vp = null;
                vp.getPointer();
            }
            if (isRestart) {
                getDirectory().incrementMoreRestartCount();
            } else {
                getDirectory().incrementRestartCount();
                isRestart = true;
            }

            _isSafe = false;
            _height = 1;
            _length = 1;
            _mark = 1;
            _lockSet = new PointerSet();
            System.out.println("FBLTLcfblVisitor.run() lockSet "+_lockSet.size());
            _isOptimistic = true;
            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (MessageException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (IOException e) {
				// TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}

            unlockRequestConcurrently(_lockSet);
        }
    }

    public void visit(MetaNode meta, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBLTLcfblVisitor.visitMetaNode");
    	System.out.println("FBLTLcfblVisitor.vist metanode "+meta.getNameNodeID());
    	System.out.println("isOptimistic "+_isOptimistic);
    	System.out.println("self "+self);
        lock(self, Lock.IX);

        System.out.println("height "+_height);
        System.out.println("length "+_length);
        System.out.println("modifiedRange "+_modifiedRange);
        if ((_height == _length) || (_height == _modifiedRange)) {
        	System.out.println("FBLTLcfblVisitor line 113");
            VPointer vp = meta.getPointerEntry();

            lock(vp, Lock.IX);
            unlock(self);

            Node node = _directory.getNode(vp);
            System.out.println("go to "+node.getNodeID());
            node.accept(this, vp);
        } else {
        	System.out.println("FBLTLcfblVisitor line 122");
            VPointer vp = meta.getRootPointer();

            unlock(self);

            Node node = _directory.getNode(vp);
            node.accept(this, vp);
        }
    }

    public void visit(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBLTLcfblVisitor.visit index");
    	System.out.println("FBLTLcfgblVisitor.visit Index node "+index.getNameNodeID());
    	System.out.println("isOptimistic "+_isOptimistic);

        if (_isOptimistic) {
            lock(self, Lock.IX);
        }

        if (index.is_deleteBit() || index.isDummy()) {
        	System.out.println("line 147");
            if (_isOptimistic) {
                unlock(self);
            }
            getDirectory().incrementCorrectingCount();
            return;
        }

        if (index.isRootIndex()) {
        	System.out.println("line 156");
            //if (_modifiedRange != Integer.MAX_VALUE) {
        	if (_modifiedRange != 10) {
                _modifiedRange += getDirectory().getTreeHeight() - _treeHeight;
            }
            _treeHeight = getDirectory().getTreeHeight();
        }

        if (_isOptimistic) {
        	System.out.println("line 165");
            if (!correctPath(index, self)) {  	//wrongPath
                _height++;
                if (_height <= _modifiedRange) {
                	System.out.println("line 169");
                    int position = locate(index);
                    memorizeNextKey(index, position);
                    markSafe(index, position);
                    if ((_height == _length) || (_height == _modifiedRange)) {
                    	System.out.println("line 174");
                        VPointer vp = index.getPointer(position);

                        lock(vp, Lock.IX);
                        //add
                        unlock(self);
                        //end
                        Node node = _directory.getNode(vp);
                        node.accept(this, self);

                    } else {
                        goChildPage(index, self, position);
                    }
                } else {
                    unlock(self);
                    _isSafe = true;
                }

                if (!_isFinished && _isSafe) {
                    modifyPage(index, self);
                }
            }
        } else {									//correctPath
        	System.out.println("line 194");
            _height++;
            setSafe(index, self);

            if (_height <= _modifiedRange) {
            	System.out.println("line 199");
                int position = locate(index);
                //memorizeNextKey(index, position);

                VPointer vp = index.getPointer(position);
                lock(vp, Lock.X + 520);
              //TODO unlock
                System.out.println("visit index node self "+self.toString());

                unlock(self.getPointer());

                Node node = _directory.getNode(vp);
                node.accept(this, vp);

                if (!_isFinished && _isSafe && !_isOptimistic) {
                    modifyNode(index, self, position);
                }

            } else {
            	System.out.println("line 213");
                if (_isSafe) {
                    modifyNode(index, self);
                } else {
                    _length = _mark;
                }
            }
        }
    }

    public void visit(PointerNode pointer, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBLTLcfblVisitor visit pointerNode");
    	System.out.println("FBLTLcfblVisitr.visit pointer node "+pointer.getNameNodeID());
    	System.out.println("isOptimistic "+_isOptimistic);
        PointerSet pointerSet = pointer.getEntry();

        System.out.println("pointerSet "+pointerSet.toString());
        if (!_directory.isLocal(pointerSet)){
        	System.out.println("FBLTLcfblVIsitor line 208");
            if (_isOptimistic) {
                unlock(self);
            }
            unlock(pointer.getPointer());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _isSafe = false;
        } else if (pointer.size() > 1) {// �������٤��ҥΡ��ɤ˥��ԡ��ڡ���������
        	System.out.println("FBLTLcfblVIsitor line 220");

            Iterator<Pointer> iter = pointerSet.iterator();
            while (iter.hasNext()) {
                VPointer vPointer = iter.next();
                lock(vPointer, Lock.IX + 530);
            }
            System.out.println("FBLTLcfblVisitor line 228");
            if (_lockSet == null) {
            	_lockSet = new PointerSet();
            	_lockSet.addPointerSet(pointerSet);
            } else {
            	_lockSet.addPointerSet(pointerSet);
            }
            System.out.println("unlock pointer "+pointer.getPointer().toString());
            unlock(pointer.getPointer());
            if (_isOptimistic) {
                unlock(self);
                System.out.println("line 242 "+ _modifiedRange);
                System.out.println("line 242 "+ _height);
                System.out.println("line 242 "+ (_modifiedRange - _height + 1));
                _locker.incrementXCount(_height + 1, pointer.size(),
                        //_modifiedRange - _height + 1);
                		_modifiedRange - _height);
                		//0);
            } else {
                _locker.incrementXCount(_height + 1, pointer.size(), 0);
            }
            getDirectory().incrementLockCount(pointer.size() - 1);

            _isOptimistic = false;
            unlockRequestConcurrently(_lockSet);
            _lockSet = new PointerSet();
            Node node = _directory.getNode(pointerSet);
            System.out.println("go to  "+node.getNodeID());
            node.accept(this, pointerSet);


        } else {// �������٤��ҥΡ��ɤ˥��ԡ��ڡ������ʤ��Τǡ��ץ�ȥ�������
            log(29);
            if (_isOptimistic) {
                unlock(self);
            } else {
                unlockRequestConcurrently(_lockSet);
                _lockSet = new PointerSet();
            }

            _isSafe = false;
            _isOptimistic = true;

            VPointer vp = pointer.getEntry().getPointer();
            unlock(pointer.getPointer());

            if (_directory.isLocal(vp)) {
                try {
                    Node node = _directory.getNode(vp);
                    node.accept(this, vp);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(vp);
                    System.out.println(pointer);
                    System.out.println(self);
                    System.out.println(_height);
                    System.out.println(_length);
                    System.out.println(_mark);
                    System.out.println(_modifiedRange);
                    System.out.println(_treeHeight);
                    VPointer v = null;
                    v.getPointer();
                }
            }
        }
    }

    protected void free(IndexNode index) {
        index.set_deleteBit(true);
        /*GarbageCollector gc =
            (GarbageCollector) AutoDisk.lookup("/service/garbagecollector");
        gc.addGarbage(index.getPointer());*/

    }

    protected void free(LeafNode leaf) {
        leaf.set_deleteBit(true);
        /*GarbageCollector gc =
            (GarbageCollector) AutoDisk.lookup("/service/garbagecollector");
        gc.addGarbage(leaf.getPointer());*/
    }

    protected void log(int p) {
//        super.log(p, _length, _height, _mark, _modifiedRange, _treeHeight, _isFinished, _isSafe);
    }

    protected abstract int locate(IndexNode index);

    protected abstract void markSafe(IndexNode index, int position);

    protected abstract void setSafe(IndexNode index, VPointer self);

    protected abstract void goChildPage(
            IndexNode index, VPointer self, int position);

    /**
     * ��ϩ��꤬���뤫�ɤ���Ĵ�١����ä������н褹��
     * @param index
     * @param self
     * @return ��ϩ��꤬���ꡢ�����н��Ԥä���true��
     * @throws IOException
     * @throws MessageException
     * @throws NotReplicatedYetException
     */
    protected abstract boolean correctPath(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException;

    /**
     * ���ԡ��ڡ����Τʤ��Ρ��ɤι���
     * @param index
     * @param self
     */
    protected abstract void modifyPage(IndexNode index, VPointer self);

    /**
     * ���ԡ��ڡ����Τ���ǲ��إΡ��ɤι���
     * @param index
     * @param self
     */
    protected abstract void modifyNode(IndexNode index, VPointer self);

    /**
     * ���ԡ��ڡ����Τ�������إΡ��ɤι���
     * @param index
     * @param self
     * @param pos
     */
    protected abstract void modifyNode(IndexNode index, VPointer self, int pos);

    protected abstract void memorizeNextKey(IndexNode index, int position);

}
