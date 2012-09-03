/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;

/**
 * @author hanhlh
 *
 */
public class SynchronizeRootRequest extends Request {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//correct information _updateChild = (sourcePartID, right NodeID)
	private final VPointer _updateChild;

	private final int  _partID;

	private final String _directoryName;

	private final int _position;

	public SynchronizeRootRequest(String directoryName, int partID,
									VPointer updateChild, int position) {
		super(updateChild);
		_directoryName = directoryName;
		_updateChild = updateChild;
		_partID = partID;
		_position = position;
	}

	public SynchronizeRootRequest(int partID,
			VPointer updateChild, int position) {
		this(FBTDirectory.DEFAULT_NAME, partID, updateChild, position);
	}

	public VPointer getUpdateChild() {
		return _updateChild;
	}
	public int getPartID() {
		return _partID;
	}

	public String getDirectoryName() {
		return _directoryName;
	}

	public int getPosition() {
		return _position;
	}

}
