package org.hyperic.hq.livedata.shared;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

/**
 * Result object from live data commands.
 */
public class LiveDataResult implements Serializable {

    private String _xml;

    public LiveDataResult(String xml) {
        _xml = xml;
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
}
