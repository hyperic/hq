/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008, Hyperic, Inc.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.AlertConditionBean;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * View an alert.
 * 
 */
public class ViewAlertAction
    extends TilesAction {
    private EventsBoss eventsBoss;
    private MeasurementBoss measurementBoss;
    private AuthzBoss authzBoss;
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public ViewAlertAction(EventsBoss eventsBoss, MeasurementBoss measurementBoss, AuthzBoss authzBoss, AlertPermissionManager alertPermissionManager) {
        super();
        this.eventsBoss = eventsBoss;
        this.measurementBoss = measurementBoss;
        this.authzBoss = authzBoss;
        this.alertPermissionManager = alertPermissionManager;
    }

    /**
     * Retrieve this data and store it in request attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        // pass-through the alertId
        Integer alertId = RequestUtils.getIntParameter(request, "a");

        int sessionID = RequestUtils.getSessionId(request).intValue();

        Alert alert = eventsBoss.getAlert(sessionID, alertId);

        request.setAttribute("alert", alert);

        AlertDefinition alertDefinition = alert.getAlertDefinition();

        assert (alertDefinition != null);

        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, alertDefinition.getAlertDefinitionValue());

        Escalation escalation = alertDefinition.getEscalation();

        if (escalation != null) {
            request.setAttribute("escalation", escalation);

            JSONObject escJson = Escalation.getJSON(escalation);

            request.setAttribute("escalationJSON", escJson.toString());
        }

        // conditions
        Collection<AlertConditionLog> conditionLogs = alert.getConditionLog();
        AlertConditionValue[] conditionValues = new AlertConditionValue[conditionLogs.size()];
        int index = 0;

        for (Iterator<AlertConditionLog> i = conditionLogs.iterator(); i.hasNext(); index++) {
            AlertConditionLog conditionLog = i.next();

            conditionValues[index] = conditionLog.getCondition().getAlertConditionValue();
        }

        boolean template = false;
        AlertDefinition parentAlertDefinition = alertDefinition.getParent();

        if (parentAlertDefinition != null) {
            template = EventConstants.TYPE_ALERT_DEF_ID.equals(parentAlertDefinition.getId());
        }

        List<AlertConditionBean> conditionBeans = AlertDefUtil.getAlertConditionBeanList(sessionID, request,
            measurementBoss, conditionValues, template);

        index = 0;

        AlertConditionLog[] conditionLogsArr = conditionLogs.toArray(new AlertConditionLog[conditionLogs.size()]);

        for (Iterator<AlertConditionBean> i = conditionBeans.iterator(); i.hasNext(); index++) {
            AlertConditionBean conditionBean = i.next();
            AlertConditionLogValue conditionLogValue = conditionLogsArr[index].getAlertConditionLogValue();
            final String logVal = conditionLogValue.getValue();

            switch (conditionValues[index].getType()) {
                case EventConstants.TYPE_CONTROL:
                    conditionBean
                        .setActualValue(RequestUtils.message(request, "alert.current.list.ControlActualValue"));
                    break;

                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                case EventConstants.TYPE_CHANGE:
                case EventConstants.TYPE_CUST_PROP:
                case EventConstants.TYPE_LOG:
                case EventConstants.TYPE_CFG_CHG:
                    conditionBean.setActualValue(logVal);
                    break;
                default:
                    conditionBean.setActualValue(Constants.UNKNOWN);
            }
        }

        request.setAttribute("alertDefConditions", conditionBeans);

        // if alert is fixed, then there should be a fixed log
        if (alert.isFixed()) {
            Collection<AlertActionLog> actionLogs = alert.getActionLog();

            // Reverse the order, the most recent log is first
            Collections.reverse(conditionBeans);

            for (Iterator<AlertActionLog> i = actionLogs.iterator(); i.hasNext();) {
                AlertActionLog actionLog = i.next();

                if (actionLog.getAction() == null) {
                    request.setAttribute("fixedNote", actionLog.getDetail());

                    break;
                }
            }
        } else {
            // See if there might be a previous fixed log
            String fixedNote = eventsBoss.getLastFix(sessionID, alertDefinition.getId());

            if (fixedNote != null) {
                request.setAttribute("fixedNote", fixedNote);
            }
        }

        // enablement
        AlertDefUtil.setEnablementRequestAttributes(request, alertDefinition.getAlertDefinitionValue());

        // Get the list of users

        PageList<AuthzSubjectValue> availableUsers = authzBoss.getAllSubjects(new Integer(sessionID), null,
            PageControl.PAGE_ALL);
        request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);
        
        // ...check to see if user has the ability to fix/acknowledge...
        try {
            AuthzSubject subject = authzBoss.getCurrentSubject(sessionID);
            alertPermissionManager.canFixAcknowledgeAlerts(subject, alertDefinition.getAppdefEntityId());
            request.setAttribute(Constants.CAN_TAKE_ACTION_ON_ALERT_ATTR, true);
        } catch(PermissionException e) {
            // We can view it, but can't take action on it
            request.setAttribute(Constants.CAN_TAKE_ACTION_ON_ALERT_ATTR, false);
        }

        return null;
    }
}
