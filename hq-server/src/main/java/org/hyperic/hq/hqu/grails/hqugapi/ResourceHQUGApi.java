package org.hyperic.hq.hqu.grails.hqugapi;

import java.util.List;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceSortField;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.context.Bootstrap;

/**
 * Middleware api to access managed resources.
 */
public class ResourceHQUGApi extends BaseHQUGApi {
	
	private ResourceManager resourceMan = Bootstrap.getBean(ResourceManager.class);
    private PlatformManager platMan = Bootstrap.getBean(PlatformManager.class);

	public ResourceHQUGApi() {
		super();
	}

	/**
	 * Returns a list of platforms.
	 * <p>This function takes PageInfo as argument which can be used to
	 * control what is returned and how returned list is sorted.
	 * 
	 * @param pInfo PageInfo to control list paging and sorting
	 * @return List of platform {@link Resource}s
	 */
	public List<Resource> findPlatforms(PageInfo pInfo) {
		return resourceMan.findResourcesOfType(AuthzConstants.authzPlatform, pInfo);
	}
	
	/**
	 * Returns a list of platforms.
	 * <p>This method returns all platforms sorted by resource name.
	 * 
	 * @return List of platform {@link Resource}s
	 */
	public List<Resource> findAllPlatforms() {
		return findPlatforms(PageInfo.getAll(ResourceSortField.NAME, true));
	}
	
	public Platform findPlatformById(int id) {
		try {
			return platMan.findPlatformById(id);
		} catch (PlatformNotFoundException e) {
			return null;
		}
	}
	
	
}
