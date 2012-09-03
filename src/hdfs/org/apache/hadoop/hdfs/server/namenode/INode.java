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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.permission.*;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;

/**
 * We keep an in-memory representation of the file/block hierarchy.
 * This is a base INode class containing common fields for file and
 * directory inodes.
 */
public abstract class INode implements Comparable<byte[]>, Serializable {
  protected byte[] name;
  protected INodeDirectory parent;
  protected long modificationTime;
  protected long accessTime;
  //append
  protected String pathName;
  protected PermissionStatus permissionStatus;

//NameNodes that have this INode
  protected List<Integer> iNodeLocations;
  protected int namenodeID;

  /** Simple wrapper for two counters :
   *  nsCount (namespace consumed) and dsCount (diskspace consumed).
   */
  public static class DirCounts {
    long nsCount = 0;
    long dsCount = 0;

    /** returns namespace count */
    public long getNsCount() {
      return nsCount;
    }
    /** returns diskspace count */
    public long getDsCount() {
      return dsCount;
    }
  }

  //Only updated by updatePermissionStatus(...).
  //Other codes should not modify it.
  private long permission;

  private static enum PermissionStatusFormat {
    MODE(0, 16),
    GROUP(MODE.OFFSET + MODE.LENGTH, 25),
    USER(GROUP.OFFSET + GROUP.LENGTH, 23);

    final int OFFSET;
    final int LENGTH; //bit length
    final long MASK;

    PermissionStatusFormat(int offset, int length) {
      OFFSET = offset;
      LENGTH = length;
      MASK = ((-1L) >>> (64 - LENGTH)) << OFFSET;
    }

    long retrieve(long record) {
      return (record & MASK) >>> OFFSET;
    }

    long combine(long bits, long record) {
      return (record & ~MASK) | (bits << OFFSET);
    }
  }

  protected INode() {
    name = null;
    parent = null;
    modificationTime = 0;
    accessTime = 0;
  }

  INode(PermissionStatus permissions, long mTime, long atime) {
	setHere(); //append
    this.name = null;
    this.parent = null;
    this.modificationTime = mTime;
    setAccessTime(atime);
    setPermissionStatus(permissions);
    //this.permissionStatus = setPermissionStatus(permissions);
  }

  protected INode(String name, PermissionStatus permissions) {
    this(permissions, 0L, 0L);
    setLocalName(name);
  }

  /** copy constructor
   *
   * @param other Other node to be copied
   */
  INode(INode other) {
    setLocalName(other.getLocalName());
    this.parent = other.getParent();
    setPermissionStatus(other.getPermissionStatus());
    setModificationTime(other.getModificationTime());
    setAccessTime(other.getAccessTime());
  }
  public INode(INodeInfo info) {
		// TODO Auto-generated constructor stub
		  name = DFSUtil.string2Bytes(info.getName());
		  namenodeID = info.getNamenodeID();
		  parent = null;
		  modificationTime = info.getModificationTime();
		  accessTime = info.getAccessTime();
		  iNodeLocations = info.getINodeLocations();
		  permission = info.getPermission();
	}

  /**
   * Check whether this is the root inode.
   */
  boolean isRoot() {
    return name.length == 0;
  }

  /** Set the {@link PermissionStatus} */
  protected void setPermissionStatus(PermissionStatus ps) {
    setUser(ps.getUserName());
    setGroup(ps.getGroupName());
    setPermission(ps.getPermission());
  }
  /** Get the {@link PermissionStatus} */
  public PermissionStatus getPermissionStatus() {
    return new PermissionStatus(getUserName(),getGroupName(),getFsPermission());
  }
  private synchronized void updatePermissionStatus(
      PermissionStatusFormat f, long n) {
    permission = f.combine(n, permission);
  }
  /** Get user name */
  public String getUserName() {
    int n = (int)PermissionStatusFormat.USER.retrieve(permission);
    return SerialNumberManager.INSTANCE.getUser(n);
  }
  /** Set user */
  protected void setUser(String user) {
    int n = SerialNumberManager.INSTANCE.getUserSerialNumber(user);
    updatePermissionStatus(PermissionStatusFormat.USER, n);
  }
  /** Get group name */
  public String getGroupName() {
    int n = (int)PermissionStatusFormat.GROUP.retrieve(permission);
    return SerialNumberManager.INSTANCE.getGroup(n);
  }
  /** Set group */
  public void setGroup(String group) {
    int n = SerialNumberManager.INSTANCE.getGroupSerialNumber(group);
    updatePermissionStatus(PermissionStatusFormat.GROUP, n);
  }
  /** Get the {@link FsPermission} */
  public FsPermission getFsPermission() {
    return new FsPermission(
        (short)PermissionStatusFormat.MODE.retrieve(permission));
  }
  protected short getFsPermissionShort() {
    return (short)PermissionStatusFormat.MODE.retrieve(permission);
  }
  /** Set the {@link FsPermission} of this {@link INode} */
  protected void setPermission(FsPermission permission) {
    updatePermissionStatus(PermissionStatusFormat.MODE, permission.toShort());
  }

  /**
   * Check whether it's a directory
   */
  public abstract boolean isDirectory();
  /**
   * Collect all the blocks in all children of this INode.
   * Count and return the number of files in the sub tree.
   * Also clears references since this INode is deleted.
   */
  abstract int collectSubtreeBlocksAndClear(List<Block> v);

  /** Compute {@link ContentSummary}. */
  public final ContentSummary computeContentSummary() {
    long[] a = computeContentSummary(new long[]{0,0,0,0});
    return new ContentSummary(a[0], a[1], a[2], getNsQuota(),
                              a[3], getDsQuota());
  }
  /**
   * @return an array of three longs.
   * 0: length, 1: file count, 2: directory count 3: disk space
   */
  abstract long[] computeContentSummary(long[] summary);

  /**
   * Get the quota set for this inode
   * @return the quota if it is set; -1 otherwise
   */
  long getNsQuota() {
    return -1;
  }

  long getDsQuota() {
    return -1;
  }

  public boolean isQuotaSet() {
    return getNsQuota() >= 0 || getDsQuota() >= 0;
  }

  /**
   * Adds total nubmer of names and total disk space taken under
   * this tree to counts.
   * Returns updated counts object.
   */
  public abstract DirCounts spaceConsumedInTree(DirCounts counts);

  /**
   * Get local file name
   * @return local file name
   */
  String getLocalName() {
    return bytes2String(name);
  }

  /**
   * Get local file name
   * @return local file name
   */
  public byte[] getLocalNameBytes() {
    return name;
  }

  /**
   * Set local file name
   */
  public void setLocalName(String name) {
    this.name = string2Bytes(name);
    //append
    this.pathName = name;
  }

  /**
   * Set local file name
   */
  void setLocalName(byte[] name) {
    this.name = name;
  }


  /**
   * Get las
  }

  /** {@inheritDoc} */
  public String toString() {
    return "\"" + getLocalName() + "\":" + getPermissionStatus();
  }

  /**
   * Get parent directory
   * @return parent INode
   */
  INodeDirectory getParent() {
    return this.parent;
  }

  /**
   * Get last modification time of inode.
   * @return access time
   */
  public long getModificationTime() {
    return this.modificationTime;
  }

  /**
   * Set last modification time of inode.
   */
  public void setModificationTime(long modtime) {
    assert isDirectory();
    if (this.modificationTime <= modtime) {
      this.modificationTime = modtime;
    }
  }

  /**
   * Always set the last modification time of inode.
   */
  void setModificationTimeForce(long modtime) {
    assert !isDirectory();
    this.modificationTime = modtime;
  }

  /**
   * Get access time of inode.
   * @return access time
   */
  public long getAccessTime() {
    return accessTime;
  }

  /**
   * Set last access time of inode.
   */
  void setAccessTime(long atime) {
    accessTime = atime;
  }

  /**
   * Is this inode being constructed?
   */
  public boolean isUnderConstruction() {
    return false;
  }
  //append
  public String getPathName() {
	  return pathName;
  }

  /**
   * Breaks file path into components.
   * @param path
   * @return array of byte arrays each of which represents
   * a single path component.
   */
  public static byte[][] getPathComponents(String path) {
    return getPathComponents(getPathNames(path));
  }

  /** Convert strings to byte arrays for path components. */
  static byte[][] getPathComponents(String[] strings) {
    if (strings.length == 0) {
      return new byte[][]{null};
    }
    byte[][] bytes = new byte[strings.length][];
    for (int i = 0; i < strings.length; i++)
      bytes[i] = string2Bytes(strings[i]);
    return bytes;
  }

  /**
   * Breaks file path into names.
   * @param path
   * @return array of names
   */
  static String[] getPathNames(String path) {
    if (path == null || !path.startsWith(Path.SEPARATOR)) {
      return null;
    }
    return path.split(Path.SEPARATOR);
  }

  public boolean removeNode() {
    if (parent == null) {
      return false;
    } else {

      parent.removeChild(this);
      parent = null;
      return true;
    }
  }

  //append
  public static String[] getParentPathNames(String path) {
	  int count=2; //starts from /user/hanhlh
	  if (path == null || !path.startsWith(FBTDirectory.DEFAULT_ROOT_DIRECTORY)) {
		  System.out.println("invalid path");
		  return null;
	  }
	  String[] names = getPathNames(path);
	  String[] parents = new String[names.length-3];
	  for (;count<names.length-1; count++) {
		  String pathBuilder = "";
		  for (int temp = 1; temp<count+1; temp++) {
			  pathBuilder = pathBuilder.concat(Path.SEPARATOR).concat(names[temp]);
		  }
		  parents[count-2] = pathBuilder;
	  }
	  return parents;
  }
  public static String[] getParentAndCurrentPathNames(String path) {
	  int count=2; //starts from /user/hanhlh
	  if (path == null || !path.startsWith(FBTDirectory.DEFAULT_ROOT_DIRECTORY)) {
		  System.out.println("invalid path");
		  return null;
	  }
	  String[] names = getPathNames(path);
	  //System.out.println("name length "+names.length);
	  String[] parents = new String[names.length-2];
	  for (;count<names.length; count++) {
		  String pathBuilder = "";
		  for (int temp = 1; temp<count+1; temp++) {
			  pathBuilder = pathBuilder.concat(Path.SEPARATOR).concat(names[temp]);
		  }
		  parents[count-2] = pathBuilder;
	  }
	  return parents;
  }
  //end append
  //
  // Comparable interface
  //
  public int compareTo(byte[] o) {
    return compareBytes(name, o);
  }

  public boolean equals(Object o) {
    if (!(o instanceof INode)) {
      return false;
    }
    return Arrays.equals(this.name, ((INode)o).name);
  }

  public int hashCode() {
    return Arrays.hashCode(this.name);
  }

  //
  // static methods
  //
  /**
   * Compare two byte arrays.
   *
   * @return a negative integer, zero, or a positive integer
   * as defined by {@link #compareTo(byte[])}.
   */
  static int compareBytes(byte[] a1, byte[] a2) {
    if (a1==a2)
        return 0;
    int len1 = (a1==null ? 0 : a1.length);
    int len2 = (a2==null ? 0 : a2.length);
    int n = Math.min(len1, len2);
    byte b1, b2;
    for (int i=0; i<n; i++) {
      b1 = a1[i];
      b2 = a2[i];
      if (b1 != b2)
        return b1 - b2;
    }
    return len1 - len2;
  }

  /**
   * Converts a byte array to a string using UTF8 encoding.
   */
  static String bytes2String(byte[] bytes) {
    try {
      return new String(bytes, "UTF8");
    } catch(UnsupportedEncodingException e) {
      assert false : "UTF8 encoding is not supported ";
    }
    return null;
  }

  /**
   * Converts a string to a byte array using UTF8 encoding.
   */
  static byte[] string2Bytes(String str) {
    try {
      return str.getBytes("UTF8");
    } catch(UnsupportedEncodingException e) {
      assert false : "UTF8 encoding is not supported ";
    }
    return null;
  }


  public LocatedBlocks createLocatedBlocks(ArrayList<LocatedBlock> blocks) {
    return new LocatedBlocks(computeContentSummary().getLength(), blocks,
        isUnderConstruction());
  }

//appended //
  public abstract boolean isHere();

  public int getNamenodeID() {
	  return this.namenodeID;
  }

  public void setNamenodeID(int nID) {
	  this.namenodeID = nID;
  }

  public List<Integer> getINodeLocations() {
	  return iNodeLocations;
  }

  public void addINodeLocation(int nID) {
	  iNodeLocations.add(new Integer(nID));
  }
  public void removeINodeLocation(int nID) {
	  iNodeLocations.remove(new Integer(nID));
  }

  public long getPermission() {
	  return permission;
  }

  public final INodeInfo getInfo() {
		if(this instanceof INodeDirectory)
			return new INodeDirectoryInfo((INodeDirectory) this);
		if(this instanceof INodeFile)
			return new INodeFileInfo((INodeFile) this);
		if(this instanceof INodeDirectoryWithQuota)
			return new INodeDirectoryWithQuotaInfo((INodeDirectoryWithQuota) this);
		if(this instanceof INodeFileUnderConstruction)
			return new INodeFileUnderConstructionInfo((INodeFileUnderConstruction) this);
		else return null;
  }

  public void setHere() {
	  Configuration conf = new Configuration();
	  int i = conf.getInt("dfs.namenode.id", 0);
		  namenodeID = i;
	  if(iNodeLocations == null) {
		  iNodeLocations = new ArrayList<Integer>();
		  //iNodeLocations.add(new Integer(i));
	  }
  }

}
