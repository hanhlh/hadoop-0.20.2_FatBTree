/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


/**
 * @author hanhlh
 *
 */
public abstract class AbstractNode implements Node{
		/**Directory that manages this node*/
	protected  FBTDirectory _directory;

	/**Cache that stores the returned value from getPointer()*/
	private  Pointer _pointer;
	//NodeName = NodeType + NodeID
	private  String _nodeName;

	// constructors ///////////////////////////////////////////////////////////

    /**
     * <p>Generate new node belonging to defined FBTDirectory.</p>
     *
     * @param directory
     */
    public AbstractNode(FBTDirectory directory) {
        _directory = directory;
        _pointer = null;
        System.out.println("generate node "+getNodeIdentifier());
    }

    public AbstractNode() {}
 // instance methods ///////////////////////////////////////////////////////

    public String toString() {
        String name = getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);

        //return "[page " + getPageID() + "] " + name;
        return name;
    }

 // interface Node /////////////////////////////////////////////////////////


    public void initOwner(FBTDirectory directory) {
        _directory = directory;
        _pointer = null;
        //_nodeType = null;
    }

    public VPointer getPointer() {
        if (_pointer == null) {
        	_pointer = new Pointer(_directory.getPartitionID(),
            					getNodeIdentifier().toString());
        }
        return _pointer;
    }

    public long initNodeID() {
    	return _directory.getNodeSequence().getAndIncrement();

    }

    public void setNodeID(long nodeID) {
    	getNodeIdentifier().setNodeID(nodeID);
    }

    public void setOwner(String owner) {
    	getNodeIdentifier().setOwner(owner);
    }

    public long getNodeID() {
    	return getNodeIdentifier().getNodeID();
    }
    public void free() {
    	synchronized (_directory.getLocalNodeMapping()) {
    		_directory.getLocalNodeMapping().remove(getNodeIdentifier().toString());
    	}
    }
}
