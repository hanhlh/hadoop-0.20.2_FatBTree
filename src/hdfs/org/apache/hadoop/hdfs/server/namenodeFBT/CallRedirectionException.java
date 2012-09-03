/**
 *
 */
package org.apache.hadoop.hdfs.server.namenodeFBT;


/**
 * @author hanhlh
 *
 */
public class CallRedirectionException extends RuntimeException{

/**
	 *
	 */
	private static final long serialVersionUID = 1L;
// instance attributes ///////////////////////////////////////////////

    private final String _url;

    // constructors //////////////////////////////////////////////////////

    public CallRedirectionException(String url) {
        super();
       //StringUtility.debugSpace("CallRedirectionException");
        _url = url;
    }

    // accessors /////////////////////////////////////////////////////////

    public String getURL() {
        return _url;
    }
}
