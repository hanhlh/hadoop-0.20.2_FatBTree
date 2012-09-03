/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.markopt;

import org.apache.hadoop.hdfs.server.namenodeFBT.incopt.IncOptProtocol;

/**
 * @author hanhlh
 *
 */
public class MarkOptProtocol extends IncOptProtocol{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private int _mark;

    public MarkOptProtocol() {
        this(Integer.MAX_VALUE, 0, 0);
    }

    public MarkOptProtocol(int length, int height, int mark) {
        super(length, height);
        _mark = mark;
    }

    public int getMark() {
        return _mark;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(", mark = ");
        buf.append(_mark);

        return buf.toString();
    }

}
