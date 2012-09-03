/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.msg.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author hanhlh
 *
 */
public class ExteriorInputStream extends ObjectInputStream {

private static final Map<String, Method> _entries;

    static {
        _entries = new ConcurrentHashMap<String, Method>();
    }

    public ExteriorInputStream(InputStream in) throws IOException {
        super(in);
    }

    public Object readExterior() throws IOException {
        try {
            int serialID = readByte();
            if (serialID == -1) {
                return null;
            }
            if (serialID == 0) {
                return readUnshared();
            }

            String name = readAscii();
            Method method = _entries.get(name);
            if (method == null) {
                Class clazz = Class.forName(name);
                Class[] parameterTyes = new Class[]{ExteriorInputStream.class};
                method = clazz.getMethod("newInstance", parameterTyes);
                _entries.put(name, method);
            }
            Exteriorizable ext = (Exteriorizable) method.invoke(null, this);
            ext.readExterior(this);
            return ext;
        } catch (EOFException e) {
        	e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidClassException("unable to create instance");
        }
    }

    @SuppressWarnings("deprecation")
    public String readAscii() throws IOException {
        if (readByte() == -1) {
            return null;
        }
        return readUTF();
//        short size = readShort();
//        if (size == -1) {
//            return null;
//        }
//
//        byte[] bytes = new byte[size];
//        read(bytes);
//        return new String(bytes, 0, 0, size);
    }


}
