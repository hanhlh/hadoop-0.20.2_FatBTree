/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import java.io.ObjectStreamField;


import org.apache.hadoop.hdfs.server.namenodeFBT.Request;
import org.apache.hadoop.hdfs.server.namenodeFBT.VPointer;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;

/**
 * @author hanhlh
 *
 */
public abstract class TargetNodeRequest extends Request{

/**
	 *
	 */
	private static final long serialVersionUID = 1L;

private static final ObjectStreamField[] serialPersistentFields;

    static {
        serialPersistentFields = new ObjectStreamField[] {
                new ObjectStreamField("_target", VPointer.class, true)
        };
    }

    // instance attributes ////////////////////////////////////////////////////

    /**
     * 要求に対する処理が行われる最上位の node を指すポインタ.
     * null のときは MetaNode から処理を行われる.
     */
    private VPointer _target;

    private transient EndPoint _destination;

    // constructors ///////////////////////////////////////////////////////////

    public TargetNodeRequest() {
        this(null);
    }

    public TargetNodeRequest(VPointer target) {
        super();
        _target = target;
    }

    // accessors //////////////////////////////////////////////////////////////

    public VPointer getTarget() {
        return _target;
    }

    public void setTarget(VPointer target) {
        _target = target;
    }

    public EndPoint getDestination() {
        return _destination;
    }

    public void setDestination(EndPoint destination) {
        _destination = destination;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(", target = ");
        buf.append(_target);

        return buf.toString();
    }

}
