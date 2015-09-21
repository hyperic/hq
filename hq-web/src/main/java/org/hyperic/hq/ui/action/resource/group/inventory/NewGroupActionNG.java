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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.HashMap;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("newGroupActionNG")
@Scope(value = "prototype")
public class NewGroupActionNG extends BaseActionNG implements ModelDriven<GroupFormNG>{
	
    private final Log log = LogFactory.getLog(NewGroupActionNG.class.getName());
    @Resource
    private AppdefBoss appdefBoss;
    
    private GroupFormNG resourceForm= new GroupFormNG();
    
	private String internalEid;
	private Integer internalRid;
	private Integer internalType;


	@SkipValidation
	public String start() throws Exception {
		request = getServletRequest();
		return "formLoad";
	}
	
	
	public String save() throws Exception {

		// Clean up after ourselves first
		HttpSession session = request.getSession();
		session.removeAttribute(Constants.ENTITY_IDS_ATTR);
		session.removeAttribute(Constants.RESOURCE_TYPE_ATTR);

		String forward = checkSubmit(resourceForm);
		if (forward != null) {
			return forward;
		}


		try {

			Integer sessionId = RequestUtils.getSessionId(request);
			ResourceGroup newGroup;

			final Integer entType = resourceForm.getEntityTypeId();

			// Append username to private groups
			if (resourceForm.isPrivateGroup()) {
				final WebUser user = RequestUtils.getWebUser(session);
				final String privateName = RequestUtils.message(request,
						"resource.group.name.private",
						new Object[] { user.getName() });
				resourceForm.setName(resourceForm.getName() + " " + privateName);
			}

			if (resourceForm.getGroupType() == Constants.APPDEF_TYPE_GROUP_COMPAT) {
				newGroup = appdefBoss.createGroup(sessionId, entType,
						resourceForm.getResourceTypeId(), resourceForm.getName(),
						resourceForm.getDescription(), resourceForm.getLocation(),
						resourceForm.getEntityIds(), resourceForm.isPrivateGroup());
			} else {
				// Constants.APPDEF_TYPE_GROUP_ADHOC
				if (entType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION
						|| entType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
					newGroup = appdefBoss.createGroup(sessionId, entType,
							resourceForm.getName(), resourceForm.getDescription(),
							resourceForm.getLocation(), resourceForm.getEntityIds(),
							resourceForm.isPrivateGroup());
				} else {
					// otherwise, create a mixed group
					newGroup = appdefBoss.createGroup(sessionId,
							resourceForm.getName(), resourceForm.getDescription(),
							resourceForm.getLocation(), resourceForm.getEntityIds(),
							resourceForm.isPrivateGroup());
				}
			}

			log.trace("creating group [" + resourceForm.getName()
					+ "] with attributes " + resourceForm);


			request.setAttribute(Constants.RESOURCE_PARAM, newGroup.getId());
			request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, AppdefEntityConstants.APPDEF_TYPE_GROUP);

			internalEid = AppdefEntityConstants.APPDEF_TYPE_GROUP + ":" + newGroup.getId();
			internalRid = newGroup.getId();
			internalType = AppdefEntityConstants.APPDEF_TYPE_GROUP;
			
			resourceForm.setRid(newGroup.getId());

			addActionMessage(getText("resource.group.inventory.confirm.CreateGroup", new String[] { resourceForm.getName() }) );
			
			if (resourceForm.getRid() != null){
				// this means that another entity is creating the group and once created we need to 
				// associate between them
				AppdefEntityID aeid = RequestUtils.getEntityId(request);
				appdefBoss.batchGroupAdd(sessionId.intValue(), aeid, new Integer[] { internalRid });
			}
			/*

			for (String resourceAppdefEntityId : resourceAppdefEntityIds) {
				getAppdefBoss().batchGroupAdd(webUser.getSessionId(),
						new AppdefEntityID(resourceAppdefEntityId), groupIds);
			}
			appdefBoss.batchGroupAdd(sessionId.intValue(), aeid, pendingGroupIds);
			 */

			return SUCCESS;
		} catch (GroupDuplicateNameException ex) {
			log.debug("group creation failed:", ex);
			addActionError("resource.group.inventory.error.DuplicateGroupName");
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
	


	public GroupFormNG getResourceForm() {
		return resourceForm;
	}

	public void setResourceForm(GroupFormNG newForm) {
		this.resourceForm = newForm;
	}

	public GroupFormNG getModel() {
		return resourceForm;
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
    
}
