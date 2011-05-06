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

package org.hyperic.hq.ui.action.resource.server.inventory;

import java.util.List;
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
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>ResourceController</code> that sets up server inventory portals.
 */
public class ServerInventoryPortalAction
    extends ResourceInventoryPortalAction {

    /**
     * The request scope attribute under which actions store the full
     * <code>List</code> of <code>EmptyValue</code> objects.
     * 
     * temporary list - (will remove - implementing the groups.)
     */
    public static final String EMPTY_VALS_ATTR = "EmptyValues";

    private final Log log = LogFactory.getLog(ServerInventoryPortalAction.class.getName());

    private final Properties keyMethodMap = new Properties();

    @Autowired
    public ServerInventoryPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
        initKeyMethodMap();
    }

    private void initKeyMethodMap() {
        keyMethodMap.setProperty(Constants.MODE_NEW, "newResource");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editResourceGeneral");
        keyMethodMap.setProperty(Constants.MODE_EDIT_CONFIG, "editConfig");
        keyMethodMap.setProperty(Constants.MODE_EDIT_TYPE, "editResourceTypeHost");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewResource");
        keyMethodMap.setProperty(Constants.MODE_ADD_GROUPS, "addGroups");
        keyMethodMap.setProperty(Constants.MODE_CHANGE_OWNER, "changeOwner");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward newResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        Portal portal = Portal.createPortal("resource.server.inventory.NewServerTitle",
            ".resource.server.inventory.NewServer");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editResourceGeneral(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.server.inventory.EditGeneralPropertiesTitle",
            ".resource.server.inventory.EditGeneralProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editResourceTypeHost(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.server.inventory.EditTypeAndHostProperties",
            ".resource.server.inventory.EditTypeAndHostProperties");
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

        Portal portal = Portal.createPortal("resource.server.inventory.AddToGroups",
            ".resource.server.inventory.AddToGroups");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        findAndSetResource(request, response);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());
        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            if (log.isDebugEnabled())
                log.debug("", pne);
        }

        Portal portal = Portal.createPortal("resource.server.inventory.ViewServerTitle",
            ".resource.server.inventory.ViewServer");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return super.viewResource(mapping, form, request, response);
    }

    public ActionForward changeOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal(Constants.CHANGE_OWNER_TITLE, ".resource.server.inventory.changeOwner");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editConfig(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {

        Portal portal = Portal.createPortal("resource.server.inventory.ConfigurationPropertiesTitle",
            ".resource.server.inventory.EditConfigProperties");

        super.editConfig(request, response, portal);

        return null;
    }

    private void findAndSetResource(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        log.trace("getting server [" + aeid + "]");

        ServerValue server = appdefBoss.findServerById(sessionId.intValue(), aeid.getId());

        RequestUtils.setResource(request, server);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, server.getName());

        PageControl pcs = RequestUtils.getPageControl(request, "pss", "pns", "sos", "scs");

        // create and initialize the remove resource groups form
        RemoveResourceForm rmServicesForm = new RemoveResourceForm();
        rmServicesForm.setRid(server.getId());
        rmServicesForm.setType(new Integer(aeid.getType()));

        int pss = RequestUtils.getPageSize(request, "pss");
        rmServicesForm.setPss(new Integer(pss));

        request.setAttribute(Constants.RESOURCE_REMOVE_FORM_ATTR, rmServicesForm);

        List<AppdefResourceValue> serviceList = appdefBoss
            .findServicesByServer(sessionId.intValue(), aeid.getId(), pcs);
        request.setAttribute(Constants.SERVICES_ATTR, serviceList);

        setResource(request, response);

    }
}
