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
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("applicationInventoryPortalActionNG")   
@Scope("prototype")
public class ApplicationInventoryPortalActionNG extends
		ResourceInventoryPortalActionNG {
	
	
    public static final String EMPTY_VALS_ATTR = "EmptyValues";

    private final Log log = LogFactory.getLog(ApplicationInventoryPortalActionNG.class);

    private final Properties keyMethodMap = new Properties();
    
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
    
	public String newResource() throws Exception {

		setResource();
        log.debug("newResource(...) creating new application");
        Portal portal = Portal.createPortal("resource.application.inventory.NewApplicationTitle",
            ".resource.application.inventory.NewApplication");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

		return "newResource";
	}

	public String editResourceGeneral() throws Exception {

		setResource();
        Portal portal = Portal.createPortal("resource.application.inventory.EditGeneralPropertiesTitle",
                ".resource.application.inventory.EditGeneralProperties");
            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editResourceGeneral";
	}

	public String viewResource() throws Exception {
		
		setResource();

        Portal portal = Portal.createPortal("resource.application.inventory.ViewApplicationTitle",
                ".resource.application.inventory.ViewApplication");
            request.setAttribute(Constants.PORTAL_KEY, portal);

		return super.viewResource();
	}

	public String changeOwner() throws Exception {

		setResource();
		
        Portal portal = Portal
                .createPortal(Constants.CHANGE_OWNER_TITLE, ".resource.application.inventory.changeOwner");
            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

		return "changeOwner";
	}

	public String addGroups() throws Exception {

		setResource();
		
        Portal portal = Portal.createPortal("resource.application.inventory.AddToGroupsTitle",
                ".resource.application.inventory.addApplicationGroups");
            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

		return "addGroups";
	}
	
	public String addServices() throws Exception {

		setResource();
		
        Portal portal = Portal.createPortal("common.title.Edit",
                ".resource.application.inventory.addApplicationServices");
            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);

		return "addServices";
	}

	public String editApplicationProperties() throws Exception {

        Portal portal = Portal.createPortal("resource.application.inventory.EditApplicationPropertiesTitle",
                ".resource.application.inventory.EditApplicationProperties");
            portal.setDialog(true);
            request.setAttribute(Constants.PORTAL_KEY, portal);
		return "editApplicationProperties";
	}
	
	public String listServiceDependencies() throws Exception {
		setResource();
		// XXX put the right title in or refactor to use a common title...
		Portal portal = Portal.createPortal(
				"resource.application.inventory.AddDependenciesTitle",
				".resource.application.inventory.listServiceDependencies");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
		this.removeValueInSession(Constants.PENDING_SVCDEPS_SES_ATTR);
		
		return "listServiceDependencies";
	}
	
	public String addDependencies() throws Exception {
		setResource();
		// XXX put the right title in or refactor to use a common title...
		Portal portal = Portal.createPortal(
				"resource.application.inventory.AddDependenciesPageTitle",
				".resource.application.inventory.addServiceDependencies");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "addDependencies";
	}


}
