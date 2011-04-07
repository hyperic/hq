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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;
import org.hyperic.hq.ui.action.resource.server.inventory.ServerInventoryPortalAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>ResourceControllerAction</code> that sets up server inventory
 * portals.
 */
public class ServiceInventoryPortalAction
    extends ResourceInventoryPortalAction {

    /**
     * The request scope attribute under which actions store the full
     * <code>List</code> of <code>EmptyValue</code> objects.
     * 
     * temporary list - (will remove - implementing the groups.)
     */
    public static final String EMPTY_VALS_ATTR = "EmptyValues";

    private final Log log = LogFactory.getLog(ServerInventoryPortalAction.class.getName());

    @Autowired
    public ServiceInventoryPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
    }

    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_NEW, "newResource");
        map.setProperty(Constants.MODE_EDIT, "editResourceGeneral");
        map.setProperty(Constants.MODE_EDIT_CONFIG, "editConfig");
        map.setProperty(Constants.MODE_VIEW, "viewResource");
        map.setProperty(Constants.MODE_ADD_GROUPS, "addGroups");
        map.setProperty(Constants.MODE_CHANGE_OWNER, "changeOwner");
        return map;
    }

    public ActionForward newResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.service.inventory.NewServiceTitle",
            ".resource.service.inventory.NewService");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editResourceGeneral(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.service.inventory.EditGeneralPropertiesTitle",
            ".resource.service.inventory.EditGeneralProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        // Check to see if it's a platform service type
        AppdefResourceValue service = RequestUtils.getResource(request);

        Integer sessionId = RequestUtils.getSessionId(request);

        appdefBoss.findServerByService(sessionId.intValue(), service.getId());

        return null;
    }

    public ActionForward viewResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        findAndSetResource(request, response);

        Portal portal = Portal.createPortal("resource.service.inventory.ViewServiceTitle",
            ".resource.service.inventory.ViewService");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return super.viewResource(mapping, form, request, response);
    }

    public ActionForward changeOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal(Constants.CHANGE_OWNER_TITLE, ".resource.service.inventory.changeOwner");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addGroups(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {

        setResource(request, response);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());
        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            if (log.isDebugEnabled())
                log.debug("returnPath error:", pne);
        }

        Portal portal = Portal.createPortal("resource.service.inventory.AddToGroups",
            ".resource.service.inventory.AddToGroups");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editConfig(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {

        Portal portal = Portal.createPortal("resource.service.inventory.ConfigurationPropertiesTitle",
            ".resource.service.inventory.EditConfigProperties");

        super.editConfig(request, response, portal);

        return null;
    }

    private void findAndSetResource(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        Integer serviceId = aeid.getId();

        log.trace("getting service [" + serviceId + "]");

        ServiceValue service = appdefBoss.findServiceById(RequestUtils.getSessionId(request).intValue(), serviceId);
        // XXX: if server == null, throw ServerNotFoundException
        RequestUtils.setResource(request, service);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, service.getName());

        log.trace("getting owner for service");
        AuthzSubject owner = authzBoss.findSubjectByNameNoAuthz(sessionId, service.getOwner());
        request.setAttribute(Constants.RESOURCE_OWNER_ATTR, owner);

        PageControl pc = RequestUtils.getPageControl(request, "pss", "pns", "sos", "scs");
        PageList<ApplicationValue> appValues = appdefBoss.findApplications(sessionId.intValue(), aeid, pc);
        request.setAttribute(Constants.APPLICATIONS_ATTR, appValues);

        setResource(request, response);
    }

}
