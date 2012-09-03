/**
 *
 */
package org.apache.hadoop.hdfs.server.namenode.writeOffLoading;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Formatter;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.StringUtils;


/**
 * @author hanhlh
 *
 */
public class WriteOffLoading {
	private int idPrivate;

	public void setIdPrivate(int id) {
		idPrivate = id;
	}

	public int getIdPrivate() {
		return idPrivate;
	}

	public static final Log LOG = LogFactory.getLog(WriteOffLoading.class.getName());

	 public static final String LOG_FORMAT =
							    "id=%s\t" +  // id
							    "block=%s\t" +  // block
							    "wolDst=%s\t" +  // writeoff loading datanode
							    "dst=%s\t";   //shoulde be datanode

	 public static final ThreadLocal<Formatter> auditFormatter =
		    new ThreadLocal<Formatter>() {
		      protected Formatter initialValue() {
		        return new Formatter(new StringBuilder(LOG_FORMAT.length() * 4));
		      }
	 };

	 public void logAuditEvent(long id, String src, DatanodeDescriptor wolDst,
			 						DatanodeDescriptor dst) {
		    final Formatter fmt = auditFormatter.get();
		    ((StringBuilder)fmt.out()).setLength(0);

		    LOG.info(fmt.format(LOG_FORMAT,id, src, wolDst, dst).toString());

	}

	 static String writeOffLoadingFile = "./logs/writeOffLoading.log";
	 static String writeOffLoadingBackUp = "./writeOffLoadingBackUp/";
	 private int id;
	 private String src;
	 private String wolDst;
	 private String dst;

	 public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String s) {
		this.src = s;
	}

	public String getWolDst() {
		return wolDst;
	}

	public void setWolDst(String wolDst) {
		this.wolDst = wolDst;
	}

	public String getDst() {
		return dst;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}

	public WriteOffLoading (int id, String src, String wolDst,
			 								String dst) {
		this.id = id;
		this.src = src;
		this.wolDst = wolDst;
		this.dst = dst;
	 }
	public String createWriteOffLoadingRecord() {
		StringBuilder builder = new StringBuilder();

		builder.append("WOL,");
		builder.append(getId() + ",");
		builder.append(getSrc() + ",");
		builder.append(getWolDst() + ",");
		builder.append(getDst());

		return builder.toString();
	}

	public void outputRecord (String record, String executorName) {
		File file = null;
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;

		try {
			file = new File(writeOffLoadingFile.concat("."+executorName));
			if (!file.exists())
				if (!file.createNewFile()) System.out.println("cannot open" + writeOffLoadingFile);

			fos = new FileOutputStream(file, true);
			osw = new OutputStreamWriter(fos);
			bw = new BufferedWriter(osw);
			pw = new PrintWriter(bw);

			pw.println(record);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pw != null) pw.close();
		}
	}
	 /**
	   * This class takes care of log file used to store the last verification
	   * times of the blocks. It rolls the current file when it is too big etc.
	   * If there is an error while writing, it stops updating with an error
	   * message.
	   */
	  private static class LogFileHandler {

	    private static final String curFileSuffix = ".curr";
	    private static final String prevFileSuffix = ".prev";

	    // Don't roll files more often than this
	    private static final long minRollingPeriod = 6 * 3600 * 1000L; // 6 hours
	    private static final long minWarnPeriod = minRollingPeriod;
	    private static final int minLineLimit = 1000;


	    static boolean isFilePresent(File dir, String filePrefix) {
	      return new File(dir, filePrefix + curFileSuffix).exists() ||
	             new File(dir, filePrefix + prevFileSuffix).exists();
	    }
	    private File curFile;
	    private File prevFile;

	    private int maxNumLines = -1; // not very hard limit on number of lines.
	    private int curNumLines = -1;

	    long lastWarningTime = 0;

	    private PrintStream out;

	    int numReaders = 0;

	    /**
	     * Opens the log file for appending.
	     * Note that rolling will happen only after "updateLineCount()" is
	     * called. This is so that line count could be updated in a separate
	     * thread without delaying start up.
	     *
	     * @param dir where the logs files are located.
	     * @param filePrefix prefix of the file.
	     * @param maxNumLines max lines in a file (its a soft limit).
	     * @throws IOException
	     */
	    LogFileHandler(File dir, String filePrefix, int maxNumLines)
	                                                throws IOException {
	      curFile = new File(dir, filePrefix + curFileSuffix);
	      prevFile = new File(dir, filePrefix + prevFileSuffix);
	      openCurFile();
	      curNumLines = -1;
	      setMaxNumLines(maxNumLines);
	    }

	    // setting takes affect when next entry is added.
	    synchronized void setMaxNumLines(int maxNumLines) {
	      this.maxNumLines = Math.max(maxNumLines, minLineLimit);
	    }

	    /**
	     * Append "\n" + line.
	     * If the log file need to be rolled, it will done after
	     * appending the text.
	     * This does not throw IOException when there is an error while
	     * appending. Currently does not throw an error even if rolling
	     * fails (may be it should?).
	     * return true if append was successful.
	     */
	    synchronized boolean appendLine(String line) {
	      out.println();
	      out.print(line);
	      curNumLines += (curNumLines < 0) ? -1 : 1;
	      try {
	        rollIfRequired();
	      } catch (IOException e) {
	        warn("Rolling failed for " + curFile + " : " + e.getMessage());
	        return false;
	      }
	      return true;
	    }

	    //warns only once in a while
	    synchronized private void warn(String msg) {
	      long now = System.currentTimeMillis();
	      if ((now - lastWarningTime) >= minWarnPeriod) {
	        lastWarningTime = now;
	        LOG.warn(msg);
	      }
	    }

	    private synchronized void openCurFile() throws FileNotFoundException {
	      close();
	      out = new PrintStream(new FileOutputStream(curFile, true));
	    }

	    //This reads the current file and updates the count.
	    void updateCurNumLines() {
	      int count = 0;
	      Reader it = null;
	      try {
	        for(it = new Reader(true); it.hasNext(); count++) {
	          it.next();
	        }
	      } catch (IOException e) {

	      } finally {
	        synchronized (this) {
	          curNumLines = count;
	        }
	        IOUtils.closeStream(it);
	      }
	    }

	    private void rollIfRequired() throws IOException {
	      if (curNumLines < maxNumLines || numReaders > 0) {
	        return;
	      }

	      long now = System.currentTimeMillis();
	      if (now < minRollingPeriod) {
	        return;
	      }

	      if (!prevFile.delete() && prevFile.exists()) {
	        throw new IOException("Could not delete " + prevFile);
	      }

	      close();

	      if (!curFile.renameTo(prevFile)) {
	        openCurFile();
	        throw new IOException("Could not rename " + curFile +
	                              " to " + prevFile);
	      }

	      openCurFile();
	      updateCurNumLines();
	    }

	    synchronized void close() {
	      if (out != null) {
	        out.close();
	        out = null;
	      }
	    }

	    /**
	     * This is used to read the lines in order.
	     * If the data is not read completely (i.e, untill hasNext() returns
	     * false), it needs to be explicitly
	     */
	    private class Reader implements Iterator<String>, Closeable {

	      BufferedReader reader;
	      File file;
	      String line;
	      boolean closed = false;

	      private Reader(boolean skipPrevFile) throws IOException {
	        synchronized (LogFileHandler.this) {
	          numReaders++;
	        }
	        reader = null;
	        file = (skipPrevFile) ? curFile : prevFile;
	        readNext();
	      }

	      private boolean openFile() throws IOException {

	        for(int i=0; i<2; i++) {
	          if (reader != null || i > 0) {
	            // move to next file
	            file = (file == prevFile) ? curFile : null;
	          }
	          if (file == null) {
	            return false;
	          }
	          if (file.exists()) {
	            break;
	          }
	        }

	        if (reader != null ) {
	          reader.close();
	          reader = null;
	        }

	        reader = new BufferedReader(new FileReader(file));
	        return true;
	      }

	      // read next line if possible.
	      private void readNext() throws IOException {
	        line = null;
	        try {
	          if (reader != null && (line = reader.readLine()) != null) {
	            return;
	          }
	          if (line == null) {
	            // move to the next file.
	            if (openFile()) {
	              readNext();
	            }
	          }
	        } finally {
	          if (!hasNext()) {
	            close();
	          }
	        }
	      }

	      public boolean hasNext() {
	        return line != null;
	      }

	      public String next() {
	        String curLine = line;
	        try {
	          readNext();
	        } catch (IOException e) {
	          LOG.info("Could not reade next line in LogHandler : " +
	                   StringUtils.stringifyException(e));
	        }
	        return curLine;
	      }

	      public void remove() {
	        throw new RuntimeException("remove() is not supported.");
	      }

	      public void close() throws IOException {
	        if (!closed) {
	          try {
	            if (reader != null) {
	              reader.close();
	            }
	          } finally {
	            file = null;
	            reader = null;
	            closed = true;
	            synchronized (LogFileHandler.this) {
	              numReaders--;
	              assert(numReaders >= 0);
	            }
	          }
	        }
	      }
	    }
	  }


}
