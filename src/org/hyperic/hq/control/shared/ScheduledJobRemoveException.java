/*
 * Created on Jun 10, 2003
 */
package org.hyperic.hq.control.shared;

import org.hyperic.hq.common.ApplicationException;

/**
 *
 * Exception when removing scheduled jobs
 */
public class ScheduledJobRemoveException extends ApplicationException {

    /**
     * Default constructor
     */
    public ScheduledJobRemoveException() {
        super();
    }

    /**
     * @param s the error string
     */
    public ScheduledJobRemoveException(String s) {
        super(s);
    }

    /**
     * @param t the nested exception
     */
    public ScheduledJobRemoveException(Throwable t) {
        super(t);
    }

    /**
     * @param s the error string
     * @param t the nested exception
     */
    public ScheduledJobRemoveException(String s, Throwable t) {
        super(s, t);
    }
}
