/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.taglib;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.appdef.server.session.ApplicationManagerImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.hub.BreadcrumbUtil;
import org.hyperic.hq.ui.action.resource.hub.ResourceHubForm;
import org.hyperic.hq.ui.util.RequestUtils;

public class ResourceBreadcrumbTag extends TagSupport {
    private final static long serialVersionUID = 1L;
    private final static String RESOURCE_BREADCRUMB_TAG_NAME = "breadcrumb";
    private final static String RESOURCE_ID_ATTR_NAME = "resourceId";
    private final static String CTYPE_ATTR_NAME = "ctype";
    private final static String BASE_BROWSE_URL_ATTR_NAME = "baseBrowseUrl";
    private final static String BASE_RESOURCE_URL_ATTR_NAME = "baseResourceUrl";
    
    private String resourceId;
    private String ctype;
    private String baseBrowseUrl;
    private String baseResourceUrl;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getCtype() {
        return ctype;
    }

    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    public String getBaseBrowseUrl() {
        return baseBrowseUrl;
    }

    public void setBaseBrowseUrl(String baseBrowseUrl) {
        this.baseBrowseUrl = baseBrowseUrl;
    }

    public String getBaseResourceUrl() {
        return baseResourceUrl;
    }

    public void setBaseResourceUrl(String baseResourceUrl) {
        this.baseResourceUrl = baseResourceUrl;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            String resourceId = getResourceId();
            String ctype = getCtype();
            String baseBrowseUrl = getBaseBrowseUrl();
            String baseResourceUrl = getBaseResourceUrl();
            
            // ...first, backup the bread crumb, we may need it later...
            List<BreadcrumbItem> backupBreadcrumbs = processBackupBreadcrumb();
            
            // ...then, process the bread crumbs...
            List<BreadcrumbItem> breadcrumbs = processBreadcrumb(resourceId, ctype, baseBrowseUrl, baseResourceUrl);

            // ...then, render them...
            renderBreadcrumb(breadcrumbs, backupBreadcrumbs);
        } catch (Exception e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    private List<BreadcrumbItem> processBackupBreadcrumb() 
    throws ServletException, CloneNotSupportedException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpSession session = request.getSession();
        String[] returnToValues = request.getParameterValues(Constants.RETURN_TO_LINK_PARAM_NAME);
        boolean useBackup = false;
        List<BreadcrumbItem> backup = null;
        
        if (returnToValues != null && returnToValues.length > 0) {
            useBackup = returnToValues[0].equals(Constants.RETURN_TO_LINK_PARAM_VALUE);
        }
        
        if (useBackup) {
            // ...reset bread crumbs from backup...
            List<BreadcrumbItem> breadcrumbs = (List<BreadcrumbItem>) session.getAttribute(Constants.BREADCRUMB_SESSION_BACKUP_ATTR_NAME);
            
            session.setAttribute(Constants.BREADCRUMB_SESSION_ATTR_NAME, breadcrumbs);
        } else {
            List<BreadcrumbItem> breadcrumbs = (List<BreadcrumbItem>) session.getAttribute(Constants.BREADCRUMB_SESSION_ATTR_NAME);
            
            if (breadcrumbs != null) {
                backup = new ArrayList<BreadcrumbItem>();
                
                // ...clone the bread crumbs...
                for (Iterator<BreadcrumbItem> i = breadcrumbs.iterator(); i.hasNext();) {
                    backup.add((BreadcrumbItem) i.next().clone());
                }
            }
        }
        
        session.setAttribute(Constants.BREADCRUMB_SESSION_BACKUP_ATTR_NAME, backup);
        
        return backup;
    }
    
    private List<BreadcrumbItem> processBreadcrumb(String resourceId, String ctype, String baseBrowseUrl, String baseResourceUrl) 
    throws ServletException,
           SessionNotFoundException,
           RemoteException,
           SessionTimeoutException,
           PermissionException,
           AppdefEntityNotFoundException
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpSession session = request.getSession();
        ServletContext ctx = pageContext.getServletContext();
        AppdefBoss appdefBoss = Bootstrap.getBean(AppdefBoss.class);
        int sessionId = RequestUtils.getSessionId(request).intValue();
        AppdefEntityID appdefEntityId = new AppdefEntityID(resourceId);
        AppdefResourceValue resource = appdefBoss.findById(sessionId, appdefEntityId);
        String browseUrl = getBrowseUrl(resource, (String) session.getAttribute(Constants.ROOT_BREADCRUMB_URL_ATTR_NAME));
        List<BreadcrumbItem> breadcrumbs = (List<BreadcrumbItem>) session.getAttribute(Constants.BREADCRUMB_SESSION_ATTR_NAME);

        if (breadcrumbs == null || browseUrl != null) {
            // ...create the new bread crumb, if we don't already have one or we're coming from the browse page...
            breadcrumbs = new ArrayList<BreadcrumbItem>();
            // ...clear out backup...
            session.removeAttribute(Constants.BREADCRUMB_SESSION_BACKUP_ATTR_NAME);
        } 
        
        // ...create the bread crumb item for this resource...
        String url = BreadcrumbUtil.createResourceURL(baseResourceUrl, resourceId, ctype);
        String label = resource.getName();
        
        if (ctype != null && !ctype.equals("")) {
            // ...we're dealing with an auto group which is a different beast...
            appdefEntityId = new AppdefEntityTypeID(ctype); 
            AppdefResourceTypeValue resourceType = appdefBoss.findResourceTypeById(sessionId, (AppdefEntityTypeID) appdefEntityId);
            label = resourceType.getName();
        }
        
        BreadcrumbItem newCrumb = new BreadcrumbItem(url, resourceId, ctype, label, appdefEntityId);
        
        // ...now loop through the bread crumbs and figure out where this crumb fits in
        // we don't iterate over index 0 bc that'll always the browse crumb...
        for (int x = breadcrumbs.size() - 1; x > 0; x--) { 
            BreadcrumbItem crumb = breadcrumbs.get(x);
            
            if (!newCrumb.equals(crumb)                 &&
                (isParentOfChild(crumb, newCrumb)       || 
                 isMemberOfApplication(crumb, newCrumb) ||
                 isMemberOfGroup(crumb, newCrumb)       ||
                 isParentOfAutoGroup(crumb, newCrumb)   ||
                 isMemberOfAutoGroup(crumb, newCrumb))) {
                // ...is this a child of the comparison crumb?..
                breadcrumbs.add(newCrumb);
                
                break;
            }
            
            // ...not a child so clean up the trail...
            breadcrumbs.remove(x);
        }
        
        if (breadcrumbs.size() < 2) {
            if (breadcrumbs.size() == 1) {
                // ...all we have is the browse crumb which may be stale so remove it...
                breadcrumbs.remove(0);
            }
            
            // ...add the newest browse crumb...
            breadcrumbs.add(createRootBreadcrumb(resource));
            
            if (newCrumb.isAutoGroup()) {
                // ...if we're dealing with an auto group, we need to include the parent resource
                // in the bread crumb...
                String parentUrl = BreadcrumbUtil.createResourceURL(baseResourceUrl, resourceId, null);
                
                breadcrumbs.add(new BreadcrumbItem(parentUrl, resourceId, null, resource.getName(), new AppdefEntityID(resourceId)));
            }
            
            // ...and finally, add the newest bread crumb...
            breadcrumbs.add(newCrumb);
        }
        
        //...then stash the bread crumbs in the session...
        session.setAttribute(Constants.BREADCRUMB_SESSION_ATTR_NAME, breadcrumbs);
        
        return breadcrumbs;
    }

    private void renderBreadcrumb(List<BreadcrumbItem> breadcrumbs, List<BreadcrumbItem> backupBreadcrumbs) 
    throws IOException
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        StringBuilder markup = new StringBuilder("<ul class=\"breadcrumbs\">");

        // Now that the bread crumb has been generated, let's render it...
        for (int x = 0; x < breadcrumbs.size(); x++) {
            BreadcrumbItem item = breadcrumbs.get(x);

            markup.append("<li class=\"item\">");

            if (x < (breadcrumbs.size() - 1)) {
                markup.append("<a href=\"").append(item.getUrl()).append("\">");
                markup.append(StringEscapeUtils.escapeHtml(item.getLabel()));
                markup.append("</a>&nbsp;&rsaquo;&nbsp;");
            } else {
                markup.append(StringEscapeUtils.escapeHtml(item.getLabel()));
            }

            markup.append("</li>");
        }

        markup.append("</ul>");
        
        // Stash the browse crumb in a hidden text field for js use
        markup.append("<input type=\"hidden\" id=\"browseUrlInput\" value=\"").append(breadcrumbs.get(0).getUrl()).append("\" />");
        
        if (backupBreadcrumbs != null && !breadcrumbs.get(0).equals(backupBreadcrumbs.get(0))) {
            // ...if the browse crumbs don't match, provide a "Return to ..." link.
            BreadcrumbItem crumb = backupBreadcrumbs.get(backupBreadcrumbs.size() - 1);
            String returnTo = RequestUtils.message(request, "breadcrumb.returnTo");
            
            markup.append("<span class=\"returnToLink\"><a href=\"").append(BreadcrumbUtil.createReturnToURL(crumb.getUrl())).append("\">");
            markup.append(returnTo).append(" ").append(StringEscapeUtils.escapeHtml(crumb.getLabel()));
            markup.append("</a></span>");
        }

        pageContext.getOut().write(markup.toString());
    }
    
    private int getGroupType(AppdefGroupValue group) {
        // ...group type ids used to distinguish between compat and mixed groups...
        // TODO there are constants that are already in the TypeConstants class but they have the values 
        // swapped for some reason.  To be safe, specifying the values here and will look into 
        // refactoring using the TypeConstants class later.
        final Integer APPDEF_TYPE_GROUP_COMPAT = new Integer(1);
        final Integer APPDEF_TYPE_GROUP_MIXED = new Integer(2);
        final Integer APPDEF_TYPE_GROUP_DYNAMIC = new Integer(3);

        return group.isDynamicGroup() ? APPDEF_TYPE_GROUP_DYNAMIC : (group.isGroupCompat() ? APPDEF_TYPE_GROUP_COMPAT : APPDEF_TYPE_GROUP_MIXED);
    }
    
    private String getBrowseUrl(AppdefResourceValue resource, String url) {
        String result = url;
        
        if (url != null) {
            //...if we have an url, check to make sure the resource is associated...
            String test = ResourceHubForm.ENTITY_TYPE_ID_PARAM + "=" + resource.getEntityId().getType();
            String testGroup = null;
            
            if (resource.getEntityId().isGroup()) {
                // ...if we're dealing with a group, need to do a secondary test...
                testGroup = ResourceHubForm.GROUP_TYPE_ID_PARAM + "=" + getGroupType((AppdefGroupValue) resource);
            }
            
            if ((url.indexOf(test) == -1 && testGroup == null) ||
                (testGroup != null && url.indexOf(test) > -1 && url.indexOf(testGroup) == -1)) {
                // ...this browse url isn't using the current resource's resource type,
                // so generate one based off the resource.
                BreadcrumbItem crumb = createRootBreadcrumb(resource);
                
                result = crumb.getUrl();
            }
        }
        
        return result;
    }

    private BreadcrumbItem createRootBreadcrumb(AppdefResourceValue resource) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpSession session = request.getSession();

        // ...we have a fresh trail of crumbs to lay down...
        String browseUrl = (String) session.getAttribute(Constants.ROOT_BREADCRUMB_URL_ATTR_NAME);

        if (browseUrl == null || browseUrl.equals("")) {
            // ...we can't find a browse url, so we need to create one from
            // the resource...
            Integer groupType = null;
            
            if (resource.getEntityId().isGroup()) {
                // ...we are dealing with a group, let's determine the type (compatible or mixed)
                groupType = new Integer(getGroupType((AppdefGroupValue) resource));
            }
            
            browseUrl = BreadcrumbUtil.createRootBrowseURL(baseBrowseUrl, resource.getEntityId().getType(), groupType);
        } else {
            // ...we got what we need, remove it from the session...
            session.removeAttribute(Constants.ROOT_BREADCRUMB_URL_ATTR_NAME);
        }
        
        String browse = RequestUtils.message(request, "breadcrumb.browse");
        
        return new BreadcrumbItem(browseUrl, null, null, browse, null);
    }
    
    private boolean isParentOfChild(BreadcrumbItem parent, BreadcrumbItem child) {
        boolean result = false;
        
        if (!child.isAutoGroup() &&
            ((parent.isPlatform() && child.isServer()) ||
             (parent.isPlatform() && child.isService()) ||
             (parent.isServer() && child.isService()))) {
            ResourceManager resourceManager =Bootstrap.getBean(ResourceManager.class);
            Resource parentResource = resourceManager.findResource(parent.getAppdefEntityId());
            Resource childResource = resourceManager.findResource(child.getAppdefEntityId());
    
            result = resourceManager.isResourceChildOf(parentResource, childResource);
        }
        
        return result;
    }
    
    private boolean isParentOfAutoGroup(BreadcrumbItem parent, BreadcrumbItem group) {
        boolean result = false;
        
        if ((parent.isPlatform() || 
             parent.isServer()   || 
             parent.isApplication()) && group.isAutoGroup()) {
            ResourceManager resourceManager =Bootstrap.getBean(ResourceManager.class);
            Resource parentResource = resourceManager.findResource(parent.getAppdefEntityId());
            Resource parentOfAutoGroupResource = resourceManager.findResource(new AppdefEntityID(group.getResourceId()));
    
            result = parentResource != null && parentResource.equals(parentOfAutoGroupResource);
        }
        
        return result;
    }

    private boolean isMemberOfApplication(BreadcrumbItem application, BreadcrumbItem member) {
        boolean result = false;
        
        if (application.isApplication()) {
            ApplicationManager applicationManager = Bootstrap.getBean(ApplicationManager.class);
            
            result = applicationManager.isApplicationMember(application.getAppdefEntityId(), 
                                                            member.getAppdefEntityId());
        }
        
        return result;
    }
    
    private boolean isMemberOfGroup(BreadcrumbItem group, BreadcrumbItem member) {
        boolean result = false;
        
        if (group.isGroup()) {
            ResourceManager resourceManager =Bootstrap.getBean(ResourceManager.class);
            Resource groupResource = resourceManager.findResource(group.getAppdefEntityId());
            Resource memberResource = resourceManager.findResource(member.getAppdefEntityId());
            ResourceGroupManager resourceGroupManager = Bootstrap.getBean(ResourceGroupManager.class);
            ResourceGroup resourceGroup = resourceGroupManager.getResourceGroupByResource(groupResource);
            
            result = resourceGroupManager.isMember(resourceGroup, memberResource);
        }
        
        return result;
    }
    
    private boolean isMemberOfAutoGroup(BreadcrumbItem group, BreadcrumbItem member) 
    throws SessionNotFoundException,
           SessionTimeoutException,
           RemoteException,
           ServletException
    {
        boolean result = false;
        
        if (group.isAutoGroup()   &&
            !member.isAutoGroup() &&
            (member.isService()   || 
             member.isServer())) {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            ServletContext ctx = pageContext.getServletContext();
            AppdefBoss appdefBoss = Bootstrap.getBean(AppdefBoss.class);
            int sessionId = RequestUtils.getSessionId(request).intValue();
            ResourceManager resourceManager =Bootstrap.getBean(ResourceManager.class);
            AppdefResourceTypeValue autoGroupResourceType = appdefBoss.findResourceTypeById(sessionId, (AppdefEntityTypeID) group.getAppdefEntityId());
            Resource memberResource = resourceManager.findResource(member.getAppdefEntityId());

            if (autoGroupResourceType.getAppdefType() == memberResource.getResourceType().getAppdefType()) {
                // ...resource types are the same, now check if parents are the same,
                // luckily resourceId represents the parent...
                AppdefEntityID parentAppdef = new AppdefEntityID(group.getResourceId());
                
                if (parentAppdef.isApplication()) {
                    // ...if the parent is an application we need to determine if the other is a member...
                    ApplicationManager applicationManager = Bootstrap.getBean(ApplicationManager.class);
                    
                    result = applicationManager.isApplicationMember(parentAppdef, member.getAppdefEntityId());
                } else {
                    // ...otherwise, we check if it's a child resource...
                    Resource parentResource = resourceManager.findResource(parentAppdef);
                    
                    result = resourceManager.isResourceChildOf(parentResource, memberResource);
                }
            }
        }
        
        return result;
    }
    
    protected class BreadcrumbItem 
    implements Cloneable
    {
        private String url;
        private String resourceId;
        private String autoGroupId;
        private String label;
        private AppdefEntityID appdefEntityId;
        
        public BreadcrumbItem() {
        }

        public BreadcrumbItem(String url, String resourceId, String autoGroupId, String label, AppdefEntityID appdefEntityId) {
            setUrl(url);
            setResourceId(resourceId);
            setAutoGroupId(autoGroupId);
            setLabel(label);
            setAppdefEntityId(appdefEntityId);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getAutoGroupId() {
            return autoGroupId;
        }

        public void setAutoGroupId(String autoGroupId) {
            this.autoGroupId = autoGroupId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public AppdefEntityID getAppdefEntityId() {
            return appdefEntityId;
        }

        public void setAppdefEntityId(AppdefEntityID appdefEntityId) {
            this.appdefEntityId = appdefEntityId;
        }

        public boolean isApplication() {
            return this.appdefEntityId.isApplication();
        }
        
        public boolean isAutoGroup() {
            return this.appdefEntityId instanceof AppdefEntityTypeID;
        }
        
        public boolean isGroup() {
            return this.appdefEntityId.isGroup();
        }
        
        public boolean isPlatform() {
            return this.appdefEntityId.isPlatform();
        }
        
        public boolean isServer() {
            return this.appdefEntityId.isServer();
        }
        
        public boolean isService() {
            return this.appdefEntityId.isService();
        }
        
        @Override
        public boolean equals(Object obj) {
            BreadcrumbItem crumb = (BreadcrumbItem) obj;
            
            return crumb != null &&
                   ((crumb.getAutoGroupId() == null && this.getAutoGroupId() == null) ||
                    (crumb.getAutoGroupId() != null && crumb.getAutoGroupId().equals(this.getAutoGroupId()))) &&
                   ((crumb.getLabel() == null && this.getLabel() == null) ||
                    (crumb.getLabel() != null && crumb.getLabel().equals(this.getLabel()))) &&
                   ((crumb.getResourceId() == null && this.getResourceId() == null) ||
                    (crumb.getResourceId() != null && crumb.getResourceId().equals(this.getResourceId()))) &&
                   ((crumb.getUrl() == null && this.getUrl() == null) ||
                    (crumb.getUrl() != null && crumb.getUrl().equals(this.getUrl()))) &&
                   ((crumb.getAppdefEntityId() == null && this.getAppdefEntityId() == null) ||
                    (crumb.getAppdefEntityId() != null && crumb.getAppdefEntityId().equals(this.getAppdefEntityId())));
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new BreadcrumbItem(getUrl(), getResourceId(), getAutoGroupId(), getLabel(), getAppdefEntityId());
        }
    }
}
