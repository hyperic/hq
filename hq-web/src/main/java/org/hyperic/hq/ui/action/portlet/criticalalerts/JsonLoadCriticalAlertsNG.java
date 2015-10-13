/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.criticalalerts;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component(value = "jsonLoadCriticalAlertsNG")
@Scope("prototype")
public class JsonLoadCriticalAlertsNG extends BaseActionNG {
	private final Log log = LogFactory.getLog(JsonLoadCriticalAlertsNG.class
			.getName());

	static final String RESOURCES_KEY = Constants.USERPREF_KEY_CRITICAL_ALERTS_RESOURCES_NG;

	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private EventsBoss eventsBoss;
	@Resource
	private DashboardManager dashboardManager;
	@Resource
	private AlertPermissionManager alertPermissionManager;

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {

		try {

			JsonActionContextNG ctx = this.setJSONContext();

			HttpSession session = request.getSession();
			WebUser user = RequestUtils.getWebUser(session);
			DashboardConfig dashConfig = dashboardManager.findDashboard(
					(Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, authzBoss);

			ConfigResponse dashPrefs = dashConfig.getConfig();

			String token;

			try {
				token = RequestUtils.getStringParameter(request, "token");
			} catch (ParameterNotFoundException e) {
				token = null;
			}

			// For multi-portlet configurations
			String resKey = RESOURCES_KEY;
			String countKey = PropertiesFormNG.ALERT_NUMBER;
			String priorityKey = PropertiesFormNG.PRIORITY;
			String timeKey = PropertiesFormNG.PAST;
			String selOrAllKey = PropertiesFormNG.SELECTED_OR_ALL;
			String titleKey = PropertiesFormNG.TITLE;

			if (token != null) {
				resKey += token;
				countKey += token;
				priorityKey += token;
				timeKey += token;
				selOrAllKey += token;
				titleKey += token;
			}

			List<AppdefEntityID> entityIds = DashboardUtils
					.preferencesAsEntityIds(resKey, dashPrefs);
			AppdefEntityID[] arrayIds = entityIds
					.toArray(new AppdefEntityID[0]);

			int count = Integer.parseInt(dashPrefs.getValue(countKey));
			int priority = Integer.parseInt(dashPrefs.getValue(priorityKey)
					.trim());
			long timeRange = Long.parseLong(dashPrefs.getValue(timeKey));
			boolean all = "all".equals(dashPrefs.getValue(selOrAllKey));

			int sessionID = user.getSessionId().intValue();

			if (all) {
				arrayIds = null;
			}

			List<Escalatable> criticalAlerts = eventsBoss.findRecentAlerts(
					sessionID, count, priority, timeRange, arrayIds);

			JSONObject alerts = new JSONObject();
			List<JSONObject> a = new ArrayList<JSONObject>();

			// MessageResources res = getResources(request);
			// String formatString =
			// res.getMessage(Constants.UNIT_FORMAT_PREFIX_KEY +
			// "epoch-millis");
			String formatString = getText(Constants.UNIT_FORMAT_PREFIX_KEY
					+ "epoch-millis");

			AuthzSubject subject = authzBoss.getCurrentSubject(sessionID);
			SimpleDateFormat df = new SimpleDateFormat(formatString);
			for (Escalatable alert : criticalAlerts) {

				AlertDefinitionInterface def;
				AppdefEntityValue aVal;
				AppdefEntityID eid;
				Escalation escalation;
				long maxPauseTime = 0;

				String date = df.format(new Date(alert.getAlertInfo()
						.getTimestamp()));
				def = alert.getDefinition().getDefinitionInfo();
				escalation = alert.getDefinition().getEscalation();
				if (escalation != null && escalation.isPauseAllowed()) {
					maxPauseTime = escalation.getMaxPauseTime();
				}
				eid = AppdefUtil.newAppdefEntityId(def.getResource());
				aVal = new AppdefEntityValue(eid, subject);

				boolean canTakeAction = false;
				try {
					alertPermissionManager
							.canFixAcknowledgeAlerts(subject, eid);
					canTakeAction = true;
				} catch (PermissionException e) {
					// We can view it, but can't take action on it
				}

				JSONObject jAlert = new JSONObject();
				jAlert.put("alertId", alert.getId());
				jAlert.put("appdefKey", eid.getAppdefKey());
				jAlert.put("resourceName", HtmlUtils.htmlEscape(aVal.getName()));
				jAlert.put("alertDefName", def.getName());
				jAlert.put("cTime", date);
				jAlert.put("fixed", alert.getAlertInfo().isFixed());
				jAlert.put("acknowledgeable", alert.isAcknowledgeable());
				jAlert.put("alertType", alert.getDefinition().getAlertType()
						.getCode());
				jAlert.put("maxPauseTime", maxPauseTime);
				jAlert.put("canTakeAction", canTakeAction);

				a.add(jAlert);
			}

			alerts.put("criticalAlerts", a);
			if (token != null) {
				alerts.put("token", token);
			} else {
				alerts.put("token", JSONObject.NULL);
			}

			alerts.put("title", dashPrefs.getValue(titleKey, ""));

			JSONResult jsonRes = new JSONResult(alerts);
			ctx.setJSONResult(jsonRes);

			inputStream = this.streamJSONResult(ctx);
			request.setAttribute("titleDescription",
					dashPrefs.getValue(titleKey, ""));

		} catch (Exception ex) {
			log.error("missing dashConfig for key "
					+ Constants.SELECTED_DASHBOARD_ID,ex);
		}

		return null;
	}
}
