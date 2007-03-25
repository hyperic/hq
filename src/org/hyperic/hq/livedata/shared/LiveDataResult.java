package org.hyperic.hq.livedata.shared;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Result object from live data commands.
 */
public class LiveDataResult implements Serializable {

    private AppdefEntityID _id;
    private boolean _error;
    private Throwable _cause;
    private String _errorMsg;
    private String _xml;

    public LiveDataResult(AppdefEntityID id, String xml) {
        _error = false;
        _id = id;
        _xml = xml;
    }

    public LiveDataResult(AppdefEntityID id, Throwable t, String errorMsg) {
        _error = true;
        _id = id;
        _cause = t;
        _errorMsg = errorMsg;
    }

    /**
     * Get the appdef entity id for this result
     */
    public AppdefEntityID getAppdefEntityID() {
        return _id;
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
