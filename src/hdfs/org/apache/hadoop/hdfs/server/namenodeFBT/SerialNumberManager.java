/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hdfs.server.namenodeFBT.utils.StringUtility;

/**
 * @author hanhlh
 *
 * Should be distributed
 *
 */
class SerialNumberManager {

	/** This is the only instance of {@link SerialNumberManager}.*/
	  static final SerialNumberManager INSTANCE = new SerialNumberManager();

	  private SerialNumberMap<String> usermap = new SerialNumberMap<String>();
	  private SerialNumberMap<String> groupmap = new SerialNumberMap<String>();

	  private SerialNumberManager() {}

	  int getUserSerialNumber(String u) {
		  StringUtility.debugSpace("getUserSerialNumber");
		  System.out.println("user "+u);
		  return usermap.get(u);
	  }
	  int getGroupSerialNumber(String g) {
		  StringUtility.debugSpace("getGroupSerialNumber");
		  System.out.println("group "+g);
		  return groupmap.get(g);
	  }

	  String getUser(int n) {
		  return usermap.get(n);
	  }
	  String getGroup(int n) {
		  return groupmap.get(n);
	  }

	  {
	    getUserSerialNumber(null);
	    getGroupSerialNumber(null);
	  }

	  private static class SerialNumberMap<T> {
	    private int max = 0;
	    private int nextSerialNumber() {return max++;}

	    private Map<T, Integer> t2i = new HashMap<T, Integer>();
	    private Map<Integer, T> i2t = new HashMap<Integer, T>();

	    synchronized
	    int get(T t) {
	    	StringUtility.debugSpace("SerialNumberManager.get(T t) "+t);
	      Integer sn = t2i.get(t);
	      if (sn == null) {
	        sn = nextSerialNumber();
	        t2i.put(t, sn);
	        i2t.put(sn, t);
	      }
	      return sn;
	    }

	    synchronized
	    T get(int i) {
	    	StringUtility.debugSpace("SerialNumberManager.get (i) "+i);
	      if (!i2t.containsKey(i)) {
	        //throw new IllegalStateException("!i2t.containsKey(" + i
	        //    + "), this=" + this);
	      }
	      return i2t.get(i);
	    }

	    /** {@inheritDoc} */
	    public String toString() {
	      return "max=" + max + ",\n  t2i=" + t2i + ",\n  i2t=" + i2t;
	    }
	  }

}
