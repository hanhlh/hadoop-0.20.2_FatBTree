/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.lock.Lock;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.request.RequestFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.response.ResponseClassFactory;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchRequest;
import org.apache.hadoop.hdfs.server.namenodeFBT.rule.SearchResponse;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public class FBTSearchNoCouplingVisitor extends FBTNodeVisitor {

	/**
     * �ǥ��쥯�ȥ긡������
     */
    private String _key;

    // 	constructors ///////////////////////////////////////////////////////////

    public FBTSearchNoCouplingVisitor(FBTDirectory directory) {
        super(directory);
    }

    // accessors //////////////////////////////////////////////////////////////

    public void setRequest(Request request) {
        setRequest((SearchRequest) request);
    }

    public void setRequest(SearchRequest request) {
        super.setRequest(request);
        _key = request.getKey();
    }

	public void run() {
		Date start = new Date();
		VPointer vp = _request.getTarget();
        if (vp == null) {
            /* �롼�Ȥ������򳫻� */
            try {
				_directory.accept(this);
			} catch (NotReplicatedYetException e) {
				e.printStackTrace();
			} catch (MessageException e) {
				e.printStackTrace();
			} catch (IOException e) {
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

        while (_response == null) {
            ((FBTDirectory) _directory).incrementCorrectingCount();

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
        }
        NameNode.LOG.info("searchVisitorCore,"+(new Date().getTime()-start.getTime())/1000.0);
	}

	@Override
	public void visit(MetaNode meta, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		//StringUtility.debugSpace("FBTSearchNoCouplingVisitor.visit "+
		//		meta.getNameNodeID());
		lock(self, Lock.IS);
        VPointer root = meta.getRootPointer();
        //System.out.println("root "+root);
        unlock(self);
        Node node = _directory.getNode(root);
        //System.out.println("getNode "+node.getNodeID());
        node.accept(this, root);

	}

	@Override
	public void visit(IndexNode index, VPointer self) throws MessageException, NotReplicatedYetException, IOException {
		//StringUtility.debugSpace("FBTSearchNoCouplingVisitor.visit "+index.getNameNodeID());
		//index.key2String();
		lock(self, Lock.IS);
//      if (_parent != null) {
//          unlock(_parent);
//          _parent = null;
//      }

      if (!index.isInRange(_key) || index._deleteBit){
    	  //System.out.println(_key +" is not in range");
          unlock(self);
          //System.out.println("unlock IS");
      } else {
          /* ���Ρ��ɤǤθ��������ΰ��� */
    	  //System.out.println(_key +" is in range");
          int position = index.binaryLocate(_key);
         // System.out.println("position "+position);

          if (position < 0) {
              /* ���ߡ��ĥ꡼�˥���ȥ꡼��¸�ߤ��ʤ� */
        	  //System.out.println("no entry");
              _response = new SearchResponse((SearchRequest) _request);
              endLock(self);
          } else {
              /* �ҥΡ��ɤ�ؤ��ݥ��� */
              VPointer vp = index.getEntry(position);
              //System.out.println("vp "+vp);
              if (_directory.isLocal(vp)) { //local PE
            	  if (_directory.isLocalDirectory(vp)) { //local FBTDirectory
            		  //System.out.println(vp + "refers to node in local directory");
	                  unlock(self);
	//                  _parent = self;
	                  Node node = _directory.getNode(vp);
	                  node.accept(this, vp);
            	  } else { //fw to backup FBTDirectory
            		  //System.out.println(vp + " does not refer to node in local directory");
            		  unlock(self);
            		  FBTDirectory targetFBTDirectory =
            			  			(FBTDirectory) NameNodeFBTProcessor.lookup(
							"/directory."+vp.getPointer().getFBTOwner());
            		  Node node = targetFBTDirectory.getNode(vp);
            		  node.accept(this, vp);
            	  }
              } else { // not local PE
                  /*
                   * �ݥ��� vp �λؤ��Ƥ���Ρ��ɤϸ��ߤ� PE �ˤϤʤ����ᡤ
                   * Ŭ���� PE �����򤷤��׵��ž��
                   */
                  endLock(self);
                  _request.setTarget(vp);
                  //System.out.println("partID "+vp.getPointer().getPartitionID());
                  callRedirectionException(vp.getPointer().
                		  					getPartitionID());
              }
          }
      }

	}


	@Override
	public void visit(LeafNode leaf, VPointer self) {
		//StringUtility.debugSpace("FBTSearchNoCoupling visit leafnode ");
		lock(self, Lock.S);
//      if (_parent != null) {
//          unlock(_parent);
//          _parent = null;
//      }

      if (leaf.get_isDummy()) {
          VPointer vp = leaf.get_sideLink();
          endLock(self);
          _request.setTarget(vp);
          callRedirectionException(vp.getPointer().getPartitionID());
      } else {
    	  //System.out.println("key "+_key);
    	  //System.out.println("highkey "+leaf.get_highKey());
          if (_key.compareTo(leaf.get_highKey()) >= 0 || leaf.get_deleteBit()) {

              unlock(self);
          } else {
              /* ���Ρ��ɤǤθ��������ΰ��� */

        	  int position = leaf.binaryLocate(_key);
        	  //System.out.println("position "+position);
              if (position <= 0) {
                  // ������¸�ߤ��ʤ�
            	  //System.out.println("key "+_key+" doesnt exist");
                  endLock(self);
                  _response = new SearchResponse((SearchRequest) _request);
              } else {
                  INode inode = leaf.getINode(position - 1);
                  endLock(self);
                  _response = new SearchResponse(
                          (SearchRequest) _request, self, _key, inode);
              }

              endLock(self);

          }
      }

	}

	@Override
	public void visit(PointerNode pointer, VPointer self) {
		// TODO ��ư�������줿�᥽�åɡ�������

	}

}
