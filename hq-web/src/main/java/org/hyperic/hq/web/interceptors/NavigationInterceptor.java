package org.hyperic.hq.web.interceptors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.web.representations.MenuItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * This class is responsible for populating the ModelAndView with data used to render 
 * links to HQU plugins in the header tabs
 * 
 * @author David Crutchfield
 *
 */
public class NavigationInterceptor extends HandlerInterceptorAdapter {
	private final static String DASHBOARD_URL = "/Dashboard.do";
	private final static String RESOURCE_HUB_URL = "/ResourceHub.do";
	private final static String ADMIN_URL = "/Admin.do";
	private final static String PLUGIN_URL = "/mastheadAttach.do?typeId=";
	
	private ProductBoss productBoss;

	@Autowired
	public NavigationInterceptor(ProductBoss productBoss) {
		this.productBoss = productBoss;
	}
	
	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// ...get the user's session id...
		HttpSession session = request.getSession();
        WebUser webUser = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
        int sessionId = webUser.getSessionId();

        // ...setup the menu items...
        Map<String, MenuItem> menu = new LinkedHashMap<String, MenuItem>();
        
        menu.put("dashboard", createDashboardMenuItem());
        
        // ...now find all plugins that should be attached to the resource head...
        Collection<AttachmentDescriptor> mastheadAttachments = productBoss.findAttachments(sessionId, AttachType.MASTHEAD);
        
        menu.put("resource", createResourceMenuItem(mastheadAttachments));
        menu.put("analyze", createAnalyzeMenuItem(mastheadAttachments));
        
        // ...and finally setup the admin menu item...
        menu.put("admin", createAdminMenuItem());
    
        // ...add it to the modelAndView...
        modelAndView.addObject(menu);
	}
	
	protected MenuItem createBasicMenuItem(String label, String url) {
		MenuItem menuItem = new MenuItem();
		
		menuItem.setLabel(label);
		menuItem.setUrl(url);
		
		return menuItem;
	}
	
	protected List<MenuItem> createPluginSubMenuItems(Collection<AttachmentDescriptor> mastheadAttachments, ViewMastheadCategory category) {
		List<MenuItem> subMenuItems = new ArrayList<MenuItem>();
		
		// ...loop through a setup the resource plugin sub menus...
        for (AttachmentDescriptor descriptor : mastheadAttachments) {
            AttachmentMasthead attachment = (AttachmentMasthead) descriptor.getAttachment();
        
            if (attachment.getCategory().equals(category)) {
            	String url = PLUGIN_URL + attachment.getId();
            	MenuItem subMenuItem = createBasicMenuItem(descriptor.getHTML(), url);
            	
            	subMenuItem.setId(descriptor.getName());
            	subMenuItems.add(subMenuItem);
            }
        }
        
        return subMenuItems;
	}
	
	protected List<MenuItem> createRecentSubMenuItems() {
		List<MenuItem> subMenuItems = new ArrayList<MenuItem>();

		// Make a service call, get recently visited resource, badaboom...
		return subMenuItems;
	}
	
	// ...setup the dashboard menu item...
	protected MenuItem createDashboardMenuItem() {
		return createBasicMenuItem("header.dashboard", DASHBOARD_URL);
	}
	
	// ...setup the resource menu item, and it's sub menus...
	protected MenuItem createResourceMenuItem(Collection<AttachmentDescriptor> mastheadAttachments) {
		MenuItem menuItem = createBasicMenuItem("header.resources", RESOURCE_HUB_URL);

        List<MenuItem> subMenuItems = new ArrayList<MenuItem>();
        
        // ...setup the browse sub menu...
        subMenuItems.add(createBasicMenuItem("header.Browse", RESOURCE_HUB_URL));
        
        // ...setup the resource plugin sub menu items...
        subMenuItems.addAll(createPluginSubMenuItems(mastheadAttachments, ViewMastheadCategory.RESOURCE));
        
        // ...setup the recently visited sub menu item and sub sub menu...
        MenuItem recentMenuItem = createBasicMenuItem(".dashContent.recentResources", null);
        
        recentMenuItem.setSubMenuItems(createRecentSubMenuItems());
        
        subMenuItems.add(recentMenuItem);
        menuItem.setSubMenuItems(subMenuItems);
        
        return menuItem;
	}
	
	// ...setup the analyze menu item, and it's sub menus...
	protected MenuItem createAnalyzeMenuItem(Collection<AttachmentDescriptor> mastheadAttachments) {
		MenuItem menuItem = createBasicMenuItem("header.analyze", null);

		// ...setup the analyze plugin sub menu items...
		menuItem.setSubMenuItems(createPluginSubMenuItems(mastheadAttachments, ViewMastheadCategory.TRACKER));
        
        return menuItem;
	}
	
	// ...setup the admin menu item...
	protected MenuItem createAdminMenuItem() {
		return createBasicMenuItem("header.admin", ADMIN_URL);
	}
}