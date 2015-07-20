package org.hyperic.hq.ui.action.portlet.metricviewer;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("metricViewerPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(PrepareActionNG.class);

	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private AppdefBoss appdefBoss;
	@Resource
	private MeasurementBoss measurementBoss;
	@Resource
	private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		try {

			this.request = getServletRequest();
			HttpSession session = request.getSession();
			WebUser user = RequestUtils.getWebUser(session);
			int sessionId = user.getSessionId().intValue();

			DashboardConfig dashConfig = dashboardManager.findDashboard(
					(Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, authzBoss);
			ConfigResponse dashPrefs = dashConfig.getConfig();

			// this quarantees that the session dosen't contain any resources it
			// shouldnt
			SessionUtils.removeList(session,
					Constants.PENDING_RESOURCES_SES_ATTR);

			// String token = pForm.getToken();
			String token = "";
			// For multi-portlet configuration
			String numKey = PropertiesFormNG.NUM_TO_SHOW;
			String resKey = PropertiesFormNG.RESOURCES;
			String resTypeKey = PropertiesFormNG.RES_TYPE;
			String metricKey = PropertiesFormNG.METRIC;
			String descendingKey = PropertiesFormNG.DECSENDING;
			String titleKey = PropertiesFormNG.TITLE;

			if (token != null) {
				numKey += token;
				resKey += token;
				resTypeKey += token;
				metricKey += token;
				descendingKey += token;
				titleKey += token;
			}

			// We set defaults here rather than in
			// DefaultUserPreferences.properites
			Integer numberToShow = new Integer(dashPrefs.getValue(numKey, "10"));
			String resourceType = dashPrefs.getValue(resTypeKey, "");
			String metric = dashPrefs.getValue(metricKey, "");
			String descending = dashPrefs.getValue(descendingKey, "true");

			request.setAttribute("titleDescription",
					dashPrefs.getValue(titleKey, ""));
			request.setAttribute("numberToShow", numberToShow);
			request.setAttribute("metric", metric);
			request.setAttribute("descending", descending);

			List<AppdefEntityID> resourceList = DashboardUtils
					.preferencesAsEntityIds(resKey, dashPrefs);
			AppdefEntityID[] aeids = resourceList
					.toArray(new AppdefEntityID[resourceList.size()]);

			PageControl pc = RequestUtils.getPageControl(request);
			PageList<AppdefResourceValue> resources = appdefBoss.findByIds(
					sessionId, aeids, pc);
			request.setAttribute("descending", descending);
			request.setAttribute("metricViewerList", resources);

			PageList<PlatformTypeValue> viewablePlatformTypes = appdefBoss
					.findViewablePlatformTypes(sessionId, PageControl.PAGE_ALL);
			request.setAttribute("platformTypes", viewablePlatformTypes);
			PageList<ServerTypeValue> viewableServerTypes = appdefBoss
					.findViewableServerTypes(sessionId, PageControl.PAGE_ALL);
			request.setAttribute("serverTypes", viewableServerTypes);
			PageList<ServiceTypeValue> viewableServiceTypes = appdefBoss
					.findViewableServiceTypes(sessionId, PageControl.PAGE_ALL);
			request.setAttribute("serviceTypes", viewableServiceTypes);

			AppdefResourceTypeValue typeVal = null;
			if (resourceType == null || resourceType.length() == 0) {
				if (viewablePlatformTypes.size() > 0) {
					// Take the first platform type
					typeVal = viewablePlatformTypes.get(0);
				} else if (viewableServerTypes.size() > 0) {
					// Take the first server type
					typeVal = viewableServerTypes.get(0);
				} else if (viewableServiceTypes.size() > 0) {
					// Take the first service type
					typeVal = viewableServiceTypes.get(0);
				}
			} else {
				AppdefEntityTypeID typeId = new AppdefEntityTypeID(resourceType);
				typeVal = appdefBoss.findResourceTypeById(sessionId, typeId);
			}

			if (typeVal != null) {
				request.setAttribute("resourceType", typeVal.getAppdefTypeKey());
				List<MeasurementTemplate> metrics = measurementBoss
						.findMeasurementTemplates(sessionId, typeVal.getName(),
								PageControl.PAGE_ALL);
				request.setAttribute("metrics", getDefualtMetricTemplates(metrics));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

	}
	
	private List<MeasurementTemplate> getDefualtMetricTemplates(List<MeasurementTemplate> allMetricTemplate) {
		
		List<MeasurementTemplate> result = new ArrayList<MeasurementTemplate>();
		for (MeasurementTemplate element   : allMetricTemplate){
			if (element.isDefaultOn()){
				result.add(element);
			}
			if (element.getId() == 10787) {
				log.debug(element.toString());
			}
		}
		return result;
	}

}
