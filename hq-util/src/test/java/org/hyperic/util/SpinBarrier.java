/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.util;

/**
 * Utility that continuosly evaluates a specified condition at specified retry
 * intervals, until either the condition is true or the specified timeout is
 * reached. This utility can be used, for example, by test classes that need to
 * verify the completion of some asynchronous task. It is expected that most
 * instrumentation tests will use this class to verify that performance monitors
 * have been asynchronously updated
 * 
 * @author Jennifer Hickey
 * 
 */
public class SpinBarrier {
    private long timeout = 120000l;
    private long retryInterval = 250l;
    private final SpinBarrierCondition condition;

    /**
     * Constructor that uses the default timeout and retry interval
     * 
     * @param condition
     *            The condition to evaluate
     */
    public SpinBarrier(SpinBarrierCondition condition) {
        this.condition = condition;
    }

    /**
     * 
     * @param timeout
     *            The amount of time, in milliseconds, to continue evaluating
     *            the condition if it has not been met
     * @param retryInterval
     *            The amount of time to wait between evaluations of the
     *            condition
     * @param condition
     *            The condition to evaluate
     */
    public SpinBarrier(long timeout, long retryInterval, SpinBarrierCondition condition) {
        this.timeout = timeout;
        this.retryInterval = retryInterval;
        this.condition = condition;
    }

    /**
     * Evaluates a specified condition at specified retry intervals, until
     * either the condition is true or the specified timeout is reached
     * 
     * @return true if the condition evaluated to true before the timeout was
     *         reached
     */
    public boolean waitFor() {
        final long startTime = System.currentTimeMillis();
        boolean conditionMet = false;
        while (!(conditionMet) && (System.currentTimeMillis() < (startTime + timeout))) {
            conditionMet = condition.evaluate();
            if (conditionMet) {
                continue;
            }
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        return conditionMet;
    }
}
