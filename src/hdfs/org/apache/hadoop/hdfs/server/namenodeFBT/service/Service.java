/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.service;

import java.util.Properties;

import org.apache.hadoop.conf.Configuration;

/**
 * @author hanhlh
 *
 */
public interface Service {

	public void initialize(Configuration conf) throws ServiceException;

    public void terminate() throws ServiceException;
}
