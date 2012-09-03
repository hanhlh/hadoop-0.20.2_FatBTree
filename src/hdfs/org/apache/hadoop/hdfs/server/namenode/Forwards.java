/**
 *
 */
package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.NNClusterProtocol;
import org.apache.hadoop.ipc.VersionedProtocol;

/**
 * @author hanhlh
 *
 */
public class Forwards  extends Thread{

	// runMethod
	public static final int BLOCKRECEIVED = 1;
	public static final int GETBLOCKLOCATIONS = 2;
	public static final int CREATE = 3;
	public static final int BLOCKREPORT = 4;
	public static final int HEARTBEAT = 5;
	public static final int BLOCKSBEINGWRITTENREPORT = 6;
	public static final int ERRORREPORT = 7;
	public static final int REGISTER = 8;

	private int runMethod;

	private DatanodeRegistration registration;
	private Block[] blocks;
	private String[] delHints;
	private VersionedProtocol protocol;
	private String src;
	private long offsetBsizeUsed;
	private long lengcap;
	private long remaining;
	private FsPermission masked;
	private String clientName;
	private boolean overwrite;
	private short replication;
	private long[] lblocks;
	private int xmitsInProgressandCode;
	private int xceiverCount;

	public Forwards(DatanodeRegistration registration, Block[] blocks, String[] delHints,
			NNClusterProtocol node) {
		runMethod = BLOCKRECEIVED;
		this.registration = registration;
		this.blocks = blocks;
		this.delHints = delHints;
		protocol = node;
	}

	public Forwards(String src, long offset, long length, ClientProtocol node) {
		runMethod = GETBLOCKLOCATIONS;
		this.src = src;
		this.offsetBsizeUsed = offset;
		this.lengcap = length;
		protocol = node;
	}

	public Forwards(String src, FsPermission masked, String clientName,
			boolean overwrite, short replication, long blockSize, ClientProtocol node) {
		runMethod = CREATE;
		this.src = src;
		this.masked = masked;
		this.clientName = clientName;
		this.overwrite = overwrite;
		this.replication = replication;
		this.offsetBsizeUsed = blockSize;
		protocol = node;
	}

	public Forwards(DatanodeRegistration nodeReg, long[] blocks, NNClusterProtocol node, int method) {
		runMethod = method;
		registration = nodeReg;
		this.lblocks = blocks;
		protocol = node;
	}

	public Forwards(DatanodeRegistration nodeReg, long capacity,
			long dfsUsed, long remaining, int xmitsInProgress, int xceiverCount, NNClusterProtocol node) {
		runMethod = HEARTBEAT;
		registration = nodeReg;
		lengcap = capacity;
		offsetBsizeUsed = dfsUsed;
		this.remaining = remaining;
		this.xmitsInProgressandCode = xmitsInProgress;
		this.xceiverCount = xceiverCount;
		protocol = node;
	}

	public Forwards(DatanodeRegistration registration,
                          int errorCode,
                          String msg, NNClusterProtocol node) {
		runMethod = ERRORREPORT;
		this.registration = registration;
		xmitsInProgressandCode = errorCode;
		src = msg;
		protocol = node;
	}

	public Forwards(DatanodeRegistration registration, NNClusterProtocol node) {
		runMethod = REGISTER;
		this.registration = registration;
	}

	private void blockReceived() throws IOException {
		((NNClusterProtocol) protocol).catchBlockReceived(registration, blocks, delHints);
	}

	//private LocatedBlocks getBlockLocations() throws IOException {
	//
	//}

	public void blockReport() throws IOException {
		((NNClusterProtocol) protocol).catchBlockReport(registration, lblocks);
	}

	public void heartBeat() throws IOException {
		((NNClusterProtocol) protocol).catchHeartBeat(registration, lengcap, offsetBsizeUsed, remaining, xmitsInProgressandCode, xceiverCount);
	}

	public void blocksBeingWrittenReport() throws IOException {
		((NNClusterProtocol) protocol).catchBlockBeingWrittenReport(registration, lblocks);
	}

	public void errorReport() throws IOException {
		((NNClusterProtocol) protocol).catchErrorReport(registration, xmitsInProgressandCode, src);
	}

	public void register() throws IOException {
		((NNClusterProtocol) protocol).catchRegister(registration);
	}

	public void run() {
		if(runMethod == BLOCKRECEIVED) {
			try {
				blockReceived();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(runMethod == GETBLOCKLOCATIONS) {

		}  else if(runMethod == BLOCKREPORT) {
			try {
				blockReport();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(runMethod == HEARTBEAT) {
			try {
				heartBeat();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(runMethod == BLOCKSBEINGWRITTENREPORT) {
			try {
				blocksBeingWrittenReport();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(runMethod == ERRORREPORT) {
			try {
				errorReport();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(runMethod == REGISTER) {
			try {
				register();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}

}
