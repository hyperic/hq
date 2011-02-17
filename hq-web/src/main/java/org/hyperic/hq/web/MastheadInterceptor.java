package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

public class MastheadInterceptor extends BaseController implements HandlerInterceptor {
	private ProductBoss productBoss;
	
	@Autowired
	public MastheadInterceptor(AppdefBoss appdefBoss, AuthzBoss authzBoss, ProductBoss productBoss) {
		super(appdefBoss, authzBoss);
		
		this.productBoss = productBoss;
	}

	public boolean preHandle(HttpServletRequest request, 
			HttpServletResponse response, Object handler) throws Exception {
		return true;
	}

	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		WebUser user = getWebUser(request.getSession());
		Collection<AttachmentDescriptor> mastheadAttachments = productBoss.findAttachments(user.getSessionId(), AttachType.MASTHEAD);
	    List<AttachmentDescriptor> resourceAttachments = new ArrayList<AttachmentDescriptor>();
	    List<AttachmentDescriptor> trackerAttachments = new ArrayList<AttachmentDescriptor>();
	    
	    for (AttachmentDescriptor d : mastheadAttachments) {
	    	AttachmentMasthead attachment = (AttachmentMasthead) d.getAttachment();
	        
	    	if (attachment.getCategory().equals(ViewMastheadCategory.RESOURCE)) {
	            resourceAttachments.add(d);
	        } else if (attachment.getCategory().equals(ViewMastheadCategory.TRACKER)) {
	            trackerAttachments.add(d);
	        }
	    }

	    modelAndView.addObject("mastheadResourceAttachments", resourceAttachments);
	    modelAndView.addObject("mastheadTrackerAttachments", trackerAttachments);

        ConfigResponse userPrefs = user.getPreferences();
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;
        
        if (userPrefs.getValue(key, null) != null) {
            Map<AppdefEntityID, Resource> list;
            
            try {
                list = getStuff(key, user, userPrefs);
            } catch (Exception e) {
            	ServletContext servletContext = RequestContextUtils.getWebApplicationContext(request).getServletContext();
            	
                DashboardUtils.verifyResources(key, servletContext, userPrefs, user, getAppdefBoss(), getAuthzBoss());
                
                list = getStuff(key, user, userPrefs);
            }

            modelAndView.addObject("resources", list);
        } else {
        	modelAndView.addObject("resources", new ArrayList());
        }
    }

	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
	}

    private Map<AppdefEntityID, Resource> getStuff(String key, WebUser user, ConfigResponse dashPrefs) throws Exception {
        List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
        Collections.reverse(entityIds); // Most recent on top

        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = entityIds.toArray(arrayIds);

        return getAuthzBoss().findResourcesByIds(user.getSessionId().intValue(), arrayIds);
	}
}

