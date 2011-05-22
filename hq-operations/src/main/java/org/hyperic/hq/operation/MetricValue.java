/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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
 */

package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hyperic.util.data.IComparableDatapoint;
import org.hyperic.util.data.IDisplayDataPoint;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MetricValue implements IDisplayDataPoint, IComparableDatapoint {

    private double value;

    private long timestamp;

    public static final double VALUE_NONE = Double.NaN;

    /**
     * Metric template is valid, but no value is available.
     * Plugins may return this value, for example, where
     * certain metrics are only available depending on resource
     * configuration, version, etc.
     * NONE.getValue() is the VALUE_NONE constant.
     * This value will not be sent back to the server.
     */
    public static final MetricValue NONE = new MetricValue(VALUE_NONE);

    public static final double VALUE_FUTURE = Double.POSITIVE_INFINITY;

    /**
     * Metric template is valid, but no value is available.
     * The value is being collected in the background and
     * is expected to be available in the near future.
     * The agent may re-try collection of measurements returning
     * this value sooner than the configured collection interval.
     * FUTURE.getValue() is the VALUE_FUTURE constant.
     * This value will not be sent back to the server.
     */
    public static final MetricValue FUTURE = new MetricValue(VALUE_FUTURE);
 
    public MetricValue() {}

    @JsonCreator
    public MetricValue(@JsonProperty("value") double value, @JsonProperty("timestamp")long timestamp) { 
        value = value;
        this.timestamp = timestamp;
    }

    public MetricValue(MetricValue src) {
        timestamp = src.getTimestamp();
        value     = src.getValue();
    }

    /**
     * Default retrieval time to System.currentTimeMillis()
     */
    public MetricValue(double value) {
        this(value, System.currentTimeMillis());
    }

    public MetricValue(Number objectValue, long rtime) {
        this(objectValue.doubleValue(), rtime);
    }

    /**
     * Default retrieval time to System.currentTimeMillis()
     */
    public MetricValue(Number objectValue) {
        this(objectValue, System.currentTimeMillis());
    }

    public MetricValue(MetricValue objectValue, long rtime) {
        this(objectValue.getValue(), rtime);
    }

    public MetricValue(long value, long rtime) {
        this((double) value, rtime);
    }

    /**
     * Default retrieval time to System.currentTimeMillis()
     */
    @JsonCreator
    public MetricValue(long value) {
        this(value, System.currentTimeMillis());
    }

    /**
     * @return true if getValue() == VALUE_NONE, false otherwise.
     */
    public boolean isNone() {
        return Double.isNaN(value);
    }

    /**
     * @return true if getValue() == VALUE_FUTURE, false otherwise.
     */
    public boolean isFuture() {
        return Double.isInfinite(value);
    }

    /**
     * @return "None" if isNone(), "Future" if isFuture(),
     *  else NumberFormat.format(getValue())
     */
    public String toString () {
        if (isNone()) {
            return "None";
        }
        else if (isFuture()) {
            return "Future";
        }
        // Format the double value
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(value);
    }

    public Double getObjectValue () {
        return value;
    }

    public double getValue () {
        return value;
    }

    public long getTimestamp () {
        return timestamp;
    }

    public String getLabel() {
        return SimpleDateFormat.getDateTimeInstance().format(new Date(getTimestamp()));
    }

    /**
     * This is for the Datapoint interface.  It compares only the
     * value of the measurements, not the timestamp.
     */
    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }

        MetricValue o2 = (MetricValue)o;
        double difference = value - o2.value;
        // can't just return subtraction, because casting to integer
        // loses the negative values for small differences (< 1), which we
        // need.
        if (difference < 0) return -1;
        if (difference > 0) return 1;
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof MetricValue) {
            MetricValue val = (MetricValue) obj;
            return getTimestamp() == val.getTimestamp() &&
                    ((getValue() == val.getValue()) || (Double.isNaN(getValue()) && Double.isNaN(val.getValue())));
        }
        return false;
    }
}
