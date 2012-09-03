/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.service;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public class ServiceException extends Exception {

	public ServiceException(Throwable cause) {
         super();

        /* J2SE 1.4 以降であれば例外チェーンを作成する */
        //super(cause);
        StringUtility.debugSpace("ServiceException");
    }

}
