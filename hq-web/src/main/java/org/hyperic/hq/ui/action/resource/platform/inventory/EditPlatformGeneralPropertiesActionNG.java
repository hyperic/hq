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

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

/**
 * A <code>BaseAction</code> subclass that edits the general properties of a
 * platform in the BizApp.
 */
@Component("editPlatformGeneralPropertiesActionNG")
@Scope(value="prototype")
public class EditPlatformGeneralPropertiesActionNG extends ResourceInventoryPortalActionNG
		implements ModelDriven<ResourceFormNG> , Preparable	{

	private final Log log = LogFactory
			.getLog(EditPlatformGeneralPropertiesActionNG.class.getName());

	public String getInternalEid() {
		return internalEid;
	}
	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	@Autowired
	private AppdefBoss appdefBoss;

	private ResourceFormNG editForm = new ResourceFormNG();

	private String internalEid;
	private String type;
	private String rid;

	
	@SkipValidation
	public String start() throws Exception {
		setResource();
		
		Portal portal = Portal.createPortal("resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
		            ".resource.platform.inventory.EditPlatformGeneralProperties");
        portal.setDialog(true);
        getServletRequest().setAttribute(Constants.PORTAL_KEY, portal);
        
        getServletRequest().removeAttribute("eid"); 
        
        AppdefResourceValue resource = RequestUtils.getResource(request);
        
        if (resource == null) {
            // RequestUtils.setError(request, Constants.ERR_PLATFORM_NOT_FOUND);
            // return null;
        	log.error(Constants.ERR_RESOURCE_NOT_FOUND);
        }
        
        editForm.loadResourceValue(resource);
        
		return "editPlatformGeneralProperties";
	}
	/**
	 * Edit the platform with the attributes specified in the given
	 * <code>ResourceForm</code>.
	 */
	
	public String save() throws Exception {

		request = getServletRequest();
		this.removeValueInSession("currentSelectedPlatformForEdit");

		Integer platformId = editForm.getRid();
		Integer entityType = editForm.getType();
		internalEid=entityType+":"+platformId;

		request.setAttribute(Constants.RESOURCE_PARAM, platformId);
		request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

		try {
			clearErrorsAndMessages();
			Integer sessionId = RequestUtils.getSessionId(request);

			// now set up the platform
			PlatformValue platform = appdefBoss.findPlatformById(
					sessionId.intValue(), platformId);
			if (platform == null) {
				addActionError("resource.platform.error.PlatformNotFound");
				return Constants.FAILURE_URL;
			}
			platform = (PlatformValue) platform.clone();

			editForm.updateResourceValue(platform);

			log.trace("editing general properties of platform ["
					+ platform.getName() + "]" + " with attributes " + editForm);

			appdefBoss.updatePlatform(sessionId.intValue(), platform);

			addActionMessage(getText("resource.platform.inventory.confirm.EditGeneralProperties"));
			return "success";
		} catch (AppdefDuplicateNameException e1) {
			addActionError(Constants.ERR_DUP_RESOURCE_FOUND);
			return INPUT;
		} catch (ApplicationException e) {
			setErrorObject( "dash.autoDiscovery.import.Error", e.getMessage() );
			return INPUT;
		}
	}

	
	public void prepare() throws Exception {
		request = getServletRequest();
		Integer sessionId = RequestUtils.getSessionId(request);
		HttpSession session = request.getSession();

		ResourceFormNG resourceForm = new ResourceFormNG();

		String resId = request.getParameter("eid");
		if (resId == null || resId.equalsIgnoreCase("")){
			resId = (String) session.getAttribute("currentSelectedPlatformForEdit");
		} else {
			this.setValueInSession("currentSelectedPlatformForEdit", resId); 
		}
		request.setAttribute("eid",resId);
		AppdefResourceValue resource = appdefBoss.findById(sessionId, new AppdefEntityID(resId));
		
		if (resource == null) {
			log.error("Could not load resource" + internalEid);
		}
		
		// now set up the platform
		resourceForm.loadResourceValue(resource);

				request.setAttribute("resourceForm", editForm);
				
		Portal portal = Portal
				.createPortal(
						"resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
						".resource.platform.inventory.EditPlatformGeneralProperties");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
		request.setAttribute(Constants.TITLE_PARAM_ATTR,resourceForm.getName());
				 
	}

	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			internalEid = aeid.toString();
		}
		return "cancel";
	}
	

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		editForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			internalEid = aeid.toString();
		}
		rid = aeid.getId().toString();
		type = String.valueOf(aeid.getType());
		return "reset";
	}

	public ResourceFormNG getEditForm() {
		return editForm;
	}

	public void setEditForm(ResourceFormNG editForm) {
		this.editForm = editForm;
	}

	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public ResourceFormNG getModel() {

		return editForm;
	}
	
	private void setErrorObject( String key, String regularMsg) {
		addActionError(getText( key, new String[] {regularMsg}) );
	}

}
