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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * Form for editing / creating new alert definitions.
 */
public class DefinitionForm extends ResourceForm  {
    private Log log = LogFactory.getLog(DefinitionForm.class.getName());

    // alert definition properties
    private Integer id; // nullable
    private String name;
    private String description;
    private int priority;
    private boolean active;
    // conditions
    private List conditions;
    
    // default metric to select, if available
    private int metricId;
    private String metricName;

    // enable actions
    private int whenEnabled;
    private Integer meetTimeTP = new Integer(1);
    private int meetTimeUnitsTP;
    private Integer howLongTP = new Integer(1);
    private int howLongUnitsTP;
    private Integer numTimesNT = new Integer(1);
    private Integer howLongNT = new Integer(1);
    private int howLongUnitsNT;

    private List metrics;
    private Collection customProperties;    
    
    private boolean disableForRecovery;

    private static int[] priorities =
        new int[] { EventConstants.PRIORITY_HIGH,
                    EventConstants.PRIORITY_MEDIUM,
                    EventConstants.PRIORITY_LOW };
    
    private static int[] timeUnits =
        new int[] { Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES,
                    Constants.ALERT_ACTION_ENABLE_UNITS_HOURS,
                    Constants.ALERT_ACTION_ENABLE_UNITS_DAYS,
                    Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS };
    
    private static String[] comparators =
        new String[] { Constants.ALERT_THRESHOLD_COMPARATOR_GT,
                       Constants.ALERT_THRESHOLD_COMPARATOR_EQ,
                       Constants.ALERT_THRESHOLD_COMPARATOR_LT,
                       Constants.ALERT_THRESHOLD_COMPARATOR_NE,
                     };

    private static String[] controlActionStatuses = {
                ControlConstants.STATUS_INPROGRESS,
                ControlConstants.STATUS_COMPLETED,
                ControlConstants.STATUS_FAILED, };


    public String[] getControlActionStatuses() {
        return controlActionStatuses;
    }

    public DefinitionForm() {
        // do nothing
    }

    public Integer getAd() {
        return id;
    }

    public void setAd(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }

    /*----------------------------------------------------------------
      This method is needed for some fancy Javascript processing.  Do
      not remove.
      ----------------------------------------------------------------*/
    public ConditionBean[] getConditions() {
        ConditionBean[] conds = new ConditionBean[conditions.size()];
        return (ConditionBean[])conditions.toArray(conds);
    }

    public ConditionBean getCondition(int index) {
        if ( index >= conditions.size() ) {
            setNumConditions(index + 1);
        }
        return (ConditionBean)conditions.get(index);
    }

    /**
     * @return Returns the metricId.
     */
    public int getMetricId() {
        return metricId;
    }
    
    /**
     * @param metricId The metricId to set.
     */
    public void setMetricId(int metricId) {
        this.metricId = metricId;
                
        // Select default metric
        if (metricId > 0)
            getCondition(0).setMetricId(new Integer(metricId));
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public int getNumConditions() {
        return conditions.size();
    }

    public void setNumConditions(int numConditions) {
        while (conditions.size() < numConditions) {
            conditions.add( new ConditionBean() );
        }
        
        // Remove extra conditions if necessary
        if (conditions.size() > numConditions) {
            for (int i = conditions.size(); i > numConditions; i--)
                conditions.remove(i - 1);
        }
    }

    /**
     * Get when to enable the actions.  One of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_EACHTIME</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_TIMEPERIOD</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_NUMTIMESINPERIOD</li>
     * </ul>
     */
    public int getWhenEnabled() {
        return whenEnabled;
    }
    
    /**
     * Set when to enable the actions.  Must be one of:
     * <ul>
     *   <li>EventConstants.FREQ_EVERYTIME</li>
     *   <li>EventConstants.FREQ_DURATION</li>
     *   <li>EventConstants.FREQ_COUNTER</li>
     *   <li>EventConstants.FREQ_ONCE</li>
     * </ul>
     */
    public void setWhenEnabled(int whenEnabled) {
        switch (whenEnabled) {
        case EventConstants.FREQ_EVERYTIME:
        case EventConstants.FREQ_DURATION:
        case EventConstants.FREQ_COUNTER:
        case EventConstants.FREQ_ONCE:
            break;
        default:
            throw new IllegalArgumentException("invalid whenEnabled property value");
        }
        this.whenEnabled = whenEnabled;
    }
    
    public Integer getMeetTimeTP() {
        return meetTimeTP;
    }
    
    public void setMeetTimeTP(Integer meetTimeTP) {
        this.meetTimeTP = meetTimeTP;
    }

    /**
     * Get units for time period.  One of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getMeetTimeUnitsTP() {
        return meetTimeUnitsTP;
    }
    
    /**
     * Set units for time period.  Must be one of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public void setMeetTimeUnitsTP(int meetTimeUnitsTP) {
        switch (meetTimeUnitsTP) {
        case Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES:
        case Constants.ALERT_ACTION_ENABLE_UNITS_HOURS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_DAYS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS:
            break;
        default:
            throw new IllegalArgumentException("invalid howLongUnits property value");
        }
        this.meetTimeUnitsTP = meetTimeUnitsTP;
    }

    public Integer getHowLongTP() {
        return howLongTP;
    }
    
    public void setHowLongTP(Integer howLongTP) {
        this.howLongTP = howLongTP;
    }

    /**
     * Get units for time period.  One of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getHowLongUnitsTP() {
        return howLongUnitsTP;
    }
    
    /**
     * Set units for time period.  Must be one of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public void setHowLongUnitsTP(int howLongUnitsTP) {
        switch (howLongUnitsTP) {
        case Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES:
        case Constants.ALERT_ACTION_ENABLE_UNITS_HOURS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_DAYS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS:
            break;
        default:
            throw new IllegalArgumentException("invalid howLongUnits property value");
        }
        this.howLongUnitsTP = howLongUnitsTP;
    }

    public Integer getNumTimesNT() {
        return numTimesNT;
    }
    
    public void setNumTimesNT(Integer numTimesNT) {
        this.numTimesNT = numTimesNT;
    }
    
    public Integer getHowLongNT() {
        return howLongNT;
    }
    
    public void setHowLongNT(Integer howLongNT) {
        this.howLongNT = howLongNT;
    }

    /**
     * Get units for time period.  One of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getHowLongUnitsNT() {
        return howLongUnitsNT;
    }
    
    /**
     * Set units for time period.  Must be one of:
     * <ul>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     *   <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public void setHowLongUnitsNT(int howLongUnitsNT) {
        switch (howLongUnitsNT) {
        case Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES:
        case Constants.ALERT_ACTION_ENABLE_UNITS_HOURS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_DAYS:
        case Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS:
            break;
        default:
            throw new IllegalArgumentException("invalid howLongUnits property value");
        }
        this.howLongUnitsNT = howLongUnitsNT;
    }

    public Integer getHowLong() {
        if (whenEnabled == EventConstants.FREQ_DURATION) {
            return howLongTP;
        } else { // (whenEnabled == EventConstants.FREQ_COUNTER)
            return howLongNT;
        }
    }

    public int getHowLongUnits() {
        if (whenEnabled == EventConstants.FREQ_DURATION) {
            return howLongUnitsTP;
        } else { // (whenEnabled == EventConstants.FREQ_COUNTER)
            return howLongUnitsNT;
        }
    }

    public void deleteCondition(int deletedCondition) {
        if ( deletedCondition < conditions.size() ) {
            conditions.remove(deletedCondition);
        }
    }

    public List getMetrics() {
        return metrics;
    }

    public void setMetrics(List metrics) {
        this.metrics = metrics;
    }
    
    public Collection getCustomProperties() {
        return customProperties;
    }
    
    /**
     * @param disableForRecovery The disableForRecovery to set.
     */
    public void setDisableForRecovery(boolean disableForRecovery) {
        this.disableForRecovery = disableForRecovery;
    }


    /**
     * @return Returns the disableForRecovery.
     */
    public boolean isDisableForRecovery() {
        return disableForRecovery;
    }


    public void setCustomProperties(Collection customProperties) {
        this.customProperties = customProperties;
    }
    
    public int[] getPriorities() {
        return priorities;
    }

    public int[] getTimeUnits() {
        return timeUnits;
    }

    public String[] getComparators() {
        return comparators;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    /**
     * Import basic properties from The AlertDefinitionValue to this
     * form.
     */
    public void importProperties(AlertDefinitionValue adv) {
        setAd( adv.getId() );
        setName( adv.getName() );
        setDescription( adv.getDescription() );
        setPriority( adv.getPriority() );
        setActive( adv.getEnabled() );
    }

    /**
     * Export basic properties from this form to the
     * AlertDefinitionValue.
     */
    void exportProperties(AlertDefinitionValue adv) {
        adv.setId( getAd() );
        adv.setName( getName() );
        adv.setDescription( getDescription() );
        adv.setEnabled( this.isActive() );
        adv.setPriority( getPriority() );
    }

    /**
     * Import the conditions and enablement properties from the
     * AlertDefinitionValue to this form.
     */
    public void importConditionsEnablement(AlertDefinitionValue adv,
                                           int sessionId, MeasurementBoss mb)
        throws MeasurementNotFoundException, SessionNotFoundException,
               SessionTimeoutException, TemplateNotFoundException,
               RemoteException {
        // we import the id here, too, so that the update will work
        setAd( adv.getId() );

        boolean isTypeAlert =
            EventConstants.TYPE_ALERT_DEF_ID.equals(adv.getParentId());
        
        //------------------------------------------------------------
        //-- conditions
        //------------------------------------------------------------
        AlertConditionValue[] acvs = adv.getConditions();
        setNumConditions(0);        // Start from 0
        for (int i = 0; i < acvs.length; ++i) {
            if (acvs[i].getType() == EventConstants.TYPE_ALERT)
                continue;
            
            setNumConditions(conditions.size() + 1);
            ConditionBean cond = (ConditionBean)conditions.get(i);
            cond.importProperties(acvs[i], isTypeAlert, sessionId, mb);
        }
        
        disableForRecovery = adv.getWillRecover();

        //------------------------------------------------------------
        //-- enablement
        //------------------------------------------------------------
        whenEnabled = adv.getFrequencyType();
        if (EventConstants.FREQ_DURATION == whenEnabled) {
            Long[] l = AlertDefUtil.getDurationAndUnits( new Long( adv.getRange() ) );
            howLongTP = new Integer(l[0].intValue());
            howLongUnitsTP = l[1].intValue();
            l = AlertDefUtil.getDurationAndUnits( new Long( adv.getCount() ) );
            meetTimeTP = new Integer(l[0].intValue());
            meetTimeUnitsTP = l[1].intValue();
        } else if (EventConstants.FREQ_COUNTER == whenEnabled) {
            Long[] l = AlertDefUtil.getDurationAndUnits( new Long( adv.getRange() ) );
            howLongNT = new Integer(l[0].intValue());
            howLongUnitsNT = l[1].intValue();
            numTimesNT = new Integer( (int) adv.getCount() );
        }
    }

    /**
     * Export the conditions and enablement properties from this form
     * to the AlertDefinitionValue.
     */
    public void exportConditionsEnablement(AlertDefinitionValue adv,
                                           HttpServletRequest request,
                                           int sessionId, MeasurementBoss mb,
                                           boolean typeAlert)
        throws SessionTimeoutException, SessionNotFoundException,
               MeasurementNotFoundException, TemplateNotFoundException,
               RemoteException {
        //------------------------------------------------------------
        //-- conditions
        //------------------------------------------------------------
        log.debug("numConditions=" + getNumConditions());
        adv.removeAllConditions();
        for (int i = 0; i < getNumConditions(); ++i) {
            ConditionBean cond = getCondition(i);
            // make sure first condition is ALWAYS required
            if (i == 0) {
                cond.setRequired(true);
            }
            AlertConditionValue acv = new AlertConditionValue();
            cond.exportProperties(acv, request, sessionId, mb, typeAlert);
            adv.addCondition(acv);
            log.trace("acv=" + acv);
        }

        //------------------------------------------------------------
        //-- enablement
        //------------------------------------------------------------
        adv.setWillRecover( this.isDisableForRecovery() );
        adv.setFrequencyType( getWhenEnabled() );

        // Count is either a number of times *or* a duration,
        // depending on which frequency type we're using.  If we're
        // using FREQ_COUNTER, EventsBoss expects at least 1 for the
        // count.
        if (getWhenEnabled() != EventConstants.FREQ_EVERYTIME) {
            long count = 1;
            try {
                if (getWhenEnabled() == EventConstants.FREQ_DURATION) {
                    count = AlertDefUtil.getSecondsConsideringUnits
                        ( getMeetTimeTP().longValue(), getMeetTimeUnitsTP() );
                } else if (getWhenEnabled() == EventConstants.FREQ_COUNTER)
                {
                    count = getNumTimesNT().intValue();
                }
            } catch (NumberFormatException e) {
                // Use default value of 1
            }
            
            if (count <= 0) {
                count = 1; // must be at least 1
            }
            adv.setCount(count);
    
            // EventsBoss expects range in seconds
            long numSeconds = 1;
            try {
                if (getHowLong() != null) {
                    numSeconds = AlertDefUtil.getSecondsConsideringUnits
                        ( getHowLong().longValue(), getHowLongUnits() );
                }
            } catch (NumberFormatException e) {
                // Use default value of 1
            }
            if (numSeconds <= 0)
                numSeconds = 1;
            
            adv.setRange(numSeconds);
        }
    }

    public void resetConditions() {
        conditions = new ArrayList();
        setNumConditions(1);
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        // don't validate if we are preparing the form ...
        if ( !shouldValidate(mapping, request) )
            return null;

        ActionErrors errs = super.validate(mapping, request);
        if (null == errs) {
            errs = new ActionErrors();
        }
        
        // only do this advanced validation if we are editing
        // conditions or creating a new definition
        if ( mapping.getName().equals("NewAlertDefinitionForm") ||
             mapping.getName().equals("EditAlertDefinitionConditionsForm") ) {
            for (int i = 0; i < getNumConditions(); ++i) {
                ConditionBean cond = getCondition(i);
                cond.validate(request, errs, i);
            }

            // make sure enablement stuff is a-ok
            if (getWhenEnabled() == EventConstants.FREQ_DURATION) {
                log.debug("howLongTP=" + getHowLongTP());
                if ( getHowLongTP() != null && getMeetTimeTP() != null ) {
                    long count = AlertDefUtil.getSecondsConsideringUnits
                        (getMeetTimeTP().longValue(), getMeetTimeUnitsTP());
                    long range = AlertDefUtil.getSecondsConsideringUnits
                        (getHowLongTP().longValue(), getHowLongUnitsTP());
                    if (count > range) {
                        String fieldName = RequestUtils.message(
                            request, "alert.config.props.CB.TimePeriod");
                        ActionMessage err = new ActionMessage(
                            "alert.config.error.RangeTooSmall", fieldName);
                        errs.add("howLongTP", err);
                    }
                }
            }
        }
        
        return errs;
    }

    protected void setDefaults() {
        id = null;
        name = null;
        description = null;
        priority = EventConstants.PRIORITY_MEDIUM;
        active = true;
        resetConditions();
        whenEnabled = 0;
        numTimesNT = null;
        howLongTP = null;
        howLongUnitsTP = 0;
        howLongNT = null;
        howLongUnitsNT = 0;
        disableForRecovery = false;
    }
}
