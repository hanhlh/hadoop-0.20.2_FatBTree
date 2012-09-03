/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.PropertyArranger;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;


/**
 * @author hanhlh
 *
 */
public class FBTInitializeHelper {

	private int _selfPartID;

    private int _fanout;

    private List<String> _partitions;

    private List<List<String>> _splitPartitionsList;

    private List<String> _keys;

    private List<Pointer> _children;

    private List<PointerSet> _pointerSets;

    private String _highKey;

    private boolean _isLeftest;

    private boolean _isRightest;

    private int _ownerID;

    public FBTInitializeHelper(int partID, int fanout,
    					Configuration conf, int ownerID) {
    	_ownerID = ownerID;
        _selfPartID = partID;
        _fanout = fanout;
        //<name>partition</name>
        //<value>edn{19-20}</value>
        String partition = conf.get("partition");
        _partitions = PropertyArranger.arrangeSerialName(partition);

        _splitPartitionsList = new LinkedList<List<String>>();
        Iterator<String> partIter = _partitions.iterator();
        while (partIter.hasNext()) {
            List<String> splitPart = new LinkedList<String>();
            splitPart.add(partIter.next());
            _splitPartitionsList.add(splitPart);
        }

        _highKey = null;
        _isLeftest = false;
        _isRightest = false;
    }

    public FBTInitializeHelper(int partID, int fanout,
    		List<EndPoint> endPoints, int ownerID) {
    	System.out.println("Helper.Helper() selfPartID: "+partID);
    	_ownerID = ownerID;
        _selfPartID = partID;
        _fanout = fanout;

        _partitions = PropertyArranger.getPartitions(endPoints);

        for (String partition:_partitions) {
        	System.out.println("partition "+partition);
        }

        _splitPartitionsList = new LinkedList<List<String>>();
        Iterator<String> partIter = _partitions.iterator();
        while (partIter.hasNext()) {
            List<String> splitPart = new LinkedList<String>();
            splitPart.add(partIter.next());
            _splitPartitionsList.add(splitPart);
        }

        _highKey = null;
        _isLeftest = false;
        _isRightest = false;
    }

    public int getOwnerID() {
    	return _ownerID;
    }
    public String getHighKey() {
        return _highKey;
    }

    public boolean isLeftest() {
        return _isLeftest;
    }

    public boolean isRightest() {
        return _isRightest;
    }

    public boolean isEnough(int height) {
        return _partitions.size() > Math.pow(_fanout, height);
    }
    public int partitionSize() {
    	return _partitions.size();
    }

    public int getLeftPartID() {
        for (int i = _partitions.size() - 1; i > 0; i--) {
            if (Integer.parseInt(_partitions.get(i)) ==
            	//_selfPartID
            	_ownerID) {
                return Integer.parseInt(_partitions.get(i - 1));
            }
        }

        return Integer.MIN_VALUE;
    }

    public int getRightPartID() {
        for (int i = 0; i < _partitions.size() - 1; i++) {
            if (Integer.parseInt(_partitions.get(i)) ==
            	//_selfPartID
            	_ownerID) {
                return Integer.parseInt(_partitions.get(i + 1));
            }
        }

        return Integer.MIN_VALUE;
    }
    public Iterator<String> getKeyIterator() {
        return _keys.iterator();
    }

    public Iterator<Pointer> getChildIterator() {
        return _children.iterator();
    }

    public Iterator<PointerSet> getPointerSetIterator() {
        return _pointerSets.iterator();
    }

    public void setParentEntry(IndexNode index) {
    	List<List<String>> includedList = getIncludedList();

        _keys = new LinkedList<String>();
        _children = new LinkedList<Pointer>();
        _pointerSets = new LinkedList<PointerSet>();
        Iterator<List<String>> sPartIter = includedList.iterator();
        _keys.add(FBTInitializeVisitor._experimentDir);
        while (sPartIter.hasNext()) {
            PointerSet pSet = new PointerSet();

            Iterator<String> partIter = sPartIter.next().iterator();
            while (partIter.hasNext()) {
                int partID = Integer.parseInt(partIter.next());
                pSet.add(new Pointer(partID,
                				index.getNodeIdentifier().toString()));
            }

            int partID = pSet.firstElement().getPartitionID();
            _keys.add(StringUtility.generateDefaultSeparator(partID, _partitions.size(),
            		FBTInitializeVisitor._experimentDir,
            		FBTInitializeVisitor._low,
            		FBTInitializeVisitor._high));

            Pointer p = pSet.getPointer(_selfPartID);
            if (p == null) {
                p = pSet.get(_selfPartID % pSet.size());
            }
            _children.add(p);

            _pointerSets.add(pSet);
        }
    }

    public void setParentEntry(LeafNode leaf) {
        List<List<String>> includedList = getIncludedList();

        _keys = new LinkedList<String>();
        _children = new LinkedList<Pointer>();
        _pointerSets = new LinkedList<PointerSet>();
        Iterator<List<String>> sPartIter = includedList.iterator();
        _keys.add(FBTInitializeVisitor._experimentDir);
        while (sPartIter.hasNext()) {
            List<String> sPart = sPartIter.next();
            //System.out.println("sPart");
            /*for (String sPartString:sPart) {
            	System.out.println(sPartString);
            }*/
            int partID = Integer.parseInt(sPart.get(0));
            //_keys.add(StringUtility.toMaxRadixTwoDigitString(partID));

           	_keys.add(StringUtility.generateDefaultSeparator(partID, _partitions.size(),
           			FBTInitializeVisitor._experimentDir,
            		FBTInitializeVisitor._low,
            		FBTInitializeVisitor._high));

            Pointer p = new Pointer(partID,
            				leaf.getNodeIdentifier().toString());
            _children.add(p);

            PointerSet pSet = new PointerSet();
            pSet.add(p);
            _pointerSets.add(pSet);
        }

        //splitPartitions(leaf.getNodeID());
        System.out.println("FBTInitializeHelper setParentEntry leaf done");
    }

    public void setParentEntryNew(LeafNode leaf) {
    	System.out.println(FBTDirectory.SPACE);
    	System.out.println("FBTInitializeHelper setParentEntryNew "+leaf.getNodeNameID());
        List<List<String>> includedList = getIncludedList();
        for (List<String> includedListStr : includedList) {
        	for (String str:includedListStr) {
        		System.out.println(str);
        	}
        }
        _keys = new LinkedList<String>();
        _children = new LinkedList<Pointer>();
        _pointerSets = new LinkedList<PointerSet>();
        Iterator<List<String>> sPartIter = includedList.iterator();
        while (sPartIter.hasNext()) {
            List<String> sPart = sPartIter.next();
            for (String sPartString:sPart) {
            	System.out.println(sPartString);
            }
            int partID = Integer.parseInt(sPart.get(0));
            //_keys.add(StringUtility.toMaxRadixTwoDigitString(partID));
            _keys.add(StringUtility.generateDefaultSeparator(partID, _partitions.size(),
            		FBTInitializeVisitor._experimentDir,
            		FBTInitializeVisitor._low,
            		FBTInitializeVisitor._high));

            Pointer p = new Pointer(partID,
            					leaf.getNodeIdentifier().toString());
            _children.add(p);

            PointerSet pSet = new PointerSet();
            pSet.add(p);
            _pointerSets.add(pSet);
        }

        splitPartitions(leaf.getNodeID());
    }

    public List<List<String>> getIncludedList() {
        int[] fillings = getFillings(_splitPartitionsList.size());

        int count = 0;
        int index = 0;
        List<List<String>> includedList = new LinkedList<List<String>>();
        Iterator<List<String>> sPartIter = _splitPartitionsList.iterator();
        while (sPartIter.hasNext()) {
            List<String> sPart = sPartIter.next();
            includedList.add(sPart);
            count++;

            if (count == fillings[index]) {
                if (contain(includedList)) {
                    break;
                }

                count = 0;
                index++;
                includedList = new LinkedList<List<String>>();
            }
        }

        if (index == 0) {
            _isLeftest = true;
        } else {
            _isLeftest = false;
        }
        if (sPartIter.hasNext()) {
            int rightPartID = Integer.parseInt(sPartIter.next().get(0));
            _highKey = StringUtility.toMaxRadixTwoDigitString(rightPartID);
            _isRightest = false;
        } else {
            char max = Character.MAX_VALUE;
            _highKey = String.valueOf(max);
            _isRightest = true;
        }

        return includedList;
    }

    private boolean contain(List<List<String>> includedList) {
        Iterator<List<String>> sPartIter = includedList.iterator();
        while (sPartIter.hasNext()) {
            List<String> sPart = sPartIter.next();
            Iterator<String> partIter = sPart.iterator();
            while (partIter.hasNext()) {
                if (_selfPartID == Integer.parseInt(partIter.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void splitPartitions(long nodeID) {
    	StringUtility.debugSpace("splitPartitions");

        double listSize = _splitPartitionsList.size();
        int[] setFillings = getFillings(listSize);

        int[] fillings = new int[(int) Math.ceil(listSize / _fanout)];
        for (int i = 0; i < fillings.length; i++) {
            fillings[i] = 0;
        }

        int count = 0;
        int index = 0;
        Iterator<List<String>> sPartIter = _splitPartitionsList.iterator();
        while (sPartIter.hasNext()) {
            fillings[index] = fillings[index] + sPartIter.next().size();
            count++;

            if (count == setFillings[index]) {
                count = 0;
                index++;
            }
        }

        _splitPartitionsList = new LinkedList<List<String>>();
        for (int i = 0; i < fillings.length; i++) {
            _splitPartitionsList.add(new LinkedList<String>());
        }

        count = 0;
        Iterator<String> partIter = _partitions.iterator();
        while (partIter.hasNext()) {
            _splitPartitionsList.get(count).add(partIter.next());
            if (_splitPartitionsList.get(count).size() == fillings[count]) {
                count++;
            }
        }
        System.out.println("splitPartitions done");
    }

    private int[] getFillings(double size) {
        int[] fillings = new int[(int) Math.ceil(size / _fanout)];
        for (int i = 0; i < fillings.length; i++) {
            fillings[i] = 0;
        }
        for (int i = 0; i < size; i++) {
            fillings[i % fillings.length]++;
        }

        return fillings;
    }

}
