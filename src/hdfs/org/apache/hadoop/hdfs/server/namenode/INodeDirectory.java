/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.namenode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.PermissionStatus;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.protocol.NNClusterProtocol;
import org.apache.hadoop.ipc.RPC;

/**
 * Directory INode class.
 */
public class INodeDirectory extends INode {
  protected static final int DEFAULT_FILES_PER_DIRECTORY = 5;
  final static String ROOT_NAME = "";

  private List<INode> children;

  public INodeDirectory(String name, PermissionStatus permissions) {
    super(name, permissions);
    this.children = null;
  }

  public INodeDirectory(PermissionStatus permissions, long mTime) {
    super(permissions, mTime, 0);
    this.children = null;
  }

  /** constructor */
  public INodeDirectory(byte[] localName, PermissionStatus permissions, long mTime) {
    this(permissions, mTime);
    this.name = localName;
  }

  public INodeDirectory(String name, PermissionStatus permissions, long mTime) {
	    this(permissions, mTime);
	    this.name = getPathComponents(name)[getPathComponents(name).length-1];
	    this.pathName = name;
	  }
  /** copy constructor
   *
   * @param other
   */
  INodeDirectory(INodeDirectory other) {
    super(other);
    this.children = other.getChildren();
  }

  /**
   * Check whether it's a directory
   */
  public boolean isDirectory() {
    return true;
  }

  INode removeChild(INode node) {
    assert children != null;
    int low = Collections.binarySearch(children, node.name);
    if (low >= 0) {
      return children.remove(low);
    } else {
      return null;
    }
  }

  /** Replace a child that has the same name as newChild by newChild.
   *
   * @param newChild Child node to be added
   */
  void replaceChild(INode newChild) {
    if ( children == null ) {
      throw new IllegalArgumentException("The directory is empty");
    }
    int low = Collections.binarySearch(children, newChild.name);
    if (low>=0) { // an old child exists so replace by the newChild
      children.set(low, newChild);
    } else {
      throw new IllegalArgumentException("No child exists to be replaced");
    }
  }

  INode getChild(String name) {
    return getChildINode(string2Bytes(name));
  }

  private INode getChildINode(byte[] name) {
    if (children == null) {
      return null;
    }
    int low = Collections.binarySearch(children, name);
    if (low >= 0) {
      return children.get(low);
    }
    return null;
  }

  /**
   */
  private INode getNode(byte[][] components) {
    INode[] inode  = new INode[1];
    getExistingPathINodes(components, inode);
    return inode[0];
  }

  /**
   * This is the external interface
   */
  INode getNode(String path) {
    return getNode(getPathComponents(path));
  }

    /**
	 * This is the external interface
	 * @param addrs
	 *            TODO
	 * @param fromCluster TODO
	 */
	INode getNode(String path, Map<Integer, InetSocketAddress> addrs, boolean fromCluster) {
		if(fromCluster == false)
		 return getNode(getPathComponents(path));
		else
			return getNodeFromCluster(getPathComponents(path), addrs, null);
	}

	// appended //
	INode getNodeFromCluster(byte[][] components,
			Map<Integer, InetSocketAddress> addrs, List<Integer> visited) {
		if (visited == null) {
			visited = new ArrayList<Integer>();
		}
		Integer now = new Integer(namenodeID);
			visited.add(now);
		INode[] curPath = new INode[components.length];
		int pos = getExistingPathINodes(components, curPath) - 1;
		if (curPath[components.length - 1] != null)
			return curPath[components.length - 1];
		if (!curPath[pos].isDirectory())
			return null;
		List<Integer> nodes = curPath[pos].getINodeLocations();
		List<Integer> nodescopy = new ArrayList<Integer>(nodes);
		for (Iterator<Integer> i = visited.iterator(); i.hasNext();) {
			Integer v = i.next();
			if (nodescopy.contains(v))
				nodescopy.remove(v);
		}
		for (Iterator<Integer> it = nodescopy.iterator(); it.hasNext();) {
			if (nodescopy.size() == 0)
				break;
			Integer curID = it.next();
			nodescopy.remove(curID);
			InetSocketAddress addr = addrs.get(curID);
			NNClusterProtocol namenode;
			Integer[] vitary = (Integer[]) visited.toArray(new Integer[0]);
			int[] vitaryint = new int[vitary.length];
			for(int k = 0;k < vitary.length;k++)
				vitaryint[k] = vitary[k].intValue();
			try {
				namenode = (NNClusterProtocol) RPC.waitForProxy(
						NNClusterProtocol.class, NNClusterProtocol.versionID,
						addr, new Configuration());
				INodeInfo target = namenode.getNodeFromOther(components,
						vitaryint);
				if(target == null)
					return null;
				return target.makeINode();
			} catch (IOException e) {
				System.err.println("exception");
				e.printStackTrace();
			}

		}
		return null;
		// end of appended //
	}

  /**
   * Retrieve existing INodes from a path. If existing is big enough to store
   * all path components (existing and non-existing), then existing INodes
   * will be stored starting from the root INode into existing[0]; if
   * existing is not big enough to store all path components, then only the
   * last existing and non existing INodes will be stored so that
   * existing[existing.length-1] refers to the target INode.
   *
   * <p>
   * Example: <br>
   * Given the path /c1/c2/c3 where only /c1/c2 exists, resulting in the
   * following path components: ["","c1","c2","c3"],
   *
   * <p>
   * <code>getExistingPathINodes(["","c1","c2"], [?])</code> should fill the
   * array with [c2] <br>
   * <code>getExistingPathINodes(["","c1","c2","c3"], [?])</code> should fill the
   * array with [null]
   *
   * <p>
   * <code>getExistingPathINodes(["","c1","c2"], [?,?])</code> should fill the
   * array with [c1,c2] <br>
   * <code>getExistingPathINodes(["","c1","c2","c3"], [?,?])</code> should fill
   * the array with [c2,null]
   *
   * <p>
   * <code>getExistingPathINodes(["","c1","c2"], [?,?,?,?])</code> should fill
   * the array with [rootINode,c1,c2,null], <br>
   * <code>getExistingPathINodes(["","c1","c2","c3"], [?,?,?,?])</code> should
   * fill the array with [rootINode,c1,c2,null]
   * @param components array of path component name
   * @param existing INode array to fill with existing INodes
   * @return number of existing INodes in the path
   */
  public int getExistingPathINodes(byte[][] components, INode[] existing) {
    assert compareBytes(this.name, components[0]) == 0 :
      "Incorrect name " + getLocalName() + " expected " + components[0];

    INode curNode = this;
    int count = 0;
    int index = existing.length - components.length;
    if (index > 0)
      index = 0;
    while ((count < components.length) && (curNode != null)) {
      if (index >= 0)
        existing[index] = curNode;
      if (!curNode.isDirectory() || (count == components.length - 1))
        break; // no more child, stop here
      INodeDirectory parentDir = (INodeDirectory)curNode;
      curNode = parentDir.getChildINode(components[count + 1]);
      count += 1;
      index += 1;
    }
    return count;
  }

  /**
   * Retrieve the existing INodes along the given path. The first INode
   * always exist and is this INode.
   *
   * @param path the path to explore
   * @return INodes array containing the existing INodes in the order they
   *         appear when following the path from the root INode to the
   *         deepest INodes. The array size will be the number of expected
   *         components in the path, and non existing components will be
   *         filled with null
   *
   * @see #getExistingPathINodes(byte[][], INode[])
   */
  INode[] getExistingPathINodes(String path) {
    byte[][] components = getPathComponents(path);
    INode[] inodes = new INode[components.length];

    this.getExistingPathINodes(components, inodes);

    return inodes;
  }

  /**
   * Add a child inode to the directory.
   *
   * @param node INode to insert
   * @param inheritPermission inherit permission from parent?
   * @return  null if the child with this name already exists;
   *          node, otherwise
   */
  public <T extends INode> T addChild(final T node, boolean inheritPermission) {
    if (inheritPermission) {
      FsPermission p = getFsPermission();
      //make sure the  permission has wx for the user
      if (!p.getUserAction().implies(FsAction.WRITE_EXECUTE)) {
        p = new FsPermission(p.getUserAction().or(FsAction.WRITE_EXECUTE),
            p.getGroupAction(), p.getOtherAction());
      }
      node.setPermission(p);
    }
    if (children == null) {
      children = new ArrayList<INode>(DEFAULT_FILES_PER_DIRECTORY);
    }
    int low = Collections.binarySearch(children, node.name);
    if(low >= 0)
      return null;
    node.parent = this;
    children.add(-low - 1, node);
    // update modification time of the parent directory
    setModificationTime(node.getModificationTime());
    if (node.getGroupName() == null) {
      node.setGroup(getGroupName());
    }
    return node;
  }

  /**
   * Equivalent to addNode(path, newNode, false).
   * @see #addNode(String, INode, boolean)
   */
  <T extends INode> T addNode(String path, T newNode) throws FileNotFoundException {
    return addNode(path, newNode, false);
  }
  /**
   * Add new INode to the file tree.
   * Find the parent and insert
   *
   * @param path file path
   * @param newNode INode to be added
   * @param inheritPermission If true, copy the parent's permission to newNode.
   * @return null if the node already exists; inserted INode, otherwise
   * @throws FileNotFoundException if parent does not exist or
   * is not a directory.
   */
  <T extends INode> T addNode(String path, T newNode, boolean inheritPermission
      ) throws FileNotFoundException {
    if(addToParent(path, newNode, null, inheritPermission) == null)
      return null;
    return newNode;
  }

  /**
   * Add new inode to the parent if specified.
   * Optimized version of addNode() if parent is not null.
   *
   * @return  parent INode if new inode is inserted
   *          or null if it already exists.
   * @throws  FileNotFoundException if parent does not exist or
   *          is not a directory.
   */
  <T extends INode> INodeDirectory addToParent(
                                      String path,
                                      T newNode,
                                      INodeDirectory parent,
                                      boolean inheritPermission
                                    ) throws FileNotFoundException {
    byte[][] pathComponents = getPathComponents(path);
    assert pathComponents != null : "Incorrect path " + path;
    int pathLen = pathComponents.length;
    if (pathLen < 2)  // add root
      return null;
    if(parent == null) {
      // Gets the parent INode
      INode[] inodes  = new INode[2];
      getExistingPathINodes(pathComponents, inodes);
      INode inode = inodes[0];
      if (inode == null) {
        throw new FileNotFoundException("Parent path does not exist: "+path);
      }
      if (!inode.isDirectory()) {
        throw new FileNotFoundException("Parent path is not a directory: "+path);
      }
      parent = (INodeDirectory)inode;
    }
    // insert into the parent children list
    newNode.name = pathComponents[pathLen-1];
    if(parent.addChild(newNode, inheritPermission) == null)
      return null;
    return parent;
  }

  /** {@inheritDoc} */
  public DirCounts spaceConsumedInTree(DirCounts counts) {
    counts.nsCount += 1;
    if (children != null) {
      for (INode child : children) {
        child.spaceConsumedInTree(counts);
      }
    }
    return counts;
  }

  /** {@inheritDoc} */
  long[] computeContentSummary(long[] summary) {
    if (children != null) {
      for (INode child : children) {
        child.computeContentSummary(summary);
      }
    }
    summary[2]++;
    return summary;
  }

  /**
   */
  List<INode> getChildren() {
    return children==null ? new ArrayList<INode>() : children;
  }
  List<INode> getChildrenRaw() {
    return children;
  }

  int collectSubtreeBlocksAndClear(List<Block> v) {
    int total = 1;
    if (children == null) {
      return total;
    }
    for (INode child : children) {
      total += child.collectSubtreeBlocksAndClear(v);
    }
    parent = null;
    children = null;
    return total;
  }

  //appended //
	public INodeDirectory() {
	};

	public INodeDirectory(INodeDirectoryInfo info) {
		// TODO Auto-generated constructor stub
		super(info);
	}

@Override
	public boolean isHere() {
		return true;
	}
}
