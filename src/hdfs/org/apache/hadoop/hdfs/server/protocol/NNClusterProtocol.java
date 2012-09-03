/**
 *
 */
package org.apache.hadoop.hdfs.server.protocol;

import java.io.IOException;

import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.namenode.INodeInfo;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;
import org.apache.hadoop.ipc.VersionedProtocol;

/**
 * @author hanhlh
 *
 */
public interface NNClusterProtocol extends VersionedProtocol{

	public static final long versionID = 1L;
	//register NameNode
	public void namenodeRegistration(String host, int port, int nID) throws IOException;
	//catch NameNode
	public void catchAddr(String host, int port, int nID) throws IOException;

	//forward DataNodeProtocol

	public void forwardRegister(DatanodeRegistration registration) throws IOException;

	public void forwardHeartBeat(DatanodeRegistration nodeReg,
								long capacity,
								long dfsUsed, long remaining,
								int xmitsInProgress,
								int xceiverCount) throws IOException;

	public void forwardBlockReport(DatanodeRegistration nodeReg,
								long[] blocks) throws IOException;

	public void forwardBlockBeingWrittenReport(DatanodeRegistration nodeReg,
								long[] blocks) throws IOException;

	public void forwardBlockReceived(DatanodeRegistration nodeReg,
								Block blocks[],
								String[] delHints) throws IOException;

	public void forwardErrorReport(DatanodeRegistration nodeReg,
								int errorCode,
								String msg) throws IOException;

	public void forwardReportBadBlocks(LocatedBlock[] blocks) throws IOException;

	public void forwardNextGenerationStamp(Block block, boolean fromNN) throws IOException;

	public void forwardCommitBlockSynchronization(Block block,
								long newgenerationstamp, long newlength,
								boolean closeFile, boolean deleteblock,
								DatanodeID[] newtargets) throws IOException;
	//catch DataNodeProtocol

	/* catch */
	public void catchRegister(DatanodeRegistration registration) throws IOException;

	public void catchHeartBeat(DatanodeRegistration nodeReg, long capacity,
								long dfsUsed, long remaining, int xmitsInProgress,
								int xceiverCount) throws IOException;

	public void catchBlockReport(DatanodeRegistration nodeReg,
								long[] blocks) throws IOException;

	public void catchBlockBeingWrittenReport(DatanodeRegistration nodeReg,
								long[] blocks) throws IOException;

	public void catchBlockReceived(DatanodeRegistration nodeReg,
								Block blocks[],
								String[] delHints) throws IOException;

	public void catchErrorReport(DatanodeRegistration nodeReg,
								int errorCode,
								String msg) throws IOException;

	public void catchReportBadBlocks(LocatedBlock[] blocks) throws IOException;

	public void catchNextGenerationStamp(Block block, boolean fromNN) throws IOException;
	public void catchCommitBlockSynchronization(Block block,
								long newgenerationstamp, long newlength,
								boolean closeFile, boolean deleteblock,
								DatanodeID[] newtargets
			      				) throws IOException;



	//ClientProtocol
	public void forwardAddBlock(String src, String clientName);
	public void catchAddBlock(String src, String clientName);
	/* INode processing */
	public INodeInfo getNodeFromOther(byte[][] components, int[] visited);

	public void setCopying(String src, int id, long atime, long mtime);

	public int getNumOfFiles();

	void setStart(String src);

	public String searchStart();

	public void copyFromRight(String end) throws IOException, MessageException, ServiceException;



	}
