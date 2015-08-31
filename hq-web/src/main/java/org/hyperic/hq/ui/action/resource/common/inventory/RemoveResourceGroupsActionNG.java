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

package org.hyperic.hq.ui.action.resource.common.inventory;
  
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.RemoveResourceFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("removeResourceGroupsActionNG")
@Scope("prototype")
public class RemoveResourceGroupsActionNG extends BaseActionNG  implements
ModelDriven<RemoveResourceGroupsFormNG>{

    private final Log log = LogFactory.getLog(RemoveResourceGroupsActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
	
    private RemoveResourceGroupsFormNG rmForm = new RemoveResourceGroupsFormNG() ;
    
	private String internalEid;
	
	@SkipValidation
	public String execute() throws Exception {
		

        Integer resourceId = rmForm.getRid();
        Integer resourceType = rmForm.getType();

        request.setAttribute(Constants.RESOURCE_PARAM, resourceId);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefEntityID entityId = new AppdefEntityID(resourceType.intValue(), resourceId);
        internalEid = entityId.toString();
        
        try {
            Integer[] groups = rmForm.getG();
            if (groups != null) {
                log.trace("removing groups " + groups + " for resource [" + resourceId + "]");
                appdefBoss.batchGroupRemove(sessionId.intValue(), entityId, groups);
    
                addActionMessage(getText( "resource.common.inventory.confirm.RemoveResourceGroups") );
            }
    
        } catch (VetoException ve) {
        	addActionError(getText( "resource.group.inventory.error.UpdateResourceListVetoed",new String[] {ve.getMessage()} ) );
            return INPUT;
        } 
        
        return SUCCESS;
	}

	public RemoveResourceGroupsFormNG getModel() {
		return rmForm;
	}

	public RemoveResourceGroupsFormNG getPForm() {
		return rmForm;
	}

	public void setPForm(RemoveResourceGroupsFormNG rwForm) {
		this.rmForm = rwForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
}
