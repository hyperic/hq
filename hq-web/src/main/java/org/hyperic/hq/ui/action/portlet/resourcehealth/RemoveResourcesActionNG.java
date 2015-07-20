package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("resourceHealthRemoveResourcesActionNG")
public class RemoveResourcesActionNG extends BaseActionNG {

	@Resource
	private AppdefBoss appdefBoss;
	
	
	public String execute() throws Exception {
        this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);

        List<String> resourceList = user.getPreferenceAsList(Constants.USERPREF_KEY_FAVORITE_RESOURCES_NG, Constants.DASHBOARD_DELIMITER);

        PageList<AppdefResourceValue> resources 
            = new PageList<AppdefResourceValue>(DashboardUtils.listAsResources(resourceList, user, appdefBoss), resourceList.size());

        request.setAttribute(Constants.RESOURCE_HEALTH_LIST, resources);		
		return SUCCESS;
	}
}
