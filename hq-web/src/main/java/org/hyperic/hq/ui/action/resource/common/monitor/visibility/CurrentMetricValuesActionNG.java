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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate
 * display of the various pages that provide metrics summaries.
 */
@Component("jsonCurrentMetricValuesActionNG")
@Scope("prototype")
public class CurrentMetricValuesActionNG extends BaseActionNG {

	private final Log log = LogFactory.getLog(CurrentMetricValuesActionNG.class
			.getName());

	@Autowired
	private MeasurementBoss measurementBoss;

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * Retrieve data needed to display a Metrics Display Form. Respond to
	 * certain button clicks that alter the form display.
	 */
	public String execute() throws Exception {

		AppdefEntityID[] entityIds = null;
		AppdefEntityTypeID ctype = null;
		try {
			entityIds = RequestUtils.getEntityIds(getServletRequest());
			ctype = RequestUtils.getChildResourceTypeId(getServletRequest());
		} catch (ParameterNotFoundException e) {
			if (entityIds == null) // Platform autogroup
				ctype = RequestUtils
						.getChildResourceTypeId(getServletRequest());
		}catch (InvalidAppdefTypeException e) {
			log.error(e);
			return null;
		}

		Long begin = null, end = null;

		// get the "metric range" user pref
		WebUser user = RequestUtils.getWebUser(getServletRequest());
		Map<String, Object> range = user.getMetricRangePreference();
		if (range != null) {
			begin = (Long) range.get(MonitorUtilsNG.BEGIN);
			end = (Long) range.get(MonitorUtilsNG.END);
		} else {
			log.error("no appropriate display range begin and end");
		}

		int sessionId = RequestUtils.getSessionId(getServletRequest())
				.intValue();

		Map<String, Set<MetricDisplaySummary>> metrics;

		if (ctype == null) {
			metrics = measurementBoss.findMetrics(sessionId, entityIds,
					MeasurementConstants.FILTER_NONE, null, begin.longValue(),
					end.longValue(), false);
		} else {
			if (null == entityIds) {
				metrics = measurementBoss.findAGPlatformMetricsByType(
						sessionId, ctype, begin.longValue(), end.longValue(),
						false);
			} else {
				metrics = measurementBoss.findAGMetricsByType(sessionId,
						entityIds, ctype, MeasurementConstants.FILTER_NONE,
						null, begin.longValue(), end.longValue(), false);
			}
		}

		if (metrics != null) {
			MonitorUtilsNG.formatMetrics(metrics, getServletRequest()
					.getLocale());

			JsonActionContextNG ctx = this.setJSONContext();
			// Create an array list of map objects for the attributes
			JSONArray objects = new JSONArray();
			for (Iterator<Set<MetricDisplaySummary>> it = metrics.values()
					.iterator(); it.hasNext();) {
				Collection<MetricDisplaySummary> metricList = it.next();
				for (Iterator<MetricDisplaySummary> m = metricList.iterator(); m
						.hasNext();) {
					MetricDisplaySummary mds = m.next();
					JSONObject values = new JSONObject();
					values.put("mid", mds.getTemplateId());
					values.put("alertCount", new Integer(mds.getAlertCount()));
					values.put("oobCount", new Integer(mds.getOobCount()));
					values.put("min", mds.getMinMetric().toString());
					values.put("average", mds.getAvgMetric());
					values.put("max", mds.getMaxMetric());
					values.put("last", mds.getLastMetric());
					objects.put(values);
				}
			}
			JSONObject ajaxJson = new JSONObject();

			ajaxJson.put("objects", objects);
			ajaxJson.put(Constants.AJAX_TYPE, Constants.AJAX_OBJECT);
			ajaxJson.put(Constants.AJAX_ID, "metricsUpdater");

			JSONResult res = new JSONResult(ajaxJson);
			ctx.setJSONResult(res);

			inputStream = this.streamJSONResult(ctx);

		} else {
			log.trace("no metrics were returned by getMetrics(...)");
		}

		return null;
	}
}
