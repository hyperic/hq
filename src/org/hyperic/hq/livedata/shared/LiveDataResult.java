package org.hyperic.hq.livedata.shared;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

/**
 * Result object from live data commands.
 */
public class LiveDataResult implements Serializable {

    private boolean _error;
    private Throwable _cause;
    private String _errorMsg;
    private String _xml;

    public LiveDataResult(String xml) {
        _error = false;
        _xml = xml;
    }

    public LiveDataResult(Throwable t, String errorMsg) {
        _error = true;
        _errorMsg = errorMsg;
        _cause = t;
    }

    /**
     * Get the raw XML result for this request.
     */
    public String getXMLResult() {
        return _xml;
    }

    /**
     * Get the Object result for this request.
     */
    public Object getObjectResult() {
        XStream xstream = new XStream();
        return xstream.fromXML(_xml);
    }

    /**
     * Get the error (if it exists) for this command.  Not always guaranteed
     * to be non-null if hasError() is true. 
     */
    public Throwable getCause() {
        return _cause;
    }

    /**
     * Get the error string for this result.
     */
    public String getErrorMessage() {
        return _errorMsg;
    }

    /**
     * True if an error occured collecting the live data
     */
    public boolean hasError() {
        return _error;
    }
}
