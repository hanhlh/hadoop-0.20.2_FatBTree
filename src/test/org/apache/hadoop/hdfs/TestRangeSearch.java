/**
 *
 */
package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.mapred.JobConf;

/**
 * @author hanhlh
 *
 */
public class TestRangeSearch {

	private static final Log LOG = LogFactory.getLog(
    "org.apache.hadoop.hdfs.NNBench");

	private static FileSystem fileSys = null;

	private static String _low;
	private static String _high;

	public static void main(String[] args) throws IOException, MessageException, ServiceException, ClassNotFoundException {
	String version = "RangeSearch.0.1";

	Configuration conf = new Configuration();
	//System.out.println("conf "+conf.toString());
	conf.addResource("hdfs-site-fbt.xml");
	JobConf jobConf = new JobConf(conf, NNBench.class);
	fileSys = FileSystem.get(jobConf);
	System.out.println("filesystem: "+fileSys);

	String usage =
		"Usage: RangeSearch " +
		"  -low <low> "+
		"  -high <high>";
	for (int i = 0; i < args.length; i++) { // parse command line
		if (args[i].equals("-low")) {
			_low = String.format("/user/hanhlh/%06d", Integer.parseInt(args[++i]));
		} else if (args[i].equals("-high")) {
			_high = String.format("/user/hanhlh/%06d", Integer.parseInt(args[++i]));
		} else {
			System.out.println(usage);
			System.exit(-1);
		}
	}

	if (_high.compareTo(_low)>=0) {
		Date start = new Date();
		fileSys.rangeSearch(_low, _high);
		System.out.println("execution time,"+
								(new Date().getTime()-start.getTime())/1000.0);

	} else {
		System.out.println("out put error");
		System.exit(-1);
	}



	}

}
