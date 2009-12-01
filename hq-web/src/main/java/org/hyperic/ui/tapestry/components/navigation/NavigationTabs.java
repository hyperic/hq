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
package org.hyperic.ui.tapestry.components.navigation;

import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.engine.IEngineService;
import org.hyperic.hq.ui.PageListing;
import org.hyperic.hq.ui.util.URLUtils;
import org.hyperic.ui.tapestry.components.BaseComponent;

/**
 * The colorful tabs at the top of the page. An aggregate of the Header
 * component.
 * 
 */
public abstract class NavigationTabs extends BaseComponent {

    @InjectObject("service:tapestry.services.Page")
    public abstract IEngineService getPageService();

    @Parameter(name = "navigationMenu")
    public abstract void setNavigationMenu(NavigationMenu menu);
    public abstract NavigationMenu getNavigationMenu();

    /**
     * Is a page link the active page?
     * 
     * @param path
     *            the page path to check
     * @return true if the page link passed eq the current page
     */
    public boolean isActive(String pageLink) {
        if (getPage().getPageName().indexOf(pageLink) != -1) {
            return true;
        } else
            return false;
    }
    
    @InjectScript("NavigationTabs.script")
    public abstract IScript getScript();
    
    protected void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        // render as if an Any component
        NavigationMenu m = this.getNavigationMenu();
        if(cycle.getPage().getPageName() != PageListing.SIGN_IN)
            super.renderComponent(writer, cycle);
        // then add the script
        if (!cycle.isRewinding()) {
            PageRenderSupport pageRenderSupport = TapestryUtils
                    .getPageRenderSupport(cycle, this);
            Map symbols = new HashMap();
            getScript().execute(this, cycle, pageRenderSupport, symbols);
        }
    }
   
    public String getLocation(String page) {
        return URLUtils.getLocation(page, getPageService());
    }
    /*       
    public String getAttachemntURL(AttachmentDescriptor desc) {
        return URLUtils.getAttachmentURL(desc, getPageService());
    }
*/ 
}
