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

package org.hyperic.hq.ui.action.resource.platform.inventory;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * A <code>BaseDispatchAction</code> that sets up platform inventory portals.
 */
public class PlatformInventoryPortalAction
    extends ResourceInventoryPortalAction {

    private final Log log = LogFactory.getLog(PlatformInventoryPortalAction.class.getName());

    private final Properties keyMethodMap = new Properties();

    @Autowired
    public PlatformInventoryPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
        initKeyMethodMap();
    }

    private void initKeyMethodMap() {
        keyMethodMap.setProperty(Constants.MODE_NEW, "newPlatform");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewPlatform");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editPlatformGeneralProperties");
        keyMethodMap.setProperty(Constants.MODE_EDIT_TYPE, "editPlatformTypeNetworkProperties");
        keyMethodMap.setProperty(Constants.MODE_EDIT_CONFIG, "editConfig");
        keyMethodMap.setProperty(Constants.MODE_CHANGE_OWNER, "changePlatformOwner");
        keyMethodMap.setProperty(Constants.MODE_ADD_GROUPS, "addPlatformGroups");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward newPlatform(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("resource.platform.inventory.NewPlatformTitle",
            ".resource.platform.inventory.NewPlatform");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewPlatform(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        setResource(request, response);

        Portal portal = Portal.createPortal("resource.platform.inventory.ViewPlatformTitle",
            ".resource.platform.inventory.ViewPlatform");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return super.viewResource(mapping, form, request, response);
    }

    public ActionForward editPlatformGeneralProperties(ActionMapping mapping, ActionForm form,
                                                       HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
            ".resource.platform.inventory.EditPlatformGeneralProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editPlatformTypeNetworkProperties(ActionMapping mapping, ActionForm form,
                                                           HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformTypeNetworkPropertiesTitle",
            ".resource.platform.inventory.EditPlatformTypeNetworkProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward changePlatformOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        setResource(request, response);

        Portal portal = Portal.createPortal(Constants.CHANGE_OWNER_TITLE,
            ".resource.platform.inventory.changePlatformOwner");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addPlatformGroups(ActionMapping mapping, ActionForm form, HttpServletRequest request,
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

        Portal portal = Portal.createPortal("resource.platform.inventory.AddToGroupsTitle",
            ".resource.platform.inventory.addPlatformGroups");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editConfig(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {

        Portal portal = Portal.createPortal("resource.platform.inventory.ConfigurationPropertiesTitle",
            ".resource.platform.inventory.EditConfigProperties");

        super.editConfig(request, response, portal);

        return null;
    }

}
