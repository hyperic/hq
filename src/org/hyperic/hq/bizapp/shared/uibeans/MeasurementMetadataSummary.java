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
import org.hyperic.hq.measurement.server.session.Baseline;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.product.MetricValue;

public class MeasurementMetadataSummary implements Serializable {

    private AppdefResourceValue _resource;
    private Double _maxExpectedValue;
    private Double _minExpectedValue;
    private Long _interval;
    private Boolean _enabled;
    private Long _mtime;
    private Long _lastValueTimestamp;
    private MeasurementTemplate _mtv;     
    private Double _lastValue;
    private Integer _metricId;

    public MeasurementMetadataSummary(DerivedMeasurement mm, MetricValue mv,
                                      AppdefResourceValue resource) {
        Baseline b = mm.getBaseline();
        setEnabled(Boolean.valueOf(mm.isEnabled()));
        setInterval(new Long(mm.getInterval()));
        setLastValue(mv == null ?
                     new Double(Double.NaN) : new Double(mv.getValue()));
        setLastValueTimestamp(mv == null ? null : new Long(mv.getTimestamp()));
        setMeasurementId(mm.getId());
        setMtime(new Long(mm.getMtime()));
        setMeasurementTemplate(mm.getTemplate());
        setResource(resource);
        
        if (b != null) {
            setMaxExpectedValue(b.getMaxExpectedVal());
            setMinExpectedValue(b.getMinExpectedVal());
        }
    }
    
    public String toString() {
        StringBuffer sb =
            new StringBuffer(MeasurementMetadataSummary.class.getName());
        return sb.toString();
    }
    
    /**
     * @return Boolean
     */
    public Boolean getEnabled() {
        return _enabled;
    }

    /**
     * @return Long
     */
    public Long getInterval() {
        return _interval;
    }

    /**
     * @return Double
     */
    public Double getLastValue() {
        return _lastValue;
    }

    /**
     * @return Long
     */
    public Long getLastValueTimestamp() {
        return _lastValueTimestamp;
    }

    /**
     * @return Double
     */
    public Double getMaxExpectedValue() {
        return _maxExpectedValue;
    }

    /**
     * @return Integer
     */
    public Integer getMeasurementId() {
        return _metricId;
    }

    /**
     * @return Double
     */
    public Double getMinExpectedValue() {
        return _minExpectedValue;
    }

    /**
     * @return Long
     */
    public Long getMtime() {
        return _mtime;
    }

    /**
     * @return MeasurementTemplateValue
     */
    public MeasurementTemplate getMeasurementTemplate() {
        return _mtv;
    }

    /**
     * @return AppdefResourceValue
     */
    public AppdefResourceValue getResource() {
        return _resource;
    }

    /**
     * Sets the enabled.
     * @param enabled The enabled to set
     */
    public void setEnabled(Boolean enabled) {
        _enabled = enabled;
    }

    /**
     * Sets the interval.
     * @param interval The interval to set
     */
    public void setInterval(Long interval) {
        _interval = interval;
    }

    /**
     * Sets the lastValue.
     * @param lastValue The lastValue to set
     */
    public void setLastValue(Double lastValue) {
        _lastValue = lastValue;
    }

    /**
     * Sets the lastValueTimestamp.
     * @param lastValueTimestamp The lastValueTimestamp to set
     */
    public void setLastValueTimestamp(Long lastValueTimestamp) {
        _lastValueTimestamp = lastValueTimestamp;
    }

    /**
     * Sets the maxExpectedValue.
     * @param maxExpectedValue The maxExpectedValue to set
     */
    public void setMaxExpectedValue(Double maxExpectedValue) {
        _maxExpectedValue = maxExpectedValue;
    }

    /**
     * Sets the measurementId.
     * @param measurementId The measurementId to set
     */
    public void setMeasurementId(Integer measurementId) {
        _metricId = measurementId;
    }


    /**
     * Sets the minExpectedValue.
     * @param minExpectedValue The minExpectedValue to set
     */
    public void setMinExpectedValue(Double minExpectedValue) {
        _minExpectedValue = minExpectedValue;
    }

    /**
     * Sets the mtime.
     * @param mtime The mtime to set
     */
    public void setMtime(Long mtime) {
        _mtime = mtime;
    }

    /**
     * Sets the MeasurementTemplate.
     * @param mtv The MeasurementTemplate to set
     */
    public void setMeasurementTemplate(MeasurementTemplate mtv) {
        _mtv = mtv;
    }

    /**
     * Sets the resource.
     * @param resource The resource to set
     */
    public void setResource(AppdefResourceValue resource) {
        _resource = resource;
    }

}
