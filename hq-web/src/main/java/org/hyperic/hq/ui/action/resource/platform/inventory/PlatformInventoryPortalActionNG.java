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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A <code>BaseDispatchAction</code> that sets up platform inventory portals.
 */
@Component("platformInventoryPortalActionNG")
@Scope("prototype")
public class PlatformInventoryPortalActionNG
    extends ResourceInventoryPortalActionNG {

    private final Log log = LogFactory.getLog(PlatformInventoryPortalActionNG.class.getName());

    private final Properties keyMethodMap = new Properties();

    
    public PlatformInventoryPortalActionNG() {
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

    public String newPlatform() throws Exception {
    	setHeaderResources();
        Portal portal = Portal.createPortal("resource.platform.inventory.NewPlatformTitle",
            ".resource.platform.inventory.NewPlatform");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return "newPlatform";
    }

    public String viewPlatform() throws Exception {
        setResource();

        Portal portal = Portal.createPortal("resource.platform.inventory.ViewPlatformTitle",
            ".resource.platform.inventory.ViewPlatform");
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return super.viewResource();
    }

    public String editPlatformGeneralProperties() throws Exception {

        setResource();

        Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
            ".resource.platform.inventory.EditPlatformGeneralProperties");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return "editPlatformGeneralProperties";
    }

    public String editPlatformTypeNetworkProperties() throws Exception {

        setResource();

        Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformTypeNetworkPropertiesTitle",
            ".resource.platform.inventory.EditPlatformTypeNetworkProperties");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return "editPlatformTypeNetworkProperties";
    }

    public String changeOwner() throws Exception {

        setResource();

        Portal portal = Portal.createPortal(Constants.CHANGE_OWNER_TITLE,
            ".resource.platform.inventory.changePlatformOwner");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return "changePlatformOwner";
    }

    public String addPlatformGroups() throws Exception {

        setResource();

        // clean out the return path
        SessionUtils.resetReturnPath(getServletRequest().getSession());
        // set the return path
        try {
        	//TODO take care of that methd in parrent impementation
//            setReturnPath(getServletRequest(), mapping);
        } catch (ParameterNotFoundException pne) {
            if (log.isDebugEnabled())
                log.debug("returnPath error:", pne);
        }

        Portal portal = Portal.createPortal("resource.platform.inventory.AddToGroupsTitle",
            ".resource.platform.inventory.addPlatformGroups");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

        return "addPlatformGroups";
    }

    public String editConfig() throws Exception {
    	setResource();

        Portal portal = Portal.createPortal("resource.platform.inventory.ConfigurationPropertiesTitle",
            ".resource.platform.inventory.EditConfigProperties");

        super.editConfig(portal);

        return "editConfigProperties";
    }

}
