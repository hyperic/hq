/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004, 2005, 2006],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;

/**
 * Form for editing / creating new alert definitions.
 */
public class DefinitionFormNG
    extends ResourceFormNG {
    private Log log = LogFactory.getLog(DefinitionFormNG.class.getName());

    // alert definition properties
    private Integer id; // nullable
//    private String name;
    private String description;
    private int priority;
    private boolean active;
    private boolean enabled;

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
    private Map<String,String> customProperties;

    private boolean disableForRecovery;

    private Map<String, String> prioritiesMap = new HashMap<String, String>();
    
    private static int[] priorities = new int[] { EventConstants.PRIORITY_HIGH,
                                                 EventConstants.PRIORITY_MEDIUM,
                                                 EventConstants.PRIORITY_LOW };

    private static int[] timeUnits = new int[] { Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES,
                                                Constants.ALERT_ACTION_ENABLE_UNITS_HOURS,
                                                Constants.ALERT_ACTION_ENABLE_UNITS_DAYS,
                                                Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS };

    private static String[] comparators = new String[] { Constants.ALERT_THRESHOLD_COMPARATOR_GT,
                                                        Constants.ALERT_THRESHOLD_COMPARATOR_EQ,
                                                        Constants.ALERT_THRESHOLD_COMPARATOR_LT,
                                                        Constants.ALERT_THRESHOLD_COMPARATOR_NE, };

    private static String[] controlActionStatuses = { ControlConstants.STATUS_INPROGRESS,
                                                     ControlConstants.STATUS_COMPLETED,
                                                     ControlConstants.STATUS_FAILED, };

    public String[] getControlActionStatuses() {
        return controlActionStatuses;
    }

    public DefinitionFormNG() {
        // do nothing
    }

    public Integer getAd() {
        return id;
    }

    public void setAd(Integer id) {
        this.id = id;
    }

    /*public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }*/

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*
     * ---------------------------------------------------------------- This
     * method is needed for some fancy Javascript processing. Do not remove.
     * ----------------------------------------------------------------
     */
    public ConditionBeanNG[] getConditions() {
        ConditionBeanNG[] conds = new ConditionBeanNG[conditions.size()];
        return (ConditionBeanNG[]) conditions.toArray(conds);
    }

    public ConditionBeanNG getCondition(int index) {
        if (index >= conditions.size()) {
            setNumConditions(index + 1);
        }
        return (ConditionBeanNG) conditions.get(index);
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
            conditions.add(new ConditionBeanNG());
        }

        // Remove extra conditions if necessary
        if (conditions.size() > numConditions) {
            for (int i = conditions.size(); i > numConditions; i--)
                conditions.remove(i - 1);
        }
    }

    /**
     * Get when to enable the actions. One of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_EACHTIME</li>
     * <li>Constants.ALERT_ACTION_ENABLE_TIMEPERIOD</li>
     * <li>Constants.ALERT_ACTION_ENABLE_NUMTIMESINPERIOD</li>
     * </ul>
     */
    public int getWhenEnabled() {
        return whenEnabled;
    }

    /**
     * Set when to enable the actions. Must be one of:
     * <ul>
     * <li>EventConstants.FREQ_EVERYTIME</li>
     * <li>EventConstants.FREQ_COUNTER</li>
     * <li>EventConstants.FREQ_ONCE</li>
     * </ul>
     */
    public void setWhenEnabled(int whenEnabled) {
        switch (whenEnabled) {
            case EventConstants.FREQ_EVERYTIME:
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
     * Get units for time period. One of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getMeetTimeUnitsTP() {
        return meetTimeUnitsTP;
    }

    /**
     * Set units for time period. Must be one of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
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
     * Get units for time period. One of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getHowLongUnitsTP() {
        return howLongUnitsTP;
    }

    /**
     * Set units for time period. Must be one of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
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
     * Get units for time period. One of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
     * </ul>
     */
    public int getHowLongUnitsNT() {
        return howLongUnitsNT;
    }

    /**
     * Set units for time period. Must be one of:
     * <ul>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_HOURS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_DAYS</li>
     * <li>Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</li>
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
        return howLongNT;
    }

    public int getHowLongUnits() {
        return howLongUnitsNT;
    }

    public void deleteCondition(int deletedCondition) {
        if (deletedCondition < conditions.size()) {
            conditions.remove(deletedCondition);
        }
    }

    public List getMetrics() {
        return metrics;
    }

    public void setMetrics(List metrics) {
        this.metrics = metrics;
    }

    public Map<String,String> getCustomProperties() {
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

    public void setCustomProperties(Map<String,String> customProperties) {
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

    public void reset() {
        super.reset();
        setDefaults();
    }

    /**
     * Import basic properties from The AlertDefinitionValue to this form.
     */
    public void importProperties(AlertDefinitionValue adv) {
        setAd(adv.getId());
        setName(adv.getName());
        setDescription(adv.getDescription());
        setPriority(adv.getPriority());
        setActive(adv.getActive());
        setEnabled(adv.getEnabled());
    }

    /**
     * Export basic properties from this form to the AlertDefinitionValue.
     */
    public void exportProperties(AlertDefinitionValue adv) {
        adv.setId(getAd());
        adv.setName(getName());
        adv.setDescription(getDescription());
        adv.setEnabled(this.isEnabled());
        adv.setActive(this.isActive());
        adv.setPriority(getPriority());
    }

    /**
     * Import the conditions and enablement properties from the
     * AlertDefinitionValue to this form.
     */
    public void importConditionsEnablement(AlertDefinitionValue adv, int sessionId, MeasurementBoss mb)
        throws MeasurementNotFoundException, SessionNotFoundException, SessionTimeoutException,
        TemplateNotFoundException, RemoteException {
        // we import the id here, too, so that the update will work
        setAd(adv.getId());

        boolean isTypeAlert = EventConstants.TYPE_ALERT_DEF_ID.equals(adv.getParentId());

        // ------------------------------------------------------------
        // -- conditions
        // ------------------------------------------------------------
        AlertConditionValue[] acvs = adv.getConditions();
        for (int i = 0, j = 0; i < acvs.length; i++) {
            if (acvs[i].getType() == EventConstants.TYPE_ALERT)
                continue;

            if (conditions.size() < j + 1) {
                setNumConditions(j + 1);
            }

            ConditionBeanNG cond = (ConditionBeanNG) conditions.get(j++);
            cond.importProperties(acvs[i], isTypeAlert, sessionId, mb);
        }

        disableForRecovery = adv.getWillRecover();

        // ------------------------------------------------------------
        // -- enablement
        // ------------------------------------------------------------
        whenEnabled = adv.getFrequencyType();
        if (EventConstants.FREQ_COUNTER == whenEnabled) {
            Long[] l = AlertDefUtil.getDurationAndUnits(new Long(adv.getRange()));
            howLongNT = new Integer(l[0].intValue());
            howLongUnitsNT = l[1].intValue();
            numTimesNT = new Integer((int) adv.getCount());
        }
    }

    /**
     * Export the conditions and enablement properties from this form to the
     * AlertDefinitionValue.
     */
    public void exportConditionsEnablement(AlertDefinitionValue adv, HttpServletRequest request, int sessionId,
                                           MeasurementBoss mb, boolean typeAlert) throws SessionTimeoutException,
        SessionNotFoundException, MeasurementNotFoundException, TemplateNotFoundException, RemoteException {
        // ------------------------------------------------------------
        // -- conditions
        // ------------------------------------------------------------
        log.debug("numConditions=" + getNumConditions());
        adv.removeAllConditions();
        for (int i = 0; i < getNumConditions(); ++i) {
            ConditionBeanNG cond = getCondition(i);
            // make sure first condition is ALWAYS required
            if (i == 0) {
                cond.setRequired(true);
            }
            AlertConditionValue acv = new AlertConditionValue();
            cond.exportProperties(acv, request, sessionId, mb, typeAlert);
            adv.addCondition(acv);
            log.trace("acv=" + acv);
        }

        // ------------------------------------------------------------
        // -- enablement
        // ------------------------------------------------------------
        adv.setWillRecover(this.isDisableForRecovery());
        adv.setFrequencyType(getWhenEnabled());

        // Count is either a number of times *or* a duration,
        // depending on which frequency type we're using. If we're
        // using FREQ_COUNTER, EventsBoss expects at least 1 for the
        // count.
        if (getWhenEnabled() != EventConstants.FREQ_EVERYTIME) {
            long count = 1;
            try {
                if (getWhenEnabled() == EventConstants.FREQ_COUNTER) {
                    count = getNumTimesNT().intValue();
                }
            } catch (NumberFormatException e) {
                // Use default value of 1
            }catch (NullPointerException e){
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
                    numSeconds = AlertDefUtil.getSecondsConsideringUnits(getHowLong().longValue(), getHowLongUnits());
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


    protected void setDefaults() {
        id = null;
        setName(null);
        description = null;
        priority = EventConstants.PRIORITY_MEDIUM;
        active = true;
        enabled = true;
        resetConditions();
        whenEnabled = 0;
        numTimesNT = null;
        howLongNT = null;
        howLongUnitsNT = 0;
        disableForRecovery = false;
    }

	public Map<String, String> getPrioritiesMap() {
		return prioritiesMap;
	}

	public void setPrioritiesMap(Map<String, String> prioritiesMap) {
		this.prioritiesMap = prioritiesMap;
	}

	public void setConditions(List conditions) {
		this.conditions = conditions;
	}
	
    public Map<String, String> validate( HttpServletRequest request, Map<String, String> map) {
        
        // only do this advanced validation if we are editing
        // conditions or creating a new definition
       
            for (int i = 0; i < getNumConditions(); ++i) {
                ConditionBeanNG cond = getCondition(i);
                cond.validate(request, map, i);
            }

        

        return map;
    }
    
}
