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
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.application.ApplicationFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


/**
 * This class handles saving the submission from the application creation screen
 * (2.1.1)
 */
@Component("newApplicationActionNG")
@Scope(value = "prototype")
public class NewApplicationActionNG extends BaseActionNG implements ModelDriven<ApplicationFormNG>{
	
    private final Log log = LogFactory.getLog(NewApplicationActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;

    private ApplicationFormNG resourceForm= new ApplicationFormNG();

	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
	
	
	public String save() throws Exception {
        
        try {
    		String forward = checkSubmit(resourceForm);
    		if (forward != null) {
    			return forward;
    		}

            Integer sessionId = RequestUtils.getSessionId(request);
            Integer applicationTypeId = resourceForm.getResourceType();

            ApplicationValue app = new ApplicationValue();
            app.setName(resourceForm.getName());
            app.setDescription(resourceForm.getDescription());
            app.setEngContact(resourceForm.getEngContact());
            app.setBusinessContact(resourceForm.getBusContact());
            app.setOpsContact(resourceForm.getOpsContact());
            app.setLocation(resourceForm.getLocation());
            log.trace("finding application type [" + applicationTypeId + "]");
            ApplicationType applicationType = appdefBoss.findApplicationTypeById(sessionId.intValue(),
                applicationTypeId);
            app.setApplicationType(applicationType);
            log.trace("creating application [" + app.getName() + "] with attributes " + resourceForm);
            // XXX ConfigResponse is a dummy arg, must be nuked when the boss
            // interface fixed
            app = appdefBoss.createApplication(sessionId.intValue(), app, 
                new ConfigResponse());
            AppdefEntityID appId = app.getEntityId();
            log.trace("created application [" + app.getName() + "] with attributes " + app.toString() +
                      " and has appdef ID " + appId);
            addActionMessage(getText("resource.application.inventory." + "confirm.CreateApplication",new String[] { app.getName()}) );
            
            setEntityRequestParams(appId);
			
            return SUCCESS;
        }  catch (AppdefDuplicateNameException e1) {
            addActionError(Constants.ERR_DUP_RESOURCE_FOUND);
            return INPUT;
        }
		
	}
	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		resourceForm.reset();
		clearErrorsAndMessages();
		return "reset";
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

	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId();
		this.internalType = eid.getType();
	}

	public ApplicationFormNG getModel() {

		return resourceForm;
	}

	public ApplicationFormNG getResourceForm() {
		return resourceForm;
	}

	public void setResourceForm(ApplicationFormNG resourceForm) {
		this.resourceForm = resourceForm;
	}
}
