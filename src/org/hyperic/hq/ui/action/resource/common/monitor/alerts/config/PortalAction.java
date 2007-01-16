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

import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.action.resource.ResourceController;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
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

    protected static Properties keyMethodMap = new Properties();
    static {
        keyMethodMap.setProperty(Constants.MODE_LIST, "listDefinitions");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newDefinition");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "listDefinitions");
        keyMethodMap.setProperty("viewDefinition", "viewEscalation");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    /**
     * We override this in case the resource has been deleted
     * ... simply ignore that fact.
     */
    protected AppdefEntityID setResource(HttpServletRequest request)
        throws Exception {
        try {
            return super.setResource(request);
        } catch (ParameterNotFoundException e) {
            log.warn("No resource found.");
        }
        return null;
    }

    private void setTitle(HttpServletRequest request, Portal portal,
                          String titleName)
        throws Exception {

        AppdefEntityID aeid;
        try {
            aeid = RequestUtils.getEntityTypeId(request);
        } catch (ParameterNotFoundException e) {
            aeid = RequestUtils.getEntityId(request);
        }
        
        titleName = BizappUtils.replacePlatform(titleName, aeid);
        portal.setName(titleName);

        // if there's an alert definition available, set our second
        // title parameter to its name
        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();
            ServletContext ctx = getServlet().getServletContext();
            EventsBoss eb = ContextUtils.getEventsBoss(ctx);
            AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition
                (request, sessionId, eb);
            request.setAttribute( Constants.TITLE_PARAM2_ATTR, adv.getName() );
        } catch (ParameterNotFoundException e) {
            // it's okay
            log.trace("couldn't find alert definition: " + e.getMessage());
        }
    }

    public ActionForward newDefinition(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alert.config.platform.edit.NewAlertDef.Title");
        portal.addPortlet(new Portlet(".events.config.new"),1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editProperties(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alert.config.platform.edit.page.Title");
        portal.addPortlet(new Portlet(".events.config.edit.properties"),1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editConditions(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alert.config.platform.edit.condition.Title");
        portal.addPortlet(new Portlet(".events.config.edit.conditions"),1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editControlAction(ActionMapping mapping,
                                           ActionForm form,
                                           HttpServletRequest request,
                                           HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alerts.config.platform.EditControlAction.Title");
        portal.addPortlet(new Portlet(".events.config.edit.controlaction"),1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editSyslogAction(ActionMapping mapping,
                                          ActionForm form,
                                          HttpServletRequest request,
                                          HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alerts.config.platform.EditSyslogAction.Title");
        portal.addPortlet(new Portlet(".events.config.edit.syslogaction"),1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewOthers(ActionMapping mapping,
                                    ActionForm form,
                                    HttpServletRequest request,
                                    HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alert.config.platform.props.ViewDef.email.Title");
        portal.addPortlet(new Portlet(".events.config.view.others"),1);
        // JW - this shouldn't be a dialog ... portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewUsers(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alert.config.platform.props.ViewDef.users.Title");
        portal.addPortlet(new Portlet(".events.config.view.users"),1);
        // JW - this shouldn't be a dialog ... portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewEscalation(ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest request,
                                        HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal,
                 "alert.config.platform.props.ViewDef.escalation.Title");
        portal.addPortlet(new Portlet(".events.config.view.escalation"),1);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        
        return null;
    }

    public ActionForward monitorConfigureAlerts(ActionMapping mapping,
                                                ActionForm form,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        portal.addPortlet(new Portlet(".events.config.list"),1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward listDefinitions(ActionMapping mapping,
                                         ActionForm form,
                                         HttpServletRequest request,
                                         HttpServletResponse response)
        throws Exception
    {
        AppdefEntityID aeid = setResource(request);

        setNavMapLocation(request, mapping, Constants.ALERT_CONFIG_LOC);
                                  
        // clean out the return path 
        SessionUtils.resetReturnPath(request.getSession());
        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            log.debug(pne);
        }

        Portal portal = Portal.createPortal();
        setTitle(request,portal,"alerts.config.platform.DefinitionList.Title");
        portal.setDialog(false);

        try {
            RequestUtils.getStringParameter(request,
                                            Constants.APPDEF_RES_TYPE_ID);
            portal.addPortlet(new Portlet(".admin.alerts.List"), 1);
        } catch (ParameterNotFoundException e) {
            if (aeid != null && aeid.isGroup()) {
                portal.addPortlet(new Portlet(".events.group.config.list"), 1);
            }
            else {
                portal.addPortlet(new Portlet(".events.config.list"), 1);
            }
        }
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;

    }

    public ActionForward addUsers(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,
                    "alerts.config.platform.AssignUsersToAlertDefinition.Title");
        portal.addPortlet(new Portlet(".events.config.addusers"),1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward addOthers(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception
    {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request,portal,
                "alerts.config.platform.AssignOthersToAlertDefinition.Title");
        portal.addPortlet(new Portlet(".events.config.addothers"),1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /** 
     * This sets the return path for a ResourceAction by appending
     * the type and resource id to the forward url.
     * 
     * @param request The current controller's request.
     * @param mapping The current controller's mapping that contains the input.
     *
     * @exception ParameterNotFoundException if the type or id are not found
     * @exception ServletException If there is not input defined for this form
     */
    protected void setReturnPath(HttpServletRequest request,
                                 ActionMapping mapping) 
        throws Exception{
        HashMap parms = new HashMap();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        parms.put(Constants.RESOURCE_PARAM, aeid.getId());
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
                  new Integer(aeid.getType()));

        try {
            Integer ad = RequestUtils.getIntParameter(request, Constants.ALERT_DEFINITION_PARAM);
            parms.put(Constants.ALERT_DEFINITION_PARAM, ad);

            AppdefEntityTypeID ctype = RequestUtils.getChildResourceTypeId(request);
            parms.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
            log.trace("couldn't find parameter: " + pnfe.getMessage());
        }

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        parms.put(Constants.MODE_PARAM, mode);
        
        String returnPath = ActionUtils.findReturnPath(mapping, parms);
        SessionUtils.setReturnPath(request.getSession(), returnPath); 
    }
}

// EOF
