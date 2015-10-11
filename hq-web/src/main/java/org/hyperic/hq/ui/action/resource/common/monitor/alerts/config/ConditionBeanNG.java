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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.GenericValidator;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.util.NumberUtil;
import org.hyperic.util.units.FormattedNumber;

import com.opensymphony.xwork2.util.LocalizedTextUtil;

/**
 * Bean that holds condition info.
 * 
 */
public final class ConditionBeanNG {
    private static final String DOUBLE_REGEX = "[0-9,.]+";
    private static final String TYPE_ABS = "absolute";
    private static final String TYPE_PERC = "percentage";
    private static final String TYPE_CHG = "changed";

    private Log log = LogFactory.getLog(ConditionBeanNG.class.getName());

    private Integer id; // nullable
    private boolean required;
    private String trigger;
    private String thresholdType;
    private String absoluteComparator;
    private String percentageComparator;
    private Integer metricId;
    private String metricName;
    private String absoluteValue;
    private String percentage;
    private String baselineOption;
    private String controlAction;
    private String controlActionStatus;
    private String customProperty;
    private int logLevel;
    private String logMatch;
    private String fileMatch;

    // -------------------------------------constructors

    public ConditionBeanNG() {
        log.trace("creating new condition form");
        setDefaults();
    }

    // -------------------------------------public methods

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public String getComparator() {
        if (thresholdType.equals(TYPE_ABS)) {
            return absoluteComparator;
        } else {
            return percentageComparator;
        }
    }

    public String getAbsoluteComparator() {
        return absoluteComparator;
    }

    public void setAbsoluteComparator(String absoluteComparator) {
        this.absoluteComparator = absoluteComparator;
    }

    public String getPercentageComparator() {
        return percentageComparator;
    }

    public void setPercentageComparator(String percentageComparator) {
        this.percentageComparator = percentageComparator;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        int ind;
        if ((ind = percentage.indexOf('%')) > -1)
            percentage = percentage.substring(0, ind);

        this.percentage = percentage;
    }

    public String getAbsoluteValue() {
        return absoluteValue;
    }

    public void setAbsoluteValue(String absoluteValue) {
        this.absoluteValue = absoluteValue;
    }

    public String getBaselineOption() {
        return baselineOption;
    }

    public void setBaselineOption(String baselineOption) {
        this.baselineOption = baselineOption;
    }

    public String getControlAction() {
        return controlAction;
    }

    public void setControlAction(String controlAction) {
        this.controlAction = controlAction;
    }

    public String getControlActionStatus() {
        return controlActionStatus;
    }

    public void setControlActionStatus(String controlActionStatus) {
        this.controlActionStatus = controlActionStatus;
    }

    public String getCustomProperty() {
        return customProperty;
    }

    public void setCustomProperty(String customProperty) {
        this.customProperty = customProperty;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogMatch() {
        return logMatch;
    }

    public void setLogMatch(String logMatch) {
        this.logMatch = logMatch;
    }

    public String getFileMatch() {
        return fileMatch;
    }

    public void setFileMatch(String fileMatch) {
        this.fileMatch = fileMatch;
    }

    public double getThresholdValue() {
        if (trigger.equals("onMetric")) {
            if (thresholdType.equals(TYPE_ABS)) {
                return NumberUtil.stringAsNumber(absoluteValue).doubleValue();
            } else if (thresholdType.equals(TYPE_PERC)) {
                return NumberUtil.stringAsNumber(percentage).doubleValue();
            }
        }
        return 0d;
    }

    public boolean validate(HttpServletRequest request, Map<String, String> errs, int idx) {
        boolean valid = true;
        if (trigger.equals("onMetric")) {
            if (getMetricId().intValue() <= 0) {
                // user didn't select a metric
                errs.put("condition[" + idx + "].metricId",LocalizedTextUtil.findDefaultText("alert.config.error.NoMetricSelected", request.getLocale()));
                valid = false;
            }
            if (thresholdType.equals(TYPE_ABS)) {
                if (!GenericValidator.matchRegexp(getAbsoluteValue(), DOUBLE_REGEX)) {
                    String fieldName = LocalizedTextUtil.findDefaultText( "alert.config.props.CB.AbsoluteValue", request.getLocale());
                    errs.put("condition[" + idx + "].absoluteValue",LocalizedTextUtil.findDefaultText( "errors.double",request.getLocale(), new Object[]{fieldName}));
                    valid = false;
                } else { // double
                    // do nothing
                }
            } else if (thresholdType.equals(TYPE_PERC)) { // percentage
                if (!GenericValidator.matchRegexp(getPercentage(), DOUBLE_REGEX)) {
                	String fieldName = LocalizedTextUtil.findDefaultText( "alert.config.props.CB.Percentage", request.getLocale());
                	errs.put("condition[" + idx + "].percentage",LocalizedTextUtil.findDefaultText( "errors.double",request.getLocale(), new Object[]{fieldName}));
                    valid = false;
                } else { // double
                    // 0-100 range
                    double percentage = NumberUtil.stringAsNumber(getPercentage()).doubleValue();
                    if (!GenericValidator.isInRange(percentage, 0d, 1000d)) {
                    	errs.put("condition[" + idx + "].percentage",LocalizedTextUtil.findDefaultText( "errors.range",request.getLocale(), new Object[]{String.valueOf(percentage),String.valueOf(0d), String.valueOf(1000d)}));
                        valid = false;
                    }
                }
                if (null == getBaselineOption() || getBaselineOption().length() == 0) {
                    log.debug("!!! NO BASELINE OPTION!");
                    errs.put("condition[" + idx + "].baselineOption",LocalizedTextUtil.findDefaultText("alert.config.error.NoBaselineOptionSelected", request.getLocale()));
                    valid = false;
                } else {
                    log.debug("*** BASELINE OPTION!");
                }
            }
        } else if (trigger.equals("onCustProp")) {
            if (0 == getCustomProperty().length() || "-1".equals(getCustomProperty())) {
            	errs.put("condition[" + idx + "].customProperty",LocalizedTextUtil.findDefaultText("alert.config.error.NoCustomPropertySelected", request.getLocale()));
                valid = false;
            }
        } else if (trigger.equals("onLog") || trigger.equals("onCfgChg")) {
            // Nothing is required
        } else { // on control action
            if (0 == getControlAction().length()) {
            	errs.put("condition[" + idx + "].controlAction",LocalizedTextUtil.findDefaultText("alert.config.error.NoControlActionSelected", request.getLocale()));
                valid = false;
            }
            if (0 == getControlActionStatus().length()) {
            	errs.put("condition[" + idx + "].controlActionStatus",LocalizedTextUtil.findDefaultText("alert.config.error.NoControlActionStatusSelected", request.getLocale()));
                valid = false;
            }
        }

        return valid;
    }

    public void importProperties(AlertConditionValue acv, boolean isTypeAlert, int sessionId, MeasurementBoss mb)
        throws MeasurementNotFoundException, SessionNotFoundException, SessionTimeoutException,
        TemplateNotFoundException, RemoteException {
        switch (acv.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
            case EventConstants.TYPE_CHANGE:
                metricId = new Integer(acv.getMeasurementId());
                metricName = acv.getName();

                trigger = "onMetric";
                if (acv.getType() == EventConstants.TYPE_THRESHOLD) {
                    thresholdType = TYPE_ABS;

                    String unit;

                    if (isTypeAlert) {
                        MeasurementTemplate mtv = mb.getMeasurementTemplate(sessionId, metricId);
                        unit = mtv.getUnits();
                    } else {
                        Measurement m = mb.getMeasurement(sessionId, metricId);
                        unit = m.getTemplate().getUnits();
                    }

                    if (unit.equals(MeasurementConstants.UNITS_NONE)) {
                        absoluteValue = String.valueOf(acv.getThreshold());
                    } else {
                        FormattedNumber absoluteFmt = UnitsConvert.convert(acv.getThreshold(), unit);
                        absoluteValue = absoluteFmt.toString();
                    }
                } else if (acv.getType() == EventConstants.TYPE_BASELINE) {
                    thresholdType = TYPE_PERC;
                    baselineOption = acv.getOption();
                    percentage = NumberUtil.numberAsString(acv.getThreshold());
                } else if (acv.getType() == EventConstants.TYPE_CHANGE) {
                    thresholdType = TYPE_CHG;
                }
                break;
            case EventConstants.TYPE_CUST_PROP:
                trigger = "onCustProp";
                customProperty = acv.getName();
                break;
            case EventConstants.TYPE_LOG:
                trigger = "onLog";
                logLevel = Integer.parseInt(acv.getName());
                logMatch = acv.getOption();
                break;
            case EventConstants.TYPE_CFG_CHG:
                trigger = "onCfgChg";
                fileMatch = acv.getOption();
                break;
            case EventConstants.TYPE_CONTROL:
            default:
                trigger = "onEvent";
                controlAction = acv.getName();
                controlActionStatus = acv.getOption();
                break;
        }

        required = acv.getRequired();
        if (thresholdType.equals(TYPE_ABS)) {
            absoluteComparator = acv.getComparator();
            percentageComparator = null;
        } else if (thresholdType.equals(TYPE_PERC)) {
            absoluteComparator = null;
            percentageComparator = acv.getComparator();
        } else {
            absoluteComparator = null;
            percentageComparator = null;
        }
    }

    public void exportProperties(AlertConditionValue acv, HttpServletRequest request, int sessionId,
                                 MeasurementBoss mb, boolean typeAlert) throws SessionTimeoutException,
        SessionNotFoundException, MeasurementNotFoundException, RemoteException, TemplateNotFoundException {
        acv.setId(getId());
        if (getTrigger().equals("onMetric")) {
            acv.setMeasurementId(getMetricId().intValue());
            if (getThresholdType().equals(TYPE_ABS)) {
                acv.setType(EventConstants.TYPE_THRESHOLD);

                String unit;

                if (typeAlert) {
                    MeasurementTemplate mtv = mb.getMeasurementTemplate(sessionId, getMetricId());
                    unit = mtv.getUnits();
                } else {
                    // parse the value
                    Measurement m = mb.getMeasurement(sessionId, getMetricId());
                    unit = m.getTemplate().getUnits();
                }

                try {
                    acv.setThreshold(BizappUtils.parseMeasurementValue(getAbsoluteValue(), unit));
                } catch (ParseException e) {
                    acv.setThreshold(getThresholdValue());
                }

                acv.setComparator(getComparator());
            } else if (getThresholdType().equals(TYPE_PERC)) {
                acv.setType(EventConstants.TYPE_BASELINE);
                acv.setOption(getBaselineOption());
                acv.setThreshold(NumberUtil.stringAsNumber(percentage).doubleValue());
                acv.setComparator(getComparator());
            } else {
                acv.setType(EventConstants.TYPE_CHANGE);
            }
            acv.setName(getMetricName());
        } else if (getTrigger().equals("onCustProp")) {
            acv.setType(EventConstants.TYPE_CUST_PROP);
            acv.setName(getCustomProperty());
        } else if (getTrigger().equals("onLog")) {
            acv.setType(EventConstants.TYPE_LOG);
            acv.setName(String.valueOf(getLogLevel()));
            acv.setOption(getLogMatch());
        } else if (getTrigger().equals("onCfgChg")) {
            acv.setType(EventConstants.TYPE_CFG_CHG);
            acv.setOption(getFileMatch());
        } else {
            acv.setType(EventConstants.TYPE_CONTROL);
            acv.setName(getControlAction());
            acv.setOption(getControlActionStatus());
        }
        acv.setRequired(isRequired());
    }

    private void setDefaults() {
        trigger = "onMetric";
        thresholdType = TYPE_ABS;
        metricId = null;
        metricName = null;
        absoluteValue = null;
        percentage = null;
        baselineOption = null;
        controlAction = null;
        controlActionStatus = null;
        customProperty = null;
        logLevel = -1;
        logMatch = null;
        fileMatch = null;
    }
}

// EOF
