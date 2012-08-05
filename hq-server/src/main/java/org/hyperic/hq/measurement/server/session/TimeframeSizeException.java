package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.common.TimeframeBaseException;

/**
 * for when the size of the frame halts hyperic from fetching dada.
 * 
 * @author yakarn
 *
 */
public class TimeframeSizeException extends TimeframeBaseException {
    public TimeframeSizeException() {
        super();
    }

    public TimeframeSizeException(String s) {
        super(s);
    }

    public TimeframeSizeException(Throwable t) {
        super(t);
    }

    public TimeframeSizeException(String s, Throwable t) {
        super(s, t);
    }

}
