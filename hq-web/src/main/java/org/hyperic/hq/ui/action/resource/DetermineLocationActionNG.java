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

package org.hyperic.hq.ui.action.resource;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> that sets up the Resource Hub portal.
 */
@Component("resourceAction")
public class DetermineLocationActionNG extends BaseActionNG {

	@Autowired
	private AppdefBoss appdefBoss;
	@Autowired
	private ResourceManager resourceManager;

	/**
	 * determines what resource default page to go to.
	 */
	public String execute() throws Exception {

		setHeaderResources();
		// We need to support auto-groups here, too. If there's a
		// ctype, we'll assume it's an autogroup.
		String ctype = RequestUtils.getStringParameter(getServletRequest(),
				Constants.CHILD_RESOURCE_TYPE_ID_PARAM, null);

		String type = null;
		if (null == ctype) {
			// non-autogroup
			AppdefEntityID aeid = RequestUtils.getEntityId(getServletRequest(),
					resourceManager);

			type = AppdefEntityConstants.typeToString(aeid.getType());

			if (aeid.isGroup()) {
				int sessionId = RequestUtils.getSessionId(getServletRequest())
						.intValue();

				AppdefGroupValue group = appdefBoss.findGroup(sessionId,
						aeid.getId());

				if (AppdefEntityConstants.isGroupAdhoc(group.getGroupType())) {
					type = "adhocGroup";
				} else if (AppdefEntityConstants.isDynamicGroup(group
						.getGroupType())) {
					type = "dynamicGroup";
				} else {
					type = "compatGroup";
				}
			}
		} else {
			// autogroup
			type = AppdefEntityConstants
					.typeToString(AppdefEntityConstants.APPDEF_TYPE_AUTOGROUP);
		}

		return type;
	}
	
	
	public String[] getEid(){
		return getServletRequest().getParameterValues(Constants.ENTITY_ID_PARAM);
	}
	
	public String getCtype(){
		return RequestUtils.getStringParameter(getServletRequest(),
				Constants.CHILD_RESOURCE_TYPE_ID_PARAM, null);
	}
}
