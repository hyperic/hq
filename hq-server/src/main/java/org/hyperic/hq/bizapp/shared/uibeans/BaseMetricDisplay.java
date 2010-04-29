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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.measurement.UnitsConvert;

/**
 * Represents a metric that may be displayed in a list context.  All fields
 * refer to display needs in a list context in the monitoring UI.
 */
public abstract class BaseMetricDisplay extends MeasurementSummary
    implements java.io.Serializable, Comparable {
    private Long    beginTimeFrame;
    private Long    endTimeFrame;
    private Integer templateCat;
    private String  units;
    private String  displayUnits;    
    private Integer collectionType;
    private Boolean showNumberCollecting;
    private boolean collecting;
    private String  metricSource;
    private Boolean designated;    
    private Map     metrics;

    protected static final List attrKeyList =
        Arrays.asList(MetricDisplayConstants.attrKey); 

    /**
     * Constructor for MetricDisplaySummary.
     */
    public BaseMetricDisplay() {
        super();
        metrics = new HashMap();
    }

    public double[] getMetricValueDoubles() {
        List values = new ArrayList();
        for (Iterator iter = metrics.entrySet().iterator(); iter.hasNext();) {
            Map.Entry ent = (Map.Entry) iter.next();
            if (ent.getKey() != null && ent.getValue() != null)
                values.add(((MetricDisplayValue)ent.getValue()).getValue());
        }
        int i = 0;
        double[] rv = new double[values.size()];
        for (Iterator iter = values.iterator(); iter.hasNext();) {
            Double v = (Double) iter.next();
            rv[i] = v.doubleValue();
            ++i;
        }
        return rv;
    }

    public String[] getMetricKeys() {
        List keys = new ArrayList();
        for (Iterator iter = metrics.entrySet().iterator(); iter.hasNext();) {
            Map.Entry ent = (Map.Entry) iter.next();
            if (ent.getKey() != null && ent.getValue() != null)
                keys.add(ent.getKey());
        }
        int i = 0;
        String[] rv = new String[keys.size()];
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            rv[i] = key;
            ++i;
        }
        return rv;
    }
    
    public void setMetric(String key, MetricDisplayValue value) {
        if (! attrKeyList.contains(key))
            throw new IllegalArgumentException(key + " is not a known metric value");
        metrics.put(key, value);
    }
    
    public MetricDisplayValue getMetric(String key) {
        if (key == null)
            throw new IllegalArgumentException("'null' is not a valid metric key");
        return (MetricDisplayValue)metrics.get(key);
    }

    public Map getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map metrics) {
        this.metrics = metrics;
    }
    
    public MetricDisplayValue getMinMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.MIN_KEY);
        if (mdv == null)
            throw new IllegalArgumentException(
                "No valid metric key: " + MetricDisplayConstants.MIN_KEY);
        
        mdv.setValueFmt(UnitsConvert.convert(mdv.getValue().doubleValue(),
                                             getUnits()));
        return mdv;
    }

    public MetricDisplayValue getMaxMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.MAX_KEY);
        if (mdv == null)
            throw new IllegalArgumentException(
                "No valid metric key: " + MetricDisplayConstants.MAX_KEY);
        
        mdv.setValueFmt(UnitsConvert.convert(mdv.getValue().doubleValue(),
                                             getUnits()));
        return mdv;
    }

    public MetricDisplayValue getAvgMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.AVERAGE_KEY);
        if (mdv == null)
            throw new IllegalArgumentException(
                "No valid metric key: " + MetricDisplayConstants.AVERAGE_KEY);
        
        mdv.setValueFmt(UnitsConvert.convert(mdv.getValue().doubleValue(),
                                             getUnits()));
        return mdv;
    }

    public MetricDisplayValue getLastMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.LAST_KEY);
        if (mdv == null)
            throw new IllegalArgumentException(
                "No valid metric key: " + MetricDisplayConstants.LAST_KEY);
        
        mdv.setValueFmt(UnitsConvert.convert(mdv.getValue().doubleValue(),
                                             getUnits()));
        return mdv;
    }
    
    /**
     * Method getBeginTimeFrame.
     * 
     * All metrics displayed are within a timeframe.  The beginning of that
     * timeframe is represented as the number of epoch seconds at which the
     * timeframe commences, this method returns that Long value.
     * 
     * @return Long
     */
    public Long getBeginTimeFrame() {
        return this.beginTimeFrame;
    }

    /**
     * Method setBeginTimeFrame.
     * 
     * All metrics displayed are within a timeframe.  The beginning of that
     * timeframe is represented as the number of epoch seconds at which the
     * timeframe commences, this method sets that Long value.
     * 
     * @param beginTimeFrame The beginTimeFrame to set
     */
    public void setBeginTimeFrame(Long beginTimeFrame) {
        this.beginTimeFrame = beginTimeFrame;
    }
    
    /**
     * Method getEndTimeFrame.
     * 
     * All metrics displayed are within a timeframe.  The end of that timeframe
     * is represented as the number of epoch seconds at which the timeframe
     * is finished, this method returns that Long value.
     * 
     * @return Long
     */
    public Long getEndTimeFrame() {
        return this.endTimeFrame;
    }
    
    /**
     * Method setEndTimeFrame.
     * 
     * All metrics displayed are within a timeframe.  The end of that timeframe
     * is represented as the number of epoch seconds at which the timeframe
     * is finished, this method sets that Long value.
     * 
     * @param endTimeFrame The endTimeFrame to set
     */
    public void setEndTimeFrame(Long endTimeFrame) {
        this.endTimeFrame = endTimeFrame;
    }


    /**
     * Method getMeasurementTemplateType.
     * 
     * Returns the id of type of metric that this metric represents
     * 
     * @return Integer
     */
    public Integer getTemplateCat() {
        return this.templateCat;
    }
    
    /**
     * Method setMeasurementTemplateType.
     * 
     * Sets the id of type of metric that this metric represents
     * 
     * @param templateCat The templateCat to set
     */
    public void setTemplateCat(Integer templateCat) {
        this.templateCat = templateCat;
    }

    /**
     * Method getIntervalUnits.
     * 
     * Returns the label for the units of the metric (if not intrinsic to the
     * metric itself) measurement
     * 
     * @return String
     */
    public String getUnits() {
        return this.units;
    }

    /**
     * Method setIntervalUnits.
     * 
     * Sets the label for the units of the metric (if not intrinsic to the
     * metric itself) measurement
     * 
     * @param units The units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * Get the collection type for the metrics.  This value matches
     * to MeasurementConstants.COLL_TYPE_*
     */
    public Integer getCollectionType() {
        return this.collectionType;
    }
    
    public void setCollectionType(Integer collectionType) {
        this.collectionType = collectionType;
    }


    // flags set by UI to indicate whether the value should by highlighted due
    // a comparison against a baseline
        

    /**
     * Returns the showNumberCollecting.
     * @return boolean
     */
    public Boolean getShowNumberCollecting() {
        return showNumberCollecting;
    }

    /**
     * @return
     */
    public String getMetricSource() {
        return metricSource;
    }

    /**
     * @param string
     */
    public void setMetricSource(String string) {
        metricSource = string;
    }

    /**
     * Sets the showNumberCollecting.
     * @param showNumberCollecting The showNumberCollecting to set
     */
    public void setShowNumberCollecting(Boolean showNumberCollecting) {
        this.showNumberCollecting = showNumberCollecting;
    }

    /**
     * @return
     */
    public String getDisplayUnits() {
        return displayUnits;
    }

    /**
     * @param string
     */
    public void setDisplayUnits(String string) {
        displayUnits = string;
    }

    /**
     * @return Boolean
     */
    public Boolean getDesignated() {
        return designated;
    }

    /**
     * Sets the designated.
     * @param designated The designated to set
     */
    public void setDesignated(Boolean designated) {
        this.designated = designated;
    }

    public boolean getCollecting() {
        return collecting;
    }

    public void setCollecting(boolean collecting) {
        this.collecting = collecting;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(BaseMetricDisplay.class.getName());
        sb.append("(label=").append(getLabel());
        sb.append(",beginTimeFrame=").append(beginTimeFrame);
        sb.append(",endTimeFrame=").append(endTimeFrame);
        sb.append(",templateCat=").append(templateCat);
        sb.append(",units=").append(units);
        sb.append(",collectionType=").append(collectionType);
        sb.append(",metricSource=").append(metricSource);
        sb.append(",templateId=").append(getTemplateId());
        sb.append(",showNumberCollecting=").append(showNumberCollecting);
        sb.append(",collecting=").append(collecting);
        sb.append("metrics[");
        for (int i = 0; i < MetricDisplayConstants.attrKey.length; i++) {
            sb.append("\n").append(MetricDisplayConstants.attrKey[i]).append("=").
                append(metrics.get(MetricDisplayConstants.attrKey[i]));
        }
        sb.append("\n])");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        if (arg0 instanceof BaseMetricDisplay) {
            BaseMetricDisplay to = (BaseMetricDisplay) arg0;
            return this.getLabel().compareTo(to.getLabel());
        }

        throw new IllegalArgumentException(
            "Cannot compare to non-BaseMetricDisplay object: " + arg0);            
    }

}

