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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.MonitorUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An <code>TilesAction</code> that retrieves metric data to facilitate display
 * of a current health page. Concrete subclasses provide methods to retrieve
 * resource-type-specific data.
 */
@Component("platformCurrentHealthPrepareActionNG")
public class CurrentHealthActionNG extends BaseActionNG implements ViewPreparer {

	protected final Log log = LogFactory.getLog(CurrentHealthActionNG.class
			.getName());
	@Autowired
	protected MeasurementBoss measurementBoss;

	private final PageControl pc = new PageControl(0,
			Constants.DEFAULT_CHART_POINTS);

	protected String getFormattedAvailability(double values) {
		UnitNumber average = new UnitNumber(values,
				UnitsConstants.UNIT_PERCENTAGE);
		return UnitsFormat.format(average).toString();
	}

	protected String getDefaultViewName() {

		return getText(Constants.DEFAULT_INDICATOR_VIEW);
	}

	protected void setupViews(HttpServletRequest request,
			IndicatorViewsFormNG ivf, String key) {
		WebUser user = SessionUtils.getWebUser(request.getSession());

		String[] views;
		// Try to get the view names from user preferences
		try {
			String viewsPref = user.getPreference(Constants.INDICATOR_VIEWS
					+ key);
			StringTokenizer st = new StringTokenizer(viewsPref,
					Constants.DASHBOARD_DELIMITER);

			views = new String[st.countTokens()];
			for (int i = 0; st.hasMoreTokens(); i++)
				views[i] = st.nextToken();
		} catch (InvalidOptionException e) {
			views = new String[] { getDefaultViewName() };
		}

		ivf.setViews(views);
		String viewName = request.getSession().getAttribute(
				Constants.PARAM_VIEW) == null ? null : (String) request
				.getSession().getAttribute(Constants.PARAM_VIEW);
		if (viewName == null) {
			viewName = RequestUtils.getStringParameter(request,
					Constants.PARAM_VIEW, views[0]);
		}

		// Make sure that the view name is one of the views
		boolean validated = false;
		for (int i = 0; i < views.length; i++) {
			if (validated = viewName.equals(views[i]))
				break;
		}

		if (!validated)
			viewName = views[0];

		ivf.setView(viewName);
	}

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		doExecute(tilesContext,attributeContext,null);

	}

	public void doExecute(TilesRequestContext tilesContext,AttributeContext attributeContext,IndicatorViewsFormNG indicatorViewsParam) {
		request  = getServletRequest();
		request.getSession().setAttribute("chartPageUrl",request.getRequestURI() + "?" + request.getQueryString());
		
		final boolean debug = log.isDebugEnabled();
		AppdefEntityID aeid = null;
		IndicatorViewsFormNG indicatorViewsForm = (indicatorViewsParam == null) ? new IndicatorViewsFormNG():indicatorViewsParam ;
		try {
			StopWatch watch = new StopWatch();
			AppdefResourceValue resource = RequestUtils
					.getResource(getServletRequest());

			if (resource == null) {
				addActionError(Constants.ERR_RESOURCE_NOT_FOUND);
				return;
			}

			aeid = resource.getEntityId();

			ServletContext ctx = getServletRequest().getSession()
					.getServletContext();
			// Check configuration
			InventoryHelper helper = InventoryHelper.getHelper(aeid);
			helper.isResourceConfigured(getServletRequest(), ctx, true);

			// Set the views
			
			if(request.getSession().getAttribute(Constants.PARAM_VIEW) != null){
				indicatorViewsForm.setView((String)request.getSession().getAttribute(Constants.PARAM_VIEW));
			}
			if(indicatorViewsParam == null){
				setupViews(getServletRequest(), indicatorViewsForm,
						aeid.getAppdefKey());
			}

			// Get the resource availability
			int sessionId = RequestUtils.getSessionId(getServletRequest())
					.intValue();

			WebUser user = RequestUtils.getWebUser(getServletRequest());

			MeasurementTemplate mt = measurementBoss
					.getAvailabilityMetricTemplate(sessionId, aeid);

			Map<String, Object> pref = user.getMetricRangePreference(true);
			long begin = ((Long) pref.get(MonitorUtilsNG.BEGIN)).longValue();
			long end = ((Long) pref.get(MonitorUtilsNG.END)).longValue();
			long interval = TimeUtil.getInterval(begin, end, Constants.DEFAULT_CHART_POINTS - 1);

			// fixed the UC when the user trying to see future chart data
			// since such data does not exist:  
			// 1) end date is limited to Now  
			// 2) the begin date taken 25 hours back 
			// 3) the interval recalculated accordingly 
			// 4) whole data saved back to user preferences
			long curTime = new Date().getTime();
			if(begin > curTime || end > curTime){
				begin = curTime - 1000*60*60*25;
				end = curTime;
				interval = TimeUtil.getInterval(begin, end, Constants.DEFAULT_CHART_POINTS - 1);
				updateRange(user, begin, end);
			}
			
			
			if(begin > end){			
				updateRange(user, begin, end);
				pref = user.getMetricRangePreference(true);
				begin = ((Long) pref.get(MonitorUtilsNG.BEGIN)).longValue();
				end = ((Long) pref.get(MonitorUtilsNG.END)).longValue();
			}
			List<HighLowMetricValue> data = measurementBoss
					.findMeasurementData(sessionId, aeid, mt, begin, end,
							interval, true, pc);
			final AppdefEntityID[] aeids = new AppdefEntityID[] { aeid };
			double availAvg = measurementBoss.getAvailabilityAverage(aeids,
					begin, end);

			// Seems like sometimes Postgres does not average cleanly for
			// groups, and the value ends up being like 0.9999999999. We don't
			// want the insignificant amount to mess up our display.
			if (aeid.isGroup()) {
				for (MetricValue val : data) {
					if (val.toString().equals("1")) {
						val.setValue(1);
					}
				}
			}

			tilesContext.getSessionScope().put(
					Constants.CAT_AVAILABILITY_METRICS_ATTR, data);
			tilesContext.getSessionScope().put(Constants.AVAIL_METRICS_ATTR,
					getFormattedAvailability(availAvg));

			getServletRequest().setAttribute("IndicatorViewsForm",
					indicatorViewsForm);
			if (debug) {
				log.debug("CurrentHealthAction.execute: " + watch);
			}

		} catch (MeasurementNotFoundException e) {
			tilesContext.getSessionScope().put(
					Constants.CAT_AVAILABILITY_METRICS_ATTR, null);
			getServletRequest().setAttribute("IndicatorViewsForm",
					indicatorViewsForm);
			// No utilization metric
			if (debug) {
				log.debug(MeasurementConstants.CAT_AVAILABILITY
						+ " not found for " + aeid);
			}
		} catch (SessionNotFoundException e) {

			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (AppdefEntityNotFoundException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (EncodingException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		} catch (ServletException e) {
			log.error(e);
		} catch (ApplicationException e) {
			log.error(e);
		}
	}

	private void updateRange(WebUser user, long begin, long end)
			throws ApplicationException, SessionTimeoutException,
			SessionNotFoundException {
		user.setPreference(MonitorUtilsNG.BEGIN, end);
		user.setPreference(MonitorUtilsNG.END, begin);
		List<Long> range = new ArrayList<Long>();
		range.add(end);
		range.add(begin);
		user.setPreference(WebUser.PREF_METRIC_RANGE ,range, Constants.DASHBOARD_DELIMITER);
		authzBoss.setUserPrefs(user.getSessionId(), user.getId(),
				user.getPreferences());
	}
}
