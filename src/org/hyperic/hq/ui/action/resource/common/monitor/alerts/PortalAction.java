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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

/**
 * A dispatcher for the alerts portal.
 *
 */
public class PortalAction extends ResourceController {
    protected static Log log =
        LogFactory.getLog(PortalAction.class.getName());

    protected Properties getKeyMethodMap() {
        log.trace("Building method map ...");
        Properties map = new Properties();
        map.put(Constants.MODE_VIEW,  "listAlerts");
        map.put(Constants.MODE_LIST,  "listAlerts");
        map.put("ACKNOWLEDGE", "acknowledgeAlert");
        map.put("FIXED", "fixAlert");
        return map;
    }

    private void setTitle(AppdefEntityID aeid, Portal portal, String titleName) 
        throws Exception  
    {
        portal.setName(BizappUtils.replacePlatform(titleName, aeid));
    }

    public ActionForward listAlerts(ActionMapping mapping,
                                    ActionForm form,
                                    HttpServletRequest request,
                                    HttpServletResponse response)
        throws Exception {
        setResource(request);
        
        super.setNavMapLocation(request, mapping, Constants.ALERT_LOC); 
        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());
        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            log.debug(pne);
        }
        
        Portal portal = Portal.createPortal();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        setTitle(aeid, portal, "alerts.alert.platform.AlertList.Title");
        portal.setDialog(false);
        if (aeid.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            portal.addPortlet(new Portlet(".events.alert.list"), 1);
        } else {
            portal.addPortlet(new Portlet(".events.group.alert.list"), 1);
        }
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        try {
            GregorianCalendar cal = new GregorianCalendar();
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

        return null;
    }

    public ActionForward viewAlert(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        // Get alert definition name
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        Integer alertId = new Integer( request.getParameter("a") );
        try {
            AlertValue av = eb.getAlert(sessionID, alertId);
            AlertDefinitionValue adv =
                eb.getAlertDefinition( sessionID, av.getAlertDefId() );
            request.setAttribute(Constants.TITLE_PARAM2_ATTR, adv.getName());
            
            AppdefEntityID aeid =
                new AppdefEntityID(adv.getAppdefType(), adv.getAppdefId());
            setResource(request, aeid, false);

            Portal portal = Portal.createPortal();
            setTitle(aeid, portal, "alert.current.platform.detail.Title");
            portal.addPortlet(new Portlet(".events.alert.view"), 1);

            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

        } catch (AlertNotFoundException e) {
            RequestUtils.setError(request, "exception.AlertNotFoundException");
            return listAlerts(mapping, form, request, response);
        }

        return null;
    }

    public ActionForward acknowledgeAlert(ActionMapping mapping,
                                          ActionForm form,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        Integer alertId = new Integer( request.getParameter("a") );
        
        long pause = 0;
        try {
            RequestUtils.getStringParameter(request, "pause");
            pause = RequestUtils.getIntParameter(request, 
                                                 "pauseTime").longValue();
        } catch(ParameterNotFoundException e) {
            // Don't need to pause
        }
        // pass pause escalation time
        // XXX:  Right now this only works with classic alerts
        eb.acknowledgeAlert(sessionID, 
                            ClassicEscalationAlertType.CLASSIC,
                            alertId, pause);
        
        return viewAlert(mapping, form, request, response);
    }

    public ActionForward fixAlert(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception 
    {
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        Integer alertId = new Integer( request.getParameter("a") );

        // XXX:  This is staticly specified as classic for now
        eb.fixAlert(sessionID, ClassicEscalationAlertType.CLASSIC, alertId); 

        return viewAlert(mapping, form, request, response);
    }
}
