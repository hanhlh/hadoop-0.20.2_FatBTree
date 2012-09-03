/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hanhlh
 *
 */
public class GroupedThreadFactory implements ThreadFactory {

	private final ThreadGroup tgroup;

	private final String tname;

	private AtomicInteger count;

	public GroupedThreadFactory(String name) {
		tgroup = new ThreadGroup("Receivers");
		count = new AtomicInteger(0);
		tname = name;
	}

	/* (»Û Javadoc)
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */

	public Thread newThread(Runnable r) {
		return new Thread(tgroup, r, tname + count.incrementAndGet());
	}

}
