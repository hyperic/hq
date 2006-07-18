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

package org.hyperic.hq.measurement;


/**
 * This helper class is full of "voodoo" functions related to
 * synchronizing timed data between the agent and the server.  We call
 * them "voodoo" because they deal mostly with rounding down and
 * guestimating percentages.
 *
 */
public final class TimingVoodoo {
    /**
     * Given the approximate time associated with a data point and the
     * interval at which that adta point is being collected, compute
     * the exact data point to which the data point corresponds.
     *
     * @param approxTime the approximate time to which the data point corresponds
     * @param interval the collection interval
     * @return the time, rounded down to the previous collection interval
     */
    public static long roundDownTime(long approxTime, long interval) {
        return approxTime - (approxTime % interval);
    }

    /**
     * Given the approximate time associated with a data point and the
     * interval at which that adta point is being collected, compute
     * the exact data point to which the data point corresponds.
     *
     * @param approxTime the approximate time to which the data point corresponds
     * @param interval the collection interval
     * @return the time, rounded up or down to the closest collection interval
     */
    public static long closestTime(long approxTime, long interval) {
        long mod = approxTime % interval;
        
        if (mod > interval / 2) {
            // Round up
            approxTime += interval;
        }
        
        return approxTime - mod;
    }

    /**
     * Compute the upper and lower bound that should be used in the
     * SQL query for finding the collected data points based on the
     * approximate time they were taken and the interval.
     *
     * @param approxTime the approximate time the data points were collected
     * @param interval the collection interval
     * @return an array of size 2: the upper and lower bound
     */
    public static long[] current(long approxTime, long interval) {
        long[] retval = new long[2];
        long offset = Math.round
            (interval * MeasurementConstants.DEFAULT_TIMING_WINDOW_PERCENTAGE);

        retval[0] = approxTime - offset;
        retval[1] = approxTime + offset;

        return retval;
    }

    /**
     * Compute the upper and lower bound that should be used in the
     * SQL query for finding the collected data points based on the
     * approximate time they were taken, the interval and how many
     * intervals ago the data was collected.
     *
     * @param approxTime the approximate time the data points were collected
     * @param interval the collection interval
     * @return an array of size 3: the upper and lower bound, and the
     * exact interval time considering backTicks
     */
    public static long[] previous(long approxTime, long interval, int backTicks) {
        long[] retval = new long[3];

        long offset = Math.round
            (interval * MeasurementConstants.DEFAULT_TIMING_WINDOW_PERCENTAGE);

        retval[2] = approxTime - (backTicks * interval);
        retval[0] = retval[2] - offset;
        retval[1] = retval[2] + offset;

        return retval;
    }

    /**
     * Compute the upper and lower bound that should be used in the
     * SQL query for finding the collected data points based on the
     * approximate time they were taken, the interval and how many
     * intervals of data are being considered in the aggregate
     * function.
     *
     * @param approxTime the approximate time the data points were collected
     * @param interval the collection interval
     * @return an array of size 2: the upper and lower bound
     */
    public static long[] aggregate(long approxTime, long interval, int backTicks) {
        long[] retval = new long[2];

        long offset = Math.round
            (interval * MeasurementConstants.DEFAULT_TIMING_WINDOW_PERCENTAGE);

        retval[0] = approxTime - (backTicks * interval) - offset;
        retval[1] = approxTime + offset;

        return retval;
    }

    /**
     * Compute (in milliseconds) what is considered the latency
     * percentage based on the interval.
     *
     * @param interval the collection interval
     * @return latency percentage cushion
     */
    public static long percOfInterval(long interval) {
        return Math.round(interval * MeasurementConstants.DATA_DEFAULT_LATENCY_PERC);
    }

    // static class, no ctor access
    private TimingVoodoo() { }
}

// EOF
