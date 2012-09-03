/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.hadoop.hdfs.server.namenode.writeOffLoading.WriteOffLoadingCommand;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;
import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBT;

/**
 * @author hanhlh
 *
 */
public class WOLCReceiver implements Runnable {


	protected final ServerSocket _ssock;

	private WriteOffLoadingCommand _command;

	private NameNodeFBT _namenodeFBT;

	public WOLCReceiver(int port) throws IOException {
		try {
			_ssock = new ServerSocket(port);
			run();
		} catch (BindException be) {
			throw be;
		}
	}

	@Override
	public void run() {
		System.out.println("WOLCReceiver run");
		//while (true) {
			try {
				receive(_ssock.accept());
			} catch (IOException ie) {
				ie.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		//}
	}

	private void receive(Socket sock) throws IOException, ClassNotFoundException {
		sock.setTcpNoDelay(true);
		//sock.setKeepAlive(true);

		ObjectInputStream ois = new ObjectInputStream(
									new BufferedInputStream(sock.getInputStream()));


		while (true) {
			try {
				_command = (WriteOffLoadingCommand) ois.readObject();
				if (_namenodeFBT!=null) {
					_namenodeFBT.writeOffLoadingCommandHandler(_command);
				}
			} catch (EOFException e) {
				//e.printStackTrace();
				ois.close();
				//sock.close();
				break;
			}
		}
	}

	protected void terminate() throws IOException {
		_ssock.close();
	}


	public void setNameNodeFBT(NameNodeFBT namenodeFBT) {
		_namenodeFBT = namenodeFBT;
	}
}
