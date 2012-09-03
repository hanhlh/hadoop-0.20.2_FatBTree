/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg.io;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;


import org.apache.hadoop.hdfs.server.namenodeFBT.msg.Message;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageHandler;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.PooledReceiver;

/**
 * @author hanhlh
 *
 */
public class ExteriorizableReceiver extends PooledReceiver {

	public ExteriorizableReceiver(int port, BlockingQueue<Message> queue,
            int threads) throws IOException, SocketException {
        super(port, queue, threads);
    }

    public ExteriorizableReceiver(int port, MessageHandler defaultHandler,
            BlockingQueue<Message> queue, int threads) throws IOException,
            SocketException {
        super(port, defaultHandler, queue, threads);
    }

    public void run() {
    	System.out.println("ExteriorizableReceiver run line 38");
        startForwarder();
        new Thread(_observer, "ReceiverObserver").start();

        while (_active.get()) {
            try {
                _ssock.setSoTimeout(5000);
                Socket sock = _ssock.accept();
                _executor.execute(new ExteriorizableReceiverThread(sock));
            } catch (SocketTimeoutException ste) {
                /* ignore */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected class ExteriorizableReceiverThread extends ReceiverThread {

        /**
         * <p> 引数で指定されたソケットからメッセージs を受け取り，
         * 対応するハンドラを起動して処理をするオブジェクトを作成します．</p>
         * @param sock 確立したコネクション
         */
        public ExteriorizableReceiverThread(Socket sock) {
            super(sock);
        }

        /**
         * ソケットからメッセージs を受け取り，
         * 対応するハンドラを起動して処理をします．
         *
         * ハンドラ内の処理は，ハンドラ内で明示的にスレッドを作成しない限り，
         * このスレッドと同じスレッドで行われます．
         * @throws IOException
         */
        protected void receive(InputStream is) throws IOException {
            ExteriorInputStream eis =
                new ExteriorInputStream(new BufferedInputStream(is));

            Message message = null;
            MessageHandler handler = null;
            while (true) {
                try {
                    message = (Message) eis.readExterior();

                    handler = getHandler(message.getHandlerID());

                    handler.handle(message);
                } catch (EOFException e) {
                    eis.close();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("error\n" + message.getClass() + "\n" +
                            message + "\n" + handler + "\n");
                }
            }
        }
    } // end-of class ExteriorizalbeReceiverThread
}
