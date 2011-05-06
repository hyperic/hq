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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.GalertBoss;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A dispatcher for the alerts portal.
 * 
 */
public class PortalAction
    extends ResourceController {
    private final Log log = LogFactory.getLog(PortalAction.class.getName());
    private GalertBoss galertBoss;
    private EventsBoss eventsBoss;
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public PortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss, GalertBoss galertBoss,
                        EventsBoss eventsBoss, AlertPermissionManager alertPermissionManager) {
        super(appdefBoss, authzBoss, controlBoss);
        this.galertBoss = galertBoss;
        this.eventsBoss = eventsBoss;
        this.alertPermissionManager = alertPermissionManager;
    }

    protected Properties getKeyMethodMap() {
        log.trace("Building method map ...");
        Properties map = new Properties();
        map.put(Constants.MODE_VIEW, "listAlerts");
        map.put(Constants.MODE_LIST, "listAlerts");
        map.put("ACKNOWLEDGE", "acknowledgeAlert");
        map.put("FIXED", "fixAlert");
        return map;
    }

    private void setTitle(AppdefEntityID aeid, Portal portal, String titleName) throws Exception {
        portal.setName(BizappUtils.replacePlatform(titleName, aeid));
    }

    public ActionForward listAlerts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {
        setResource(request, response);

        super.setNavMapLocation(request, mapping, Constants.ALERT_LOC);
        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());
        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            log.debug(pne);
        }

        GregorianCalendar cal = new GregorianCalendar();
        try {
            Integer year = RequestUtils.getIntParameter(request, "year");
            Integer month = RequestUtils.getIntParameter(request, "month");
            Integer day = RequestUtils.getIntParameter(request, "day");
            cal.set(Calendar.YEAR, year.intValue());
            cal.set(Calendar.MONTH, month.intValue());
            cal.set(Calendar.DAY_OF_MONTH, day.intValue());
            request.setAttribute("date", new Long(cal.getTimeInMillis()));
        } catch (ParameterNotFoundException e) {
            request.setAttribute("date", new Long(System.currentTimeMillis()));
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        Portal portal = Portal.createPortal();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        boolean canTakeAction = false;
        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();
       
            AuthzSubject subject = authzBoss.getCurrentSubject(sessionId);
            // ...check that the user can fix/acknowledge...
            alertPermissionManager.canFixAcknowledgeAlerts(subject, aeid);
            canTakeAction = true;
        } catch(PermissionException e) {
            // ...the user can't fix/acknowledge...
        }
        
        request.setAttribute(Constants.CAN_TAKE_ACTION_ON_ALERT_ATTR, canTakeAction);
        setTitle(aeid, portal, "alerts.alert.platform.AlertList.Title");
        portal.setDialog(false);
        if (aeid.isGroup()) {
            portal.addPortlet(new Portlet(".events.group.alert.list"), 1);

            // Set the total alerts

            int sessionId = RequestUtils.getSessionId(request).intValue();

            request.setAttribute("listSize", new Integer(galertBoss.countAlertLogs(sessionId, aeid.getId(), cal
                .getTimeInMillis(), cal.getTimeInMillis() + Constants.DAYS)));
        } else {
            portal.addPortlet(new Portlet(".events.alert.list"), 1);
        }
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewAlert(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
        // Get alert definition name

        int sessionID = RequestUtils.getSessionId(request).intValue();

        AppdefEntityID aeid = setResource(request, response);
        Integer alertId = new Integer(request.getParameter("a"));

        try {
            Portal portal = Portal.createPortal();

            if (aeid != null && aeid.isGroup()) {

                // properties
                Escalatable av = galertBoss.findEscalatableAlert(sessionID, alertId);

                request.setAttribute(Constants.TITLE_PARAM2_ATTR, av.getDefinition().getName());

                portal.addPortlet(new Portlet(".events.group.alert.view"), 1);
            } else {
                Alert alert = eventsBoss.getAlert(sessionID, alertId);
                AlertDefinition alertDefinition = alert.getAlertDefinition();

                assert (alertDefinition != null);

                request.setAttribute(Constants.TITLE_PARAM2_ATTR, alertDefinition.getName());

                if (aeid == null) {
                    aeid = setResource(request, response, new AppdefEntityID(alertDefinition.getAppdefType(), alertDefinition
                        .getAppdefId()), false);
                }

                portal.addPortlet(new Portlet(".events.alert.view"), 1);
            }

            setTitle(aeid, portal, "alert.current.platform.detail.Title");

            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

        } catch (AlertNotFoundException e) {
            RequestUtils.setError(request, "exception.AlertNotFoundException");
            return listAlerts(mapping, form, request, response);
        }

        return null;
    }

    public ActionForward acknowledgeAlert(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {

        int sessionID = RequestUtils.getSessionId(request).intValue();

        Integer alertId = new Integer(request.getParameter("a"));
        String ackNote = RequestUtils.getStringParameter(request, "ackNote", "");

        long pause = 0;
        try {
            RequestUtils.getStringParameter(request, "pause");
            pause = Long.valueOf(RequestUtils.getStringParameter(request, "pauseTime")).longValue();
        } catch (ParameterNotFoundException e) {
            // Don't need to pause
        }

        // pass pause escalation time
        AppdefEntityID aeid = null;
        boolean ackOk = false;
        try {
            aeid = RequestUtils.getEntityId(request);

            if (aeid.isGroup()) {
                ackOk = eventsBoss.acknowledgeAlert(sessionID, GalertEscalationAlertType.GALERT, alertId, pause,
                    ackNote);
            }
        } catch (ParameterNotFoundException e) {
            // not a problem, this can be null
        }

        if (aeid == null || !aeid.isGroup()) {
            // Classic alerts
            ackOk = eventsBoss.acknowledgeAlert(sessionID, ClassicEscalationAlertType.CLASSIC, alertId, pause, ackNote);
        }

        if (ackOk) {
            RequestUtils.setConfirmation(request, "alert.view.confirm.acknowledged");
        } else {
            RequestUtils.setError(request, "alert.view.error.acknowledged");
        }
        return viewAlert(mapping, form, request, response);
    }

    public ActionForward fixAlert(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {

        int sessionID = RequestUtils.getSessionId(request).intValue();

        Integer alertId = RequestUtils.getIntParameter(request, "a");
        String note = RequestUtils.getStringParameter(request, "fixedNote", "");

        MessageResources res = getResources(request);
        String fixNote = res
            .getMessage("resource.common.alert.fixBy", RequestUtils.getWebUser(request).getName(), note);

        AppdefEntityID aeid = null;
        try {
            aeid = RequestUtils.getEntityId(request);

            if (aeid.isGroup()) {
                eventsBoss.fixAlert(sessionID, GalertEscalationAlertType.GALERT, alertId, fixNote);
            }
        } catch (ParameterNotFoundException e) {
            // not a problem, this can be null
        }

        if (aeid == null || !aeid.isGroup()) {
            // Fix alert the old fashion way
            eventsBoss.fixAlert(sessionID, ClassicEscalationAlertType.CLASSIC, alertId, fixNote);
        }

        RequestUtils.setConfirmation(request, "alert.view.confirm.fixed");
        return viewAlert(mapping, form, request, response);
    }
}
