/**
 *
 */
package org.apache.hadoop.hdfs.server.namenode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;


/**
 * @author hanhlh
 *
 */
public class ReplicationTargetChooserHashDataPlacement
			extends ReplicationTargetChooser {


	@SuppressWarnings("unused")
	private Map<String[], String> _datanodeRangeMapping;
	private int _currentGear = 1;

	private String _src = null;

	private Map<Integer, ArrayList<String>> _gearToActivateNodeMapping;

	private Map<String, String[]> _backUpMapping;

	public ReplicationTargetChooserHashDataPlacement(boolean considerLoad,
			FSNamesystem namesystem, NetworkTopology clusterMap,
			Map<String[], String> datanodeRangeMapping,
			Map<Integer, ArrayList<String>> gearToActivateNodeMapping,
			Map<String, String[]> backUpMapping) {
		super(considerLoad, namesystem, clusterMap);
		_datanodeRangeMapping = datanodeRangeMapping;
		_gearToActivateNodeMapping = gearToActivateNodeMapping;
		_backUpMapping = backUpMapping;

	}

	public void setDatanodeRangeMapping(Map<String[], String> drm) {
		_datanodeRangeMapping = drm;
	}

	public void setCurrentGear(int gear) {
		_currentGear = gear;
	}

	public void setSrc(String src) {
		_src = src;
	}

	public void setGearToActivateNodeMapping(Map<Integer, ArrayList<String>> gearMap) {
		_gearToActivateNodeMapping = gearMap;
	}

	public void setBackUpMapping(Map<String, String[]> bum) {
		_backUpMapping = bum;
	}
	public DatanodeDescriptor[] chooseTarget(int numOfReplicas,
            DatanodeDescriptor writer,
            List<Node> excludedNodes,
            long blocksize) {
		if (excludedNodes == null) {
			excludedNodes = new ArrayList<Node>();
		}
		DatanodeDescriptor[] target = new DatanodeDescriptor[numOfReplicas];
		String targetName = _datanodeRangeMapping.get(getRange(_src));
		if (!_gearToActivateNodeMapping.get(_currentGear).
										contains(targetName)) {
			targetName = _backUpMapping.get(targetName)[0]; //1st replica
		}

		NameNode.LOG.info("selected targetName: "+targetName);
		NameNode.LOG.info("clusterMap: "+getClusterMap().toString());
		target[0] = (DatanodeDescriptor) getClusterMap().getNode("/default-rack/192.168.0."+
								(100+Integer.valueOf(targetName.substring(targetName.length()-2)))
								+":50010");
		NameNode.LOG.info(target[0].getName());
		return target;
	}

	private String[] getRange(String src) {
		String[] range = new String[2];
		Set<String[]> keySet = _datanodeRangeMapping.keySet();
		for (Iterator iter = (Iterator) keySet.iterator(); iter.hasNext();) {
			String[] key = (String[]) iter.next();
			if ((key[0].compareTo(src)<=0) && (src.compareTo(key[1])<=0)) {
				range = key;
				break;
			}
		}
		return range;
	}

	//edn14
	public String getDatanodeName(String src) {
		return _datanodeRangeMapping.get(getRange(src));
	}

	public String getDatanodeName(String src, int gear) {
		String primaryDatanodeName = getDatanodeName(src);
		return _backUpMapping.get(primaryDatanodeName)[gear-1];
	}
}
