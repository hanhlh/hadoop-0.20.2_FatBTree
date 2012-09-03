/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.rule;

import org.apache.hadoop.hdfs.server.namenodeFBT.Response;

/**
 * @author hanhlh
 *
 */
public final class DumpResponse extends Response{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public DumpResponse(DumpRequest request){
        super(request);
    }

}
