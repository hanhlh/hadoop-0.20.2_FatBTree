/**
 *
 */
package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.util.StringUtils;

/**
 * @author hanhlh
 *
 */
public class NNBenchThread implements Callable<Integer>{

	private static long maxExceptionsPerFile = 200;
	private static final Log LOG = (Log) LogFactory.getLog(
            "org.apache.hadoop.hdfs.NNBenchThread");
	private final long start;

	public int totalExceptions = 0;
	private boolean create;
	private boolean noData;
	private static byte[] buffer;
	private FileSystem fs;
	private String filename;
	private long bytesPerBlock;
	private long bytesPerFile;
	private Path taskDir;

	public NNBenchThread(FileSystem fs, String writeFile, long bytesPerBlock, long bytesPerFile, Path taskDir, boolean nowrite) {
		this.noData = nowrite;
		create = true;
		this.fs = fs;
		filename = writeFile;
		this.bytesPerBlock = bytesPerBlock;
		this.bytesPerFile = bytesPerFile;
		this.taskDir = taskDir;
		buffer = new byte[(int) Math.min(bytesPerFile, 32768L)];
		this.start= System.currentTimeMillis();
	}

	public NNBenchThread(FileSystem fs, String readFile, long bytesPerFile, Path taskDir, boolean nogbl) {
		this.noData = nogbl;
		create = false;
		this.fs = fs;
		filename = readFile;
		bytesPerBlock = 0L;
		this.bytesPerFile = bytesPerFile;
		this.taskDir = taskDir;
		buffer = new byte[(int) Math.min(bytesPerFile, 32768L)];
		this.start= System.currentTimeMillis();
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

	public Integer call() throws Exception {
		Date startTime = new Date();
		//System.out.println("start creating "+filename+": "+startTime.getTime());
		Date endTime;
		int singleFileExceptions = 0;
		if(create) {
			if(noData) {
				boolean success = false;
				FSDataOutputStream out = null;
				do { // create file until is succeeds or max exceptions reached
			        try {
			         /* out = fileSys.create(
			                  new Path(taskDir, "" + index), false, 512,
			                  (short)1, bytesPerBlock); */

			        	out = fs.create(
			        			new Path(taskDir, "" + filename), false, 512,
			        			(short) 1, bytesPerBlock);
			        	endTime = new Date();

			        	System.out.println("create no write "+ filename + ": "
								+(endTime.getTime()-startTime.getTime())/1000.0);
			          success = true;
			        } catch (IOException ioe) {
			          success=false;
			          totalExceptions++;
			          handleException("creating file #" + filename, ioe,
			                  ++singleFileExceptions);
			        } catch (MessageException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} catch (ServiceException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				} while (!success);
			      do { // close file until is succeeds
			        try {
			          out.close();
			          success = true;
			        } catch (IOException ioe) {
			        	System.out.println(ioe.getMessage());
			          success=false;
			          totalExceptions++;
			          handleException("closing file #" + filename, ioe,
			                  ++singleFileExceptions);
			        }
			      } while (!success);
			} else {
			boolean success=false;
			FSDataOutputStream out = null;
			do { // create file until is succeeds or max exceptions reached
		        try {
		         /* out = fileSys.create(
		                  new Path(taskDir, "" + index), false, 512,
		                  (short)1, bytesPerBlock); */
		        	out = fs.create(
		        			new Path(taskDir, "" + filename), false, 512,
		        			(short) 1, bytesPerBlock);
		        	endTime = new Date();
		        	System.out.println("create no write "+ filename + ": "
							+(endTime.getTime()-startTime.getTime())/1000.0);
		        	success = true;
		        } catch (IOException ioe) {
		        	success=false;
		        	totalExceptions++;
		        	handleException("creating file #" + filename, ioe,
		                  ++singleFileExceptions);
		        } catch (MessageException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (ServiceException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			} while (!success);
			long toBeWritten = bytesPerFile;
			while (toBeWritten > 0) {
				int nbytes = (int) Math.min(buffer.length, toBeWritten);
		       	toBeWritten -= nbytes;
		        try { // only try once
		        	out.write(buffer, 0, nbytes);
		        } catch (IOException ioe) {
		        	totalExceptions++;
		        	handleException("writing to file #" + filename, ioe,
		                  ++singleFileExceptions);
		        }
			}
			do { // close file until is succeeds
				try {
					out.close();
					endTime = new Date();
					System.out.println("create write "+ filename + ": "
							+(endTime.getTime()-startTime.getTime())/1000.0);
					success = true;

		        	} catch (IOException ioe) {
		        		success=false;
		        		totalExceptions++;
		        		handleException("closing file #" + filename, ioe,
		                  ++singleFileExceptions);
		        	}
		      	} while (!success);
			}
		} else if(!create) { //read
			if(noData) { //read only
				try {
					FSDataInputStream in = null;
					//in = fileSys.open(new Path(taskDir, "" + index), 512);
					in = fs.open(new Path(taskDir, "" + filename), 512);
					//FileStatus fstatus = fs.getFileStatus(taskDir, false);

					endTime = new Date();
					System.out.println("open only "+ filename + ": "
							+(endTime.getTime()-startTime.getTime())/1000.0);
				} catch (IOException ioe) {
					totalExceptions++;
					handleException("opening file #" + filename, ioe, ++singleFileExceptions);
				} catch (MessageException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}

			} else { // openRead

				try {
					FSDataInputStream in = null;
					//in = fileSys.open(new Path(taskDir, "" + index), 512);
		      	  	in = fs.open(new Path(taskDir, "" + filename), 512);
		      	  	endTime = new Date();
    	  			System.out.println("open only "+ filename + ": "
						+(endTime.getTime()-startTime.getTime())/1000.0);
		      	  	long toBeRead = bytesPerFile;
		      	  	while (toBeRead > 0) {
		      	  		int nbytes = (int) Math.min(buffer.length, toBeRead);
		      	  		toBeRead -= nbytes;
		      	  		try { // only try once && we don't care about a number of bytes read
		      	  			in.read(buffer, 0, nbytes);
		      	  		} catch (IOException ioe) {
		      	  			totalExceptions++;
		      	  			handleException("reading from file #" + filename, ioe,
		                      ++singleFileExceptions);
		      	  		}
		      	  	}
		      	  	in.close();
		      	  	endTime = new Date();
		      	  	System.out.println("open read "+ filename + ": "
							+(endTime.getTime()-startTime.getTime())/1000.0);
		        	} catch (IOException ioe) {
		        		totalExceptions++;
		        		handleException("opening file #" + filename, ioe, ++singleFileExceptions);
		        	} catch (MessageException e) {
		        		// TODO 自動生成された catch ブロック
		        		e.printStackTrace();
		        	}
				}
			}
		return totalExceptions;
	}


}
