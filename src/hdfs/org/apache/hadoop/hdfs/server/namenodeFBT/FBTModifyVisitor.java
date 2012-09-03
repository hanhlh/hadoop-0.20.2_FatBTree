/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.NotReplicatedYetException;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class FBTModifyVisitor extends FBTNodeVisitor {

	private ArrayList<String> _activateNodes;
	private int _previousGear;

	public FBTModifyVisitor(FBTDirectory directory) {
		super(directory);
		_previousGear = 1;
		_activateNodes = directory.getGearActivateNodes().get(_previousGear);
	}


	public ArrayList<String> getActivateNodes() {
		return _activateNodes;
	}

	public int getPreviousGear() {
		return _previousGear;
	}

	public void setActivateNodes(ArrayList<String> activateNodes) {
		_activateNodes = activateNodes;
	}

	public void setPreviousGear(int previousGear) {
		_previousGear = previousGear;
	}
	@Override
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
	public void visit(MetaNode meta, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
		//StringUtility.debugSpace("FBTModifyVisitor.visit "+meta.getNameNodeID());
		//System.out.println(meta);
		IndexNode root = (IndexNode) _directory.getNode(meta.getRootPointer());
		root.accept(this, self);

		PointerNode pointerNode =
				(PointerNode) _directory.getNode(meta.getPointerEntry());
		pointerNode.accept(this, meta.getPointerEntry());

	}

	@Override
	public void visit(IndexNode index, VPointer self) throws MessageException,
			NotReplicatedYetException, IOException {
		// TODO 自動生成されたメソッド・スタブ
		//StringUtility.debugSpace("FBTModifyVisitor.visit "+index.getNameNodeID());
		//System.out.println(index);
		List<VPointer> _children = index.get_children();
		List<VPointer> _pointers = index.get_pointers();


		//1 = low gear
		int position=0;
		for (VPointer child : _children) {
			String fbtOwner = child.getPointer().getFBTOwner();
			child = modifyPointer(child, fbtOwner, getActivateNodes());
			if (child!=null) {
				index.replaceEntry(position, child);
			}
			//System.out.println("position: "+position);
			position++;
		}
		for (VPointer pointer : _pointers) {
			if (_directory.isLocalDirectory(pointer)) {
				pointer.getPointer().setPartitionID(_directory.getSelfPartID());
			}
		}
		index.setPointers(_pointers);
		if (index.getSideLink()!=null) {
			VPointer newSideLink = modifyPointer(index.getSideLink(),
								index.getSideLink().getPointer().getFBTOwner(),
								getActivateNodes());

			index.set_sideLink(newSideLink);
		}

		//System.out.println("modify index done ");
		//System.out.println(index);

		for (VPointer child : index.get_children()) {
			if (_directory.isLocal(child)) {
				Node node = _directory.getNode(child);
				node.accept(this, child);
			}
		}


	}


	@Override
	public void visit(LeafNode leaf, VPointer self)
			throws QuotaExceededException, MessageException,
			NotReplicatedYetException, IOException {
		//StringUtility.debugSpace("FBTModifyVisitor.visit "+leaf.getNodeNameID());
		//System.out.println(leaf);
	}

	@Override
	public void visit(PointerNode pointer, VPointer self)
			throws NotReplicatedYetException, MessageException, IOException {
		//StringUtility.debugSpace("FBTModifyVisitor.visit "+pointer.getNameNodeID());
		//System.out.println(pointer);
		PointerSet entries = pointer.getEntry();
		for (Iterator iter=(Iterator) entries.iterator(); iter.hasNext();) {
			VPointer vp = (VPointer) iter.next();
			vp = modifyPointerNode(vp,
								vp.getPointer().getFBTOwner(),
								getActivateNodes());
		}
		pointer.setEntry(entries);
		//System.out.println(pointer);

	}

	public VPointer modifyPointer(VPointer target, String fbtOwner,
			ArrayList<String> activateNodes) {
	if (!activateNodes.contains(fbtOwner)) {
		target.getPointer().setPartitionID(100+
			Integer.parseInt(fbtOwner.substring(fbtOwner.length()-2)));
		return target;
	}
	else return null;
	}

	public VPointer modifyPointerNode(VPointer target, String fbtOwner,
			ArrayList<String> activateNodes) {
	if (!activateNodes.contains(fbtOwner)) {
		target.getPointer().setPartitionID(100+
			Integer.parseInt(fbtOwner.substring(fbtOwner.length()-2)));
		return target;
	}
	return target;
	}

}
