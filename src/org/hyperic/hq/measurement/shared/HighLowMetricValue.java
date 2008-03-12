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

package org.hyperic.hq.measurement.shared;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.data.IComparableDatapoint;
import org.hyperic.util.data.IHighLowDataPoint;

/**
 * Represents a value of a Metric.
 *
 * @see MeasurementPluginManager#getValue
 * @see MeasurementPlugin#getValue
 */
public class HighLowMetricValue extends MetricValue
    implements IHighLowDataPoint, IComparableDatapoint, Serializable {
    private double  highValue;
    private double  lowValue;
    private int     count = 0;

    /** Empty constructor for SOAP serialization
     */
    public HighLowMetricValue() {
        super();
    }

    /**
     * Construct with value.
     */
    public HighLowMetricValue(double value, double highValue, double lowValue,
                              long rtime) {
        super(value, rtime);
        this.highValue = highValue;
        this.lowValue = lowValue;
    }

    /**
     * Construct with values.
     */
    public HighLowMetricValue(double value, long rtime) {
        this(value, value, value, rtime);
    }

    /**
     * Default retrieval time to System.currentTimeMillis()
     */
    public HighLowMetricValue(double value) {
        this(value, System.currentTimeMillis());
    }

    /**
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(Number objectValue, long rtime) {
        this(objectValue.doubleValue(), rtime);
    }

    /*
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(HighLowMetricValue objectValue, long rtime) {
        this(objectValue.getValue(), rtime);
    }

    /*
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(long value, long rtime) {
        this((double) value, rtime);
    }

    /** Get the Object value. Useful if you don't yet care what the type is. */
    public Double getObjectValue () { return new Double(getValue()); }

    /* (non-Javadoc)
     * @see net.covalent.chart.IDataPoint#getLabel()
     */
    public String getLabel() {
        return SimpleDateFormat.getDateTimeInstance().format(
            new Date(this.getTimestamp()));
    }

    public double getLowValue() {
        return lowValue;
    }

    public void setLowValue(double lowValue) {
        this.lowValue = lowValue;
    }

    public double getHighValue() {
        return highValue;
    }

    public void setHighValue(double highValue) {
        this.highValue = highValue;
    }
    public void incrementCount() {
        this.count++;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public int getCount() {
        return count;
    }

    /**
     * This is for the Datapoint interface.  It compares only the
     * value of the measurements, not the timestamp.
     */
    public int compareTo(Object o) {
        HighLowMetricValue o2 = (HighLowMetricValue)o;
        double difference = this.getValue() - o2.getValue();
        // can't just return subtraction, because casting to integer
        // loses the negative values for small differences (< 1), which we 
        // need.
        if (difference < 0) return -1;
        if (difference > 0) return 1;
        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof HighLowMetricValue) {
            HighLowMetricValue val = (HighLowMetricValue) obj;
            return (this.getTimestamp() == val.getTimestamp() &&
                    this.getValue() == val.getValue());
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.hyperic.util.data.IStackedDataPoint#getValues()
     */
    public double[] getValues() {
        return new double[] { this.getValue() };
    }
}
