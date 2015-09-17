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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("groupInventoryPortalActionNG")
@Scope(value = "prototype")
public class GroupInventoryPortalActionNG extends
		ResourceInventoryPortalActionNG {

	/**
	 * The request scope attribute under which actions store the full
	 * <code>List</code> of <code>EmptyValue</code> objects.
	 * 
	 * temporary list - (will remove - implementing the groups.)
	 */
	public static final String EMPTY_VALS_ATTR = "EmptyValues";

	protected final Log log = LogFactory
			.getLog(GroupInventoryPortalActionNG.class.getName());

	/**
	 * @see org.apache.struts.actions.LookupDispatchAction#getKeyMethodMap()
	 */
	private final Properties keyMethodMap = new Properties();

	public GroupInventoryPortalActionNG() {
		initKeyMethodMap();
	}

	private void initKeyMethodMap() {
		keyMethodMap.setProperty(Constants.MODE_NEW, "newResource");
		keyMethodMap.setProperty(Constants.MODE_EDIT, "editResourceGeneral");
		keyMethodMap.setProperty(Constants.MODE_EDIT_TYPE,
				"editResourceTypeHost");
		keyMethodMap.setProperty(Constants.MODE_VIEW, "viewResource");
		keyMethodMap.setProperty(Constants.MODE_ADD_RESOURCES, "addResources");
		keyMethodMap.setProperty(Constants.MODE_CHANGE_OWNER, "changeOwner");
		keyMethodMap.setProperty(Constants.MODE_ADD_ROLES, "addRoles");
	}

	protected Properties getKeyMethodMap() {
		return keyMethodMap;
	}

	public String newResource() throws Exception {

		Portal portal = Portal.createPortal(
				"resource.group.inventory.NewGroup",
				".resource.group.inventory.NewGroup");
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "newResource";
	}

	public String editResourceGeneral() throws Exception {

		setResource();

		Portal portal = Portal.createPortal(
				"resource.group.inventory.EditGeneralProperties",
				".resource.group.inventory.EditGeneralProperties");
		portal.setDialog(true);
		getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);

		return "editGroupProperties";
	}

	public String editResourceTypeHost() throws Exception {

		request = getServletRequest();
		findAndSetResource(request, response);

		Portal portal = Portal.createPortal(
				"resource.group.inventory.EditTypeAndHostProperties",
				".resource.group.inventory.EditTypeAndHostProperties");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editResourceTypeHost";
	}

	public String addResources() throws Exception {

		request = getServletRequest();
		findAndSetResource(request, response);

		Portal portal = Portal.createPortal(
				"resource.group.inventory.AddResources",
				".resource.group.inventory.AddResources");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
		this.removeValueInSession("resourceParentGroupsEid");
		return "addResources";
	}

	public String changeOwner() throws Exception {
	
		setResource();
		
		Portal portal = Portal.createPortal(Constants.CHANGE_OWNER_TITLE,
				".resource.group.inventory.changeOwner");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "changeOwner";
	}

	public String viewResource() throws Exception {

		setResource();
		request = getServletRequest();

		Portal portal = Portal.createPortal(
				"resource.group.inventory.ViewGroup",
				".resource.group.inventory.ViewGroup");
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return super.viewResource();
	}

	public String addRoles() throws Exception {
		request = getServletRequest();
		findAndSetResource(request, response);

		Portal portal = Portal.createPortal(
				"resource.group.inventory.AddRoles",
				".resource.group.inventory.AddRoles");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "addRoles";
	}

	protected void findAndSetResource(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		AppdefEntityID aeid = setResource();

		// If this is a cluster, then it's possible that it's also part of an
		// application
		Integer sessionId = RequestUtils.getSessionId(request);
		PageList<ApplicationValue> appValues = appdefBoss.findApplications(
				sessionId.intValue(), aeid, PageControl.PAGE_ALL);

		if (appValues.getTotalSize() > 0) {
			request.setAttribute(Constants.APPLICATIONS_ATTR, appValues);
		}
	}
}
