package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.CheckPermissionsUtil;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.units.FormattedNumber;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component("resourceHealthViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewActionNG.class);
	
	@Resource
	private AuthzBoss authzBoss;

	@Resource
	private AppdefBoss appdefBoss;

	@Resource
	private EventsBoss eventsBoss;

	@Resource
	private MeasurementBoss measurementBoss;

	@Resource
	private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		
		try {
			this.request = getServletRequest();
			HttpSession session = request.getSession();
			WebUser user = SessionUtils.getWebUser(session);
			DashboardConfig dashConfig = dashboardManager
					.findDashboard((Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
							authzBoss);
	
			if (dashConfig == null) {
				// return new ActionRedirect("/Dashboard.do");
				log.error("missing dashConfig for key " + Constants.SELECTED_DASHBOARD_ID);
				return;
			}
	
			ConfigResponse dashPrefs = dashConfig.getConfig();
	
			String key = Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG;
	
			// First determine what entityIds can be viewed by this user
			// This code probably should be in the boss somewhere but
			// for now doing it here...
			List<AppdefEntityID> entityIds = CheckPermissionsUtil
					.filterEntityIdsByViewPermission(
							RequestUtils.getSessionId(request).intValue(),
							DashboardUtils.preferencesAsEntityIds(key, dashPrefs),
							appdefBoss);
	
			AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
			arrayIds = entityIds.toArray(arrayIds);
	
			List<ResourceDisplaySummary> list;
			int sessionID = user.getSessionId().intValue();
			try {
				list = measurementBoss.findResourcesCurrentHealth(sessionID,
						arrayIds);
			} catch (Exception e) {
				DashboardUtils.verifyResources(key, dashPrefs, user, appdefBoss,
						authzBoss);
				list = measurementBoss.findResourcesCurrentHealth(sessionID,
						arrayIds);
			}
	
			// Get alert counts for each resource
			Map<AppdefEntityID, Integer> alertsCount = eventsBoss
					.getAlertCountMapped(sessionID, arrayIds);
	
			// Due to the complexity of the UIBeans, we need to construct the
			// JSON objects by hand.
			JSONObject favorites = new JSONObject();
	
			List<JSONObject> resources = new ArrayList<JSONObject>();
	
			for (ResourceDisplaySummary bean : list) {
				JSONObject res = new JSONObject();
	
				res.put("resourceName",
						HtmlUtils.htmlEscape(bean.getResourceName()));
				res.put("resourceTypeName", bean.getResourceTypeName());
				res.put("resourceTypeId", bean.getResourceTypeId());
				res.put("resourceId", bean.getResourceId());
				res.put("performance",
						getFormattedValue(bean.getPerformance(),
								bean.getPerformanceUnits()));
				res.put("throughput",
						getFormattedValue(bean.getThroughput(),
								bean.getThroughputUnits()));
				res.put("availability", getAvailString(bean.getAvailability()));
				res.put("monitorable", bean.getMonitorable());
				Integer alertsNum = alertsCount.get(bean.getEntityId());
				if (null == alertsNum) {
					alertsNum = 0;
				}
				res.put("alerts", alertsNum);
				resources.add(res);
			}
			if (resources.size() > 0 ) {
				favorites.put("favorites", resources);
			}
	
			this.response = ServletActionContext.getResponse();
			
			// response.getWriter().write(favorites.toString());
			
		} catch (Exception ex) {
			log.error("missing dashConfig for key " + Constants.SELECTED_DASHBOARD_ID);
		}

	}

	private String getFormattedValue(Double value, String units) {
		if (value != null) {
			FormattedNumber fn = UnitsConvert.convert(value.doubleValue(),
					units);
			return fn.toString();
		}
		return null;
	}

	/**
	 * Get the availability string for the given metric value. The returned
	 * string should match the availabilty icon filenames.
	 * 
	 * @param availability
	 *            The availability metric value.
	 * @return The mapped string for the given availablity metric. If the given
	 *         metric is not valid, unknown is returned.
	 */
	private String getAvailString(Double availability) {
		if (availability != null) {
			double avail = availability.doubleValue();

			if (avail == MeasurementConstants.AVAIL_UP) {
				return "green";
			} else if (avail == MeasurementConstants.AVAIL_DOWN) {
				return "red";
			} else if (avail == MeasurementConstants.AVAIL_PAUSED) {
				return "orange";
			} else if (avail == MeasurementConstants.AVAIL_POWERED_OFF) {
				return "black";
			} else if (avail > MeasurementConstants.AVAIL_DOWN
					&& avail < MeasurementConstants.AVAIL_UP) {
				return "yellow";
			}
		}
		return "unknown";
	}

}
