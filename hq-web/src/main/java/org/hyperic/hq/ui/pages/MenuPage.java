/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.hq.ui.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.util.CSSUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.UIUtilsImpl;
import org.hyperic.hq.ui.util.URLUtils;
import org.hyperic.ui.tapestry.components.navigation.MenuItem;
import org.hyperic.ui.tapestry.components.navigation.NavigationMenu;

/**
 * Creates the menu items used in the NavigationTabs component. NOTE: All pages
 * that have a header with active NavigationTabs *must* extend this class.
 * 
 * The only exception to this is the SignIn page.
 */
public abstract class MenuPage extends BasePage implements PageBeginRenderListener{

    private static Log log = LogFactory.getLog(MenuPage.class);
    
    @InjectObject("service:tapestry.services.Page")
    public abstract IEngineService getPageService();
    
    /**
     * Listener that builds the menu items for the Header
     * @param event
     */
    public void pageBeginRender(PageEvent event) {
        super.pageBeginRender(event);
        setNavigationMenu(new NavigationMenu());
        // Construct the list of HQU plugins for the main menu
        buildHQUMenuItems();
        // Construct the list of Recent and Favorite resources for the main menu
        buildFavoriteResourceMenuItems(UIUtilsImpl.getFavoriteResources(
                getServletContext(), getBaseSessionBean().getWebUser()));
    }

    @Persist
    public abstract void setNavigationMenu(NavigationMenu menu);
    public abstract NavigationMenu getNavigationMenu();
    
    private void buildHQUMenuItems() {
        ServletContext ctx = getServletContext();
        ProductBoss pBoss = ContextUtils.getProductBoss(ctx);
        Collection<AttachmentDescriptor> mastheadAttachments = null;
        try {
            Integer sessionId = getBaseSessionBean().getSessionId();
            mastheadAttachments = pBoss.findAttachments(sessionId.intValue(),
                    AttachType.MASTHEAD);
        } catch (Exception e) {
            log.error("Cannot get masthead attachments. " + e.getLocalizedMessage());
        }
        List<MenuItem> resourceMenuItems = new ArrayList<MenuItem>();
        List<MenuItem> analyzeMenuItems = new ArrayList<MenuItem>();
        if (mastheadAttachments != null) {
            for (AttachmentDescriptor d : mastheadAttachments) {
                AttachmentMasthead attachment = (AttachmentMasthead) d.getAttachment();
                if (attachment.getCategory().equals(ViewMastheadCategory.RESOURCE)) {
                    resourceMenuItems.add(new MenuItem(d.getHTML(), URLUtils.getAttachmentURL(d, getPageService()), ""));
                } else if (attachment.getCategory().equals(ViewMastheadCategory.TRACKER)) {
                    analyzeMenuItems.add(new MenuItem(d.getHTML(), URLUtils.getAttachmentURL(d, getPageService()), ""));            
                }
            }
        }
        getNavigationMenu().setResourceMenuItems(resourceMenuItems);
        getNavigationMenu().setAnalyzeMenuItems(analyzeMenuItems);
    }

    private void buildFavoriteResourceMenuItems(List<AppdefResourceValue> resList){
        List<MenuItem> recentResourceItems = new ArrayList<MenuItem>();
        for (AppdefResourceValue r : resList) {
            recentResourceItems.add(new MenuItem(r.getName(), URLUtils.buildResourceURL(r), CSSUtils.getStyleClassByResourceType(r)));
        }
        getNavigationMenu().setRecentResourceItems(recentResourceItems);
    }
    
}
