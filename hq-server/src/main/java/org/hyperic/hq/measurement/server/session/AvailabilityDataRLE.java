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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "HQ_AVAIL_DATA_RLE")
public class AvailabilityDataRLE implements Serializable {

    private static final long MAX_ENDTIME = Long.MAX_VALUE;

    public static long getLastTimestamp() {
        return MAX_ENDTIME;
    }

    @Column(name = "AVAILVAL", nullable = false)
    @Index(name = "AVAIL_RLE_ENDTIME_VAL_IDX")
    private double availVal;

    @Column(name = "ENDTIME", nullable = false)
    @Index(name = "AVAIL_RLE_ENDTIME_VAL_IDX")
    private long endtime = 9223372036854775807l;

    @EmbeddedId
    private AvailabilityDataId id;

    @ManyToOne
    @JoinColumn(name = "MEASUREMENT_ID", nullable = false, insertable = false, updatable = false)
    private Measurement measurement;

    @Column(name = "STARTIME", nullable = false, insertable = false, updatable = false)
    private long startime;

    public AvailabilityDataRLE() {
    }

    public AvailabilityDataRLE(Measurement meas, long startime, double availType) {
        init(meas, startime, MAX_ENDTIME, availType);
    }

    public AvailabilityDataRLE(Measurement meas, long startime, long endtime, double availType) {
        init(meas, startime, endtime, availType);
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof AvailabilityDataRLE)) {
            return false;
        }
        AvailabilityDataRLE rle = (AvailabilityDataRLE) rhs;
        AvailabilityDataId id = rle.getAvailabilityDataId();
        return getAvailabilityDataId().equals(id);
    }

    public long getApproxEndtime() {
        long approxEndtime = endtime;

        if (approxEndtime == MAX_ENDTIME) {
            long interval = measurement.getInterval();
            // java will round down
            long multiplier = (System.currentTimeMillis() - startime) / interval;

            approxEndtime = startime + (multiplier * interval);
        }

        return approxEndtime;
    }

    public AvailabilityDataId getAvailabilityDataId() {
        if (this.id == null) {
            this.id = new AvailabilityDataId(startime, measurement);
        }
        return id;
    }

    public double getAvailVal() {
        return availVal;
    }

    public long getEndtime() {
        return endtime;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public long getStartime() {
        return startime;
    }

    public int hashCode() {
        return getAvailabilityDataId().hashCode();
    }

    private void init(Measurement meas, long startime, long endtime, double availVal) {
        setAvailabilityDataId(new AvailabilityDataId(startime, meas));
        this.measurement = meas;
        this.startime = startime;
        this.endtime = endtime;
        this.availVal = availVal;
    }

    protected void setAvailabilityDataId(AvailabilityDataId id) {
        this.id = id;
        startime = id.getStartime();
        measurement = id.getMeasurement();
    }

    public void setAvailVal(double val) {
        availVal = val;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }

    public void setMeasurement(Measurement meas) {
        measurement = meas;
    }

    public void setStartime(long startime) {
        this.startime = startime;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        return buf.append(" measurement -> ").append(measurement.getId()).append(" startime -> ")
            .append(startime).append(" endtime -> ").append(endtime).append(" approxEndtime -> ")
            .append(getApproxEndtime()).append(" availVal -> ").append(availVal).toString();
    }
}
