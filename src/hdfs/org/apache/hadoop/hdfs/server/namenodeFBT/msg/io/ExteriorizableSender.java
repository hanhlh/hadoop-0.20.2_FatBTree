package org.apache.hadoop.hdfs.server.namenodeFBT.msg.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.apache.hadoop.hdfs.server.namenodeFBT.msg.EndPoint;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Sender;

public class ExteriorizableSender extends Sender {

	final static int MAX_OBJECTS_PER_CONNECTION = 16;

	public ExteriorizableSender(EndPoint source, BlockingQueue<Message> queue) {
        super(source, queue, MAX_OBJECTS_PER_CONNECTION);
    }

    protected OutputConnection getOutputConnection(EndPoint destination) {
        OutputConnection oc = _connections.get(destination);
        if (oc == null) {
            OutputConnection newOc = new ExteriorOutputConnection(destination);
            oc = _connections.putIfAbsent(destination, newOc);
            if (oc == null) {
                return newOc;
            }
        }
        return oc;
    }

    protected class ExteriorOutputConnection extends OutputConnection {


        /**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private ExteriorOutputStream _eos;

        public ExteriorOutputConnection(EndPoint ep) {
            super(ep);
            _eos = null;
        }

        public void sendObject(Object obj) throws IOException {
            lock();
            try {
                if (_eos == null) {
                    Socket sock = _endPoint.createSocket();
                    sock.setTcpNoDelay(true);
                    sock.setKeepAlive(true);
                    _eos = new ExteriorOutputStream(
                            new BufferedOutputStream(sock.getOutputStream()));
                }

                _eos.writeExterior(obj);
                _eos.flush();
                _sentObjectCount++;

                if (_sentObjectCount > MAX_OBJECTS_PER_CONNECTION) {
                    _eos.close();
                    _eos = null;
                    _sentObjectCount = 0;
                }
            } finally {
                unlock();
            }
        }

        public void close() throws IOException {
            lock();
            try {
                if (_eos != null) {
                    _eos.close();
                }
            } finally {
                unlock();
            }
        }
    }

}
