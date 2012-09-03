package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter{
	public String format(LogRecord arg0) {
		return arg0.getMessage();
	}
}
