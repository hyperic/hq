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
package org.hyperic.ui.tapestry.components.layout;
import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry.IAsset;
import org.apache.tapestry.IRender;
import org.apache.tapestry.annotations.Asset;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Parameter;
import org.hyperic.ui.tapestry.components.BaseLayout;
import org.hyperic.ui.tapestry.components.navigation.NavigationMenu;

/**
 * The single column liquid layout
 *
 */
public abstract class Layout1Col extends BaseLayout{
    @Parameter(name ="styleSheets")
    public abstract List<IAsset> getStyleSheets();
    public abstract void setStyleSheets(List<IAsset> sheets);
    
    @Parameter(name = "pageTitle")
    public abstract String getPageTitle();
    public abstract void setPageTitle(String title);
    
    @Parameter(name = "title")
    public abstract String getTitle();
    public abstract void setTitle(String title);
 
    @Parameter(name = "subTitle")
    public abstract String getSubTitle();
    public abstract void setSubTitle(String title);
    
    @Parameter(name = "statusElement", defaultValue = "false")
    public abstract boolean getStatusElement();
    public abstract void setStatusElement(boolean statusElement);

    @Parameter(name = "helpLink")
    public abstract String getHelpLink();
    public abstract void setHelpLink(String link);
    
    @Parameter(name = "navigationMenu")
    public abstract void setNavigationMenu(NavigationMenu menu);
    public abstract NavigationMenu getNavigationMenu();
    
    @InjectObject("service:hq.ajax.DojoOnePointOneShellDelegate")
    public abstract IRender getAjaxDelegate();
    
    @Asset("context:css/HQ_40_OS.css")
    public abstract IAsset getHQ40Theme();
    
    /**
     * Need to build a page title &lt;title&gt; tag in the form of {pagename} -
     * {application name}
     * 
     * @return the application name
     */
    public String getStaticTitle(){
        return getMessages().getMessage("applicationTitle");
    }

    public List<IAsset> getStyleSheetList(){
        List<IAsset> list = new ArrayList<IAsset>();
        list.add(getHQ40Theme());
        list.addAll(getDojoTundraThemeSheets());
        list.addAll(getDojoGridTundraThemeSheets());
        list.addAll(getDojoSheets());
        list.addAll(getDijitSheets());
        List<IAsset> optional = getStyleSheets();
        if(optional != null)
            list.addAll(optional);
        return list;
    }
}
