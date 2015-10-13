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

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("changeResourceOwnerActionNG")
@Scope("prototype")
public class ChangeResourceOwnerActionNG extends BaseActionNG implements
		ModelDriven<ChangeResourceOwnerFormNG> {

	private final Log log = LogFactory
			.getLog(ChangeResourceOwnerActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;

	private ChangeResourceOwnerFormNG chownForm = new ChangeResourceOwnerFormNG();
	
	private String internalEid;

	public String load() throws Exception {
		Integer resourceId = chownForm.getRid();
		Integer resourceType = chownForm.getType();
		Integer ownerId = chownForm.getOwner();

		request.setAttribute(Constants.RESOURCE_PARAM, resourceId);
		request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

		String forward = checkSubmit(chownForm);
		if (forward != null) {
            return forward;
		}
		Integer sessionId = RequestUtils.getSessionId(request);

		AppdefEntityID entityId = new AppdefEntityID(resourceType.intValue(),
				resourceId);
		log.trace("setting owner [" + ownerId + "] for resource [" + entityId
				+ "]");
		try {
			appdefBoss.changeResourceOwner(sessionId.intValue(), entityId,
					ownerId);
		} catch (GroupException e) {
			setErrorObject("resource.common.inventory.error.ChangeDynamicGroupOwner",
					e.getMessage());
			return INPUT;

		}

		addActionMessage(getText("resource.common.inventory.confirm.ChangeResourceOwner") );
		// fix for 5265. Check if we've lost viewability of the
		// resource. If we have, then return to the resource hub
		try {
			appdefBoss.findById(sessionId.intValue(), entityId);
		} catch (PermissionException e) {
			// looks like we cant see the thing anymore...
			// you, sir, are going to the resource hub
			return INPUT;
		}
		internalEid = entityId.toString();
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

	private void setErrorObject(String key, String regularMsg) {
		addActionError(getText(key, new String[] { regularMsg }));
	}

	public ChangeResourceOwnerFormNG getModel() {
		return chownForm;
	}
	
	public ChangeResourceOwnerFormNG getChownForm() {
		return chownForm;
	}

	public void setChownForm(ChangeResourceOwnerFormNG chownForm) {
		this.chownForm = chownForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
}
