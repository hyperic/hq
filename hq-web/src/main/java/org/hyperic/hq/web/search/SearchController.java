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

package org.hyperic.hq.web.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
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
 * performing a text-based search for resources.
 * 
 * @author David Crutchfield
 * 
 */
@Controller
public class SearchController extends BaseController {
	private final static Log log = LogFactory.getLog(SearchController.class.getName());
	
	@Autowired
	public SearchController(AppdefBoss appdefBoss, AuthzBoss authzBoss) {
		super(appdefBoss, authzBoss);
	}

	// ...helper function that iterates through the list of search results and
	// constructs a list of maps that contain the id and name...
	private List<Map<String, String>> constructResourceMaps(
			PageList<SearchResult> resources) {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		for (SearchResult searchResult : resources) {
			// TODO create a representation object for this...
			Map<String, String> resultMap = new HashMap<String, String>();

			resultMap.put("id", searchResult.getAdeId());
			resultMap.put("name", searchResult.getName());

			result.add(resultMap);
		}

		return result;
	}

	// ...helper function that iterates through the list of authz subjects and
	// constructs a list of maps that contain the id and name...
	private List<Map<String, String>> constructUserMaps(
			PageList<AuthzSubject> users) {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		for (AuthzSubject user : users) {
			// TODO create a representation object for this...
			Map<String, String> resultMap = new HashMap<String, String>();

			resultMap.put("id", user.getId().toString());
			resultMap.put("name", user.getName());

			result.add(resultMap);
		}

		return result;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search")
	public @ResponseBody
	Map<String, List<Map<String, String>>> listSearchResults(
			@RequestParam(RequestParameterKeys.SEARCH_STRING) String searchString,
			HttpSession session) {
		// TODO Should this be exposed as a user preference?
		final int DEFAULT_PAGE_SIZE = 10;

		Map<String, List<Map<String, String>>> result = new LinkedHashMap<String, List<Map<String, String>>>();
		PageControl page = new PageControl(0, DEFAULT_PAGE_SIZE);

		// ...get the user...
		WebUser webUser = getWebUser(session);

		// ...search for resources matching the searchString...
		try {
			PageList<SearchResult> resources = getAppdefBoss().search(
					webUser.getSessionId(), searchString, page);

			result.put("resources", constructResourceMaps(resources));
		} catch (PermissionException e) {
			log.debug("User doesn't have the permission to perform this operation", e);
		} catch (SessionNotFoundException e) {
			log.debug("User's session can't be found", e);
		} catch (SessionTimeoutException e) {
			log.debug("User's session has timed out", e);
		} catch (Exception e) {
			log.debug(e);
		}

		// ...search for users matching the searchString...
		try {
			PageList<AuthzSubject> users = getAuthzBoss().getSubjectsByName(
					webUser.getSessionId(), searchString, page);

			result.put("users", constructUserMaps(users));
		} catch (PermissionException e) {
			log.debug("User doesn't have the permission to perform this operation", e);
		} catch (SessionNotFoundException e) {
			log.debug("User's session can't be found", e);
		} catch (SessionTimeoutException e) {
			log.debug("User's session has timed out", e);
		} catch (Exception e) {
			log.debug(e);
		}

		return result;
	}
}