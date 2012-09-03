/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hdfs.server.namenode.NameNode;

/**
 * @author hanhlh
 *
 */
public class NameNodeFBTProcessor {

	//private static Log log = LogFactory.getLog(NameNodeFBTProcessor.class);

    /** サービスレジストリ */
    private static Map<String, Object> _registry;

    static {
        _registry = new ConcurrentHashMap<String, Object>();
    }

    // class methods //////////////////////////////////////////////////////////

    public static void bind(String name, Object obj) {
        if (NameNode.LOG.isDebugEnabled()) {
            NameNode.LOG.debug(name + " = " + obj);
        }
        _registry.put(name, obj);
    }

    public static Object lookup(String name) {
        return _registry.get(name);
    }

}
