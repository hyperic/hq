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

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * This class handles saving the general properties editing performed on screen
 * 2.1.6.1
 */
@Component("editApplicationGeneralActionNG")
@Scope(value = "prototype")
public class EditGeneralPropertiesActionNG extends BaseActionNG implements ModelDriven<ResourceFormNG>{

    private final Log log = LogFactory.getLog(EditGeneralPropertiesActionNG.class);

    @Resource
    private AppdefBoss appdefBoss;
    
	private ResourceFormNG editForm = new ResourceFormNG();

	private String internalEid;
	private String internalType;
	private String internalRid;
	
	public String save() throws Exception {
        Integer appId = editForm.getRid();
        Integer entityType = editForm.getType();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        if (aeid != null) {
        	setEntityRequestParams(aeid);
        } else {
        	addActionError(getText( "resource.application.error.ApplicationNotFound" ) );
            return INPUT;
        }
        
        request.setAttribute(Constants.RESOURCE_PARAM, appId);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

        try {
			String forward = checkSubmit(editForm);
			if (forward != null) {
				return forward;
			}

            Integer sessionId = RequestUtils.getSessionId(request);

            // now set up the application
            ApplicationValue app = appdefBoss.findApplicationById(sessionId.intValue(), appId);
            log.trace("in preparation to update it, retrieved app " + app);
            if (app == null) {
                addActionError(getText( "resource.application.error.ApplicationNotFound" ) );
                return INPUT;
            }

            editForm.updateResourceValue(app);

            log.trace("editing general properties of application [" + app.getName() + "]" + " with attributes " +
                      editForm);

            appdefBoss.updateApplication(sessionId.intValue(), app);

            addActionMessage(getText("resource.application.inventory.confirm.EditGeneralProperties", new String[] {app.getName()}) );
        } catch (AppdefDuplicateNameException e1) {
            addActionError(getText( Constants.ERR_DUP_RESOURCE_FOUND) );
            return INPUT;
        }		
		return SUCCESS;
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

		editForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid != null) {
			setEntityRequestParams(aeid);
		}
		return "reset";
	}

	public ResourceFormNG getModel() {

		return editForm;
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

	public ResourceFormNG getEditForm() {
		return editForm;
	}

	public void setEditForm(ResourceFormNG editForm) {
		this.editForm = editForm;
	}
	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId().toString();
		this.internalType = String.valueOf( eid.getType() );
	}
}
