/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RangeSearchRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RangeSearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTRangeSearchBayerVisitor extends FBTNodeVisitor{

	private String _minKey;

	private String _maxKey;

	private List<INode> _inodes;

	public FBTRangeSearchBayerVisitor(FBTDirectory directory) {
		super(directory);
		_inodes = new ArrayList<INode>();
	}

	public void setRequest(Request request) {
		setRequest((RangeSearchRequest) request);
	}

	public void setRequest(RangeSearchRequest request) {
        super.setRequest(request);
        _minKey = request.getMinKey();
        _maxKey = request.getMaxKey();
    }
	public void run() {
		VPointer vp = _request.getTarget();

        if (vp == null) {
            /* �롼�Ȥ������򳫻� */
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
        } else {
            /*
             * ¾�� PE ����ž������Ƥ����׵�ξ��� vp != null �Ȥʤ�
             * vp �ǻ��ꤵ�줿�Ρ��ɤ� Visitor ���Ϥ�
             */
            Node node = _directory.getNode(vp);
            try {
				node.accept(this, vp);
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
        }
	}

	@Override
	public void visit(MetaNode meta, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
		//
		VPointer root = meta.getRootPointer();

        lock(root, Lock.IS);
        Node node = _directory.getNode(root);
        node.accept(this, root);

	}

	@Override
	public void visit(IndexNode index, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
		StringUtility.debugSpace("FBTRangeSearch visit index,"+index.getNodeID());
		//index.key2String();
		if (index.isLeafIndex()) {
            visitLeafIndex(index, self);
        } else {
            visitIndex(index, self);
        }
	}

	@Override
	public void visit(LeafNode leaf, VPointer self)
			throws QuotaExceededException, MessageException,
			NotReplicatedYetException, IOException {
		StringUtility.debugSpace("FBTRangeSearch visit leaf,"+leaf.getNodeID());
		for (String key : leaf.get_keys()) {
			System.out.println(key);
		}
		if (leaf._isDummy) {
            VPointer vp = leaf.get_sideLink();

            if (vp == null) {
            	System.out.println("sideLink null");
                endLock(self);
                _response =
                    new RangeSearchResponse((RangeSearchRequest) _request);
            } else {
            	System.out.println("sideLink,"+vp);
                lockRequest(vp, Lock.S);
                endLock(self);

                ((RangeSearchRequest) _request).setTarget(vp);
                ((RangeSearchRequest) _request).setDirectoryName("/directory.edn"+(vp.getPointer().getPartitionID()-100));
                System.out.println("visitLeaf,directoryName,"+_request.getDirectoryName());
                callRedirectionException(vp.getPointer().getPartitionID());
            }
        } else {
            /* ���Ρ��ɤǤθ��������ΰ��� */
            int min = leaf.binaryLocate(_minKey);

            if (min <= 0) {
                min = -min;
            } else {
                min = min - 1;
            }
            System.out.println("_minKey,"+_minKey);
            System.out.println("_maxKey,"+_maxKey);
            System.out.println("leafHighKey,"+leaf.get_highKey());
            System.out.println(_maxKey.compareTo(leaf.get_highKey()));
            if (_maxKey.compareTo(leaf.get_highKey()) >= 0) {
                for (int i = min; i < leaf.size(); i++) {
                    //System.out.println(leaf.getKey(i));
                    _inodes.add(leaf.getINode(i));
                }

                VPointer vp = leaf.get_sideLink();
                //System.out.println("sideLink,"+vp);
                lock(vp, Lock.S);
                unlock(self);
                Node node = _directory.getNode(vp);
                node.accept(this, vp);
            } else {
                int max = leaf.binaryLocate(_maxKey);
                if (max <= 0) {
                    max = -max;
                } else {
                    max = max + 1;
                }

                for (int i = min; i < max-1; i++) {
                    //System.out.println(leaf.getKey(i));
                    //System.out.println(((INodeFile) leaf.getINode(i)).getBlocks());
                    _inodes.add(leaf.getINode(i));
                }
                _response =
                    new RangeSearchResponse((RangeSearchRequest) _request, _inodes);
                endLock(self);
            }
        }
	}

	/**
     * Fat-Btree ���̾�Υ���ǥå����ڡ��� (�꡼�ե���ǥå����ʳ�) ��
     * �������������Ȥ���ư���������ޤ�.
     *
     * @param index visitor ��ˬ��� Fat-Btree ���̾�Υ���ǥå����ڡ���
     * @param self index ��ؤ��ݥ���
	 * @throws IOException
	 * @throws MessageException
	 * @throws NotReplicatedYetException
     */
    private void visitIndex(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBTRangeSearch visitIndex index,"+index.getNodeID());
    	index.key2String();
        /* ���Ρ��ɤǤθ��������ΰ��� */
        int position = index.binaryLocate(_minKey);
        System.out.println("position,"+position);
        /* �ҥΡ��ɤ�ؤ��ݥ��� */
        VPointer vp = index.getEntry(position);
        System.out.println("vp,"+vp);
        if (_directory.isLocal(vp)) {
            lock(vp, Lock.IS);
            unlock(self);
            Node node = _directory.getNode(vp);
            node.accept(this, vp);
        } else {
            /*
             * �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
             * Ŭ���� PE �����򤷤��׵��ž��
             */
            lockRequest(vp, Lock.IS);
            endLock(self);

            ((RangeSearchRequest) _request).setTarget(vp);
            ((RangeSearchRequest) _request).setDirectoryName("/directory.edn"+(vp.getPointer().getPartitionID()-100));
            System.out.println("visitIndex,directoryName,"+((RangeSearchRequest)_request).getDirectoryName());
            callRedirectionException(vp.getPointer().getPartitionID());
        }
    }

	/**
     * Fat-Btree �Υ꡼�ե���ǥå����ڡ�����
     * �������������Ȥ���ư���������ޤ�.
     *
     * @param index visitor ��ˬ��� Fat-Btree �Υ꡼�ե���ǥå����ڡ���
     * @param self index ��ؤ��ݥ���
	 * @throws IOException
	 * @throws MessageException
	 * @throws NotReplicatedYetException
     */
    private void visitLeafIndex(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
    	StringUtility.debugSpace("FBTRangeSearch visit leafIndex,"+index.getNodeID());
        /* ���Ρ��ɤǤθ��������ΰ��� */
        int position = index.binaryLocate(_minKey);
        System.out.println("minKey,"+_minKey);
        index.key2String();
        System.out.println("position,"+position);
        if (position < 0) {
            /* ���ߡ��ĥ꡼�˥���ȥ꡼��¸�ߤ��ʤ� */
            _response =
                new RangeSearchResponse((RangeSearchRequest) _request);
            endLock(self);
        } else {
            /* �ҥΡ��ɤ�ؤ��ݥ��� */
            VPointer vp = index.getEntry(position);
            System.out.println("vp,"+vp);
            if (_directory.isLocal(vp)) {
                lock(vp, Lock.S);
                unlock(self);

                Node node = _directory.getNode(vp);
                node.accept(this, vp);
            } else {
                /*
                 * �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
                 * Ŭ���� PE �����򤷤��׵��ž��
                 */
                lockRequest(vp, Lock.S);
                endLock(self);

                ((RangeSearchRequest) _request).setTarget(vp);
                ((RangeSearchRequest) _request).setDirectoryName("/directory.edn"+(vp.getPointer().getPartitionID()-100));
                System.out.println("visitLeafIndex,directoryName,"+_request.getDirectoryName());
                callRedirectionException(vp.getPointer().getPartitionID());
            }
        }
    }


	@Override
	public void visit(PointerNode pointer, VPointer self)
			throws NotReplicatedYetException, MessageException, IOException {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
