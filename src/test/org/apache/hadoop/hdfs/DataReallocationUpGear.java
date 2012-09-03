/**
 *
 */
package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.StringUtils;

/**
 * @author hanhlh
 *
 */
public class DataReallocationUpGear {

	private static final Log LOG = LogFactory.getLog(
    "org.apache.hadoop.hdfs.NNBench");

	private static FileSystem fileSys = null;

	static Map<String,String> _transferNamespaceMapping =
		new ConcurrentHashMap<String, String>();

	public static void main(String[] args) throws IOException, MessageException, ServiceException, ClassNotFoundException {
	String version = "DataReallocationUpGear.0.1";
	initializeTransferMapping();
	Configuration conf = new Configuration();
	//System.out.println("conf "+conf.toString());
	conf.addResource("hdfs-site-fbt.xml");
	JobConf jobConf = new JobConf(conf, NNBench.class);
	fileSys = FileSystem.get(jobConf);

	System.out.println("filesystem: "+fileSys);
	String hostName = java.net.InetAddress.getLocalHost().getHostName();
	boolean result = false;
	Date wolStart = new Date();
	System.out.println("Start transferring WOL data");
	result = fileSys.transferNamespace(_transferNamespaceMapping.get(hostName));
	if (result) {
		Date wolEnd = new Date();
		System.out.println("WOL end after "+(wolEnd.getTime()-wolStart.getTime())/1000.0);
	}
	}

	public static void initializeTransferMapping() {
		_transferNamespaceMapping.put("edn15", "edn13");
		_transferNamespaceMapping.put("edn16", "edn14");
		_transferNamespaceMapping.put("edn17", "edn19");
		_transferNamespaceMapping.put("edn18", "edn20");

		/*_transferNamespaceMapping.put("edn14", "edn13");
		_transferNamespaceMapping.put("edn15", "edn16");*/
	}

}
