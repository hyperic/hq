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

package org.hyperic.hq.product;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hyperic.util.data.IComparableDatapoint;
import org.hyperic.util.data.IDisplayDataPoint;

/**
 * Represents a value of a Metric.
 *
 * @see MeasurementPluginManager#getValue
 * @see MeasurementPlugin#getValue
 *
 * XXX: It would be best if this object were immutable. -- JMT 11/31/09
 */
public class MetricValue
    implements IDisplayDataPoint, IComparableDatapoint, Serializable 
{
    private static final long serialVersionUID = 8322695266759550631L;
    
    private double  _value;
    private long    _timestamp; 

    public static final double VALUE_NONE = Double.NaN;

    /**
     * Metric template is valid, but no value is available.
     * Plugins may return this value, for example, where
     * certain metrics are only available depending on resource
     * configuration, version, etc.
     * NONE.getValue() is the VALUE_NONE constant.
     * This value will not be sent back to the server.
     */
    public static final MetricValue NONE =
        new MetricValue(VALUE_NONE);

    public static final double VALUE_FUTURE =
        Double.POSITIVE_INFINITY;

    /**
     * Metric template is valid, but no value is available.
     * The value is being collected in the background and
     * is expected to be available in the near future.
     * The agent may re-try collection of measurements returning
     * this value sooner than the configured collection interval.
     * FUTURE.getValue() is the VALUE_FUTURE constant.
     * This value will not be sent back to the server.
     */
    public static final MetricValue FUTURE =
        new MetricValue(VALUE_FUTURE);

    public MetricValue() {}

    public MetricValue(double value, long rtime) {
        _timestamp = rtime;
        _value = value;
    }

    public MetricValue(MetricValue src) {
        _timestamp = src.getTimestamp();
        _value     = src.getValue();
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
    public MetricValue(long value) {
        this(value, System.currentTimeMillis());
    }

    /**
     * @return true if getValue() == VALUE_NONE, false otherwise.
     */
    public boolean isNone() {
        return Double.isNaN(_value);
    }

    /**
     * @return true if getValue() == VALUE_FUTURE, false otherwise.
     */
    public boolean isFuture() {
        return Double.isInfinite(_value);
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
        return nf.format(_value);
    }

    public Double getObjectValue () { return new Double(_value); }

    public double getValue () {
        return _value;
    }

    public void setValue(double value) {
        _value = value;
    }

    public long getTimestamp () {
        return _timestamp;
    }

    public void setTimestamp( long t ) {
        _timestamp = t;
    }

    public String getLabel() {
        return SimpleDateFormat.getDateTimeInstance().format(
            new Date(getTimestamp()));
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
        double difference = _value - o2._value;
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
                    ((getValue() == val.getValue()) || 
                     (Double.isNaN(getValue()) && Double.isNaN(val.getValue())));
        }

        return false;
    }
}
