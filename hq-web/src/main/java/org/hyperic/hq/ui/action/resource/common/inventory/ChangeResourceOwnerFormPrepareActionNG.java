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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("changeResourceOwnerFormPrepareActionNG")
public class ChangeResourceOwnerFormPrepareActionNG extends BaseActionNG
		implements ViewPreparer {

	private final Log log = LogFactory
			.getLog(ChangeResourceOwnerFormPrepareActionNG.class);
	@Resource
	private AuthzBoss authzBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
			ChangeResourceOwnerFormNG changeForm = new ChangeResourceOwnerFormNG();
			Integer resourceId = changeForm.getRid();
			Integer resourceType = changeForm.getType();
	
			if (resourceId == null) {
				resourceId = RequestUtils.getResourceId(request);
			}
			if (resourceType == null) {
				resourceType = RequestUtils.getResourceTypeId(request);
			}
	
			AppdefResourceValue resource = RequestUtils.getResource(request);
			if (resource == null) {
				log.error(getText(Constants.ERR_RESOURCE_NOT_FOUND));
				return;
			}
			changeForm.setRid(resource.getId());
			changeForm.setType(new Integer(resource.getEntityId().getType()));
	
			Object resourceOwner = request
					.getAttribute(Constants.RESOURCE_OWNER_ATTR);
	
			if (resourceOwner == null) {
				
				log.error(getText("resource.common.inventory.error.ResourceOwnerNotFound"));
				return;
			}
	
			Integer sessionId = RequestUtils.getSessionId(request);
			PageControl pc = RequestUtils.getPageControl(request);
	
			log.trace("getting all users");
			PageList<AuthzSubjectValue> allUsers = authzBoss.getAllSubjects(
					sessionId, null, pc);
	
			// remove the resource's owner from the list of users
			ArrayList<Object> owner = new ArrayList<Object>();
			owner.add(resourceOwner);
			List<AuthzSubjectValue> users = BizappUtilsNG.grepSubjects(allUsers,
					owner);
	
			request.setAttribute(Constants.ALL_USERS_ATTR, users);
			request.setAttribute(Constants.NUM_USERS_ATTR,
					new Integer(allUsers.getTotalSize() - 1));
			request.setAttribute("changeForm", changeForm);
		} catch (Exception ex) {
			log.error(ex);
		}

	}

}
