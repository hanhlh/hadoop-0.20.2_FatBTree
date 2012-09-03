/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

/**
 * @author hanhlh
 *
 */
public interface NodeVisitorFactory {

	public NodeVisitor createSearchVisitor();

    public NodeVisitor createInsertVisitor();

    public NodeVisitor createDeleteVisitor();

    public NodeVisitor createDumpVisitor();

    public NodeVisitor createInsertModifyVisitor();

    public NodeVisitor createDeleteModifyVisitor();

    public NodeVisitor createMigrateVisitor();

    public NodeVisitor createMigrateModifyVisitor();

    public NodeVisitor createRangeSearchVisitor();

    public NodeVisitor createGetAdditionalBlockVisitor();

    public NodeVisitor createGetBlockLocationsVisitor();

    public NodeVisitor createCompleteFileVisitor();
}
