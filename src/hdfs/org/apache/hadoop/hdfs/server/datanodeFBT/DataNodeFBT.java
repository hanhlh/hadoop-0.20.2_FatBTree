/**
 *
 */
package org.apache.hadoop.hdfs.server.datanodeFBT;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.ClientDatanodeProtocol;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.protocol.FBTDatanodeProtocol;

/**
 * @author hanhlh
 *
 */
public class DataNodeFBT extends DataNode implements ClientDatanodeProtocol,
										FBTDatanodeProtocol {

	DataNodeFBT(Configuration conf, AbstractList<File> dataDirs)
			throws IOException {
		super(conf, dataDirs);
		// TODO 自動生成されたコンストラクター・スタブ
	}

}
