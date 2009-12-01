/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import org.hyperic.hq.measurement.server.session.Baseline;

/**
 * Value object for Baseline.
 */
public class BaselineValue implements java.io.Serializable {
    private Integer _id;
    private long _computeTime;
    private boolean _userEntered;
    private Double _mean;
    private Double _minExpectedValue;
    private Double _maxExpectedValue;

    public BaselineValue() {
    }

    public Integer getId() {
        return _id;
    }

    public void setId(Integer id) {
        _id = id;
    }

    public long getComputeTime() {
        return _computeTime;
    }

    public void setComputeTime(long computeTime) {
        _computeTime = computeTime;
    }

    public boolean getUserEntered() {
        return _userEntered;
    }

    public void setUserEntered(boolean userEntered) {
        _userEntered = userEntered;
    }

    public Double getMean() {
        return _mean;
    }

    public void setMean(Double mean) {
        _mean = mean;
    }

    public Double getMinExpectedValue() {
        return _minExpectedValue;
    }

    public void setMinExpectedValue(Double minExpectedValue) {
        _minExpectedValue = minExpectedValue;
    }

    public Double getMaxExpectedValue() {
        return _maxExpectedValue;
    }

    public void setMaxExpectedValue(Double maxExpectedValue) {
        _maxExpectedValue = maxExpectedValue;
    }

    public void setBaseline(Baseline baseline) {
        setId(baseline.getId());
        setComputeTime(baseline.getComputeTime());
        setUserEntered(baseline.isUserEntered());
        setMean(baseline.getMean());
        setMinExpectedValue(baseline.getMinExpectedVal());
        setMaxExpectedValue(baseline.getMaxExpectedVal());
    }

    public String toString() {
        StringBuffer str = new StringBuffer("{")
            .append("id=").append(getId())
            .append(" computeTime=").append(getComputeTime())
            .append(" userEntered=").append(getUserEntered())
            .append(" mean=").append(getMean())
            .append(" minExpectedValue=").append(getMinExpectedValue())
            .append(" maxExpectedValue=").append(getMaxExpectedValue())
            .append('}');

        return str.toString();
    }

    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (other instanceof BaselineValue) {
            BaselineValue that = (BaselineValue) other;
            boolean lEquals = true;
            if (_id == null) {
                lEquals = lEquals && (that._id == null);
            } else {
                lEquals = lEquals && _id.equals(that._id);
            }

            lEquals = lEquals && isIdentical(that);

            return lEquals;
        } else {
            return false;
        }
    }

    public boolean isIdentical(Object other) {
        if (other instanceof BaselineValue) {
            BaselineValue that = (BaselineValue) other;
            boolean lEquals = true;
            lEquals = lEquals && _computeTime == that._computeTime;
            lEquals = lEquals && _userEntered == that._userEntered;
            if (_mean == null) {
                lEquals = lEquals && (that._mean == null);
            } else {
                lEquals = lEquals && _mean.equals(that._mean);
            }
            if (_minExpectedValue == null) {
                lEquals = lEquals && (that._minExpectedValue == null);
            } else {
                lEquals = lEquals
                        && _minExpectedValue.equals(that._minExpectedValue);
            }
            if (_maxExpectedValue == null) {
                lEquals = lEquals && (that._maxExpectedValue == null);
            } else {
                lEquals = lEquals
                        && _maxExpectedValue.equals(that._maxExpectedValue);
            }

            return lEquals;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + ((_id != null) ? _id.hashCode() : 0);

        result = 37 * result + (int) (_computeTime ^ (_computeTime >>> 32));

        result = 37 * result + (_userEntered ? 0 : 1);

        result = 37 * result + ((_mean != null) ? _mean.hashCode() : 0);

        result = 37
                * result
                + ((_minExpectedValue != null) ? _minExpectedValue.hashCode()
                        : 0);

        result = 37
                * result
                + ((_maxExpectedValue != null) ? _maxExpectedValue.hashCode()
                        : 0);

        return result;
    }

}
