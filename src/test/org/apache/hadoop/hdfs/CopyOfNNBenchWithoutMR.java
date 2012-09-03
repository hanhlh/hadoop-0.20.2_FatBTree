package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.net.UnknownHostException;
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

public class CopyOfNNBenchWithoutMR {

	private static final Log LOG = LogFactory.getLog(
    "org.apache.hadoop.hdfs.NNBench");

	// variable initialzed from command line arguments
	private static long startTime = 0;
	private static int numFiles = 0;
	private static long bytesPerBlock = 1;
	private static long blocksPerFile = 0;
	private static long bytesPerFile = 1;
	private static Path baseDir = null;

	// variables initialized in main()
	private static FileSystem fileSys = null;
	private static Path taskDir = null;
	private static String uniqueId = null;
	private static byte[] buffer;
	private static long maxExceptionsPerFile = 200;

	//modified
	private static FileSystem[] fss;
	private static int numSystems;
	private static int numClientThreads;
	private static int targetSystem;

	public static final long RANDOM_SEED = 1000L;

	static Map<String,String> _transferNamespaceMapping =
					new ConcurrentHashMap<String, String>();
	/**
	* Returns when the current number of seconds from the epoch equals
	* the command line argument given by <code>-startTime</code>.
	* This allows multiple instances of this program, running on clock
	* synchronized nodes, to start at roughly the same time.
	*/
	static void barrier() {
		long sleepTime;
		while ((sleepTime = startTime - System.currentTimeMillis()) > 0) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ex) {
			}
		}
	}

	static private void handleException(String operation, Throwable e,
			int singleFileExceptions) {
		LOG.warn("Exception while " + operation + ": " +
				StringUtils.stringifyException(e));
		if (singleFileExceptions >= maxExceptionsPerFile) {
			throw new RuntimeException(singleFileExceptions +
			" exceptions for a single file exceeds threshold. Aborting");
		}
	}

	/**
	* Create and write to a given number of files.  Repeat each remote
	* operation until is suceeds (does not throw an exception).
	*
	* @return the number of exceptions caught
	* @throws ServiceException
	* @throws MessageException
	 * @throws IOException
	 * @throws ClassNotFoundException
	*/
	static int createWrite() throws MessageException, ServiceException, IOException, ClassNotFoundException {
		int totalExceptions = 0;
		Date workloadGenStart = new Date();
		List<Integer> w = new ArrayList<Integer>();
		for (int i=0; i<numFiles; i++) {
			if (i % numSystems == (targetSystem-1)) {
				w.add(new Integer(i));
			}
		}
		Collections.shuffle(w, new Random(RANDOM_SEED));
		Date workloadGenEnd = new Date();
		System.out.println("workloadGentime: "+
		(workloadGenEnd.getTime()-workloadGenStart.getTime())/1000.0);
		int files =0;
		String writeFile;

		ExecutorService executor =  Executors.newFixedThreadPool(numClientThreads);
		List<Future<Integer>> list = new ArrayList<Future<Integer>>();


		for (;files<w.size();files++) {
			writeFile = String.format("%06d", w.get(files));
			NNBenchThread task = new NNBenchThread(fileSys, writeFile,
			bytesPerBlock, bytesPerFile, taskDir, false);
			Future<Integer> future = executor.submit(task);
			list.add(future);

		}

		for (Future<Integer> f:list) {
			try {
				totalExceptions +=f.get();
			} catch (InterruptedException e) {
	// 	TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			} catch (ExecutionException e) {
	// 	TODO ��ư�������줿 catch �֥�å�
				e.printStackTrace();
			}
		}
		executor.shutdown();
		//} while (files<numFiles);
		return totalExceptions;
	}

	/**
	* Open and read a given number of files.
	*
	* @return the number of exceptions caught
	* @throws MessageException
	*/
	static int openRead() throws MessageException {
		int totalExceptions = 0;
		//FSDataInputStream in = null;
		List<Integer> r = new ArrayList<Integer>();
		Random random = new Random(RANDOM_SEED);
		int count = 0;
		Date workloadGenStart = new Date();
		while (count<numFiles) {
			int temp = random.nextInt(numFiles);
			if (!r.contains(temp)) {
				r.add(temp);
				count++;
			}
		}
		Collections.shuffle(r, new Random(RANDOM_SEED));
		Date workloadGenEnd = new Date();
		System.out.println("workloadGentime: "+
				(workloadGenEnd.getTime()-workloadGenStart.getTime())/1000.0);
		int accessTo = 0;
		int files = 0;
		String readfile;

		ExecutorService executor = Executors.newFixedThreadPool(numClientThreads);
		List<Future<Integer>> list = new ArrayList<Future<Integer>>();
		// multiThread? //
	//Thread[] ths = new Thread[numFiles];
	//do {
	//	int nowFiles = files;
		for(;files < numFiles; files++) {
			readfile = String.format("%06d", r.get(files));
			//System.out.println("readfile "+readfile);
			//ths[files] = new NNBenchThread(fileSys, readfile, bytesPerFile, taskDir, true);
			//ths[files].start();
			NNBenchThread task = new NNBenchThread(fileSys, readfile, bytesPerFile,
					taskDir, false);
			Future<Integer> future = executor.submit(task);
			list.add(future);
		}

		for (Future<Integer> f:list) {
			try {
				totalExceptions +=f.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		}
		executor.shutdown();
		return totalExceptions;
	}

	static int createOnly() {
	int totalExceptions = 0;
	List<Integer> w = new ArrayList<Integer>();
	for (int i=0; i<numFiles; i++) {
	if (i % numSystems == (targetSystem-1)) {
	w.add(new Integer(i));
	}
	}
	Collections.shuffle(w, new Random(RANDOM_SEED));

	int accessTo = 0;
	int files =0;
	String writeFile;

	ExecutorService executor =  Executors.newFixedThreadPool(numClientThreads);
	List<Future<Integer>> list = new ArrayList<Future<Integer>>();


	//NNBenchThread[] ths = new NNBenchThread[numFiles];
	//do {
	//int nowFiles = files;
	for (;files<w.size();files++) {
	writeFile = String.format("%06d", w.get(files));
	//System.out.println("writeFile: "+writeFile);
	//ths[files] = new NNBenchThread(fileSys, writeFile,
	//		bytesPerBlock, bytesPerFile, taskDir, true);
	//ths[files].start();

	NNBenchThread task = new NNBenchThread(fileSys, writeFile,
	bytesPerBlock, bytesPerFile, taskDir, true);
	Future<Integer> future = executor.submit(task);
	list.add(future);

	}

	for (Future<Integer> f:list) {
	try {
	totalExceptions +=f.get();
	} catch (InterruptedException e) {
	// TODO ��ư�������줿 catch �֥�å�
	e.printStackTrace();
	} catch (ExecutionException e) {
	// TODO ��ư�������줿 catch �֥�å�
	e.printStackTrace();
	}
	}
	executor.shutdown();
	//} while (files<numFiles);

	return totalExceptions;

	}

	//Read only
	static int read() {

	int totalExceptions = 0;
	//FSDataInputStream in;

	List<Integer> r = new ArrayList<Integer>();
	int count = 0;

	while (count<numFiles) {
	Random random = new Random(RANDOM_SEED);
	r.add(random.nextInt(numFiles));
	count++;
	}

	Collections.shuffle(r, new Random(RANDOM_SEED));
	//int writeFile;

	int accessTo = 0;
	int files = 0;
	String readfile;

	ExecutorService executor = Executors.newFixedThreadPool(numClientThreads);
	List<Future<Integer>> list = new ArrayList<Future<Integer>>();
	// multiThread? //
	//Thread[] ths = new Thread[numFiles];
	//do {
	//	int nowFiles = files;
	for(;files < numFiles; files++) {
	readfile = String.format("%06d", r.get(files));
	//System.out.println("readfile "+readfile);
	//ths[files] = new NNBenchThread(fileSys, readfile, bytesPerFile, taskDir, true);
	//ths[files].start();
	NNBenchThread task = new NNBenchThread(fileSys, readfile, bytesPerFile,
	taskDir, true);
	Future<Integer> future = executor.submit(task);
	list.add(future);
	}

	for (Future<Integer> f:list) {
	try {
	totalExceptions +=f.get();
	} catch (InterruptedException e) {
	e.printStackTrace();
	} catch (ExecutionException e) {
	e.printStackTrace();
	}
	}
	executor.shutdown();
	return totalExceptions;
	}


	/**
	* Rename a given number of files.  Repeat each remote
	* operation until is suceeds (does not throw an exception).
	*
	* @return the number of exceptions caught
	 * @throws ServiceException
	 * @throws MessageException
	*/
	static int rename() throws MessageException, ServiceException {
		int totalExceptions = 0;
		boolean success = false;
		for (int index = 0; index < numFiles; index++) {
			int singleFileExceptions = 0;
			do { // rename file until is succeeds
				try {
					boolean result = fileSys.rename(
							new Path(taskDir, "" + index), new Path(taskDir, "A" + index));
					success = true;
				} catch (IOException ioe) {
					success=false;
					totalExceptions++;
					handleException("creating file #" + index, ioe, ++singleFileExceptions);
				}
			} while (!success);
		}
		return totalExceptions;
		}

	/**
	* Delete a given number of files.  Repeat each remote
	* operation until is suceeds (does not throw an exception).
	*
	* @return the number of exceptions caught
	 * @throws MessageException
	*/
	static int delete() throws MessageException {
		int totalExceptions = 0;
		boolean success = false;
		for (int index = 0; index < numFiles; index++) {
			int singleFileExceptions = 0;
			do { // delete file until is succeeds
				try {
					boolean result = fileSys.delete(new Path(taskDir, "A" + index), true);
					success = true;
				} catch (IOException ioe) {
					success=false;
		totalExceptions++;
		handleException("creating file #" + index, ioe, ++singleFileExceptions);
				}
			} while (!success);
		}
		return totalExceptions;
	}

	/**
	* This launches a given namenode operation (<code>-operation</code>),
	* starting at a given time (<code>-startTime</code>).  The files used
	* by the openRead, rename, and delete operations are the same files
	* created by the createWrite operation.  Typically, the program
	* would be run four times, once for each operation in this order:
	* createWrite, , rename, delete.
	*
	* <pre>
	* Usage: nnbench
	*          -operation <one of createWrite, openRead, rename, or delete>
	*          -baseDir <base output/input DFS path>
	*          -startTime <time to start, given in seconds from the epoch>
	*          -numFiles <number of files to create, read, rename, or delete>
	*          -blocksPerFile <number of blocks to create per file>
	*         [-bytesPerBlock <number of bytes to write to each block, default is 1>]
	*         [-bytesPerChecksum <value for io.bytes.per.checksum>]
	* </pre>
	*
	* @throws IOException indicates a problem with test startup
	* @throws MessageException
	* @throws ServiceException
	 * @throws ClassNotFoundException
	*/
	public static void main(String[] args) throws IOException, MessageException, ServiceException, ClassNotFoundException {
	String version = "NameNodeBenchmark.0.4";
	//System.out.println(version);
	int bytesPerChecksum = -1;

	String usage =
	"Usage: CopyOfNNBenchWithoutMR " +
	"  -operation <one of createWrite, openRead, rename, delete, create or read> " +
	"  -baseDir <base output/input DFS path> " +
	"  -startTime <time to start, given in seconds from the epoch> " +
	"  -numFiles <number of files to create> " +
	"  -blocksPerFile <number of blocks to create per file> " +
	"  [-bytesPerBlock <number of bytes to write to each block, default is 1>] " +
	"  [-bytesPerChecksum <value for io.bytes.per.checksum>]" +
	"  -numSystems"+
	"  -numClientThreads"+
	"  -targetSystem <target namesystem, starts from 1, 2, ...>"+
	"Note: bytesPerBlock MUST be a multiple of bytesPerChecksum";

	//initializeTransferMapping();
	String operation = null;
	for (int i = 0; i < args.length; i++) { // parse command line
	if (args[i].equals("-baseDir")) {
	baseDir = new Path(args[++i]);
	} else if (args[i].equals("-numFiles")) {
	numFiles = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-blocksPerFile")) {
	blocksPerFile = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-bytesPerBlock")) {
	bytesPerBlock = Long.parseLong(args[++i]);
	} else if (args[i].equals("-bytesPerChecksum")) {
	bytesPerChecksum = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-startTime")) {
	startTime = Long.parseLong(args[++i]) * 1000;
	} else if (args[i].equals("-operation")) {
	operation = args[++i];
	}	else if (args[i].equals("-numSystems")) {
	numSystems = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-numClientThreads")) {
	numClientThreads = Integer.parseInt(args[++i]);
	} else if (args[i].equals("-targetSystem")) {
	targetSystem = Integer.parseInt(args[++i]);
	}else {
	System.out.println(usage);
	System.exit(-1);
	}
	}
	bytesPerFile = bytesPerBlock * blocksPerFile;

	Configuration conf = new Configuration();
	//System.out.println("conf "+conf.toString());
	conf.addResource("hdfs-site-fbt.xml");
	JobConf jobConf = new JobConf(conf, NNBench.class);

	//System.out.println("jobConf "+jobConf.toString());

	/*for (int i=0; i<numSystems; i++) {
	System.out.println("i: "+i);
	fss[i] = FileSystem.get(jobConf);
	}*/
	if ( bytesPerChecksum < 0 ) { // if it is not set in cmdline
	bytesPerChecksum = jobConf.getInt("io.bytes.per.checksum", 512);
	}
	jobConf.set("io.bytes.per.checksum", Integer.toString(bytesPerChecksum));

	System.out.println("Inputs: ");
	System.out.println("   operation: " + operation);
	System.out.println("   baseDir: " + baseDir);
	System.out.println("   startTime: " + startTime);
	System.out.println("   numFiles: " + numFiles);
	System.out.println("   blocksPerFile: " + blocksPerFile);
	System.out.println("   bytesPerBlock: " + bytesPerBlock);
	System.out.println("   bytesPerChecksum: " + bytesPerChecksum);
	System.out.println("   targetSystem: " + targetSystem);

	if (operation == null ||  // verify args
	//baseDir == null ||
	numFiles < 1 ||
	blocksPerFile < 1 ||
	bytesPerBlock < 0 ||
	bytesPerBlock % bytesPerChecksum != 0)
	{
	System.err.println(usage);
	System.exit(-1);
	}

	fileSys = FileSystem.get(jobConf);
	//System.out.println("fileSys "+fileSys.toString());
	uniqueId = java.net.InetAddress.getLocalHost().getHostName();
	uniqueId = "";
	//taskDir = new Path(baseDir, uniqueId);
	taskDir = new Path("/user", "/user/hanhlh");
	// initialize buffer used for writing/reading file
	buffer = new byte[(int) Math.min(bytesPerFile, 32768L)];

	Date execTime;
	Date endTime;
	long duration;
	int exceptions = 0;
	barrier(); // wait for coordinated start time
	execTime = new Date();
	System.out.println("Job started: " + startTime);
	if (operation.equals("createWrite")) {
	//if (!fileSys.mkdirs(taskDir)) {
	//	if (!fileSys.mkdirs(taskDir, true)) {
	//   throw new IOException("Mkdirs failed to create " + taskDir.toString());
	// }
	exceptions = createWrite();
	} else if (operation.equals("openRead")) {
	exceptions = openRead();
	} else if (operation.equals("rename")) {
	exceptions = rename();
	} else if (operation.equals("delete")) {
	exceptions = delete();
	} else if (operation.equals("create")) {
	/*if (!fileSys.mkdirs(taskDir, true)) {
	throw new IOException("Mkdirs failed to create "+taskDir.toString());
	}*/
	exceptions = createOnly();
	} else if (operation.equals("read")) {
	System.out.println("readOnly");
	exceptions = read();
	}else {
	System.err.println(usage);
	System.exit(-1);
	}
	endTime = new Date();
	System.out.println("Job ended: " + endTime);
	duration = (endTime.getTime() - execTime.getTime()) /1000;
	System.out.println("The " + operation + " job took " + duration + " seconds.");
	System.out.println("The job recorded " + exceptions + " exceptions.");

		}

	public static void initializeTransferMapping() {
		_transferNamespaceMapping.put("edn14", "edn13");
		_transferNamespaceMapping.put("edn15", "edn16");
	}
}
