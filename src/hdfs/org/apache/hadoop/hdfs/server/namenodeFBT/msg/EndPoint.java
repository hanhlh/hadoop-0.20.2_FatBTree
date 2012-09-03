/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


import org.apache.hadoop.hdfs.server.namenodeFBT.NameNodeFBT;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorInputStream;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.io.ExteriorOutputStream;
import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 */
public final class EndPoint implements Serializable {
	// instance attributes ////////////////////////////////////////////////////

    /** ホストアドレス */
    private final InetAddress _address;

    /** ポート番号 */
    private final int _port;

    // constructors ///////////////////////////////////////////////////////////

    public EndPoint(InetAddress address, int port) {

        _address = address;
        _port = port;
    }

    public EndPoint(String hostname, int port) throws UnknownHostException {
        this(InetAddress.getByName(hostname), port);
    }

    public EndPoint(Socket sock) {
        this(sock.getInetAddress(), sock.getPort());
    }

 // class methods //////////////////////////////////////////////////////////

    public static EndPoint parse(String str) throws UnknownHostException {
    	System.out.println("EndPoint.parse "+str);
    	if (str.contains(":")) {
	    	int sepPos = str.indexOf(':');
	    	if (sepPos>0) {
	    		String hostname = str.substring(0, sepPos);
	    		int port = Integer.parseInt(str.substring(sepPos + 1));
	    		return new EndPoint(hostname, port);
	    	}

    	}else {
    		return new EndPoint(str, NameNodeFBT.FBT_MESSAGE_PORT);
    	}
    	return null;
    }

    // accessors //////////////////////////////////////////////////////////////

    public InetAddress getInetAddress() {
        return _address;
    }

    public int getPort() {
        return _port;
    }

    // instance methods ///////////////////////////////////////////////////////

    public String getHostName() {
        return _address.getHostName();
    }

    public Socket createSocket() throws IOException {
    	return new Socket(_address, _port);
    }

    public String toString() {
        return _address.getHostName() + ":" + String.valueOf(_port);
    }

    public int hashCode() {
        return _address.hashCode() ^ _port;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EndPoint)) {
            return false;
        }
        return equals((EndPoint) obj);
    }

    public boolean equals(EndPoint endp) {
        return _address.equals(endp._address) && _port == endp._port;
    }

    public static void write(EndPoint ep, ExteriorOutputStream out)
    										throws IOException {
		out.writeByte(ep._address.getAddress()[0]);
		out.writeByte(ep._address.getAddress()[1]);
		out.writeByte(ep._address.getAddress()[2]);
		out.writeByte(ep._address.getAddress()[3]);
		out.writeInt(ep._port);
    }

    public static EndPoint read(ExteriorInputStream in) throws IOException {
		byte[] bytes = new byte[4];
		bytes[0] = in.readByte();
		bytes[1] = in.readByte();
		bytes[2] = in.readByte();
		bytes[3] = in.readByte();
		InetAddress address = InetAddress.getByAddress(bytes);
		int port = in.readInt();
		return new EndPoint(address, port);
    }

}
