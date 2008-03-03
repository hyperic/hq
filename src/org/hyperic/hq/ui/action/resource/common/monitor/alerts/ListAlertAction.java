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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.AlertBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.NumberUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.units.FormatSpecifics;
import org.hyperic.util.units.FormattedNumber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * List all alerts for this entity
 *
 */
public class ListAlertAction extends TilesAction {

    private Log log = LogFactory.getLog(ListAlertAction.class.getName());
    
    /**
     * Create a list of AlertBean objects based on the AlertValue
     * objects for this resource.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        int sessionId = RequestUtils.getSessionId(request).intValue();
        AppdefEntityID appEntId = RequestUtils.getEntityId(request);

        ServletContext ctx = getServlet().getServletContext();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);

        GregorianCalendar cal = new GregorianCalendar();
        try {
            Integer year = RequestUtils.getIntParameter(request, "year");
            Integer month = RequestUtils.getIntParameter(request, "month");
            Integer day = RequestUtils.getIntParameter(request, "day");
            cal.set(Calendar.YEAR, year.intValue());
            cal.set(Calendar.MONTH, month.intValue());
            cal.set(Calendar.DAY_OF_MONTH, day.intValue());
        } catch (ParameterNotFoundException e) {
            // Ignore
        }
        
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        PageControl pc = RequestUtils.getPageControl(request);
        
        try {
            RequestUtils.getIntParameter(request, Constants.SORTCOL_PARAM);
        } catch (ParameterNotFoundException e) {
            // By default we sort by descending ctime
            pc.setSortorder(PageControl.SORT_DESC);
        }
        
        PageList alerts;
        try {
            long begin = cal.getTimeInMillis();
            alerts = eb.findAlerts(sessionId, appEntId, begin,
                                   begin + Constants.DAYS, pc);
        } catch(PermissionException e) {
            // user is not allowed to see/manage alerts. 
            // return empty list for now
            alerts = new PageList();
        }

        PageList uiBeans = new PageList();
        for (Iterator itr = alerts.iterator();itr.hasNext();) {
            AlertValue av = (AlertValue)itr.next();
            AlertDefinitionValue adv = eb.getAlertDefinition
                ( sessionId, av.getAlertDefId() );
            AlertBean bean = new AlertBean(av.getId(), av.getCtime(),
                                           adv.getId(), adv.getName(),
                                           adv.getPriority(), appEntId.getId(),
                                           new Integer(appEntId.getType()),
                                           av.isFixed(), av.isAcknowledgeable());

            AlertConditionLogValue[] condLogs = av.getConditionLogs();
            if (condLogs.length > 1) {
                _setupMultiCondition(bean, request);
            } else if (condLogs.length == 1) {
                AlertConditionValue cond = condLogs[0].getCondition();
                _setupCondition(bean, cond, condLogs[0].getValue(), request,
                                mb, sessionId);
            } else {
                // fall back to alert definition conditions: PR 6992
                AlertConditionValue[] conds = adv.getConditions();
                if (conds.length > 1) {
                    _setupMultiCondition(bean, request);
                } else if (conds.length == 1) {
                    _setupCondition(bean, conds[0], null, request, mb, sessionId);
                } else {
                    // *serious* trouble
                    log.error("No condition logs for alert: " + av.getId());
                    bean.setMultiCondition(true);
                    bean.setConditionName(Constants.UNKNOWN);
                    bean.setValue(Constants.UNKNOWN);
                }
            }

            uiBeans.add(bean);
        }
        context.putAttribute( Constants.RESOURCE_ATTR,
                              RequestUtils.getResource(request) );
        context.putAttribute( Constants.RESOURCE_OWNER_ATTR,
                              request.getAttribute(Constants.RESOURCE_OWNER_ATTR) );
        context.putAttribute( Constants.RESOURCE_MODIFIER_ATTR,
                              request.getAttribute(Constants.RESOURCE_MODIFIER_ATTR) );
        request.setAttribute( Constants.ALERTS_ATTR, uiBeans );
        request.setAttribute( Constants.LIST_SIZE_ATTR,
                              new Integer( alerts.getTotalSize() ) );

        return null;
    }

    private void _setupCondition(AlertBean bean, AlertConditionValue cond,
                                 String value, HttpServletRequest request,
                                 MeasurementBoss mb, int sessionId)
        throws SessionTimeoutException,
               SessionNotFoundException,
               MeasurementNotFoundException,
               RemoteException
    {
        bean.setConditionName( cond.getName() );

        switch( cond.getType() ) {
        case EventConstants.TYPE_CONTROL:
            bean.setComparator("");
            bean.setThreshold( cond.getOption() );
            bean.setValue
                ( RequestUtils.message(request, "alert.current.list.ControlActualValue") );
            break;

        case EventConstants.TYPE_THRESHOLD:
        case EventConstants.TYPE_BASELINE:
        case EventConstants.TYPE_CHANGE:
            // format threshold and value
            Measurement m = mb.getMeasurement(sessionId, 
                                              new Integer(cond.getMeasurementId()));
            FormatSpecifics precMax = new FormatSpecifics();
            precMax.setPrecision(FormatSpecifics.PRECISION_MAX);

            // convert() can't handle Double.NaN -- just display ?? for the value
            if ( value == null || value.length() == 0 ) {
                bean.setValue(Constants.UNKNOWN);
            } else {
                double dval =
                    NumberUtil.stringAsNumber( value ).doubleValue();

                FormattedNumber val =
                    UnitsConvert.convert(dval, m.getTemplate().getUnits());
                
                bean.setValue( val.toString() );
            }

            if ( cond.getType() == EventConstants.TYPE_THRESHOLD ) {
                bean.setComparator( cond.getComparator() );
                FormattedNumber th = UnitsConvert.convert(cond.getThreshold(),
                        m.getTemplate().getUnits(),
                        precMax);
                bean.setThreshold( th.toString() );
            }
            else if ( cond.getType() == EventConstants.TYPE_BASELINE) {
                bean.setComparator( cond.getComparator() );
                bean.setThreshold( cond.getThreshold() + "% of " + cond.getOption());
            } else if ( cond.getType() == EventConstants.TYPE_CHANGE) {
                bean.setComparator("");
                bean.setThreshold
                    ( RequestUtils.message(request, "alert.current.list.ValueChanged") );
            }
            break;

        case EventConstants.TYPE_CUST_PROP:
            bean.setComparator("");
            bean.setThreshold
                ( RequestUtils.message(request, "alert.current.list.ValueChanged") );
            
            if (value != null)
                bean.setValue(value);
            else
                bean.setValue(Constants.UNKNOWN);
            break;

        case EventConstants.TYPE_LOG:
            bean.setConditionName(
                RequestUtils.message(request,"alert.config.props.CB.LogLevel"));
            bean.setComparator("=");
            bean.setThreshold(
                ResourceLogEvent.getLevelString(
                    Integer.parseInt(cond.getName())));
            bean.setValue(value);
            break;
        case EventConstants.TYPE_CFG_CHG:
            bean.setValue(value);
            break;
        default:
            bean.setName(Constants.UNKNOWN);
            bean.setComparator(Constants.UNKNOWN);
            bean.setThreshold(Constants.UNKNOWN);
            bean.setValue(Constants.UNKNOWN);
            break;
        }
    }

    private void _setupMultiCondition(AlertBean bean, HttpServletRequest request) {
        bean.setMultiCondition(true);
        bean.setConditionName
            ( RequestUtils.message(request, "alert.current.list.MultiCondition") );
        bean.setValue
            ( RequestUtils.message(request, "alert.current.list.MultiConditionValue") );
    }
}