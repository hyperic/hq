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
import java.util.Collection;
import java.util.GregorianCalendar;

import javax.print.attribute.standard.DateTimeAtCompleted;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.AlertBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.units.FormatSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * List all alerts for this entity
 * 
 */
public class ListAlertAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ListAlertAction.class.getName());
    private EventsBoss eventsBoss;
    private MeasurementBoss measurementBoss;
    private AuthzBoss authzBoss;
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public ListAlertAction(EventsBoss eventsBoss, MeasurementBoss measurementBoss, AuthzBoss authzBoss, 
                           AlertPermissionManager alertPermissionManager) {
        super();
        this.eventsBoss = eventsBoss;
        this.measurementBoss = measurementBoss;
        this.authzBoss = authzBoss;
        this.alertPermissionManager = alertPermissionManager;
    }

    /**
     * Create a list of AlertBean objects based on the AlertValue objects for
     * this resource.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        int sessionId = RequestUtils.getSessionId(request).intValue();
        AppdefEntityID appEntId = RequestUtils.getEntityId(request);

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

        PageList<Alert> alerts;

        try {
            final DateTime begin = new DateTime(cal);
            final DateTime end = addAlmostOneDay(begin);
            alerts = eventsBoss.findAlerts(sessionId, appEntId, begin.getMillis(), end.getMillis(), pc);
        } catch (PermissionException e) {
            // user is not allowed to see/manage alerts.
            // return empty list for now
            alerts = new PageList<Alert>();
        }

        PageList<AlertBean> uiBeans = new PageList<AlertBean>();
        
        AuthzSubject subject = authzBoss.getCurrentSubject(sessionId);
        boolean canTakeAction = false;
        try {
            // ...check that the user can fix/acknowledge...
            alertPermissionManager.canFixAcknowledgeAlerts(subject, appEntId);
            canTakeAction = true;
        } catch(PermissionException e) {
            // ...the user can't fix/acknowledge...
        }
       
        for (Alert alert : alerts) {

            AlertDefinition alertDefinition = alert.getAlertDefinition();
            AlertBean bean = new AlertBean(alert.getId(), alert.getCtime(), alertDefinition.getId(), alertDefinition
                .getName(), alertDefinition.getPriority(), appEntId.getId(), new Integer(appEntId.getType()), alert
                .isFixed(), alert.isAckable(), canTakeAction);
            Escalation escalation = alertDefinition.getEscalation();

            if (escalation != null && escalation.isPauseAllowed()) {
                bean.setMaxPauseTime(escalation.getMaxPauseTime());
            }

            // Determine whether or not this alert definition is viewable
            bean.setViewable(!alertDefinition.isDeleted() && alertDefinition.getResource() != null &&
                             !alertDefinition.getResource().isInAsyncDeleteState());

            Collection<AlertConditionLog> conditionLogs = alert.getConditionLog();

            if (conditionLogs.size() > 1) {
                setupMultiCondition(bean, request);
            } else if (conditionLogs.size() == 1) {
                AlertConditionLog conditionLog = (AlertConditionLog) conditionLogs.iterator().next();
                AlertConditionValue condition = conditionLog.getCondition().getAlertConditionValue();

                setupCondition(bean, condition, conditionLog.getValue(), request,

                sessionId);
            } else {
                // fall back to alert definition conditions: PR 6992
                Collection<AlertCondition> conditions = alertDefinition.getConditions();

                if (conditions.size() > 1) {
                    setupMultiCondition(bean, request);
                } else if (conditions.size() == 1) {
                    AlertCondition condition = conditions.iterator().next();

                    setupCondition(bean, condition.getAlertConditionValue(), null, request,

                    sessionId);
                } else {
                    // *serious* trouble
                    log.error("No condition logs for alert: " + alert.getId());

                    bean.setMultiCondition(true);
                    bean.setConditionName(Constants.UNKNOWN);
                    bean.setValue(Constants.UNKNOWN);
                }
            }

            uiBeans.add(bean);
        }

        context.putAttribute(Constants.RESOURCE_ATTR, RequestUtils.getResource(request));
        context.putAttribute(Constants.RESOURCE_OWNER_ATTR, request.getAttribute(Constants.RESOURCE_OWNER_ATTR));
        context.putAttribute(Constants.RESOURCE_MODIFIER_ATTR, request.getAttribute(Constants.RESOURCE_MODIFIER_ATTR));
        request.setAttribute(Constants.ALERTS_ATTR, uiBeans);
        request.setAttribute(Constants.LIST_SIZE_ATTR, new Integer(alerts.getTotalSize()));

        return null;
    }

    /**
     * Take the existing DateTime and add 23:59:59.
     * @param begin
     * @return
     */
	public DateTime addAlmostOneDay(final DateTime begin) {
		return begin.plusDays(1).minusSeconds(1);
	}

    private void setupCondition(AlertBean bean, AlertConditionValue cond, String value, HttpServletRequest request,
                                int sessionId) throws SessionTimeoutException, SessionNotFoundException,
        MeasurementNotFoundException, RemoteException {
        bean.setConditionName(cond.getName());

        switch (cond.getType()) {
            case EventConstants.TYPE_CONTROL:
                bean.setComparator("");
                bean.setThreshold(cond.getOption());
                bean.setValue(RequestUtils.message(request, "alert.current.list.ControlActualValue"));
                break;

            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
            case EventConstants.TYPE_CHANGE:
                FormatSpecifics precMax = new FormatSpecifics();
                precMax.setPrecision(FormatSpecifics.PRECISION_MAX);

                Measurement m = null;
                if (cond.getType() == EventConstants.TYPE_THRESHOLD) {
                    m = measurementBoss.getMeasurement(sessionId, new Integer(cond.getMeasurementId()));
                    bean.setComparator(cond.getComparator());
                    FormattedNumber th = UnitsConvert.convert(cond.getThreshold(), m.getTemplate().getUnits(), precMax);
                    bean.setThreshold(th.toString());
                } else if (cond.getType() == EventConstants.TYPE_BASELINE) {
                    bean.setComparator(cond.getComparator());
                    bean.setThreshold(cond.getThreshold() + "% of " + cond.getOption());
                } else if (cond.getType() == EventConstants.TYPE_CHANGE) {
                    bean.setComparator("");
                    bean.setThreshold(RequestUtils.message(request, "alert.current.list.ValueChanged"));
                }

                // convert() can't handle Double.NaN -- just display ?? for the
                // value
                if (value == null || value.length() == 0) {
                    bean.setValue(Constants.UNKNOWN);
                } else {
                    bean.setValue(value);
                }
                break;

            case EventConstants.TYPE_CUST_PROP:
                bean.setComparator("");
                bean.setThreshold(RequestUtils.message(request, "alert.current.list.ValueChanged"));

                if (value != null)
                    bean.setValue(value);
                else
                    bean.setValue(Constants.UNKNOWN);
                break;

            case EventConstants.TYPE_LOG:
                bean.setConditionName(RequestUtils.message(request, "alert.config.props.CB.LogLevel"));
                bean.setComparator("=");
                bean.setThreshold(ResourceLogEvent.getLevelString(Integer.parseInt(cond.getName())));
                bean.setValue(value);
                break;
            case EventConstants.TYPE_CFG_CHG:
                bean.setComparator("");
                bean.setThreshold(RequestUtils.message(request, "alert.current.list.ValueChanged"));
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

    private void setupMultiCondition(AlertBean bean, HttpServletRequest request) {
        bean.setMultiCondition(true);
        bean.setConditionName(RequestUtils.message(request, "alert.current.list.MultiCondition"));
        bean.setValue(RequestUtils.message(request, "alert.current.list.MultiConditionValue"));
    }
}