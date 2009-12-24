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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * MetricsDisplayForm.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.util.MonitorUtils;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.util.LabelValueBean;

/**
 * Represents the controls on various pages that display metrics summaries.
 * 
 * 
 */
public class MetricsDisplayForm
    extends MetricsFilterForm {

    // -------------------------------------instance variables

    // clears highlight state
    private ImageButtonBean clear;
    // links to compare metrics page
    private ImageButtonBean compare;
    // refreshes with current values
    private ImageButtonBean current;
    // links to chart metrics page
    private ImageButtonBean chart;
    // sets highlight state
    private ImageButtonBean highlight;
    // highlight fields: "Low Value" (hv) is "10%" (hp) "Under
    // Baseline" (ht)
    private Boolean h; // is highlighted or not
    private Integer hp;
    private Integer ht;
    private Integer hv;
    // selected metrics upon which we'll take some action
    private Integer[] m;
    // selected resources upon which we'll take some action
    private Integer[] r;
    // threshold selection
    private Integer t;
    // changes threshold selection
    private ImageButtonBean threshold;

    /** Holds value of property collectionInterval */
    private Long collectionInterval;

    /** Holds value of property collectionUnit. */
    private long collectionUnit;

    private List categoryList = new ArrayList();

    private Boolean displayBaseline;
    private Boolean displayHighRange;
    private Boolean displayLowRange;

    // for each category present, this is the order that the metric
    // categories should be displayed in
    public static final String[] METRIC_CATEGORIES = { MeasurementConstants.CAT_AVAILABILITY,
                                                      MeasurementConstants.CAT_PERFORMANCE,
                                                      MeasurementConstants.CAT_THROUGHPUT,
                                                      MeasurementConstants.CAT_UTILIZATION };

    /**
     * In the cases where this bean holds MetricDisplaySummary's that are
     * aggregates we show the number collecting/unavailable/total.
     */
    private Boolean showNumberCollecting = Boolean.FALSE;

    /**
     * When the MetricDisplaySummary's are for a single resource, we show the
     * baseline, high range and low range.
     */
    private Boolean showBaseline = Boolean.FALSE;

    /*
     * Note: on the favorite metrics display pages <i>both</i> can be true!
     * Everywhere else it's usually one or the other.
     */

    /**
     * On favorite metrics pages, we need to show the specific resource type for
     * each metric.
     */
    private Boolean showMetricSource = Boolean.FALSE;

    // -------------------------------------constructors

    public MetricsDisplayForm() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    /**
     * Method setupCategoryList.
     * 
     * The Map of MetricDisplaySummarys from the backend (Map( categories =>
     * List(MetricDisplaySummary)) is an unordered data structure - we want the
     * the UI to display the categorized metrics in a consistent order so we
     * make a list of the keys here.
     * 
     * @param metricMap
     */
    public void setupCategoryList(Map metricMap) {
        for (int i = 0; i < METRIC_CATEGORIES.length; i++) {
            if (metricMap.containsKey(METRIC_CATEGORIES[i])) {
                Collection metrics = (Collection) metricMap.get(METRIC_CATEGORIES[i]);
                if (metrics.size() > 0) {
                    getCategoryList().add(METRIC_CATEGORIES[i]);
                }
            }
        }
    }

    public Integer getCategoryListSize() {
        return new Integer(getCategoryList().size());
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" showNumberCollecting=").append(showNumberCollecting);
        s.append(" showBaseline=").append(showBaseline);
        s.append(" categoryList=").append(categoryList);
        s.append(" clear=").append(clear);
        s.append(" compare=").append(compare);
        s.append(" current=").append(current);
        s.append(" chart=").append(chart);
        s.append(" highlight=").append(highlight);
        s.append(" h=").append(h);
        s.append(" hp=").append(hp);
        s.append(" ht=").append(ht);
        s.append(" hv=").append(hv);
        s.append(" m=").append(Arrays.asList(m));
        s.append(" r=").append(Arrays.asList(r));
        s.append(" t=").append(t);
        s.append(" threshold=").append(threshold);
        return s.toString();
    }

    // -------------------------------------public accessors

    public ImageButtonBean getClear() {
        return clear;
    }

    public void setClear(ImageButtonBean b) {
        clear = b;
    }

    public ImageButtonBean getCompare() {
        return compare;
    }

    public void setCompare(ImageButtonBean b) {
        compare = b;
    }

    public ImageButtonBean getCurrent() {
        return current;
    }

    public void setCurrent(ImageButtonBean b) {
        current = b;
    }

    public ImageButtonBean getChart() {
        return chart;
    }

    public void setChart(ImageButtonBean b) {
        chart = b;
    }

    public ImageButtonBean getHighlight() {
        return highlight;
    }

    public void setHighlight(ImageButtonBean b) {
        highlight = b;
    }

    public Boolean getH() {
        return h;
    }

    public void setH(Boolean b) {
        h = b;
    }

    // highlight percentage
    public Integer getHp() {
        return hp;
    }

    public void setHp(Integer i) {
        hp = i;
    }

    // highlight threshold
    public Integer getHt() {
        return ht;
    }

    public void setHt(Integer i) {
        ht = i;
    }

    // highlight value
    public Integer getHv() {
        return hv;
    }

    public void setHv(Integer i) {
        hv = i;
    }

    public Integer[] getM() {
        return m;
    }

    public void setM(Integer[] l) {
        m = l;
    }

    public Integer[] getR() {
        return r;
    }

    public void setR(Integer[] l) {
        r = l;
    }

    // threshold selection
    public Integer getT() {
        return t;
    }

    public void setT(Integer i) {
        t = i;
    }

    public ImageButtonBean getThreshold() {
        return threshold;
    }

    public void setThreshold(ImageButtonBean b) {
        threshold = b;
    }

    public boolean isClearClicked() {
        return getClear().isSelected();
    }

    public boolean isCompareClicked() {
        return getCompare().isSelected();
    }

    public boolean isCurrentClicked() {
        return getCurrent().isSelected();
    }

    public boolean isChartClicked() {
        return getChart().isSelected();
    }

    public boolean isHighlightClicked() {
        return getHighlight().isSelected();
    }

    public boolean isThresholdClicked() {
        return getThreshold().isSelected();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    /**
     * @return Boolean
     */
    public Boolean getShowNumberCollecting() {
        return showNumberCollecting;
    }

    /**
     * Sets the showNumberCollecting.
     * @param showNumberCollecting The showNumberCollecting to set
     */
    public void setShowNumberCollecting(Boolean showNumberCollecting) {
        this.showNumberCollecting = showNumberCollecting;
    }

    /**
     * @return Boolean
     */
    public Boolean getShowBaseline() {
        return showBaseline;
    }

    /**
     * Sets the showBaseline.
     * @param showBaseline The showBaseline to set
     */
    public void setShowBaseline(Boolean showBaseline) {
        this.showBaseline = showBaseline;
    }

    /**
     * @return Boolean
     */
    public Boolean getShowMetricSource() {
        return showMetricSource;
    }

    /**
     * Sets the showMetricSource.
     * @param showMetricSource The showMetricSource to set
     */
    public void setShowMetricSource(Boolean showMetricSource) {
        this.showMetricSource = showMetricSource;
    }

    /**
     * @return List
     */
    public List getCategoryList() {
        return categoryList;
    }

    /**
     * Sets the categoryList.
     * @param categoryList The categoryList to set
     */
    public void setCategoryList(List categoryList) {
        this.categoryList = categoryList;
    }

    /**
     * @return Boolean
     */
    public Boolean getDisplayBaseline() {
        return displayBaseline;
    }

    /**
     * @return Boolean
     */
    public Boolean getDisplayHighRange() {
        return displayHighRange;
    }

    /**
     * @return Boolean
     */
    public Boolean getDisplayLowRange() {
        return displayLowRange;
    }

    /**
     * Sets the displayBaseline.
     * @param displayBaseline The displayBaseline to set
     */
    public void setDisplayBaseline(Boolean displayBaseline) {
        this.displayBaseline = displayBaseline;
    }

    /**
     * Sets the displayHighRange.
     * @param displayHighRange The displayHighRange to set
     */
    public void setDisplayHighRange(Boolean displayHighRange) {
        this.displayHighRange = displayHighRange;
    }

    /**
     * Sets the displayLowRange.
     * @param displayLowRange The displayLowRange to set
     */
    public void setDisplayLowRange(Boolean displayLowRange) {
        this.displayLowRange = displayLowRange;
    }

    public Long getCollectionInterval() {
        return collectionInterval;
    }

    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    public long getCollectionUnit() {
        return collectionUnit;
    }

    public void setCollectionUnit(long collectionUnit) {
        this.collectionUnit = collectionUnit;
    }

    /**
     * Derived property based on collectionInterval and collectionUnit, return
     * the time as a long
     */
    public long getIntervalTime() {
        return collectionInterval.longValue() * collectionUnit;
    }

    public List getThresholdMenu() {
        return MonitorUtils.getThresholdMenu();
    }

    public List getHighlightThresholdMenu() {
        List items = new ArrayList();

        String underLabel = null;
        String overLabel = null;

        if (getT() != null) {
            switch (getT().intValue()) {
                case MonitorUtils.THRESHOLD_HIGH_RANGE_VALUE:
                    underLabel = "UnderHighRange";
                    overLabel = "OverHighRange";
                    break;
                case MonitorUtils.THRESHOLD_LOW_RANGE_VALUE:
                    underLabel = "UnderLowRange";
                    overLabel = "OverLowRange";
                    break;
            }
        }

        if (underLabel == null || overLabel == null) {
            underLabel = "UnderBaseline";
            overLabel = "OverBaseline";
        }

        Integer underValue = new Integer(MonitorUtils.THRESHOLD_UNDER_VALUE);
        Integer overValue = new Integer(MonitorUtils.THRESHOLD_OVER_VALUE);

        items.add(new LabelValueBean(underLabel, underValue.toString()));
        items.add(new LabelValueBean(overLabel, overValue.toString()));

        return items;
    }

    // -------------------------------------private methods

    protected void setDefaults() {
        super.setDefaults();
        clear = new ImageButtonBean();
        compare = new ImageButtonBean();
        current = new ImageButtonBean();
        chart = new ImageButtonBean();
        highlight = new ImageButtonBean();
        h = Boolean.FALSE;
        hp = null;
        ht = null;
        hv = null;
        m = new Integer[0];
        r = new Integer[0];
        threshold = new ImageButtonBean();
        t = null;
        showNumberCollecting = Boolean.FALSE;
        showBaseline = Boolean.FALSE;
        showMetricSource = Boolean.FALSE;
        categoryList = new ArrayList();
        collectionInterval = null;
    }
}
