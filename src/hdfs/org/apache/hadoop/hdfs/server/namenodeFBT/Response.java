/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import org.apache.hadoop.hdfs.server.namenodeFBT.rule.RuleEvent;


/**
 * @author hanhlh
 *
 */
public abstract class Response extends RuleEvent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final String _dummy = "";

	public Response(Request source) {
		super(source);
	}

	public Class getEventClass() {
        return Response.class;
    }
	protected Response() {
        super(_dummy);
    }
}
