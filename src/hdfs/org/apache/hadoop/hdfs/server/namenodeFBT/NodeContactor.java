/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


/**
 * @author hanhlh
 *
 */
public interface NodeContactor {

	public void contact(MetaNode meta);

    public void contact(IndexNode index);

    public void contact(LeafNode leaf);

    public void contact(PointerNode pointer);
}
