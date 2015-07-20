package org.hyperic.hq.ui.action.portlet.summaryCounts;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;
@Component("summaryCountsPrepareActionNG")
public class PrepareActionNG extends BaseActionNG implements ViewPreparer {
	@Resource
	private AuthzBoss authzBoss;
    @Resource
	private AppdefBoss appdefBoss;
    @Resource
    private DashboardManager dashboardManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
			this.request = getServletRequest();
			
			PropertiesFormNG pForm = new PropertiesFormNG();

	        HttpSession session = request.getSession();
	        WebUser user = null;
			try {
				user = RequestUtils.getWebUser(session);
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        ConfigResponse dashPrefs = dashConfig.getConfig();

	        boolean application = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.application")).booleanValue();
	        boolean platform = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.platform")).booleanValue();
	        boolean server = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.server")).booleanValue();
	        boolean service = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.service")).booleanValue();
	        boolean cluster = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.cluster")).booleanValue();

	        boolean groupMixed = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.mixed")).booleanValue();
	        boolean groupGroups = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.groups")).booleanValue();
	        boolean groupPlatServerService = new Boolean(dashPrefs
	            .getValue(".dashContent.summaryCounts.group.plat.server.service")).booleanValue();
	        boolean groupApplication = new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.group.application"))
	            .booleanValue();

	        pForm.setApplication(application);
	        pForm.setCluster(cluster);
	        pForm.setPlatform(platform);
	        pForm.setServer(server);
	        pForm.setService(service);

	        pForm.setGroupMixed(groupMixed);
	        pForm.setGroupGroups(groupGroups);
	        pForm.setGroupPlatServerService(groupPlatServerService);
	        pForm.setGroupApplication(groupApplication);

	        String[] applicationTypes = null;
			try {
				applicationTypes = getStringArray(".dashContent.summaryCounts.applicationTypes", dashPrefs);
			
				String[] platformTypes = getStringArray(".dashContent.summaryCounts.platformTypes", dashPrefs);
		        String[] serverTypes = getStringArray(".dashContent.summaryCounts.serverTypes", dashPrefs);
		        String[] serviceTypes = getStringArray(".dashContent.summaryCounts.serviceTypes", dashPrefs);
		        String[] clusterTypes = getStringArray(".dashContent.summaryCounts.group.clusterTypes", dashPrefs);
		        pForm.setApplicationTypes(applicationTypes);
		        
		        pForm.setClusterTypes(clusterTypes);
		        
				pForm.setPlatformTypes(platformTypes);
		        
				pForm.setServerTypes(serverTypes);
		        
				pForm.setServiceTypes(serviceTypes);
				  
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     
			 	
	
	       
		
		       
		        AppdefInventorySummary summary = null;
			try {
				summary = appdefBoss.getInventorySummary(user.getSessionId().intValue(), true);
			} catch (SessionNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SessionTimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			request.setAttribute("filter", pForm);
	        request.setAttribute("summary", summary);

	        
		// TODO Auto-generated method stub
		
	}
	
	private String[] getStringArray(String preference, ConfigResponse config) throws Exception {
        List<String> preferences = StringUtil.explode(config.getValue(preference), ",");

        int element;
        Iterator<String> i;

        String[] array = new String[preferences.size()];

        for (i = preferences.iterator(), element = 0; i.hasNext(); element++) {
            array[element] = i.next();
        }

        return array;

    }

}
