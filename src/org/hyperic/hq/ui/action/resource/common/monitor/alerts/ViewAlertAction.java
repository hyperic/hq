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

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.AlertConditionBean;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.NumberUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.units.FormatSpecifics;
import org.hyperic.util.units.FormattedNumber;
import org.json.JSONObject;

/**
 * View an alert.
 *
 */
public class ViewAlertAction extends TilesAction {
    private Log log = LogFactory.getLog(ViewAlertAction.class.getName());

    /**
     * Retrieve this data and store it in request attributes.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        // pass-through the alertId
        Integer alertId = RequestUtils.getIntParameter(request, "a");
        
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);

        // properties
        AlertValue av = eb.getAlert(sessionID, alertId);
        request.setAttribute("alert", av);
        AlertDefinitionValue adv =
            eb.getAlertDefinition( sessionID, av.getAlertDefId() );
        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, adv);
        
        if (adv.getEscalationId() != null) {
            Escalation escalation =
                eb.findEscalationById(sessionID, adv.getEscalationId());
            request.setAttribute("escalation", escalation);
            
            JSONObject escJson = Escalation.getJSON(escalation);
            request.setAttribute("escalationJSON", escJson.toString());
        }            

        // conditions
        AlertConditionLogValue[] condLogs = av.getConditionLogs();
        AlertConditionValue[] conds = new AlertConditionValue[condLogs.length];
        for (int i = 0; i < condLogs.length; i++) {
            conds[i] = condLogs[i].getCondition();
        }

        List alertDefConditions = AlertDefUtil.getAlertConditionBeanList(
                sessionID, request, mb, conds,
                EventConstants.TYPE_ALERT_DEF_ID.equals(adv.getParentId()));

        for (int i = 0; i < alertDefConditions.size(); i++) {
            AlertConditionBean ab =
                (AlertConditionBean) alertDefConditions.get(i);
            switch ( conds[i].getType() ) {
            case EventConstants.TYPE_CONTROL:
                ab.setActualValue
                    ( RequestUtils.message(request, "alert.current.list.ControlActualValue") );
                break;

            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
            case EventConstants.TYPE_CHANGE:
                // Let's actually format the value
                double value = NumberUtil.stringAsNumber(
							condLogs[i].getValue()).doubleValue();
                if ( Double.isNaN(value) ) {
                	ab.setActualValue(Constants.UNKNOWN);
                }
                else {
                    // format threshold and value
                    Integer mid =
                        new Integer(condLogs[i].getCondition()
                                               .getMeasurementId());
                    Measurement m = mb.getMeasurement(sessionID, mid);
                    FormatSpecifics precMax = new FormatSpecifics();
                    precMax.setPrecision(FormatSpecifics.PRECISION_MAX);

                    FormattedNumber val =
                        UnitsConvert.convert(value,
                                             m.getTemplate().getUnits());
                    ab.setActualValue( val.toString() );
                }
                break;

            case EventConstants.TYPE_CUST_PROP:
            case EventConstants.TYPE_LOG:
            case EventConstants.TYPE_CFG_CHG:
                ab.setActualValue(condLogs[i].getValue());
                break;

            default:
                ab.setActualValue(Constants.UNKNOWN);
                break;
            }
        }

        request.setAttribute("alertDefConditions", alertDefConditions);

        // if alert is fixed, then there should be a fixed log
        if (av.isFixed()) {
            AlertActionLog[] logs = av.getActionLogs();
            for (int i = logs.length - 1; i >= 0; i--) {
                if (logs[i].getAction() == null) {
                    request.setAttribute("fixedNote", logs[i].getDetail());
                    break;
                }
            }
        }
        else {
            // See if there might be a previous fixed log
            String fixedNote = eb.getLastFix(sessionID, adv.getId());
            if (fixedNote != null) {
                request.setAttribute("fixedNote", fixedNote);
            }
        }
        
        // enablement
        AlertDefUtil.setEnablementRequestAttributes(request, adv);

        // Get the list of users
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        PageList availableUsers =
            authzBoss.getAllSubjects(new Integer(sessionID), null,
                                     PageControl.PAGE_ALL);
        request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

        return null;
    }
}
