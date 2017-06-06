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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Action which saves the general properties for a group
 */
@Component ("editMixedGroupsGeneralProperties")
@Scope(value = "prototype")
public class EditGeneralActionNG
    extends BaseActionNG implements ModelDriven<ResourceFormNG> {

    private final Log log = LogFactory.getLog(EditGeneralActionNG.class.getName());
	private String internalEid;
	private String internalType;
	private String internalRid;

    @Resource
    private AppdefBoss appdefBoss;
    private ResourceFormNG rForm =new ResourceFormNG() ;
    

    /**
     * Create the server with the attributes specified in the given
     * <code>GroupForm</code>.
     */
    
    public String save() throws Exception {
    	request = getServletRequest();
        Integer rid=rForm.getRid();
        Integer entityType=rForm.getType();
        internalEid=entityType+":"+rid;
        internalType = entityType.toString();
        internalRid = rid.toString();
        request.setAttribute(Constants.RESOURCE_PARAM, rid);
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);

        String forward = checkSubmit(rForm);
		
        if (forward != null) {
            return forward;
        }


        AppdefGroupValue rValue;

        try {
            Integer sessionId = RequestUtils.getSessionId(request);

            Integer groupId = RequestUtils.getResourceId(request);

            rValue = appdefBoss.findGroup(sessionId.intValue(), groupId);

            ResourceGroup group = appdefBoss.findGroupById(sessionId.intValue(), groupId);

            // See if this is a private group
            boolean isPrivate = true;
            Collection<ResourceGroup> groups = appdefBoss.getGroupsForResource(sessionId, group.getResource());
            for (Iterator<ResourceGroup> it = groups.iterator(); it.hasNext();) {
                ResourceGroup g = it.next();
                isPrivate = !g.getId().equals(AuthzConstants.rootResourceGroupId);
                if (!isPrivate)
                    break;
            }

            if (isPrivate) {
                // Make sure the username appears in the name
                final String owner = group.getResource().getOwner().getName();
                if (rForm.getName().indexOf(owner) < 0) {
                    final String privateName = RequestUtils.message(request, "resource.group.name.private",
                        new Object[] { owner });
                    rForm.setName(rForm.getName() + " " + privateName);
                }
            }

            appdefBoss.updateGroup(sessionId.intValue(), group, rForm.getName(), rForm.getDescription(), rForm
                .getLocation());

            // XXX: enable when we have a confirmed functioning API
            log.trace("saving group [" + rValue.getName() + "]" + " with attributes " + rForm);
            
            addActionMessage(getText("resource.group.inventory.confirm.EditGeneralProperties"));

            return SUCCESS;
        } catch (ParameterNotFoundException e1) {
        	addActionError(getText(Constants.ERR_RESOURCE_ID_FOUND));
            return Constants.FAILURE_URL;
        } catch (GroupDuplicateNameException ex) {
            log.debug("group creation failed:", ex);
            addActionError(getText("resource.group.inventory.error.DuplicateGroupName"));
            return Constants.FAILURE_URL;
        }
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
		rForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid != null) {
			setEntityRequestParams(aeid);
		}
		return "reset";
	}

	public ResourceFormNG getModel() {
		// TODO Auto-generated method stub
		return rForm;
	}
	public ResourceFormNG getrForm() {
		return rForm;
	}
	public void setrForm(ResourceFormNG rForm) {
		this.rForm = rForm;
	}
	public String getInternalEid() {
		return internalEid;
	}
	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.setInternalRid(eid.getId().toString());
		this.setInternalType(String.valueOf( eid.getType() ));
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
