/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.server.session;

public class Schedule {
    
    // The maximum number of times to calculate a measurement
    public static final int MAX_ATTEMPT =
        CalculateDerivedMeasurementJob.MAX_ATTEMPTS_RECENT;
    
    // The amount of time to wait to recalculate
    public static final long ATTEMPT_WAIT = 10000;

    private int measurementId;

    private int attempt = 1;

    private long time;

    private long reservation;

    private int dequeue = 0;
    
    /**
     * Constructor
     * @param time when this measurement is scheduled for
     * @param measurementId the measurementId
     *
     */
    public Schedule(int measurementId, long time) {
        init(measurementId, time, time);
    }
    
    /** Constructor
     * @param time when this measurement is scheduled for
     * @param measurementId the measurementId in Integer object
     *
     */
    public Schedule(Integer measurementId, long time) {
        init(measurementId.intValue(), time, time);
    }
    
    /** Constructor
     * @param reservation the time the measurement is to be calculated
     * @param time when this measurement is scheduled for
     * @param measurementId the measurementId
     * @param attempt the number of attempts
     *
     */
    public Schedule(int measurementId, long time, long reservation,
                    int attempt, int dequeue) {
        init(measurementId, time, reservation);
        this.attempt = attempt;
        this.dequeue = dequeue;
    }
    
    private void init(int measurementId, long time, long reservation) {
        this.measurementId = measurementId;
        this.time = time;
        this.reservation = reservation;
    }
    
    /** Getter for property measurementId.
     * @return Value of property measurementId.
     *
     */
    public int getMeasurementId() {
        return this.measurementId;
    }
    
    /** Setter for property measurementId.
     * @param measurementId New value of property measurementId.
     *
     */
    public void setMeasurementId(int measurementId) {
        this.measurementId = measurementId;
    }
    
    /** Getter for property attempt.
     * @return Value of property attempt.
     *
     */
    public int getAttempt() {
        return this.attempt;
    }
    
    /** Setter for property attempt.
     * @param attempt New value of property attempt.
     *
     */
    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }
    
    /** Increase the number of attempts on this Schedule
     *
     */
    public void incrementAttempt() {
        this.attempt++;
        
        // Push the scheduled time
        setTime(this.time + ATTEMPT_WAIT);
    }
    
    /** Getter for property time.
     * @return Value of property time.
     *
     */
    public long getTime() {
        return this.time;
    }
    
    /** Setter for property time.
     * @param time New value of property time.
     *
     */
    public void setTime(long time) {
        this.time = Math.max(time, System.currentTimeMillis());
    }
    
    /** Getter for property reservation.
     * @return Value of property reservation.
     *
     */
    public long getReservation() {
        return this.reservation;
    }
    
    /** Setter for property reservation.
     * @param reservation New value of property reservation.
     *
     */
    public void setReservation(long reservation) {
        // Assume that we are starting over
        this.reservation = reservation;
        setTime(reservation);
        setAttempt(1);
    }
    
    /** Getter for property dequeue.
     * @return Value of property dequeue.
     *
     */
    public int getDequeue() {
        return this.dequeue;
    }
    
    /** Setter for property dequeue.
     * @param dequeue New value of property dequeue.
     *
     */
    public void setDequeue(int dequeue) {
        this.dequeue = dequeue;
    }
}
