/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.web.resource.association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.web.BaseController;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This controller handles the different actions that can be performed when
 * associating one or more resources to a group.
 * 
 * @author David Crutchfield
 * 
 */
@Controller
public class GroupAssociationController extends BaseController {
	private final static Log log = LogFactory.getLog(GroupAssociationController.class.getName());
			
	@Autowired
	public GroupAssociationController(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
		super(appdefBoss, authzBoss);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/resource/associations")
	public @ResponseBody
	Map<String, List<Map<String, Object>>> getAvailableAssociations(
			@RequestParam(RequestParameterKeys.RESOURCE_IDS) String[] resourceAppdefEntityIds,
			HttpSession session) {
		// First create an array of AppdefEntityIDs from the passed in String array...
		AppdefEntityID[] appdefEntityIds = new AppdefEntityID[resourceAppdefEntityIds.length];

		for (int x = 0; x < resourceAppdefEntityIds.length; x++) {
			appdefEntityIds[x] = new AppdefEntityID(resourceAppdefEntityIds[x]);
		}

		// ...then get the web user...
		WebUser webUser = getWebUser(session);
		PageList<AppdefGroupValue> availableGroups;
		Map<String, List<Map<String, Object>>> result = new LinkedHashMap<String, List<Map<String, Object>>>();

		try {
			// ...followed by a list of available groups...
			availableGroups = getAppdefBoss().findAllGroupsMemberExclusive(
					webUser.getSessionId(), PageControl.PAGE_ALL,
					appdefEntityIds);

			List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();

			// ...iterate through the list of groups and populate the result list...
			for (AppdefGroupValue group : availableGroups) {
				Map<String, Object> groupInfo = new HashMap<String, Object>();

				groupInfo.put("id", group.getId());
				groupInfo.put("name", group.getName());
				groupInfo.put("description", group.getDescription());
				groups.add(groupInfo);
			}

			// ...update the result with a list of groups...
			result.put("groups", groups);
		} catch (PermissionException e) {
			log.debug("User doesn't have the permission to perform this operation", e);
		} catch (SessionException e) {
			log.debug("There's a problem with the user's session", e);
		} catch (Exception e) {
			log.debug(e);
		}

		return result;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/resource/association")
	public String createAssociation(
			@RequestParam(RequestParameterKeys.GROUP_ID) Integer[] groupIds,
			@RequestParam(RequestParameterKeys.RESOURCE_IDS) String[] resourceAppdefEntityIds,
			HttpSession session) {
		String redirectString = "redirect:/app/resource/";

		try {
			// First, get the web user...
			WebUser webUser = getWebUser(session);

			// ...then iterate through the AppdefEntitIds, adding them to the specified group(s)...
			for (String resourceAppdefEntityId : resourceAppdefEntityIds) {
				getAppdefBoss().batchGroupAdd(webUser.getSessionId(),
						new AppdefEntityID(resourceAppdefEntityId), groupIds);
			}

			// TODO come up with a better strategy...for now this works 
			if (groupIds.length == 1) {
				redirectString += "association/" + groupIds[0];
			} else {
				redirectString += "associations";
			}
		} catch (PermissionException e) {
			log.debug("User doesn't have the permission to perform this operation", e);
		} catch (SessionException e) {
			log.debug("There's a problem with the user's session", e);
		} catch (VetoException e) {
			log.debug(e);
		} catch (Exception e) {
			log.debug(e);
		}

		return redirectString;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/resource/association/{groupId}")
	public @ResponseBody
	Map<String, String> getAssociation() {
		// TODO this doesn't currently do anything, but ideally when we create
		// an association the request would be redirected to GET the newly
		// created association (or parent resource)...
		return new HashMap<String, String>();
	}
}