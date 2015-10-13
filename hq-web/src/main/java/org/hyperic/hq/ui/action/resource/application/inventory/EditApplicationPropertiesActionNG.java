/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.application.ApplicationFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * This class handles saving edit operations performed on Application Properties
 * (screen 2.1.6.2)
 */

@Component("editApplicationPropertiesActionNG")
@Scope("prototype")
public class EditApplicationPropertiesActionNG extends BaseActionNG implements ModelDriven<ApplicationFormNG>{

    private final Log log = LogFactory.getLog(EditApplicationPropertiesActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    
	private ApplicationFormNG appForm = new ApplicationFormNG();
	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
	
	public String save() throws Exception {
		
        AppdefEntityID aeid = new AppdefEntityID(appForm.getType().intValue(), appForm.getRid());

        request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        request.setAttribute(Constants.ACCORDION_PARAM, "1");
        setEntityRequestParams(aeid);
        

        String forward = checkSubmit(appForm);

        if (forward != null) {
            return forward;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        Integer applicationTypeId = appForm.getResourceType();
        // XXX there is no findApplicationTypeById(...) boss signature, so
        // we'll hope for the best .... when the api is updated, we'll use it
        /*
         */
        // List applicationTypes =
        // appdefBoss.findAllApplicationTypes(sessionId.intValue());
        log.trace("finding application type [" + applicationTypeId + "]");
        ApplicationType applicationType = appdefBoss.findApplicationTypeById(sessionId.intValue(), applicationTypeId);

        // now set up the application
        ApplicationValue appVal = appdefBoss.findApplicationById(sessionId.intValue(), aeid.getId());
        if (appVal == null) {
            addActionError(getText("resource.application.error.ApplicationNotFound" ) );
            return INPUT;
        }

        appForm.updateResourceValue(appVal);
        appVal.setApplicationType(applicationType);

        log.trace("updating general properties of application [" + appVal.getName() + "]" + " with attributes " +
                  appVal);
        appdefBoss.updateApplication(sessionId.intValue(), appVal);

        addActionMessage(getText("resource.application.inventory.confirm.EditGeneralProperties", new String [] {appVal.getName()} ) );		
		return SUCCESS;
	}
	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			setInternalEid(aeid.toString());
		}
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		appForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			setEntityRequestParams(aeid);
		}
		return "reset";
	}
	
	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.setInternalEid(eid.toString());
		this.setInternalRid(eid.getId());
		this.setInternalType(eid.getType());
	}


	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
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

	public ApplicationFormNG getModel() {
		return appForm;
	}

	public ApplicationFormNG getAppForm() {
		return appForm;
	}

	public void setAppForm(ApplicationFormNG appForm) {
		this.appForm = appForm;
	}

}
