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

import java.util.HashMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

@Component("editServerTypeHostActionNG")
@Scope(value = "prototype")
public class EditTypeHostActionNG extends BaseActionNG implements ModelDriven<ServerFormNG> , Preparable{
	
    private final Log log = LogFactory.getLog(EditTypeHostActionNG.class);
    
    @Resource
    private AppdefBoss appdefBoss;
    
	private ServerFormNG serverForm = new ServerFormNG();
	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
    
    public String save() throws Exception {
        AppdefEntityID aeid = new AppdefEntityID(serverForm.getType().intValue(), serverForm.getRid());

        
        request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        String forward = checkSubmit(serverForm);

        if (forward != null) {
            return forward;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        ServerValue server = appdefBoss.findServerById(sessionId.intValue(), serverForm.getRid());

        serverForm.updateServerValue(server);

        appdefBoss.updateServer(sessionId.intValue(), server);

        // XXX: enable when we have a confirmed functioning API
        log.trace("saving server [" + server.getName() + "]" + " with attributes " + serverForm);

        Integer serverId = new Integer(-1);
        serverForm.setRid(serverId);

        addActionMessage(getText( "resource.server.inventory.confirm.SaveServer", new String[] {server.getName()} ));
        internalEid = aeid.getAppdefKey().toString();

        return SUCCESS;
    }

	public void prepare() throws Exception {
		setHeaderResources();
		
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
		serverForm.reset();
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

	public ServerFormNG getServerForm() {
		return serverForm;
	}

	public void setServerForm(ServerFormNG serverForm) {
		this.serverForm = serverForm;
	}

	public ServerFormNG getModel() {
		return serverForm;
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

}
