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
package org.hyperic.hq.web.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This controller handles all interactions relative to the dashboard.
 * 
 * @author David Crutchfield
 * 
 */

@Controller
public class DashboardController extends BaseDashboardController {
	private final static Log log = LogFactory.getLog(DashboardController.class
			.getName());
	private final static String TOKEN_DELIMITER = "_";

	private List<String> multiplePortletsList;

	@Autowired
	public DashboardController(AuthzBoss authzBoss,
			ConfigurationProxy configurationProxy,
			DashboardManager dashboardManager) {
		super(null, authzBoss, configurationProxy, dashboardManager, null, null);
	}

	public void setMultiplePortletsList(List<String> multiplePortletsList) {
		this.multiplePortletsList = multiplePortletsList;
	}

	// ...helper function to convert a delimited string of portlet names to a
	// List of Strings...
	private List<String> deconstructDelimitedStringOfPortletNames(
			String delimitedStringOfPortletNames) {
		return new ArrayList<String>(
				Arrays
						.asList(deconstructDelimitedStringToStringArray(delimitedStringOfPortletNames)));
	}

	// ...helper function to convert an String array of portlet names to a
	// delimited string...
	private String constructDelimitedStringOfPortletNames(
			List<String> portletNames) {
		return constructDelimitedString(portletNames
				.toArray(new String[portletNames.size()]), false);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/dashboard/{dashboardId}/portlets")
	public @ResponseBody
	Map<String, Object> getDashboardPortlet(@PathVariable Integer dashboardId) {
		return new HashMap<String, Object>();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/dashboard/{dashboardId}/portlets")
	public String addPortletToDashboard(@PathVariable Integer dashboardId,
			@RequestParam(RequestParameterKeys.PORTLET_NAME) String portletName,
			@RequestParam(RequestParameterKeys.IS_PORTLET_WIDE) Boolean isWide,
			HttpSession session) {
		WebUser webUser = getWebUser(session);
		ConfigResponse dashboardSettings = getDashboardSettings(dashboardId,
				webUser);
		String userPreferenceKey;

		// ...determine which portlet column we're dealing with...
		if (isWide) {
			userPreferenceKey = UserPreferenceKeys.WIDE_PORTLETS;
		} else {
			userPreferenceKey = UserPreferenceKeys.NARROW_PORTLETS;
		}

		// ...then grab the list of associated portlets...
		String delimitedStringOfPortletNames = dashboardSettings
				.getValue(userPreferenceKey);

		// ...convert the delimited list of portlet names to an actual List...
		List<String> currentDashboardPortlets = deconstructDelimitedStringOfPortletNames(delimitedStringOfPortletNames);

		// ...check if the portlet to add, can be added multiple times OR not
		// already in the list...
		if (multiplePortletsList.contains(portletName)
				|| !currentDashboardPortlets.contains(portletName)) {
			// ...add the requested portlet to the list of dashboard portlets,
			// and in the event there can be
			// multiple portlets make sure to make the name unique by appending
			// an index to the name (index
			// does not imply ordering, just makes the name unique...
			int index = 2; // Index starts at two since the first portlet would
							// already exist in the list
			String uniquePortletName = portletName;

			while (currentDashboardPortlets.contains(uniquePortletName)) {
				// ...increment the index until we stop getting a hit...
				uniquePortletName = portletName + TOKEN_DELIMITER + index++;
			}

			// ...update the list of dashboard portlets...
			currentDashboardPortlets.add(uniquePortletName);

			// ...create the map of updated settings
			// TODO probably should create a representation object for this
			Map<String, Object> updatedSettings = new HashMap<String, Object>();

			updatedSettings
					.put(
							userPreferenceKey,
							constructDelimitedStringOfPortletNames(currentDashboardPortlets));

			try {
				boolean updated = compareAndUpdateSettings(session,
						dashboardSettings, updatedSettings);

				if (updated) {
					log.debug("[" + portletName + "] added to dashboard ["
							+ dashboardId + "] successfully");
				}
			} catch (PermissionException e) {
				log
						.debug(
								"User doesn't have the permission to perform this operation",
								e);
			} catch (SessionNotFoundException e) {
				log.debug("User's session can't be found", e);
			} catch (SessionTimeoutException e) {
				log.debug("User's session has timed out", e);
			} catch (Exception e) {
				log.debug(e);
			}
		}

		// TODO loop back when we convert the dashboard over to MVC
		return "redirect:/Dashboard.do";
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/dashboard/{dashboardId}/portlets/{portletName:.*}")
	public String removePortletFromDashboard(@PathVariable Integer dashboardId,
			@PathVariable String portletName, HttpSession session) {
			WebUser webUser = getWebUser(session);
		ConfigResponse dashboardSettings = getDashboardSettings(dashboardId,
				webUser);
		
		// ...grab the lists of all dashboard portlets...
		List<String> narrowDashboardPortlets = deconstructDelimitedStringOfPortletNames(dashboardSettings
				.getValue(UserPreferenceKeys.NARROW_PORTLETS));
		List<String> wideDashboardPortlets = deconstructDelimitedStringOfPortletNames(dashboardSettings
				.getValue(UserPreferenceKeys.WIDE_PORTLETS));

		// ...search and destroy...
		if (narrowDashboardPortlets.remove(portletName)
				|| wideDashboardPortlets.remove(portletName)) {
			// ...if we get a hit, also clean up that portlet's settings...
			// TODO portlet preferences are stored as <portlet name>.<pref
			// name>[_<index>]
			// this makes it a little harder than it has to be, would like to
			// move portlets into the db
			// and manage the dashboard, portlet, preference relationship there
			// rather than in the
			// preference name, for now we'll use a regex to match associated
			// settings

			// ...get the base portlet name and token, if any...
			String[] portletNameTokens = portletName.split(TOKEN_DELIMITER);

			// ...setup the regex starting with the basename...
			String regex = "^" + portletNameTokens[0].toLowerCase() + ".*";

			if (portletNameTokens.length == 2) {
				// ...adding in the token, if we have one...
				regex += TOKEN_DELIMITER + portletNameTokens[1];
			} else {
				// ...make sure we don't end with a multi portlet token...
				regex += "(?<!_\\d)";
			}
			
			// ...make sure we include the end of the string in the mix...
			regex += "$";
			
			// ...create the map of updated settings
			// TODO probably should create a representation object for this
			Map<String, Object> updatedSettings = new HashMap<String, Object>();

			// ...add modified portlet lists to updated settings map...
			updatedSettings.put(UserPreferenceKeys.NARROW_PORTLETS, constructDelimitedStringOfPortletNames(narrowDashboardPortlets));
			updatedSettings.put(UserPreferenceKeys.WIDE_PORTLETS, constructDelimitedStringOfPortletNames(wideDashboardPortlets));
			
			// ...now iterate thru the dashboard's setting keys...
			for (String settingKey : dashboardSettings.getKeys()) {
				if (settingKey.toLowerCase().matches(regex)) {
					// ...we have a hit, so remove it...
					updatedSettings.put(settingKey, null);
				}
			}

			try {
				boolean updated = compareAndUpdateSettings(session,
						dashboardSettings, updatedSettings);

				if (updated) {
					log.debug("[" + portletName + "] remove from dashboard ["
							+ dashboardId + "] successfully");
				}
			} catch (PermissionException e) {
				log.debug("User doesn't have the permission to perform this operation", e);
			} catch (SessionNotFoundException e) {
				log.debug("User's session can't be found", e);
			} catch (SessionTimeoutException e) {
				log.debug("User's session has timed out", e);
			} catch (Exception e) {
				log.debug(e);
			}
		}
		
		// ...now we're done, 302 out of here...
		return "redirect:/app/dashboard/" + dashboardId + "/portlets";
	}
}