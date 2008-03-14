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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.config.SyslogActionForm;
import org.hyperic.hq.ui.beans.AlertConditionBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.units.FormattedNumber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for dealing with rendering alert definition
 * conditions.
 */
public class AlertDefUtil {
    private static Log log = LogFactory.getLog(AlertDefUtil.class.getName());

    /**
     * Converts the duration and units into a number of seconds.
     *
     * @param duration duration
     * @param units one of <code>
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_HOURS,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_DAYS,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</code>
     * @return the number of seconds
     */
    public static long getSecondsConsideringUnits(long duration, int units) {
        long numSeconds = duration;
        if (units >= Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES) {
            numSeconds *= 60l;
            if (units >= Constants.ALERT_ACTION_ENABLE_UNITS_HOURS) {
                numSeconds *= 60l;
                if (units >= Constants.ALERT_ACTION_ENABLE_UNITS_DAYS) {
                    numSeconds *= 24l;
                    if (units >= Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS) {
                        numSeconds *= 7l;
                    }
                }
            }
        } else {
            log.warn("Units invalid ... assuming seconds.");
        }
        return numSeconds;
    }

    /**
     * <p>Return the duration and units for the passed-in number of
     * seconds.  The first element of the returned array will be the
     * duration.  The second element in the returned array will be one
     * of
     * <code>org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_SECONDS,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_HOURS,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_DAYS,
     * org.hyperic.hq.ui.Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS</code>.</p>
     *
     * @param seconds number of seconds <b>(will be updated)</b>
     * @return two-element Long array
     */
    public static Long[] getDurationAndUnits(Long seconds) {
        Long[] retVal = new Long[2];
        long secs = seconds.longValue();
        if (secs % Constants.SECS_IN_WEEK == 0) {
            retVal[0] = new Long(secs / Constants.SECS_IN_WEEK);
            retVal[1] = new Long(Constants.ALERT_ACTION_ENABLE_UNITS_WEEKS);
        } else if (secs % Constants.SECS_IN_DAY == 0) {
            retVal[0] = new Long(secs / Constants.SECS_IN_DAY);
            retVal[1] = new Long(Constants.ALERT_ACTION_ENABLE_UNITS_DAYS);
        } else if (secs % Constants.SECS_IN_HOUR == 0) {
            retVal[0] = new Long(secs / Constants.SECS_IN_HOUR);
            retVal[1] = new Long(Constants.ALERT_ACTION_ENABLE_UNITS_HOURS);
        } else if (secs % Constants.SECS_IN_MINUTE == 0) {
            retVal[0] = new Long(secs / Constants.SECS_IN_MINUTE);
            retVal[1] = new Long(Constants.ALERT_ACTION_ENABLE_UNITS_MINUTES);
        } else {
            retVal[0] = seconds;
            retVal[1] = new Long(Constants.ALERT_ACTION_ENABLE_UNITS_SECONDS);
        }
        return retVal;
    }


    /**
     * Converts the list of alert conditions into a list of
     * AlertConditionBean objects.
     *
     * @param acvList the list of alert conditions to convert
     * @return List of AlertConditionBean objects
     */
    public static List getAlertConditionBeanList(int sessionID,
                                                 HttpServletRequest request,
                                                 MeasurementBoss mb,
                                                 AlertConditionValue[] acvList,
                                                 boolean template)
    {
        String msgKey;
        ArrayList args;
        
        // conditions
        ArrayList alertDefConditions = new ArrayList(acvList.length);
        for (int i = 0; i < acvList.length; ++i) {
            AlertConditionValue acv = (AlertConditionValue)acvList[i];
            StringBuffer textValue = new StringBuffer();
            textValue.append( acv.getName() ).append(' ');
            
            switch ( acv.getType() ) {
            case EventConstants.TYPE_CONTROL:
                textValue.append( acv.getOption() );
                break;

            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                textValue.append( acv.getComparator() );
                textValue.append(' ');
                
                MeasurementTemplate mt = null;
                Measurement m = null;
                try {
                    if (template) {
                        List mtvs = mb.findMeasurementTemplates(
                            sessionID,
                            new Integer[] {new Integer(acv.getMeasurementId())},
                            PageControl.PAGE_ALL);
                        
                        if (mtvs.size() > 0)
                            mt = (MeasurementTemplate) mtvs.get(0);
                    }
                    else {
                        m = mb.getMeasurement(sessionID,
                                              new Integer(acv.getMeasurementId()));
                        mt = m.getTemplate();
                    }
                } catch (Exception e) {
                    // Use NULL values
                }
                
                String format = MeasurementConstants.UNITS_NONE;
                double value = acv.getThreshold();
                if (acv.getType() != EventConstants.TYPE_BASELINE) {
                    if (mt != null)
                        format = mt.getUnits();
                }
                else {
                    format = MeasurementConstants.UNITS_PERCENTAGE;
                    // Baseline threshold is stored in absolute number
                    value /= 100.0;
                }
                
                FormattedNumber absoluteFmt =
                    UnitsConvert.convert(value, format);
                textValue.append(absoluteFmt.toString());
                
                if (acv.getType() == EventConstants.TYPE_BASELINE) {
                    textValue.append(" of ");
                    textValue.append(
                        BizappUtils.getBaselineText(acv.getOption(), m) );
                }
                break;

            case EventConstants.TYPE_CHANGE:
            case EventConstants.TYPE_CUST_PROP:
                textValue.append
                    ( RequestUtils.message(request, "alert.current.list.ValueChanged") );
                break;

            case EventConstants.TYPE_LOG:
                msgKey = "alert.config.props.CB.LogCondition";
                args = new ArrayList(2);
                args.add(ResourceLogEvent.getLevelString(
                         Integer.parseInt(acv.getName())));
                if (acv.getOption() != null && acv.getOption().length() > 0) {
                    msgKey += ".StringMatch";
                    args.add(acv.getOption());
                }
                
                textValue = new StringBuffer(
                        RequestUtils.message(request, msgKey, args.toArray()));
                break;
            case EventConstants.TYPE_CFG_CHG:
                msgKey = "alert.config.props.CB.ConfigCondition";
                args = new ArrayList(1);
                if (acv.getOption() != null && acv.getOption().length() > 0) {
                    msgKey += ".FileMatch";
                    args.add(acv.getOption());
                }
                
                textValue = new StringBuffer(
                        RequestUtils.message(request, msgKey, args.toArray()));
                break;
            default:
                // do nothing
                continue;
            }

            AlertConditionBean acb = new AlertConditionBean
                ( textValue.toString(), acv.getRequired(), (i==0) /* first */);
            alertDefConditions.add(acb);
        }

        return alertDefConditions;
    }

    /**
     * Sets the following request attributes based on what's contained
     * in the AlertConditionValue.
     *
     * <ul>
     * <li>enableActionsResource - resource bundle key for display</li>
     * <li>enableActionsHowLong - how long</li>
     * <li>enableActionsHowLongUnits - units (i.e. -- ALERT_ACTION_ENABLE_UNITS_WEEKS)</li>
     * <li>enableActionsHowMany - number of times condition occurs</li>
     * </ul>
     *
     * @param request the http request
     * @param adv the condition
     */   
    public static void setEnablementRequestAttributes(HttpServletRequest request,
                                                      AlertDefinitionValue adv)
    {
        // enablement

        // If we can't cleanly compute the time period, units, etc., we'll assume
        // that we're computing for a time period in seconds.
        String enableActionsResource = "alert.config.props.CB.EnableTimePeriod";
        Long enableActionsHowLong = new Long( adv.getRange() );
        Long enableActionsHowLongUnits =
            new Long(Constants.ALERT_ACTION_ENABLE_UNITS_SECONDS);
        Long enableActionsHowMany = new Long( adv.getCount() );
        Long enableActionsHowManyUnits =
            new Long(Constants.ALERT_ACTION_ENABLE_UNITS_SECONDS);
                
        if (EventConstants.FREQ_EVERYTIME == adv.getFrequencyType()) {
			enableActionsResource = "alert.config.props.CB.EnableEveryTime";
		} else if (EventConstants.FREQ_ONCE == adv.getFrequencyType()) {
			enableActionsResource = "alert.config.props.CB.EnableOnce";
        } else if ( EventConstants.FREQ_DURATION == adv.getFrequencyType() ) {
            enableActionsResource = "alert.config.props.CB.EnableTimePeriod";
            Long[] l = getDurationAndUnits(enableActionsHowLong);
            enableActionsHowLong = l[0];
            enableActionsHowLongUnits = l[1];
            l = getDurationAndUnits(enableActionsHowMany);
            enableActionsHowMany = l[0];
            enableActionsHowManyUnits = l[1];
        } else { // ( EventConstants.FREQ_COUNTER == adv.getFrequencyType() )
            enableActionsResource = "alert.config.props.CB.EnableNumTimesInPeriod";
            Long[] l = getDurationAndUnits(enableActionsHowLong);
            enableActionsHowLong = l[0];
            enableActionsHowLongUnits = l[1];
        }
        request.setAttribute("enableActionsResource", enableActionsResource);
        request.setAttribute("enableActionsHowLong", enableActionsHowLong);
        request.setAttribute("enableActionsHowLongUnits", enableActionsHowLongUnits);
        request.setAttribute("enableActionsHowMany", enableActionsHowMany);
        request.setAttribute("enableActionsHowManyUnits", enableActionsHowManyUnits);
    }

    /**
     * Retrieve the alert definition from either the request or from
     * the bizapp as necessary.  First check to see if the alertDef is
     * already in the request attributes.  If it is, return it.  If
     * not, look for an "ad" parameter and then get the alert
     * definition from the bizapp and return it.
     */
    public static AlertDefinitionValue getAlertDefinition(HttpServletRequest request,
                                                          int sessionID,
                                                          EventsBoss eb)
        throws SessionNotFoundException,
               SessionTimeoutException,
               NamingException,
               CreateException,
               SystemException,
               FinderException,
               RemoteException,
               PermissionException,
               ParameterNotFoundException
    {
        AlertDefinitionValue adv = (AlertDefinitionValue)
            request.getAttribute(Constants.ALERT_DEFINITION_ATTR);
        if (null == adv) {
            String adS = request.getParameter(Constants.ALERT_DEFINITION_PARAM);
            if (null == adS) {
                throw new ParameterNotFoundException(Constants.ALERT_DEFINITION_PARAM);
            } else {
                Integer ad = new Integer(adS);
                adv = eb.getAlertDefinition(sessionID, ad);
                request.setAttribute(Constants.ALERT_DEFINITION_ATTR, adv);
            }
            log.trace( "adv.id=" + adv.getId() );
        }

        return adv;
    }

    public static ActionValue getSyslogActionValue(AlertDefinitionValue adv) {
        ActionValue[] actions = adv.getActions();
        for (int i=0; i<actions.length; ++i) {
            if ( actions[i].classnameHasBeenSet() &&
                 !( actions[i].getClassname().equals(null) ||
                    actions[i].getClassname().equals("") ) ) {
                try {
                    Class clazz = Class.forName( actions[i].getClassname() );
                    if ( SyslogActionConfig.class.isAssignableFrom(clazz) ) {
                        return actions[i];
                    }
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
        }
        return null;
    }

    public static void prepareSyslogActionForm(AlertDefinitionValue adv,
                                               SyslogActionForm form)
        throws EncodingException
    {
        ActionValue actionValue = getSyslogActionValue(adv);
        if (null != actionValue) {
            SyslogActionConfig sa = new SyslogActionConfig();
            ConfigResponse configResponse =
                ConfigResponse.decode( actionValue.getConfig() );
            sa.init(configResponse);
            form.setAd( adv.getId() );
            form.setMetaProject( sa.getMeta() );
            form.setProject( sa.getProduct() );
            form.setVersion( sa.getVersion() );
            form.setId( actionValue.getId() );
        }
    }

    /**
     * Returns a List of LabelValueBean objects whose labels and
     * values are both set to the string of the control actions for
     * the passed-in resource.
     * @throws RemoteException
     * @throws PermissionException
     * @throws PluginNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     */
    public static List getControlActions(int sessionID, AppdefEntityID adeId, ControlBoss cb)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PluginNotFoundException, PermissionException, RemoteException {
            List controlActions;
            
            if (adeId instanceof AppdefEntityTypeID)
                controlActions =
                    cb.getActions(sessionID, (AppdefEntityTypeID) adeId);
            else
                controlActions = cb.getActions(sessionID, adeId);
                
            return controlActions;
        }
}

// EOF
