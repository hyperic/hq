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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.product.MetricValue;

public class MeasurementMetadataSummary implements Serializable {

    private AppdefResourceValue resource;
    private Double maxExpectedValue;
    private Double minExpectedValue;
    private Long interval;
    private Boolean enabled;
    private Long mtime;
    private Long lastValueTimestamp;
    private MeasurementTemplateValue mtv;     
    private Double lastValue;
    private Integer measurementId;

    public MeasurementMetadataSummary(Integer measurementId,
                                      Long lastValueTimestamp,
                                      Double lastValue,
                                      MeasurementTemplate mt,
                                      Boolean enabled, Long interval,
                                      Long mtime, Baseline b,
                                      AppdefResourceValue resource) {
        setEnabled(enabled);
        setInterval(interval);
        setLastValue(lastValue);
        setLastValueTimestamp(lastValueTimestamp);
        setMeasurementId(measurementId);
        setMtime(mtime);
        setMeasurementTemplate(mtv);
        setResource(resource);
        
        if (b != null) {
            setMaxExpectedValue(b.getMaxExpectedVal());
            setMinExpectedValue(b.getMinExpectedVal());
        }
    }

    public MeasurementMetadataSummary(DerivedMeasurement mm,
                                      MetricValue mv,
                                      AppdefResourceValue resource) {
        this(mm.getId(),
             mv == null ? null : new Long(mv.getTimestamp()),
             mv == null ? new Double(Double.NaN) : new Double(mv.getValue()),
             mm.getTemplate(),
             Boolean.valueOf(mm.isEnabled()),
             new Long(mm.getInterval()),
             new Long(mm.getMtime()),
             mm.getBaseline(),
             resource);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(MeasurementMetadataSummary.class.getName());
        return sb.toString();
    }
    
    /**
     * @return Boolean
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @return Long
     */
    public Long getInterval() {
        return interval;
    }

    /**
     * @return Double
     */
    public Double getLastValue() {
        return lastValue;
    }

    /**
     * @return Long
     */
    public Long getLastValueTimestamp() {
        return lastValueTimestamp;
    }

    /**
     * @return Double
     */
    public Double getMaxExpectedValue() {
        return maxExpectedValue;
    }

    /**
     * @return Integer
     */
    public Integer getMeasurementId() {
        return measurementId;
    }

    /**
     * @return Double
     */
    public Double getMinExpectedValue() {
        return minExpectedValue;
    }

    /**
     * @return Long
     */
    public Long getMtime() {
        return mtime;
    }

    /**
     * @return MeasurementTemplateValue
     */
    public MeasurementTemplateValue getMeasurementTemplate() {
        return mtv;
    }

    /**
     * @return AppdefResourceValue
     */
    public AppdefResourceValue getResource() {
        return resource;
    }

    /**
     * Sets the enabled.
     * @param enabled The enabled to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the interval.
     * @param interval The interval to set
     */
    public void setInterval(Long interval) {
        this.interval = interval;
    }

    /**
     * Sets the lastValue.
     * @param lastValue The lastValue to set
     */
    public void setLastValue(Double lastValue) {
        this.lastValue = lastValue;
    }

    /**
     * Sets the lastValueTimestamp.
     * @param lastValueTimestamp The lastValueTimestamp to set
     */
    public void setLastValueTimestamp(Long lastValueTimestamp) {
        this.lastValueTimestamp = lastValueTimestamp;
    }

    /**
     * Sets the maxExpectedValue.
     * @param maxExpectedValue The maxExpectedValue to set
     */
    public void setMaxExpectedValue(Double maxExpectedValue) {
        this.maxExpectedValue = maxExpectedValue;
    }

    /**
     * Sets the measurementId.
     * @param measurementId The measurementId to set
     */
    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }


    /**
     * Sets the minExpectedValue.
     * @param minExpectedValue The minExpectedValue to set
     */
    public void setMinExpectedValue(Double minExpectedValue) {
        this.minExpectedValue = minExpectedValue;
    }

    /**
     * Sets the mtime.
     * @param mtime The mtime to set
     */
    public void setMtime(Long mtime) {
        this.mtime = mtime;
    }

    /**
     * Sets the MeasurementTemplate.
     * @param mtv The MeasurementTemplate to set
     */
    public void setMeasurementTemplate(MeasurementTemplateValue mtv) {
        this.mtv = mtv;
    }

    /**
     * Sets the resource.
     * @param resource The resource to set
     */
    public void setResource(AppdefResourceValue resource) {
        this.resource = resource;
    }

}
