package org.hyperic.hq.web.representations;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.Constants;
import org.hyperic.util.config.ConfigResponse;

/**
 * This class wraps the existing ConfigResponse object encapsulating the logic that was formerly exposed in the DashboardUtils
 * as static methods. Using this abstraction removes a lot of the logic that was formerly contained in the action/controller. 
 * 
 * @author David Crutchfield
 *
 */
public class UserPreference extends ConfigResponse {
	private final static String RECENTLY_VIEWED_RESOURCES_LIST = Constants.USERPREF_KEY_RECENT_RESOURCES;
	private final static String FAVORITE_RESOURCES_LIST = Constants.USERPREF_KEY_FAVORITE_RESOURCES;
	private final static String RESOURCE_ID_STRING_DELIMITER = ",";
	
	public void addResourceToRecentlyViewedList(Resource resource) {
		
	}
	
	public List<Resource> getRecentlyViewedList() {
		List<Resource> result = new ArrayList<Resource>();
		String resourceList = getValue(RECENTLY_VIEWED_RESOURCES_LIST);
		ResourceManager resourceManager = Bootstrap.getBean(ResourceManager.class);
		
		if (resourceList != null) {
			String[] resourceIds = resourceList.split(RESOURCE_ID_STRING_DELIMITER);
			
			
		}
		
		return result;
	}
	
	public void addResourceToFavoriteResourcesList(Resource resource) {
		
	}
	
	public List<Resource> getFavoriteResourcesList() {
		return new ArrayList<Resource>();
	}
}