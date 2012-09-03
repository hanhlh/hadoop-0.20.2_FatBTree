/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.response;

/**
 * @author hanhlh
 *
 */
public interface ResponseClassFactory {

	public Class<?> createDumpResponseClass();

    public Class<?> createSearchResponseClass();
    public Class<?> createRangeSearchResponseClass();

    public Class<?> createInsertResponseClass();

    public Class<?> createSynchronizeRootResponseClass();

    public Class<?> createDeleteResponseClass();

    public Class<?> createMigrateResponseClass();

    public Class<?> createGetAdditionalBlockResponseClass();
    public Class<?> createGetBlockLocationsResponseClass();
    public Class<?> createCompleteFileResponseClass();
}
