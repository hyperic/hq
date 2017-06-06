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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.units.DateFormatter;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 
 * Set an array for the timeline display
 */
@Component("eventDetailsActionNG")
@Scope("prototype")
public class EventDetailsActionNG extends BaseActionNG {

	@Autowired
	private EventLogBoss eventLogBoss;

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.apache.struts.action.Action#execute(org.apache.struts.action.
	 * ActionMapping, org.apache.struts.action.ActionForm,
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public String execute() throws Exception {
		request = getServletRequest();
		WebUser user = RequestUtils.getWebUser(request);
		Map<String, Object> range = user.getMetricRangePreference();
		long begin = ((Long) range.get(MonitorUtilsNG.BEGIN)).longValue();
		long end = ((Long) range.get(MonitorUtilsNG.END)).longValue();
		long interval = TimeUtil.getInterval(begin, end,
				Constants.DEFAULT_CHART_POINTS);

		begin = Long.parseLong(RequestUtils
				.getStringParameter(request, "begin"));

		AppdefEntityID aeid = RequestUtils.getEntityId(request);

		int sessionId = user.getSessionId().intValue();

		List<EventLog> events;
		try {
			String status = RequestUtils.getStringParameter(request, "status");

			// Control logs are different, they store their return status
			// So we have to look it up by the type
			if (status.equals("CTL")) {
				events = eventLogBoss.getEvents(sessionId,
						ControlEvent.class.getName(), aeid, begin, begin
								+ interval);
			} else {
				events = eventLogBoss.getEvents(sessionId, aeid, status, begin,
						begin + interval);
			}
		} catch (ParameterNotFoundException e) {
			String[] types = null;
			events = eventLogBoss.getEvents(user.getSessionId().intValue(),
					aeid, types, begin, begin + interval);
		}

		String formatString = getText(Constants.UNIT_FORMAT_PREFIX_KEY
				+ "epoch-millis");
		DateFormatter.DateSpecifics dateSpecs;

		dateSpecs = new DateFormatter.DateSpecifics();
		dateSpecs.setDateFormat(new SimpleDateFormat(formatString));

		StringBuffer html;

		if (events.size() == 0) {
			html = new StringBuffer(
					getText("resource.common.monitor.text.events.None"));
		} else {
			html = new StringBuffer("<ul class=\"eventDetails\">");

			for (EventLog elv : events) {

				html.append("<li ");

				String status = elv.getStatus();
				if (status.equals("EMR") || status.equals("ALR")
						|| status.equals("CRT") || status.equals("ERR")) {
					html.append("class=\"red\"");
				} else if (status.equals("WRN")) {
					html.append("class=\"yellow\"");
				} else if (status.equals("NTC") || status.equals("INF")
						|| status.equals("DBG")) {
					html.append("class=\"green\"");
				} else {
					html.append("class=\"navy\"");
				}

				html.append('>');

				FormattedNumber fmtd = UnitsFormat.format(
						new UnitNumber(elv.getTimestamp(),
								UnitsConstants.UNIT_DATE,
								UnitsConstants.SCALE_MILLI), request
								.getLocale(), dateSpecs);

				html.append(StringEscapeUtils.escapeHtml(getText(
						elv.getType(),
						new String[] { fmtd.toString(), elv.getDetail(),
								elv.getSubject(), elv.getStatus() })));

				html.append("</li>");
			}

			html.append("</ul>");
		}

		JsonActionContextNG ctx = this.setJSONContext();

		JSONObject details = new JSONObject();
		details.put("id", begin);
		details.put("html", html);

		JSONResult res = new JSONResult(details);
		ctx.setJSONResult(res);

		inputStream = this.streamJSONResult(ctx);
		return null;
	}
}
