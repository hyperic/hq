/*
 * Created on Jun 10, 2003
 */
package org.hyperic.hq.control.shared;

import org.hyperic.hq.common.ApplicationException;

/**
 *
 * Exception when scheduled jobs not found
 */
public class ScheduledJobNotFoundException extends ApplicationException {

    /**
     * Default constructor
     */
    public ScheduledJobNotFoundException() {
        super();
    }

    /**
     * @param s the error string
     */
    public ScheduledJobNotFoundException(String s) {
        super(s);
    }

    /**
     * @param t the nested exception
     */
    public ScheduledJobNotFoundException(Throwable t) {
        super(t);
    }

    /**
     * @param s the error string
     * @param t the nested exception
     */
    public ScheduledJobNotFoundException(String s, Throwable t) {
        super(s, t);
    }
}
