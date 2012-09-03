/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryWithQuota;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTInitializeVisitor extends FBTNodeVisitor{

	private int _height;

    protected FBTInitializeHelper _helper;
    //NNBenchmark's parameters
    protected static String _experimentDir;

    protected static int _low;

    protected static int _high;

	public FBTInitializeVisitor(FBTDirectory directory) {
		super(directory);
	}

	public void initialize (Configuration conf) {
		_height = 0;
        _helper = new FBTInitializeHelper(
                _directory.getPartitionID(), _directory.getFanout(), conf,
                0);
   	}

	public void initialize(List<EndPoint> endPoints, Configuration conf) {
		conf.addResource("hdfs-site-fbt.xml");
        _height = 0;
        _helper = new FBTInitializeHelper(
                _directory.getPartitionID(), _directory.getFanout(), endPoints,
                _directory.getOwnerID());
        _experimentDir = conf.get("dfs.nnbenchmark.dir");
  		_low = conf.getInt("dfs.nnbenchmark.low", 0);
  		_high = conf.getInt("dfs.nnbenchmark.high", 1000);
  		System.out.println("low "+_low);
  		System.out.println("high "+_high);
  		if (_high < _low) {
       		System.err.println("parameters invalid");
       		System.exit(0);
  		}
    }
	public void run() {
		try {
			_directory.accept(this);
		} catch (NotReplicatedYetException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (MessageException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	public void visit(MetaNode meta, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		//System.out.println("FBTInitializaVisitor visit meta "+meta.getNameNodeID());
		IndexNode root = (IndexNode) _directory.createIndexNode();
		root.set_isRightest(true);
		root.set_isLeftest(true);
		synchronized (_directory.getLocalNodeMapping()) {
			_directory.getLocalNodeMapping().put(root.
									getNodeIdentifier().toString(), root);
		}
        meta.setRootPointer(root.getPointer());
        root.setRootIndex(true);
        root.accept(this, root.getPointer());

        PointerNode pointerNode = (PointerNode)_directory.
        			createPointerNode();
        synchronized (_directory.getLocalNodeMapping()) {
        	_directory.getLocalNodeMapping().put(pointerNode.
        							getNodeIdentifier().toString(), pointerNode);
        }
        meta.setPointerEntry(pointerNode.getPointer());
        System.out.println("pointerEntry at meta: "+meta.getPointerEntry().toString());
        //System.out.println("Meta.PoiterEntry "+pointerNode.getPointer());
        Iterator<PointerSet> pSetIter = _helper.getPointerSetIterator();
        //System.out.println("pSetIter "+pSetIter.toString());
        while (pSetIter.hasNext()) {
            PointerSet pSet = pSetIter.next();
            Iterator<Pointer> pIter = pSet.iterator();
            while (pIter.hasNext()) {
                pointerNode.addEntry(pIter.next());
            }
        }

        //reset entries containing all pointer to other Root nodes
        //at pointerNode
        //edn13_1, 1 is the order of sequenced node of root
        PointerSet ps = pointerNode.getEntry();
        for (Iterator iter = ps.iterator(); iter.hasNext();) {
        	Pointer p = (Pointer) iter.next();
        	p.setNodeID("edn"+(p.getPartitionID()-100)+"_1");
        }

        for (Iterator iter = ps.iterator(); iter.hasNext();) {
        	Pointer p = (Pointer) iter.next();
        	String[] locations = _directory.
        			getFBTDirectoryReplicationMapping().get("edn"+(p.getPartitionID()-100));

        	try {
				if (!locations[1].equals("null")) {//no backup, only primary
					p.getPointer().setPartitionID(
							100+Integer.parseInt(locations[1].substring(
									locations[1].length()-2)));
				}
			} catch (NullPointerException ne){
				System.out.println(ne.getMessage() + " no backup location identified");
			}
        }
        for (int i=0; i<pointerNode.getEntry().size();i++) {
        	System.out.println("pointer node "+i+": " +pointerNode.getEntry().get(i));
        }
        //System.out.println("visit MetaNode done");
	}

	@Override
	public void visit(IndexNode index, VPointer self) throws NotReplicatedYetException, MessageException, IOException {
		StringUtility.debugSpace("FBTInitilizeVisitor.visit index "+index.getNodeIdentifier());
		_height++;

        if (_helper.isEnough(_height)) {
        	//System.out.println("heigh is enoug "+_height);
            Node node = _directory.createIndexNode();
            //System.out.println("create index Node "+node.getNodeID());
            synchronized (_directory.getLocalNodeMapping()) {
            	_directory.getLocalNodeMapping().put(((IndexNode) node).getNodeIdentifier().toString(), node);
            }
            node.accept(this, node.getPointer());
        } else {
        	//System.out.println("heigh is not enoug"+_height);
            index.setLeafIndex(true);
            Node node = _directory.createLeafNode();
            //System.out.println("create LeafNode "+node.getNodeID());
            synchronized (_directory.getLocalNodeMapping()) {
            	_directory.getLocalNodeMapping().put(((LeafNode) node).getNodeIdentifier().toString(), node);
            }
            node.accept(this, node.getPointer());
        }

        Iterator<String> kIter = _helper.getKeyIterator();
        Iterator<Pointer> cIter = _helper.getChildIterator();
        Iterator<PointerSet> pSetIter = _helper.getPointerSetIterator();
        while (kIter.hasNext() && cIter.hasNext() && pSetIter.hasNext()) {
        	Pointer childPointer = cIter.next();
        	childPointer.setNodeID("edn".concat(String.valueOf(childPointer.getPartitionID()-100))
        								.concat("_2"));
            index.addEntry(index.size(), kIter.next(), childPointer);

            if (index.isLeafIndex()) {
                Pointer p = pSetIter.next().firstElement();
                index.addPointer(index.size() - 1, p);
            } else {
                PointerNode pNode = (PointerNode)_directory.createPointerNode();
                synchronized (_directory.getLocalNodeMapping()) {
                	_directory.getLocalNodeMapping().put(pNode.
                							getNodeIdentifier().toString(), pNode);
                }
                Iterator<Pointer> pIter = pSetIter.next().iterator();
                while (pIter.hasNext()) {
                    pNode.addEntry(pIter.next());
                }

                index.addPointer(index.size() - 1, pNode.getPointer());
            }
        }
        index.set_nextKeyOnParent(_helper.getHighKey());
        index.set_isLeftest(_helper.isLeftest());
        index.set_isRightest(_helper.isRightest());
        _helper.setParentEntry(index);
        System.out.println("visitIndexNode done");

        //Blink

        /*IndexNode dummy = new IndexNode(_directory);
        synchronized (_directory.localNodeMapping) {
        	_directory.localNodeMapping.put(dummy.getNodeIdentifier().toString(), dummy);
        }
        dummy.setDummy(true);
        dummy.setLeafIndex(index.isLeafIndex());

        int rightPartID = _helper.getRightPartID();
        if (rightPartID == Integer.MIN_VALUE) {
//            dummy.setSideLink(null);
        } else {
            dummy.setSideLink(new Pointer(rightPartID, index.getNodeID()));
        }
        index.setSideLink(dummy.getPointer());
*/
     }

	@Override
	public void visit(LeafNode leaf, VPointer self) {
		StringUtility.debugSpace("FBTInitilizeVisitor.visit leaf "+leaf.getNodeIdentifier());

		LeafNode dummy = (LeafNode) _directory.createLeafNode();
		synchronized (_directory.getLocalNodeMapping()) {
			_directory.getLocalNodeMapping().put(dummy.getNodeIdentifier().toString(), dummy);
		}
        int rightPartID = _helper.getRightPartID();
        //System.out.println("rightPartID "+rightPartID);
        String separator;
        if (rightPartID == Integer.MIN_VALUE) {
            separator = _experimentDir.concat(String.format("%06d", _high));
            System.out.println("separator" +separator);
        } else {
        	separator = StringUtility.generateDefaultSeparator(rightPartID,
        								_helper.partitionSize(), _experimentDir,
        								_low, _high);
        	System.out.println("separator" +separator);
            dummy.set_sideLink(new Pointer(rightPartID,
            				leaf.getNodeIdentifier().toString()));
        }

        dummy.set_highKey(separator);
        dummy.set_isDummy(true);

        leaf.set_highKey(separator);

        //System.out.println("leaf "+leaf.getNodeNameID()+ " sideLink "+dummy.
        //										getPointer().getPointer());
        leaf.set_sideLink(dummy.getPointer().getPointer());

        /*
        INodeDirectory inodeDirectory = new INodeDirectory(separator);
        LeafValue lv = new LeafValue(inodeDirectory, null);
        leaf.addLeafValue(0, separator, lv);
         */

        getDirectory().setDummyLeaf(dummy.getPointer());

        _helper.setParentEntry(leaf);
        //System.out.println("visit LeafNode done");

     }

	@Override
	public void visit(PointerNode pointer, VPointer self) {
		// TODO 自動生成されたメソッド・スタブ

	}

}
