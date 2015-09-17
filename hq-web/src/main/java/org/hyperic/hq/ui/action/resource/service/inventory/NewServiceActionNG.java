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

import java.util.ArrayList;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.server.inventory.ServerFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

@Component("newServiceActionNG")
@Scope(value = "prototype")
public class NewServiceActionNG extends BaseActionNG implements ModelDriven<ServiceFormNG>, Preparable {
	private final Log log = LogFactory.getLog(NewServiceActionNG.class);

	private ServiceFormNG newForm = new ServiceFormNG();

	@Resource
	private AppdefBoss appdefBoss;

	private String internalEid;
	private Integer internalRid;
	private Integer internalType;

	@SkipValidation
	public String start() throws Exception {
		try {

			this.removeValueInSession("newServiceResourcesTypeList");
			AppdefEntityID aeid = RequestUtils.getEntityId(request);
			
			request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

			switch (aeid.getType()) {
			case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
				request.setAttribute(Constants.ACCORDION_PARAM, "2");
				break;
			}

			String forward = checkSubmit(newForm);
			if (forward != null) {
				return forward;
			}

			Integer sessionId = RequestUtils.getSessionId(request);

		} catch (Exception e) {
			throw new ServletException("Can't prepare new server form", e);
		}
		return "formLoad";
	}

	public String save() throws Exception {

		try {

			AppdefEntityID aeid = RequestUtils.getEntityId(request);

			request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

			switch (aeid.getType()) {
			case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
				request.setAttribute(Constants.ACCORDION_PARAM, "3");
				break;
			}

			String forward = checkSubmit(newForm);
			if (forward != null) {
				return forward;
			}

			Integer sessionId = RequestUtils.getSessionId(request);

			ServiceValue service = new ServiceValue();

			service.setName(newForm.getName());
			service.setDescription(newForm.getDescription());

			Integer stPk = newForm.getResourceType();
			ServiceValue newService = appdefBoss.createService(
					sessionId.intValue(), service, stPk, aeid);

			log.trace("creating service [" + service.getName()
					+ "] with attributes " + newForm);

			Integer serviceId = newService.getId();
			newForm.setRid(serviceId);

			addActionMessage(getText("resource.service.inventory.confirm.CreateService",new String[] {service.getName() }) );
			String eid = newService.getEntityId().getAppdefKey();
			request.setAttribute(Constants.ENTITY_ID_PARAM,eid );
			setInternalEid(eid);
			request.setAttribute(Constants.ACCORDION_PARAM, "0");
			
			
		} catch (AppdefDuplicateNameException e) {
			addActionError(getText( Constants.ERR_DUP_RESOURCE_FOUND ) );
			return INPUT;
		}
		return SUCCESS;
	}

	@SkipValidation
	public String cancel() throws Exception {
		this.request = getServletRequest();
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
		this.request = getServletRequest();
		setHeaderResources();
		newForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			setEntityRequestParams(aeid);
		}
		return "reset";
	}

	public void prepare() throws Exception {
		request = getServletRequest();
		HttpSession session = request.getSession();
		newForm.setResourceTypes((ArrayList<ServiceTypeValue>) session.getAttribute("newServiceResourcesTypeList"));

	}

	public ServiceFormNG getNewForm() {
		return newForm;
	}

	public void setNewForm(ServiceFormNG newForm) {
		this.newForm = newForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public ServiceFormNG getModel() {		
		return this.newForm;
	}
	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId();
		this.internalType = eid.getType();
	}

	public Integer getInternalRid() {
		return internalRid;
	}

	public void setInternalRid(Integer internalRid) {
		this.internalRid = internalRid;
	}

	public Integer getInternalType() {
		return internalType;
	}

	public void setInternalType(Integer internalType) {
		this.internalType = internalType;
	}
}
