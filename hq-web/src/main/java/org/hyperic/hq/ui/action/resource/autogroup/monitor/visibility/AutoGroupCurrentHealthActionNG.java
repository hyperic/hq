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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.visibility;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.CurrentHealthActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.IndicatorViewsFormNG;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.InventoryHelper;
import org.hyperic.hq.ui.action.resource.platform.monitor.visibility.RootInventoryHelper;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A <code>TilesAction</code> that retrieves data from the Bizapp to be
 * displayed on an <code>AutoGroup Current Health</code> page. Ths is the only
 * resource type that needs its own CurrentHealthAction due to the different
 * APIs it calls.
 */
@Component("autogroupCurrentHealthPrepareActionNG")
public class AutoGroupCurrentHealthActionNG extends CurrentHealthActionNG {

	private final Log log = LogFactory
			.getLog(AutoGroupCurrentHealthActionNG.class.getName());

	private final PageControl pc = new PageControl(0,
			Constants.DEFAULT_CHART_POINTS);

	@Autowired
	private AppdefBoss appdefBoss;

	@Override
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		ServletContext ctx = getServletRequest().getSession()
				.getServletContext();
		Integer sessionId = null;
		try {
			sessionId = RequestUtils.getSessionId(getServletRequest());
		} catch (ServletException e2) {
			log.error(e2);
			return;
		}

		// There are two possibilities for an auto-group. Either it
		// is an auto-group of platforms, in which case there will be
		// no parent entity ids, or it is an auto-group of servers or
		// services.
		InventoryHelper helper = null;
		AppdefEntityID[] entityIds = null;
		AppdefEntityID typeHolder = null;
		String parentKey;
		try {
			entityIds = RequestUtils.getEntityIds(getServletRequest());
			// if we get this far, we are dealing with an auto-group
			// of servers or services

			// find the resource type of the autogrouped resources
			typeHolder = entityIds[0];
			helper = InventoryHelper.getHelper(typeHolder);

			parentKey = typeHolder.getAppdefKey();
		} catch (ParameterNotFoundException e) {
			// if we get here, we are dealing with an auto-group of
			// platforms
			helper = new RootInventoryHelper(appdefBoss);
			parentKey = "autogroup";
		}

		AppdefEntityTypeID childTypeId;
		try {
			childTypeId = RequestUtils
					.getChildResourceTypeId(getServletRequest());
		} catch (ParameterNotFoundException e1) {
			// must be an autogroup resource type
			// childTypeId = RequestUtils.getAutogroupResourceTypeId(request);
			// REMOVE ME?
			throw e1;
		}

		AppdefResourceType selectedType;
		IndicatorViewsFormNG indicatorViewsFormNG = new IndicatorViewsFormNG();
		try {
			selectedType = helper.getChildResourceType(getServletRequest(),
					ctx, childTypeId);

			getServletRequest().setAttribute(
					Constants.CHILD_RESOURCE_TYPE_ATTR, selectedType);

			
			// Set the views
			setupViews(getServletRequest(), indicatorViewsFormNG, parentKey
					+ "." + childTypeId.getAppdefKey());

			// Get the resource availability

			WebUser user = RequestUtils.getWebUser(getServletRequest());

			try {
				MeasurementTemplate mt = measurementBoss
						.getAvailabilityMetricTemplate(sessionId.intValue(),
								entityIds[0], childTypeId);

				Map<String, Object> pref = user.getMetricRangePreference(true);
				long begin = ((Long) pref.get(MonitorUtilsNG.BEGIN)).longValue();
				long end = ((Long) pref.get(MonitorUtilsNG.END)).longValue();
				long interval = TimeUtil.getInterval(begin, end,
						Constants.DEFAULT_CHART_POINTS);

				List<HighLowMetricValue> data = measurementBoss
						.findAGMeasurementData(sessionId.intValue(), entityIds,
								mt, childTypeId, begin, end, interval, true, pc);
				double availAvg = measurementBoss.getAGAvailabilityAverage(
						sessionId.intValue(), entityIds[0], childTypeId, begin,
						end);
				// Seems like sometimes Postgres does not average cleanly, and
				// the value ends up being like 0.9999999999. We don't want the
				// insignificant amount to mess up our display.
				for (MetricValue val : data) {
					if (val.toString().equals("1")) {
						val.setValue(1);
					}
				}

				getServletRequest().setAttribute(
						Constants.CAT_AVAILABILITY_METRICS_ATTR, data);
				getServletRequest().setAttribute(Constants.AVAIL_METRICS_ATTR,
						getFormattedAvailability(availAvg));
			} catch (MeasurementNotFoundException e) {
				// No utilization metric
				log.debug(MeasurementConstants.CAT_AVAILABILITY
						+ " not found for autogroup" + childTypeId);
			}

		} catch (Exception e1) {
			log.error(e1);
		}
		super.doExecute(tilesContext, attributeContext,indicatorViewsFormNG);
	}

}
