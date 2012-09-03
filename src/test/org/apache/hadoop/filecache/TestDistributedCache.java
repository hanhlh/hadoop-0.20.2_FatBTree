package org.apache.hadoop.filecache;

import java.io.IOException;
import java.net.URI;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenodeFBT.msg.MessageException;
import org.apache.hadoop.hdfs.server.namenodeFBT.service.ServiceException;

import junit.framework.TestCase;

public class TestDistributedCache extends TestCase {

  static final URI LOCAL_FS = URI.create("file:///");
  private static String TEST_CACHE_BASE_DIR =
    new Path(System.getProperty("test.build.data","/tmp/cachebasedir"))
    .toString().replace(' ', '+');
  private static String TEST_ROOT_DIR =
    System.getProperty("test.build.data", "/tmp/distributedcache");
  private static final int TEST_FILE_SIZE = 4 * 1024; // 4K
  private static final int LOCAL_CACHE_LIMIT = 5 * 1024; //5K
  private Configuration conf;
  private Path firstCacheFile;
  private Path secondCacheFile;
  private FileSystem localfs;

  /**
   * @see TestCase#setUp()
   */
  @Override
  protected void setUp() throws IOException {
    conf = new Configuration();
    conf.setLong("local.cache.size", LOCAL_CACHE_LIMIT);
    try {
		localfs = FileSystem.get(LOCAL_FS, conf);
	} catch (MessageException e) {
		// TODO 自動生成された catch ブロック
		e.printStackTrace();
	}
    firstCacheFile = new Path(TEST_ROOT_DIR+"/firstcachefile");
    secondCacheFile = new Path(TEST_ROOT_DIR+"/secondcachefile");
    try {
		createTempFile(localfs, firstCacheFile);
		 createTempFile(localfs, secondCacheFile);
	} catch (MessageException e) {
		// TODO 自動生成された catch ブロック
		e.printStackTrace();
	} catch (ServiceException e) {
		// TODO 自動生成された catch ブロック
		e.printStackTrace();
	}

  }

  /** test delete cache */
  public void testDeleteCache() throws Exception {
    DistributedCache.getLocalCache(firstCacheFile.toUri(), conf, new Path(TEST_CACHE_BASE_DIR),
        false, System.currentTimeMillis(), new Path(TEST_ROOT_DIR));
    DistributedCache.releaseCache(firstCacheFile.toUri(), conf);
    //in above code,localized a file of size 4K and then release the cache which will cause the cache
    //be deleted when the limit goes out. The below code localize another cache which's designed to
    //sweep away the first cache.
    DistributedCache.getLocalCache(secondCacheFile.toUri(), conf, new Path(TEST_CACHE_BASE_DIR),
        false, System.currentTimeMillis(), new Path(TEST_ROOT_DIR));
    FileStatus[] dirStatuses = localfs.listStatus(new Path(TEST_CACHE_BASE_DIR));
    assertTrue("DistributedCache failed deleting old cache when the cache store is full.",
        dirStatuses.length > 1);
  }

  private void createTempFile(FileSystem fs, Path p) throws IOException, MessageException, ServiceException {
    FSDataOutputStream out = fs.create(p);
    byte[] toWrite = new byte[TEST_FILE_SIZE];
    new Random().nextBytes(toWrite);
    out.write(toWrite);
    out.close();
    FileSystem.LOG.info("created: " + p + ", size=" + TEST_FILE_SIZE);
  }

  /**
   * @see TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws IOException {
    try {
		localfs.delete(firstCacheFile, true);
		localfs.delete(secondCacheFile, true);
	} catch (MessageException e) {
		// TODO 自動生成された catch ブロック
		e.printStackTrace();
	}

    localfs.close();
  }
}
