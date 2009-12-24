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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.ImageButtonBean;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.MonitorUtils;

/**
 * Represents the controls on the metric chart page(s).
 */
public class ViewChartForm
    extends MetricDisplayRangeForm {
    public static String NO_CHILD_TYPE = "";

    private static Log log = LogFactory.getLog(ViewChartForm.class.getName());

    private String mode;
    private Integer[] m;
    private Integer[] origM;
    private Integer[] resourceIds;
    private String ctype;
    private boolean showPeak;
    private boolean showHighRange;
    private boolean showValues;
    private boolean showAverage;
    private boolean showLowRange;
    private boolean showLow;
    private boolean showEvents;
    private boolean showBaseline;
    private boolean saveChart;
    private ImageButtonBean redraw;
    private ImageButtonBean changeBaseline;
    private ImageButtonBean saveBaseline;
    private ImageButtonBean cancelBaseline;
    private ImageButtonBean changeHighRange;
    private ImageButtonBean saveHighRange;
    private ImageButtonBean cancelHighRange;
    private ImageButtonBean changeLowRange;
    private ImageButtonBean saveLowRange;
    private ImageButtonBean cancelLowRange;
    private ImageButtonBean prevPage;
    private String baseline;
    private String newBaseline;
    private String highRange;
    private String lowRange;
    private String baselineRaw;
    private String newBaselineRaw;
    private String highRangeRaw;
    private String lowRangeRaw;
    private Integer threshold;
    private String chartName; // just for saving to dashboard

    public ViewChartForm() {
        super();
        setDefaults();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append("\t");
        sb.append("mode=");
        sb.append(mode);
        sb.append("\n");
        sb.append("\t");
        sb.append("showPeak=");
        sb.append(showPeak);
        sb.append("\n");
        sb.append("\t");
        sb.append("showHighRange=");
        sb.append(showHighRange);
        sb.append("\n");
        sb.append("\t");
        sb.append("showValues=");
        sb.append(showValues);
        sb.append("\n");
        sb.append("\t");
        sb.append("showAverage=");
        sb.append(showAverage);
        sb.append("\n");
        sb.append("\t");
        sb.append("showLowRange=");
        sb.append(showLowRange);
        sb.append("\n");
        sb.append("\t");
        sb.append("showLow=");
        sb.append(showLow);
        sb.append("\n");
        sb.append("\t");
        sb.append("showEvents=");
        sb.append(showEvents);
        sb.append("\n");
        sb.append("\t");
        sb.append("showBaseline=");
        sb.append(showBaseline);
        sb.append("\n");
        sb.append("\t");
        sb.append("isRedrawClicked=");
        sb.append(isRedrawClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isPrevRangeClicked=");
        sb.append(isPrevRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isNextRangeClicked=");
        sb.append(isNextRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isChangeBaselineClicked=");
        sb.append(isChangeBaselineClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isSaveBaselineClicked=");
        sb.append(isSaveBaselineClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isCancelBaselineClicked=");
        sb.append(isCancelBaselineClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isChangeHighRangeClicked=");
        sb.append(isChangeHighRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isSaveHighRangeClicked=");
        sb.append(isSaveHighRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isCancelHighRangeClicked=");
        sb.append(isCancelHighRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isChangeLowRangeClicked=");
        sb.append(isChangeLowRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isSaveLowRangeClicked=");
        sb.append(isSaveLowRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isCancelLowRangeClicked=");
        sb.append(isCancelLowRangeClicked());
        sb.append("\n");
        sb.append("\t");
        sb.append("isPrevPageClicked=");
        sb.append(isPrevPageClicked());
        sb.append("baseline=");
        sb.append(baseline);
        sb.append("\n");
        sb.append("\t");
        sb.append("newBaseline=");
        sb.append(newBaseline);
        sb.append("\n");
        sb.append("\t");
        sb.append("highRange=");
        sb.append(highRange);
        sb.append("\n");
        sb.append("\t");
        sb.append("lowRange=");
        sb.append(lowRange);
        sb.append("\n");
        sb.append("\t");
        sb.append("baselineRaw=");
        sb.append(baselineRaw);
        sb.append("\n");
        sb.append("\t");
        sb.append("newBaselineRaw=");
        sb.append(newBaselineRaw);
        sb.append("\n");
        sb.append("\t");
        sb.append("highRangeRaw=");
        sb.append(highRangeRaw);
        sb.append("\n");
        sb.append("\t");
        sb.append("lowRangeRaw=");
        sb.append(lowRangeRaw);
        sb.append("\n");
        sb.append("\t");
        sb.append("threshold=");
        sb.append(threshold);
        sb.append("\n");
        sb.append("\t");
        sb.append("chartName=");
        sb.append(chartName);
        return sb.toString();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        setDefaults();
        super.reset(mapping, request);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer[] getM() {
        return m;
    }

    public void setM(Integer[] m) {
        this.m = m;
    }

    public Integer[] getOrigM() {
        return origM;
    }

    public void setOrigM(Integer[] origM) {
        this.origM = origM;
    }

    public Integer[] getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Integer[] resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String getCtype() {
        return ctype;
    }

    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    public boolean getShowPeak() {
        return showPeak;
    }

    public void setShowPeak(boolean showPeak) {
        this.showPeak = showPeak;
    }

    public boolean getShowHighRange() {
        return showHighRange;
    }

    public void setShowHighRange(boolean showHighRange) {
        this.showHighRange = showHighRange;
    }

    public boolean getShowValues() {
        return showValues;
    }

    public void setShowValues(boolean showValues) {
        this.showValues = showValues;
    }

    public boolean getShowAverage() {
        return showAverage;
    }

    public void setShowAverage(boolean showAverage) {
        this.showAverage = showAverage;
    }

    public boolean getShowBaseline() {
        return showBaseline;
    }

    public void setShowBaseline(boolean showBaseline) {
        this.showBaseline = showBaseline;
    }

    public boolean getShowLowRange() {
        return showLowRange;
    }

    public void setShowLowRange(boolean showLowRange) {
        this.showLowRange = showLowRange;
    }

    public boolean getShowLow() {
        return showLow;
    }

    public void setShowLow(boolean showLow) {
        this.showLow = showLow;
    }

    public boolean getShowEvents() {
        return showEvents;
    }

    public void setShowEvents(boolean showEvents) {
        this.showEvents = showEvents;
    }

    public ImageButtonBean getRedraw() {
        return redraw;
    }

    public void setRedraw(ImageButtonBean redraw) {
        this.redraw = redraw;
    }

    public boolean isRedrawClicked() {
        return getRedraw().isSelected();
    }

    public boolean isRangeNow() {
        if (getEndDate() == null) {
            return false;
        }
        return (System.currentTimeMillis() - getEndDate().getTime()) < MetricRange.SHIFT_RANGE;
    }

    public boolean getSaveChart() {
        return saveChart;
    }

    public void setSaveChart(boolean b) {
        saveChart = b;
    }

    public ImageButtonBean getChangeBaseline() {
        return changeBaseline;
    }

    public void setChangeBaseline(ImageButtonBean changeBaseline) {
        this.changeBaseline = changeBaseline;
    }

    public boolean isChangeBaselineClicked() {
        return getChangeBaseline().isSelected();
    }

    public ImageButtonBean getSaveBaseline() {
        return saveBaseline;
    }

    public void setSaveBaseline(ImageButtonBean saveBaseline) {
        this.saveBaseline = saveBaseline;
    }

    public boolean isSaveBaselineClicked() {
        return getSaveBaseline().isSelected();
    }

    public ImageButtonBean getCancelBaseline() {
        return cancelBaseline;
    }

    public void setCancelBaseline(ImageButtonBean cancelBaseline) {
        this.cancelBaseline = cancelBaseline;
    }

    public boolean isCancelBaselineClicked() {
        return getCancelBaseline().isSelected();
    }

    public ImageButtonBean getChangeHighRange() {
        return changeHighRange;
    }

    public void setChangeHighRange(ImageButtonBean changeHighRange) {
        this.changeHighRange = changeHighRange;
    }

    public boolean isChangeHighRangeClicked() {
        return getChangeHighRange().isSelected();
    }

    public ImageButtonBean getSaveHighRange() {
        return saveHighRange;
    }

    public void setSaveHighRange(ImageButtonBean saveHighRange) {
        this.saveHighRange = saveHighRange;
    }

    public boolean isSaveHighRangeClicked() {
        return getSaveHighRange().isSelected();
    }

    public ImageButtonBean getCancelHighRange() {
        return cancelHighRange;
    }

    public void setCancelHighRange(ImageButtonBean cancelHighRange) {
        this.cancelHighRange = cancelHighRange;
    }

    public boolean isCancelHighRangeClicked() {
        return getCancelHighRange().isSelected();
    }

    public ImageButtonBean getChangeLowRange() {
        return changeLowRange;
    }

    public void setChangeLowRange(ImageButtonBean changeLowRange) {
        this.changeLowRange = changeLowRange;
    }

    public boolean isChangeLowRangeClicked() {
        return getChangeLowRange().isSelected();
    }

    public ImageButtonBean getSaveLowRange() {
        return saveLowRange;
    }

    public void setSaveLowRange(ImageButtonBean saveLowRange) {
        this.saveLowRange = saveLowRange;
    }

    public boolean isSaveLowRangeClicked() {
        return getSaveLowRange().isSelected();
    }

    public ImageButtonBean getCancelLowRange() {
        return cancelLowRange;
    }

    public void setCancelLowRange(ImageButtonBean cancelLowRange) {
        this.cancelLowRange = cancelLowRange;
    }

    public boolean isCancelLowRangeClicked() {
        return getCancelLowRange().isSelected();
    }

    public ImageButtonBean getPrevPage() {
        return prevPage;
    }

    public void setPrevPage(ImageButtonBean prevPage) {
        this.prevPage = prevPage;
    }

    public boolean isPrevPageClicked() {
        return getPrevPage().isSelected();
    }

    public String getBaseline() {
        return baseline;
    }

    public void setBaseline(String baseline) {
        this.baseline = baseline;
    }

    public String getNewBaseline() {
        return newBaseline;
    }

    public void setNewBaseline(String newBaseline) {
        this.newBaseline = newBaseline;
    }

    public String getHighRange() {
        return highRange;
    }

    public void setHighRange(String highRange) {
        this.highRange = highRange;
    }

    public String getLowRange() {
        return lowRange;
    }

    public void setLowRange(String lowRange) {
        this.lowRange = lowRange;
    }

    public String getBaselineRaw() {
        return baselineRaw;
    }

    public void setBaselineRaw(String baselineRaw) {
        this.baselineRaw = baselineRaw;
    }

    public String getNewBaselineRaw() {
        return newBaselineRaw;
    }

    public void setNewBaselineRaw(String newBaselineRaw) {
        this.newBaselineRaw = newBaselineRaw;
    }

    public String getHighRangeRaw() {
        return highRangeRaw;
    }

    public void setHighRangeRaw(String highRangeRaw) {
        this.highRangeRaw = highRangeRaw;
    }

    public String getLowRangeRaw() {
        return lowRangeRaw;
    }

    public void setLowRangeRaw(String lowRangeRaw) {
        this.lowRangeRaw = lowRangeRaw;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public String getChartName() {
        return chartName;
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
    }

    public void synchronizeDisplayRange() {
        long now = System.currentTimeMillis();
        if (now - getEndDate().getTime() < MetricRange.SHIFT_RANGE) {
            long diff = getEndDate().getTime() - getStartDate().getTime();
            if (diff % Constants.DAYS == 0) {
                setA(ACTION_LASTN);
                int days = (int) (diff / Constants.DAYS);
                setRn(new Integer(days));
                setRu(new Integer(MonitorUtils.UNIT_DAYS));
            } else if (diff % Constants.HOURS == 0) {
                setA(ACTION_LASTN);
                int hours = (int) (diff / Constants.HOURS);
                setRn(new Integer(hours));
                setRu(new Integer(MonitorUtils.UNIT_HOURS));
            } else if (diff % Constants.MINUTES == 0) {
                setA(ACTION_LASTN);
                int minutes = (int) (diff / Constants.MINUTES);
                setRn(new Integer(minutes));
                setRu(new Integer(MonitorUtils.UNIT_MINUTES));
            }
        } else {
            setA(ACTION_DATE_RANGE);
        }
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (!shouldValidate(mapping, request)) {
            return null;
        }

        ActionErrors errs = super.validate(mapping, request);
        if (errs == null) {
            errs = new ActionErrors();
        }

        // If we are doing "Last N collection points", N must be
        // between 1 and 60.
        if (ACTION_LASTN.equals(getA()) && (getRu().intValue() == MonitorUtils.UNIT_COLLECTION_POINTS)) {
            if (null != getRn()) {
                int numPoints = getRn().intValue();
                if (numPoints < 1 || numPoints > 60) {
                    errs.add("rn", new ActionMessage("errors.range", getRn(), new Integer(1), new Integer(60)));
                }
            }
        }

        return errs;
    }

    /*
     * Only validate if. 1) Any self-submitting buttons were clicked, and 2) the
     * mapping specifies an input form to return to.
     * 
     * Child classes should call this to decide whether or not to perform custom
     * validation steps.
     */
    protected boolean shouldValidate(ActionMapping mapping, HttpServletRequest request) {
        boolean isRedrawing = isRedrawClicked() || isPrevRangeClicked() || isNextRangeClicked() || getSaveChart() ||
                              isChangeBaselineClicked() || isSaveBaselineClicked() || isCancelBaselineClicked() ||
                              isChangeHighRangeClicked() || isSaveHighRangeClicked() || isCancelHighRangeClicked() ||
                              isChangeLowRangeClicked() || isSaveLowRangeClicked() || isCancelLowRangeClicked() ||
                              isDateRangeSelected();
        return isRedrawing && (mapping.getInput() != null);
    }

    // -------------------------------------drop-downs

    public List getThresholdMenu() {
        return MonitorUtils.getThresholdMenu();
    }

    protected void setDefaults() {
        super.setDefaults();
        super.setWantEndDate(true);
        this.mode = null;
        this.ctype = null;
        this.showPeak = true;
        this.showHighRange = true;
        this.showValues = true;
        this.showAverage = true;
        this.showLowRange = true;
        this.showLow = true;
        this.showEvents = true;
        this.showBaseline = true;
        this.redraw = new ImageButtonBean();
        this.prevRange = new ImageButtonBean();
        this.nextRange = new ImageButtonBean();
        this.saveChart = false;
        this.changeBaseline = new ImageButtonBean();
        this.saveBaseline = new ImageButtonBean();
        this.cancelBaseline = new ImageButtonBean();
        this.changeHighRange = new ImageButtonBean();
        this.saveHighRange = new ImageButtonBean();
        this.cancelHighRange = new ImageButtonBean();
        this.changeLowRange = new ImageButtonBean();
        this.saveLowRange = new ImageButtonBean();
        this.cancelLowRange = new ImageButtonBean();
        this.prevPage = new ImageButtonBean();
        this.baseline = null;
        this.newBaseline = null;
        this.highRange = null;
        this.lowRange = null;
        this.baselineRaw = null;
        this.newBaselineRaw = null;
        this.highRangeRaw = null;
        this.lowRangeRaw = null;
        this.threshold = null;
        this.chartName = null;
    }
}
