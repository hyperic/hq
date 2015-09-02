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

package org.hyperic.hq.ui.action.resource.common.inventory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.components.ActionError;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.action.resource.platform.inventory.EditPlatformTypeNetworkPropertiesFormPrepareActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("editResourceGeneralPropertiesFormPrepareActionNG")
public class EditResourceGeneralPropertiesFormPrepareActionNG extends
		BaseActionNG implements ViewPreparer {
	private final Log log = LogFactory.getLog(EditResourceGeneralPropertiesFormPrepareActionNG.class);
	
	@Resource
	private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext arg0, AttributeContext arg1) {
		try {
			ResourceFormNG resourceForm = new ResourceFormNG();
			request = getServletRequest();
			clearErrorsAndMessages();

			// Check if resource id was delivered in request params in eid parameter
			String resId = request.getParameter("eid");
			Integer sessionId = RequestUtils.getSessionId(request);
			AppdefResourceValue resource=null;
			if (resId == null) {
				// Check if resource id was delivered in request params in rid parameter
				resId = request.getParameter("rid");
			} 
			if (resId != null) {
				if (resId.contains(":")) {
					resource = appdefBoss.findById(sessionId, new AppdefEntityID(resId));
					
				} else {
					if (resId.equalsIgnoreCase("")) {
						// extract value from the session
						HttpSession session = request.getSession();
						resId = (String) session.getAttribute("currentSelectedPlatformForEdit");
						resource = appdefBoss.findById(sessionId, new AppdefEntityID(resId));
					} else {
						String etype = request.getParameter("type");
						String internalEid=etype+":"+resId;
						resource = appdefBoss.findById(sessionId, new AppdefEntityID(internalEid));
					}
				}

			}
			
			if (resource == null) {
				// RequestUtils.setError(request,
				// Constants.ERR_RESOURCE_NOT_FOUND);
				// return;
				addActionError(Constants.ERR_RESOURCE_NOT_FOUND);
				return;
			} else {
				resourceForm.loadResourceValue(resource);
			}

			request.setAttribute("resourceForm", resourceForm);
			
			Portal portal = Portal
					.createPortal(
							"resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
							".resource.platform.inventory.EditPlatformGeneralProperties");
			portal.setDialog(true);
			request.setAttribute(Constants.PORTAL_KEY, portal);
			request.setAttribute(Constants.TITLE_PARAM_ATTR,
					resourceForm.getName());
					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}

}
