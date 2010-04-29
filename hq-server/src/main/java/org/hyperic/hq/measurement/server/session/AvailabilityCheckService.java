/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.measurement.server.session;

/**
 * .
 */
public interface AvailabilityCheckService {

    void backfill();
    /**
     * Forces a backfill to start immediately with timeInMillis as the "current time."
     * Only used for integration tests
     */
    void backfill(long timeInMillis);

}
