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

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;


/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate
 * display of the various pages that provide metrics summaries.
 */
public abstract class MetricsDisplayFormPrepareActionNG extends
		MetricsControlFormPrepareActionNG {

	protected final Log log = LogFactory
			.getLog(MetricsDisplayFormPrepareActionNG.class.getName());

	@Override
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		
		
		MetricsDisplayFormNG displayForm = null;
		if(getServletRequest().getAttribute("MetricsControlForm") instanceof MetricsDisplayFormNG){
			displayForm = (MetricsDisplayFormNG) getServletRequest().getAttribute("MetricsControlForm");  
		}else{
			displayForm = new MetricsDisplayFormNG();
		}
		if(getServletRequest().getSession().getAttribute("displayMetrics_showAll") != null){
			displayForm.setShowAll((Boolean) getServletRequest().getSession().getAttribute("displayMetrics_showAll"));
		}
		doExecute(tilesContext, attributeContext, displayForm);
		

	}

	protected MetricsDisplayFormNG doExecute(TilesRequestContext tilesContext,
			AttributeContext attributeContext, MetricsDisplayFormNG displayForm) {
		request  = getServletRequest();
		request.getSession().setAttribute("chartPageUrl",request.getRequestURI() + "?" + request.getQueryString());
		displayForm.setShowNumberCollecting(getShowNumberCollecting());
		displayForm.setShowBaseline(getShowBaseline());
		displayForm.setShowMetricSource(getShowMetricSource());
		Long begin = null;
		Long end = null;
		AppdefEntityID[] entityIds = null;
		try {
			entityIds = RequestUtils.getEntityIds(getServletRequest());
		} catch (ParameterNotFoundException e) {
			// this is fine until we finish migrating to eids
		}

		try {
			if (begin == null || end == null) {

				// get the "metric range" user pref
				WebUser user = RequestUtils.getWebUser(getServletRequest());
				Map<String, Object> range = user.getMetricRangePreference();
				if (range != null) {
					begin = (Long) range.get(MonitorUtilsNG.BEGIN);
					end = (Long) range.get(MonitorUtilsNG.END);
				} else {
					log.error("no appropriate display range begin and end");
				}
			}

			long filters = MeasurementConstants.FILTER_NONE;
			String keyword = null;

			if (displayForm.isFilterSubmitClicked() || getServletRequest().getSession().getAttribute("displayForm_filters") != null) {
				displayForm.setFilter((int[]) getServletRequest().getSession().getAttribute("displayForm_filters"));
				displayForm.setKeyword( (String) getServletRequest().getSession().getAttribute("displayForm_keyword"));
				filters = displayForm.getFilters();
				keyword = displayForm.getKeyword();
			}

			Map<String, Set<MetricDisplaySummary>> metrics = getMetrics(
					getServletRequest(), entityIds, filters, keyword, begin,
					end, displayForm.getShowAll());

			if (metrics != null) {
				Integer resourceCount = MonitorUtilsNG.formatMetrics(metrics,
						getServletRequest().getLocale());

				tilesContext.getRequestScope().put(Constants.NUM_CHILD_RESOURCES_ATTR,
						resourceCount);
				tilesContext.getRequestScope().put(Constants.METRIC_SUMMARIES_ATTR, metrics);

				// populate the form
				displayForm.setupCategoryList(metrics);

			} else {
				log.trace("no metrics were returned by getMetrics(...)");
			}

			// Clear any compare metric workflow
			SessionUtils.clearWorkflow(getServletRequest().getSession(),
					Constants.WORKFLOW_COMPARE_METRICS_NAME);
			super.execute(tilesContext, attributeContext, displayForm);
			getServletRequest().setAttribute("metricsDisplayForm", displayForm);
		} catch (Exception e) {
			log.error(e);
		}
		
		return displayForm;
	}

	// ---------------------------------------------------- Protected Methods

	/**
	 * Do we show the number collecting column on this page? The default answer
	 * is no, but subclasses can specify otherwise.
	 */
	protected Boolean getShowNumberCollecting() {
		return Boolean.FALSE;
	}

	/**
	 * Do we show the baseline column on this page? The default answer is no,
	 * but subclasses can specify otherwise.
	 */
	protected Boolean getShowBaseline() {
		return Boolean.FALSE;
	}

	/**
	 * Do we show the metric source column on this page? The default answer is
	 * no, but subclasses can specify otherwise.
	 */
	protected Boolean getShowMetricSource() {
		return Boolean.FALSE;
	}

	/**
	 * Get from the Bizapp the set of metric summaries for the specified entity
	 * that will be displayed on the page. Returns a <code>Map</code> keyed by
	 * metric category.
	 * 
	 * @param request
	 *            the http request
	 * @param entityId
	 *            the entity id of the currently viewed resource
	 * @param begin
	 *            the time (in milliseconds since the epoch) that begins the
	 *            timeframe for which the metrics are summarized
	 * @param end
	 *            the time (in milliseconds since the epoch) that ends the
	 *            timeframe for which the metrics are summarized
	 * @return Map keyed on the category (String), values are List's of
	 *         MetricDisplaySummary beans
	 */
	protected abstract Map<String, Set<MetricDisplaySummary>> getMetrics(
			HttpServletRequest request, AppdefEntityID entityId, long filters,
			String keyword, Long begin, Long end, boolean showAll)
			throws Exception;

	/**
	 * Get from the Bizapp the set of metric summaries for the specified
	 * entities that will be displayed on the page. Returns a <code>Map</code>
	 * keyed by metric category.
	 * 
	 * @param request
	 *            the http request
	 * @param entityId
	 *            the entity id of the currently viewed resource
	 * @param begin
	 *            the time (in milliseconds since the epoch) that begins the
	 *            timeframe for which the metrics are summarized
	 * @param end
	 *            the time (in milliseconds since the epoch) that ends the
	 *            timeframe for which the metrics are summarized
	 * @return Map keyed on the category (String), values are List's of
	 *         MetricDisplaySummary beans
	 */
	protected Map<String, Set<MetricDisplaySummary>> getMetrics(
			HttpServletRequest request, AppdefEntityID[] entityIds,
			long filters, String keyword, Long begin, Long end, boolean showAll)
			throws Exception {
		// XXX when we finish migrating to eids, the other form of
		// getMetrics will go away and this one will become abstract
		// in the meantime, preserve friggin backwards compatibility
		if (entityIds != null && entityIds.length == 1) {
			return getMetrics(request, entityIds[0], filters, keyword, begin,
					end, showAll);
		}
		return null;
	}
}
