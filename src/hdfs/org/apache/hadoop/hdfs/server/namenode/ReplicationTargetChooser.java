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

import org.apache.commons.logging.*;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.net.NodeBase;
import java.util.*;

/** The class is responsible for choosing the desired number of targets
 * for placing block replicas.
 * The replica placement strategy is that if the writer is on a datanode,
 * the 1st replica is placed on the local machine,
 * otherwise a random datanode. The 2nd replica is placed on a datanode
 * that is on a different rack. The 3rd replica is placed on a datanode
 * which is on the same rack as the first replca.
 */
public class ReplicationTargetChooser {
  private final boolean considerLoad;
  private NetworkTopology clusterMap;
  private FSNamesystem fs;
  //append
  private FBTDirectory fbtdirectory;
  public ReplicationTargetChooser(boolean considerLoad,  FSNamesystem fs,
                           NetworkTopology clusterMap) {
    this.considerLoad = considerLoad;
    this.fs = fs;
    this.clusterMap = clusterMap;
  }

  public ReplicationTargetChooser(boolean considerLoad,  FBTDirectory fbt,
          NetworkTopology clusterMap) {
	this.considerLoad = considerLoad;
	this.fbtdirectory = fbt;
	this.clusterMap = clusterMap;
  }
  private static class NotEnoughReplicasException extends Exception {
    NotEnoughReplicasException(String msg) {
      super(msg);
    }
  }

  public NetworkTopology getClusterMap() {
	  return clusterMap;
  }
  /**
   * choose <i>numOfReplicas</i> data nodes for <i>writer</i> to replicate
   * a block with size <i>blocksize</i>
   * If not, return as many as we can.
   *
   * @param numOfReplicas: number of replicas wanted.
   * @param writer: the writer's machine, null if not in the cluster.
   * @param excludedNodes: datanodesthat should not be considered targets.
   * @param blocksize: size of the data to be written.
   * @return array of DatanodeDescriptor instances chosen as targets
   * and sorted as a pipeline.
   */
  public DatanodeDescriptor[] chooseTarget(int numOfReplicas,
                                    DatanodeDescriptor writer,
                                    List<Node> excludedNodes,
                                    long blocksize) {
    if (excludedNodes == null) {
      excludedNodes = new ArrayList<Node>();
    }

    return chooseTarget(numOfReplicas, writer,
                        new ArrayList<DatanodeDescriptor>(), excludedNodes, blocksize);
  }

  /**
   * choose <i>numOfReplicas</i> data nodes for <i>writer</i>
   * to re-replicate a block with size <i>blocksize</i>
   * If not, return as many as we can.
   *
   * @param numOfReplicas: additional number of replicas wanted.
   * @param writer: the writer's machine, null if not in the cluster.
   * @param choosenNodes: datanodes that have been choosen as targets.
   * @param excludedNodes: datanodesthat should not be considered targets.
   * @param blocksize: size of the data to be written.
   * @return array of DatanodeDescriptor instances chosen as target
   * and sorted as a pipeline.
   */
  DatanodeDescriptor[] chooseTarget(int numOfReplicas,
                                    DatanodeDescriptor writer,
                                    List<DatanodeDescriptor> choosenNodes,
                                    List<Node> excludedNodes,
                                    long blocksize) {
    if (numOfReplicas == 0 || clusterMap.getNumOfLeaves()==0) {
      return new DatanodeDescriptor[0];
    }

    if (excludedNodes == null) {
      excludedNodes = new ArrayList<Node>();
    }

    int clusterSize = clusterMap.getNumOfLeaves();
    int totalNumOfReplicas = choosenNodes.size()+numOfReplicas;
    if (totalNumOfReplicas > clusterSize) {
      numOfReplicas -= (totalNumOfReplicas-clusterSize);
      totalNumOfReplicas = clusterSize;
    }

    int maxNodesPerRack =
      (totalNumOfReplicas-1)/clusterMap.getNumOfRacks()+2;
    //System.out.println("maxNodesPerRack "+maxNodesPerRack);

    List<DatanodeDescriptor> results =
      new ArrayList<DatanodeDescriptor>(choosenNodes);
    excludedNodes.addAll(choosenNodes);

    if (!clusterMap.contains(writer)) {
      writer=null;
    }

    DatanodeDescriptor localNode = chooseTarget(numOfReplicas, writer,
                                                excludedNodes, blocksize, maxNodesPerRack, results);

    results.removeAll(choosenNodes);

    // sorting nodes to form a pipeline
    return getPipeline((writer==null)?localNode:writer,
                       results.toArray(new DatanodeDescriptor[results.size()]));
  }

  /* choose <i>numOfReplicas</i> from all data nodes */
  private DatanodeDescriptor chooseTarget(int numOfReplicas,
                                          DatanodeDescriptor writer,
                                          List<Node> excludedNodes,
                                          long blocksize,
                                          int maxNodesPerRack,
                                          List<DatanodeDescriptor> results) {
	  StringUtility.debugSpace("ReplicationTargetChooser chooseTarget 148");
    if (numOfReplicas == 0 || clusterMap.getNumOfLeaves()==0) {
      return writer;
    }

    int numOfResults = results.size();
    System.out.println("numOfResults "+numOfResults);
    boolean newBlock = (numOfResults==0);
    if (writer == null && !newBlock) {
      writer = (DatanodeDescriptor)results.get(0);
      System.out.println("writer "+writer.name);
    }

    try {
      switch(numOfResults) {
      case 0:
    	  System.out.println("case 0");
        writer = chooseLocalNode(writer, excludedNodes,
                                 blocksize, maxNodesPerRack, results);
        if (--numOfReplicas == 0) {
          break;
        }
      case 1:
    	  System.out.println("case 1");
        chooseRemoteRack(1, results.get(0), excludedNodes,
                         blocksize, maxNodesPerRack, results);
        if (--numOfReplicas == 0) {
          break;
        }
      case 2:
    	  System.out.println("case 2");
        if (clusterMap.isOnSameRack(results.get(0), results.get(1))) {
          chooseRemoteRack(1, results.get(0), excludedNodes,
                           blocksize, maxNodesPerRack, results);
        } else if (newBlock){
          chooseLocalRack(results.get(1), excludedNodes, blocksize,
                          maxNodesPerRack, results);
        } else {
          chooseLocalRack(writer, excludedNodes, blocksize,
                          maxNodesPerRack, results);
        }
        if (--numOfReplicas == 0) {
          break;
        }
      default:
        chooseRandom(numOfReplicas, NodeBase.ROOT, excludedNodes,
                     blocksize, maxNodesPerRack, results);
      }
    } catch (NotEnoughReplicasException e) {
      FSNamesystem.LOG.warn("Not able to place enough replicas, still in need of "
               + numOfReplicas);
    }
    return writer;
  }

  /* choose <i>localMachine</i> as the target.
   * if <i>localMachine</i> is not availabe,
   * choose a node on the same rack
   * @return the choosen node
   */
  private DatanodeDescriptor chooseLocalNode(
                                             DatanodeDescriptor localMachine,
                                             List<Node> excludedNodes,
                                             long blocksize,
                                             int maxNodesPerRack,
                                             List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
	  StringUtility.debugSpace("ReplicationTargetChooser line 223");
    // if no local machine, randomly choose one node
    if (localMachine == null) {
    	System.out.println("localMachine null");
    	return chooseRandom(NodeBase.ROOT, excludedNodes,
                          blocksize, maxNodesPerRack, results);
    }

    // otherwise try local machine first
    if (!excludedNodes.contains(localMachine)) {
      excludedNodes.add(localMachine);
      if (isGoodTarget(localMachine, blocksize,
                       maxNodesPerRack, false, results)) {
        results.add(localMachine);
        return localMachine;
      }
    }

    // try a node on local rack
    return chooseLocalRack(localMachine, excludedNodes,
                           blocksize, maxNodesPerRack, results);
  }

  /* choose one node from the rack that <i>localMachine</i> is on.
   * if no such node is availabe, choose one node from the rack where
   * a second replica is on.
   * if still no such node is available, choose a random node
   * in the cluster.
   * @return the choosen node
   */
  private DatanodeDescriptor chooseLocalRack(
                                             DatanodeDescriptor localMachine,
                                             List<Node> excludedNodes,
                                             long blocksize,
                                             int maxNodesPerRack,
                                             List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
    // no local machine, so choose a random machine
    if (localMachine == null) {
      return chooseRandom(NodeBase.ROOT, excludedNodes,
                          blocksize, maxNodesPerRack, results);
    }

    // choose one from the local rack
    try {
      return chooseRandom(
                          localMachine.getNetworkLocation(),
                          excludedNodes, blocksize, maxNodesPerRack, results);
    } catch (NotEnoughReplicasException e1) {
      // find the second replica
      DatanodeDescriptor newLocal=null;
      for(Iterator<DatanodeDescriptor> iter=results.iterator();
          iter.hasNext();) {
        DatanodeDescriptor nextNode = iter.next();
        if (nextNode != localMachine) {
          newLocal = nextNode;
          break;
        }
      }
      if (newLocal != null) {
        try {
          return chooseRandom(
                              newLocal.getNetworkLocation(),
                              excludedNodes, blocksize, maxNodesPerRack, results);
        } catch(NotEnoughReplicasException e2) {
          //otherwise randomly choose one from the network
          return chooseRandom(NodeBase.ROOT, excludedNodes,
                              blocksize, maxNodesPerRack, results);
        }
      } else {
        //otherwise randomly choose one from the network
        return chooseRandom(NodeBase.ROOT, excludedNodes,
                            blocksize, maxNodesPerRack, results);
      }
    }
  }

  /* choose <i>numOfReplicas</i> nodes from the racks
   * that <i>localMachine</i> is NOT on.
   * if not enough nodes are availabe, choose the remaining ones
   * from the local rack
   */

  private void chooseRemoteRack(int numOfReplicas,
                                DatanodeDescriptor localMachine,
                                List<Node> excludedNodes,
                                long blocksize,
                                int maxReplicasPerRack,
                                List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
    int oldNumOfReplicas = results.size();
    // randomly choose one node from remote racks
    try {
      chooseRandom(numOfReplicas, "~"+localMachine.getNetworkLocation(),
                   excludedNodes, blocksize, maxReplicasPerRack, results);
    } catch (NotEnoughReplicasException e) {
      chooseRandom(numOfReplicas-(results.size()-oldNumOfReplicas),
                   localMachine.getNetworkLocation(), excludedNodes, blocksize,
                   maxReplicasPerRack, results);
    }
  }

  /* Randomly choose one target from <i>nodes</i>.
   * @return the choosen node
   */
  private DatanodeDescriptor chooseRandom(
                                          String nodes,
                                          List<Node> excludedNodes,
                                          long blocksize,
                                          int maxNodesPerRack,
                                          List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
	  StringUtility.debugSpace("ReplicationTargetChooser.chooseRandom line 335");
    DatanodeDescriptor result;
    do {
      DatanodeDescriptor[] selectedNodes =
        chooseRandom(1, nodes, excludedNodes);
      if (selectedNodes.length == 0) {
        throw new NotEnoughReplicasException(
                                             "Not able to place enough replicas");
      }
      result = (DatanodeDescriptor)(selectedNodes[0]);
    } while(!isGoodTarget(result, blocksize, maxNodesPerRack, results));
    System.out.println("result: "+result.getHostName());
    results.add(result);
    return result;
  }

  /* Randomly choose <i>numOfReplicas</i> targets from <i>nodes</i>.
   */
  private void chooseRandom(int numOfReplicas,
                            String nodes,
                            List<Node> excludedNodes,
                            long blocksize,
                            int maxNodesPerRack,
                            List<DatanodeDescriptor> results)
    throws NotEnoughReplicasException {
	  StringUtility.debugSpace("ReplicationTargetChooser.chooseRandom() line 359");
    boolean toContinue = true;
    do {
      DatanodeDescriptor[] selectedNodes =
        chooseRandom(numOfReplicas, nodes, excludedNodes);
      System.out.println("selectedNodes.length "+selectedNodes.length);
      if (selectedNodes.length < numOfReplicas) {
        toContinue = false;
      }
      for(int i=0; i<selectedNodes.length; i++) {
        DatanodeDescriptor result = selectedNodes[i];
        //checkout target is good or not because of setting no datanode report
        //if (isGoodTarget(result, blocksize, maxNodesPerRack, results)) {
          numOfReplicas--;
          results.add(result);
       //}
      } // end of for
    } while (numOfReplicas>0 && toContinue);

    if (numOfReplicas>0) {
      throw new NotEnoughReplicasException(
                                           "Not able to place enough replicas");
    }
  }

  /* Randomly choose <i>numOfNodes</i> nodes from <i>scope</i>.
   * @return the choosen nodes
   */
  private DatanodeDescriptor[] chooseRandom(int numOfReplicas,
                                            String nodes,
                                            List<Node> excludedNodes) {
	  StringUtility.debugSpace("ReplicationTargetChooser.chooseRandom() 388");
	  System.out.println("excludedNodes size "+excludedNodes.size());
	  System.out.println("numOfReplicas 390 "+numOfReplicas);
    List<DatanodeDescriptor> results =
      new ArrayList<DatanodeDescriptor>();
    int numOfAvailableNodes =
      clusterMap.countNumOfAvailableNodes(nodes, excludedNodes);
    numOfReplicas = (numOfAvailableNodes<numOfReplicas)?
      numOfAvailableNodes:numOfReplicas;
    System.out.println("numOfReplicas 397 "+numOfReplicas);
    while(numOfReplicas > 0) {
      DatanodeDescriptor choosenNode =
        (DatanodeDescriptor)(clusterMap.chooseRandom(nodes));
      System.out.println("choosenNode "+choosenNode.name);
      if (!excludedNodes.contains(choosenNode)) {
    	  System.out.println("add choosenNode ");
        results.add(choosenNode);
        excludedNodes.add(choosenNode);
        numOfReplicas--;
      }
    }
    return (DatanodeDescriptor[])results.toArray(
                                                 new DatanodeDescriptor[results.size()]);
  }

  /* judge if a node is a good target.
   * return true if <i>node</i> has enough space,
   * does not have too much load, and the rack does not have too many nodes
   */
  private boolean isGoodTarget(DatanodeDescriptor node,
                               long blockSize, int maxTargetPerLoc,
                               List<DatanodeDescriptor> results) {
    return isGoodTarget(node, blockSize, maxTargetPerLoc,
                        this.considerLoad, results);
  }

  private boolean isGoodTarget(DatanodeDescriptor node,
                               long blockSize, int maxTargetPerLoc,
                               boolean considerLoad,
                               List<DatanodeDescriptor> results) {
	  StringUtility.debugSpace("ReplicationTargetChooser.isGoodTarget()");
    Log logr = FSNamesystem.LOG;
    // check if the node is (being) decommissed
    if (node.isDecommissionInProgress() || node.isDecommissioned()) {
    	System.out.println("line 434");
      logr.debug("Node "+NodeBase.getPath(node)+
                " is not chosen because the node is (being) decommissioned");
      return false;
    }

    long remaining = node.getRemaining() -
                     (node.getBlocksScheduled() * blockSize);
    System.out.println("remaining "+remaining);
    // check the remaining capacity of the target machine
    if (blockSize* FSConstants.MIN_BLOCKS_FOR_WRITE>remaining) {
    	System.out.println("line 444");
      logr.debug("Node "+NodeBase.getPath(node)+
                " is not chosen because the node does not have enough space");
      //return false;
    }

    System.out.println("considerLoad "+considerLoad);
    // check the communication traffic of the target machine
    if (considerLoad) {
      double avgLoad = 0;
      int size = clusterMap.getNumOfLeaves();
      if (size != 0) {
        avgLoad = (double)fs.getTotalLoad()/size;
        System.out.println("avgLoad 457  "+avgLoad);
      }
      System.out.println("avgLoad 459 "+avgLoad);
      if (node.getXceiverCount() > (2.0 * avgLoad)) {
        logr.debug("Node "+NodeBase.getPath(node)+
                  " is not chosen because the node is too busy");
        return false;
      }
    }

    // check if the target rack has chosen too many nodes
    String rackname = node.getNetworkLocation();
    int counter=1;
    for(Iterator<DatanodeDescriptor> iter = results.iterator();
        iter.hasNext();) {
      Node result = iter.next();
      if (rackname.equals(result.getNetworkLocation())) {
        counter++;
      }
    }
    if (counter>maxTargetPerLoc) {
    	System.out.println("478");
      logr.debug("Node "+NodeBase.getPath(node)+
                " is not chosen because the rack has too many chosen nodes");
      return false;
    }
    return true;
  }

  /* Return a pipeline of nodes.
   * The pipeline is formed finding a shortest path that
   * starts from the writer and tranverses all <i>nodes</i>
   * This is basically a traveling salesman problem.
   */
  private DatanodeDescriptor[] getPipeline(
                                           DatanodeDescriptor writer,
                                           DatanodeDescriptor[] nodes) {
    if (nodes.length==0) return nodes;

    synchronized(clusterMap) {
      int index=0;
      if (writer == null || !clusterMap.contains(writer)) {
        writer = nodes[0];
      }
      for(;index<nodes.length; index++) {
        DatanodeDescriptor shortestNode = nodes[index];
        int shortestDistance = clusterMap.getDistance(writer, shortestNode);
        int shortestIndex = index;
        for(int i=index+1; i<nodes.length; i++) {
          DatanodeDescriptor currentNode = nodes[i];
          int currentDistance = clusterMap.getDistance(writer, currentNode);
          if (shortestDistance>currentDistance) {
            shortestDistance = currentDistance;
            shortestNode = currentNode;
            shortestIndex = i;
          }
        }
        //switch position index & shortestIndex
        if (index != shortestIndex) {
          nodes[shortestIndex] = nodes[index];
          nodes[index] = shortestNode;
        }
        writer = shortestNode;
      }
    }
    return nodes;
  }

  /**
   * Verify that the block is replicated on at least 2 different racks
   * if there is more than one rack in the system.
   *
   * @param lBlk block with locations
   * @param cluster
   * @return 1 if the block must be relicated on additional rack,
   * or 0 if the number of racks is sufficient.
   */
  public static int verifyBlockPlacement(LocatedBlock lBlk,
                                         short replication,
                                         NetworkTopology cluster) {
    int numRacks = verifyBlockPlacement(lBlk, Math.min(2,replication), cluster);
    return numRacks < 0 ? 0 : numRacks;
  }

  /**
   * Verify that the block is replicated on at least minRacks different racks
   * if there is more than minRacks rack in the system.
   *
   * @param lBlk block with locations
   * @param minRacks number of racks the block should be replicated to
   * @param cluster
   * @return the difference between the required and the actual number of racks
   * the block is replicated to.
   */
  public static int verifyBlockPlacement(LocatedBlock lBlk,
                                         int minRacks,
                                         NetworkTopology cluster) {
    DatanodeInfo[] locs = lBlk.getLocations();
    if (locs == null)
      locs = new DatanodeInfo[0];
    int numRacks = cluster.getNumOfRacks();
    if(numRacks <= 1) // only one rack
      return 0;
    minRacks = Math.min(minRacks, numRacks);
    // 1. Check that all locations are different.
    // 2. Count locations on different racks.
    Set<String> racks = new TreeSet<String>();
    for (DatanodeInfo dn : locs)
      racks.add(dn.getNetworkLocation());
    return minRacks - racks.size();
  }



} //end of Replicator

