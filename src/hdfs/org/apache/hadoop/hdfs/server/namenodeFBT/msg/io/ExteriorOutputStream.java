/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
 * @author hanhlh
 *
 */
public class ExteriorOutputStream extends ObjectOutputStream{

	protected ExteriorOutputStream(OutputStream out)
				throws IOException, SecurityException {
		super(out);
		// TODO 自動生成されたコンストラクター・スタブ
	}
	public void writeExterior(Object obj) throws IOException {
        if (obj == null) {
            writeByte(-1);
        } else if (obj instanceof Exteriorizable) {
            writeByte(1);
            writeAscii(obj.getClass().getName());
            ((Exteriorizable) obj).writeExterior(this);
        } else {
            writeByte(0);
            writeUnshared(obj);
        }
    }

    public void writeAscii(String str) throws IOException {
        if (str == null) {
            writeByte(-1);
        } else {
            writeByte(0);
            writeUTF(str);
        }
//        if (str == null) {
//            writeShort(-1);
//        } else {
//            int size = str.length();
//            byte[] bytes = new byte[size];
//            for (int i = 0; i < bytes.length; i++) {
//                bytes[i] = (byte) str.charAt(i);
//            }
//
//            writeShort(size);
//            write(bytes);
//        }
    }


}
