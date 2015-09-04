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



package org.hyperic.hq.ui.action.resource.server.inventory;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

@Component("editServerGeneralActionNG")
@Scope(value="prototype")
public class EditGeneralActionNG extends ResourceInventoryPortalActionNG implements ModelDriven<ServerFormNG> , Preparable{

    private final Log log = LogFactory.getLog(EditGeneralActionNG.class);
    
    @Resource
    private AppdefBoss appdefBoss;
    
    private ServerFormNG serverForm = new ServerFormNG ();
    
	private String internalEid;
	private String internalType;
	private String internalRid;
	
	@SkipValidation
	public String load() throws Exception {
		setResource();
		
        Integer rid = serverForm.getRid();
        Integer entityType = serverForm.getType();
        
        request.setAttribute(Constants.RESOURCE_PARAM, rid);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);


        Integer sessionId = RequestUtils.getSessionId(request);

        Integer serverId = RequestUtils.getResourceId(request);
        ServerValue sValue = appdefBoss.findServerById(sessionId.intValue(), serverId);
        
        serverForm.loadResourceValue(sValue);
        
		return "editServerGeneralProperties";
	}
	
	
	public String save() throws Exception {
        
        Integer rid = serverForm.getRid();
        Integer entityType = serverForm.getType();
        internalEid=entityType+":"+rid;
       
        request.setAttribute(Constants.RESOURCE_PARAM, rid);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
        
        try {

            Integer sessionId = RequestUtils.getSessionId(request);

            Integer serverId = RequestUtils.getResourceId(request);
            ServerValue sValue = appdefBoss.findServerById(sessionId.intValue(), serverId);
            serverForm.updateServerValue(sValue);
            ServerValue updatedServer = appdefBoss.updateServer(sessionId.intValue(), sValue, null);
            // XXX: enable when we have a confirmed functioning API
            log.trace("saving server [" + sValue.getName() + "]" + " with attributes " + serverForm);
            addActionMessage(getText("resource.server.inventory.confirm.EditGeneralProperties", new String [] {updatedServer.getName()}) );
            
            return SUCCESS;
        } catch (AppdefDuplicateNameException e1) {
            addActionError( Constants.ERR_DUP_RESOURCE_FOUND);
            return INPUT;
        }
	}
	
	public void prepare() throws Exception {
		setResource();
		
        Integer rid = serverForm.getRid();
        Integer entityType = serverForm.getType();
        
        request.setAttribute(Constants.RESOURCE_PARAM, rid);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

        
        Integer sessionId = RequestUtils.getSessionId(request);

        Integer serverId = RequestUtils.getResourceId(request);
        ServerValue sValue = appdefBoss.findServerById(sessionId.intValue(), serverId);
        
        serverForm.loadResourceValue(sValue);		
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

		serverForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			internalEid = aeid.toString();
		}
		internalRid = aeid.getId().toString();
		internalType = String.valueOf(aeid.getType());
		return "reset";
	}

	public ServerFormNG getModel() {
		return serverForm;
	}

	public ServerFormNG getServerForm() {
		return serverForm;
	}

	public void setServerForm(ServerFormNG serverForm) {
		this.serverForm = serverForm;
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
}
