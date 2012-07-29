package org.hyperic.hq.common;

/**
 * base exception class for timeframe related exceptions
 * 
 * @author yakarn
 *
 */
public abstract class TimeframeBaseException extends Exception {

    public TimeframeBaseException() {
        super();
    }

    public TimeframeBaseException(String s) {
        super(s);
    }

    public TimeframeBaseException(Throwable t) {
        super(t);
    }

    public TimeframeBaseException(String s, Throwable t) {
        super(s, t);
    }
}
