/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hdfs.server.namenodeFBT.FBTDirectory;



/**
 * @author hanhlh
 *
 */
public class StringUtility {

	static int PAD_LIMIT = 10;
	 public static String toMaxRadixString(int i) {
	        return Integer.toString(i, Character.MAX_RADIX);
	    }

	    public static String toMaxRadixTwoDigitString(int i) {
	        return toMaxRadixString(i, 2);
	    }

	    public static String toMaxRadixString(int i, int digit) {
	        return toString(i, digit, Character.MAX_RADIX);
	    }

	    public static String toString(int i, int digit, int radix) {
	        String buf = Integer.toString(i, radix);
	        return StringUtils.leftPad(buf, digit, '0');
	    }

	    public static String generateDefaultSeparator(int partitionID, int datanodeNumber,
	    		String experimentDir, int low, int high) {
	    	int separator = low + (partitionID-113+1)*(high-low)/(datanodeNumber);
	    	System.out.println("separator "+separator);
	    	return experimentDir.concat(String.format("%06d", separator));
	    }

	    public static String[] generateRange(int namenodeID, int datanodeNumber,
	    		String experimentDir, int low, int high) {
	    	String[] range = new String[2];
	    	if (namenodeID>0 && namenodeID<=datanodeNumber) {

		    	int start = low +(namenodeID-1)*(high-low)/(datanodeNumber);
		    	int end = low + (namenodeID)*(high-low)/(datanodeNumber)-1;
		    	if (namenodeID == 1) {
		    		start = low;
		    	}
		    	range[0] = experimentDir.concat(String.format("%06d", start));
		    	System.out.println("generateRange.start "+range[0]);
		    	if (namenodeID == datanodeNumber) {
		    		end = high-1;
		    	}

		    	range[1] = experimentDir.concat(String.format("%06d", end));
		    	System.out.println("generateRange.end "+range[1]);
	    	}
	    	return range;
	    }
	    public static void debugSpace(String className) {
	    	System.out.println(FBTDirectory.SPACE);
	    	System.out.println(className);

	    }
	}
