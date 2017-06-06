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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.units.FormattedNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("compareMetricsFormPrepareActionNG")
public class CompareMetricsFormPrepareActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory
			.getLog(CompareMetricsFormPrepareActionNG.class.getName());
	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
	private MeasurementBoss measurementBoss;

	protected void prepareForm(HttpServletRequest request,
			MetricsControlFormNG form, MetricRange range)
			throws InvalidOptionException {
		WebUser user = SessionUtils.getWebUser(request.getSession());

		// set metric range defaults
		Map<String, Object> pref = user.getMetricRangePreference(true);
		form.setReadOnly((Boolean) pref.get(MonitorUtilsNG.RO));
		form.setRn((Integer) pref.get(MonitorUtilsNG.LASTN));
		form.setRu((Integer) pref.get(MonitorUtilsNG.UNIT));

		Long begin, end;

		if (range != null) {
			begin = range.getBegin();
			end = range.getEnd();
		} else {
			begin = (Long) pref.get(MonitorUtilsNG.BEGIN);
			end = (Long) pref.get(MonitorUtilsNG.END);
		}

		form.setRb(begin);
		form.setRe(end);

		form.populateStartDate(new Date(begin.longValue()), request.getLocale());
		form.populateEndDate(new Date(end.longValue()), request.getLocale());

		Boolean readOnly = (Boolean) pref.get(MonitorUtilsNG.RO);
		if (readOnly.booleanValue()) {
			form.setA(MetricDisplayRangeFormNG.ACTION_DATE_RANGE);
		} else {
			form.setA(MetricDisplayRangeFormNG.ACTION_LASTN);
		}
	}

	private Map<String, Map<MeasurementTemplate, List<MetricDisplaySummary>>> mapCategorizedMetrics(
			Map<MeasurementTemplate, List<MetricDisplaySummary>> metrics) {
		Map<String, Map<MeasurementTemplate, List<MetricDisplaySummary>>> returnMap = new LinkedHashMap<String, Map<MeasurementTemplate, List<MetricDisplaySummary>>>();
		for (int i = 0; i < MeasurementConstants.VALID_CATEGORIES.length; i++) {
			Map<MeasurementTemplate, List<MetricDisplaySummary>> categoryMetrics = getMetricsByCategory(
					metrics, MeasurementConstants.VALID_CATEGORIES[i]);
			if (categoryMetrics.size() > 0)
				returnMap.put(MeasurementConstants.VALID_CATEGORIES[i],
						categoryMetrics);
		}
		return returnMap;
	}

	// returns a "sub map" with entries that match the category
	private Map<MeasurementTemplate, List<MetricDisplaySummary>> getMetricsByCategory(
			Map<MeasurementTemplate, List<MetricDisplaySummary>> metrics,
			String category) {
		Map<MeasurementTemplate, List<MetricDisplaySummary>> returnMap = new HashMap<MeasurementTemplate, List<MetricDisplaySummary>>();
		for (Map.Entry<MeasurementTemplate, List<MetricDisplaySummary>> entry : metrics
				.entrySet()) {
			MeasurementTemplate mt = entry.getKey();
			if (mt.getCategory().getName().equals(category)) {
				List<MetricDisplaySummary> metricList = entry.getValue();
				returnMap.put(mt, metricList);
			}
		}
		return returnMap;
	}

	private void formatComparisonMetrics(
			Map<MeasurementTemplate, List<MetricDisplaySummary>> metrics,
			Locale userLocale) {
		for (Map.Entry<MeasurementTemplate, List<MetricDisplaySummary>> entry : metrics
				.entrySet()) {

			MeasurementTemplate mt = entry.getKey();
			List<MetricDisplaySummary> metricList = entry.getValue();
			if (metricList == null) {
				// apparently, there may be meaurement templates populated but
				// none of the included resources are config'd for it, so
				// instead of being a zero length list, it's null
				if (log.isTraceEnabled())
					log.trace(mt.getAlias() + " had no resources configured "
							+ "for it in the included map of metrics");
				continue;
			}
			for (MetricDisplaySummary mds : metricList) {

				// the formatting subsystem doesn't interpret
				// units set to empty strings as "no units" so
				// we'll explicity set it so
				if (mds.getUnits().length() < 1) {
					mds.setUnits(MeasurementConstants.UNITS_NONE);
				}
				FormattedNumber[] fs = UnitsConvert
						.convertSame(mds.getMetricValueDoubles(),
								mds.getUnits(), userLocale);
				String[] keys = mds.getMetricKeys();
				for (int i = 0; i < keys.length; i++) {
					mds.getMetric(keys[i]).setValueFmt(fs[i]);
				}
			}
		}
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		request = getServletRequest();
		try {

			CompareMetricsFormNG cform = (request
					.getAttribute("CompareMetricsForm") == null) ? new CompareMetricsFormNG()
					: (CompareMetricsFormNG) request
							.getAttribute("CompareMetricsForm");

			MetricsControlActionNG metricsControlActionNG = (MetricsControlActionNG) appContext
							.getBean("metricsControlActionNG");
			try {
				metricsControlActionNG.doExecute(getServletRequest());
			} catch (SessionTimeoutException e) {
				log.error(e);
			} catch (SessionNotFoundException e) {
				log.error(e);
			} catch (ApplicationException e) {
				log.error(e);
			}
			WebUser user = RequestUtils.getWebUser(request.getSession());
			int sessionId = user.getSessionId().intValue();
			Map<String, Object> range = user.getMetricRangePreference();

			long begin = ((Long) range.get(MonitorUtilsNG.BEGIN)).longValue();
			long end = ((Long) range.get(MonitorUtilsNG.END)).longValue();

			// assemble the ids, making sure none are duplicated
			Integer[] raw = cform.getR();
			ArrayList<Integer> cooked = new ArrayList<Integer>();
			HashMap<Integer, Integer> idx = new HashMap<Integer, Integer>();
			for (int i = 0; i < raw.length; i++) {
				Integer val = raw[i];
				if (idx.get(val) == null) {
					cooked.add(val);
					idx.put(val, val);
				}
			}
			Integer[] rids = (Integer[]) cooked.toArray(new Integer[cooked
					.size()]);

			AppdefEntityID[] entIds = new AppdefEntityID[rids.length];
			for (int i = 0; i < rids.length; i++) {
				entIds[i] = new AppdefEntityID(
						cform.getAppdefType().intValue(), rids[i]);
			}

			try {

				Map<MeasurementTemplate, List<MetricDisplaySummary>> metrics = measurementBoss
						.findResourceMetricSummary(sessionId, entIds, begin,
								end);

				formatComparisonMetrics(metrics, request.getLocale());
				cform.setMetrics(mapCategorizedMetrics(metrics));
			} catch (Exception e) {
				log.error(e);
			}

			MetricRange mr = new MetricRange(new Long(begin), new Long(end));
			prepareForm(request, cform, mr);

		} catch (ServletException e) {
			log.error(e);
		}

	}
}
