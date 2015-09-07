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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

@Component("editServiceGeneralActionNG")
@Scope(value = "prototype")
public class EditGeneralActionNG extends BaseActionNG implements
		ModelDriven<ServiceFormNG> , Preparable {

	private final Log log = LogFactory.getLog(EditGeneralActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;

	private ServiceFormNG serviceForm = new ServiceFormNG();

	private String internalEid;
	private String internalType;
	private String internalRid;

	public String save() throws Exception {
		try {

			Integer rid = serviceForm.getRid();
			Integer entityType = serviceForm.getType();
			internalEid=entityType+":"+rid;

			request.setAttribute(Constants.RESOURCE_PARAM, rid);
			request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
			
			String forward = checkSubmit(serviceForm);
			if (forward != null) {
				return forward;
			}

			Integer sessionId = RequestUtils.getSessionId(request);

			Integer serviceId = RequestUtils.getResourceId(request);

			ServiceValue sValue = appdefBoss.findServiceById(
					sessionId.intValue(), serviceId);

			serviceForm.updateServiceValue(sValue);

			ServiceValue updatedServer = appdefBoss.updateService(
					sessionId.intValue(), sValue, null);

			// XXX: enable when we have a confirmed functioning API
			log.trace("saving service [" + sValue.getName() + "]"
					+ " with attributes " + serviceForm);

			addActionMessage(getText("resource.service.inventory.confirm.EditGeneralProperties", new String[] {updatedServer.getName()} ));

			return SUCCESS;
		} catch (AppdefDuplicateNameException e1) {
            addActionError( Constants.ERR_DUP_RESOURCE_FOUND);
            return INPUT;
		}

	}

	public void prepare() throws Exception {
		setHeaderResources();
		
        // Integer rid = serviceForm.getRid();
        // Integer entityType = serviceForm.getType();
		AppdefEntityID aeid;

		try {
			aeid = RequestUtils.getEntityId(request);
		} catch (ParameterNotFoundException ex) {
			aeid= null;
		}
		
		if ( aeid!= null) {
			setEntityRequestParams(aeid);
	        request.setAttribute(Constants.RESOURCE_PARAM, aeid.getId());
	        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, aeid.getType());
		} 
        
        Integer sessionId = RequestUtils.getSessionId(request);

        Integer serviceId = RequestUtils.getResourceId(request);
		ServiceValue sValue = appdefBoss.findServiceById(sessionId.intValue(), serviceId);
        
        serviceForm.loadResourceValue(sValue);		
	}	
	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid != null) {
			internalEid = aeid.toString();
		}
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		serviceForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid != null) {
			internalEid = aeid.toString();
		}
		internalRid = aeid.getId().toString();
		internalType = String.valueOf(aeid.getType());
		return "reset";
	}

	public ServiceFormNG getModel() {

		return serviceForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public String getInternalType() {
		return internalType;
	}

	public void setInternalType(String internalType) {
		this.internalType = internalType;
	}

	public String getInternalRid() {
		return internalRid;
	}

	public void setInternalRid(String internalRid) {
		this.internalRid = internalRid;
	}

	public ServiceFormNG getServiceForm() {
		return serviceForm;
	}

	public void setServiceForm(ServiceFormNG serviceForm) {
		this.serviceForm = serviceForm;
	}
	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId().toString();
		this.internalType = String.valueOf( eid.getType() );
	}
}
