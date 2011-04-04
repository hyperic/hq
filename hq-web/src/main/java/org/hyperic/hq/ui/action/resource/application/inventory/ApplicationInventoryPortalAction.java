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

package org.hyperic.hq.ui.action.resource.application.inventory;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The portal manager class for the tiles in handled in this package.
 */
public class ApplicationInventoryPortalAction
    extends ResourceInventoryPortalAction {
    public static final String EMPTY_VALS_ATTR = "EmptyValues";

    private final Log log = LogFactory.getLog(ApplicationInventoryPortalAction.class.getName());

    private final Properties keyMethodMap = new Properties();

    @Autowired
    public ApplicationInventoryPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
        initKeyMethodMap();
    }

    private void initKeyMethodMap() {
        keyMethodMap.setProperty(Constants.MODE_NEW, "newResource");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewResource");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editGeneralProperties");
        keyMethodMap.setProperty(Constants.MODE_CHANGE_OWNER, "changeOwner");
        keyMethodMap.setProperty(Constants.MODE_EDIT_RESOURCE, "editApplicationProperties");
        keyMethodMap.setProperty(Constants.MODE_ADD_GROUPS, "addApplicationGroups");
        keyMethodMap.setProperty(Constants.MODE_ADD_SERVICES, "addApplicationServices");
        // XXX
        keyMethodMap.setProperty("listServiceDependencies", "listServiceDependencies");
        keyMethodMap.setProperty("addDependencies", "addDependencies");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward newResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        log.debug("newResource(...) creating new application");
        Portal portal = Portal.createPortal("resource.application.inventory.NewApplicationTitle",
            ".resource.application.inventory.NewApplication");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        setResource(request, response);

        Portal portal = Portal.createPortal("resource.application.inventory.ViewApplicationTitle",
            ".resource.application.inventory.ViewApplication");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return super.viewResource(mapping, form, request, response);
    }

    public ActionForward editGeneralProperties(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal("resource.application.inventory.EditGeneralPropertiesTitle",
            ".resource.application.inventory.EditGeneralProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward editApplicationProperties(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal("resource.application.inventory.EditApplicationPropertiesTitle",
            ".resource.application.inventory.EditApplicationProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward changeOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal
            .createPortal(Constants.CHANGE_OWNER_TITLE, ".resource.application.inventory.changeOwner");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addApplicationGroups(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        setResource(request, response);

        Portal portal = Portal.createPortal("resource.application.inventory.AddToGroupsTitle",
            ".resource.application.inventory.addApplicationGroups");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addApplicationServices(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {
        setResource(request, response);
        Portal portal = Portal.createPortal("common.title.Edit",
            ".resource.application.inventory.addApplicationServices");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward listServiceDependencies(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {
        setResource(request, response);
        // XXX put the right title in or refactor to use a common title...
        Portal portal = Portal.createPortal("resource.application.inventory.AddDependenciesTitle",
            ".resource.application.inventory.listServiceDependencies");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addDependencies(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        setResource(request, response);
        // XXX put the right title in or refactor to use a common title...
        Portal portal = Portal.createPortal("resource.application.inventory.AddDependenciesPageTitle",
            ".resource.application.inventory.addServiceDependencies");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }
}
