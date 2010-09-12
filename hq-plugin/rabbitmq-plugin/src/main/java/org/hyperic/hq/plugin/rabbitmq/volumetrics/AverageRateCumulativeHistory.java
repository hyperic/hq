/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.volumetrics;

/**
 * AverageRateCumulativeHistory
 * Cumulative statistics for rate with higher weight given to recent data but without storing any history. Older values
 * are given exponentially smaller weight, with a decay factor determined by a duration chosen by the client.
 *
 * @author Helena Edelson
 */
public class AverageRateCumulativeHistory {

    private int count;
    private double weight;
    private double sum;
    private double sumSquares;
    private double min;
    private double max;
    private volatile long t0 = System.currentTimeMillis();
    private final double lapse;
    private final double decay;
    private final double period;

    public AverageRateCumulativeHistory(double period, double lapsePeriod, int window) {
        this.lapse = lapsePeriod > 0 ? 1. / lapsePeriod : 0;
        this.period = period;
        this.decay = window > 0 ? 1 - 1. / window : 1;
    }

    public void increment() {

        long t = System.currentTimeMillis();
        double value = t > t0 ? (t - t0) / period : 0;
        if (value > max || count == 0) {
            max = value;
        }
        if (value < min || count == 0) {
            min = value;
        }
        double alpha = Math.exp((t0 - t) * lapse) * decay;
        t0 = t;
        sum = alpha * sum + value;
        sumSquares = alpha * sumSquares + value * value;
        weight = alpha * weight + 1;
        count++;
    }

    public int getCount() {
        return count;
    }

    /**
     * @return the time in seconds since the last measurement
     */
    public int getTimeSinceLastMeasurement() {
        return new Long((System.currentTimeMillis() - t0) / 1000).intValue();
    }

    public double getMean() {
        if (count == 0) {
            return 0;
        }
        long t = System.currentTimeMillis();
        double value = t > t0 ? (t - t0) / period : 0;
        double alpha = Math.exp((t0 - t) * lapse) * decay;
        double sum = alpha * this.sum + value;
        double weight = alpha * this.weight + 1;
        return sum > 0 ? weight / sum : 0.;
    }

    public double getStandardDeviation() {
        if (count == 0) {
            return 0;
        }
        long t = System.currentTimeMillis();
        double value = t > t0 ? (t - t0) / period : 0;
        double alpha = Math.exp((t0 - t) * lapse) * decay;
        double sum = alpha * this.sum + value;
        double weight = alpha * this.weight + 1;
        double sumSquares = alpha * this.sumSquares + value * value;
        double mean = sum / weight;
        double stdevPeriod = Math.sqrt(sumSquares / weight - mean * mean);
        return stdevPeriod > 0 ? 1 / stdevPeriod : 0.;
    }

    public double getMax() {
        return min > 0 ? 1 / min : 0;
    }

    public double getMin() {
        return max > 0 ? 1 / max : 0;
    }

    @Override
    public String toString() {
        return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f, timeSinceLast=%f]", getCount(), getMin(), getMax(), getMean(),
                getStandardDeviation(), getTimeSinceLastMeasurement());
    }

}
