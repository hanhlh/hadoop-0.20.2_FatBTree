/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg.io;

import java.io.IOException;
import java.io.Serializable;


/**
 * @author hanhlh
 *
 */
public interface Exteriorizable extends Serializable {

	public void writeExterior(ExteriorOutputStream out) throws IOException;

    public void readExterior(ExteriorInputStream in) throws IOException;
}
