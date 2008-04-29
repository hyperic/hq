/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.summaryCounts;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * An <code>Action</code> that loads the admin screen for a portlet 
 */
public class PrepareAction extends BaseAction {

    // --------------------------------------------------------- Public Methods
    
    /**
     *
     * @param mapping The ActionMapping used to select this instance
     * @param actionForm The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                            ActionForm form,
                            HttpServletRequest request,
                            HttpServletResponse response)
    throws Exception {
          
        ServletContext ctx = getServlet().getServletContext();
        PropertiesForm pForm = (PropertiesForm) form;
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx); 
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        DashboardConfig dashConfig = DashboardUtils.findDashboard(
        		(Integer)session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
        		user, boss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        boolean application = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.application")).booleanValue();
        boolean platform =  new Boolean(dashPrefs.getValue(".dashContent.summaryCounts.platform")).booleanValue();             
        boolean server = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.server")).booleanValue();
        boolean service = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.service")).booleanValue();
        boolean cluster = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.group.cluster")).booleanValue();

        boolean groupMixed= new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.group.mixed")).booleanValue();
        boolean groupGroups = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.group.groups")).booleanValue();
        boolean groupPlatServerService = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.group.plat.server.service")).booleanValue();
        boolean groupApplication = new Boolean( dashPrefs.getValue(".dashContent.summaryCounts.group.application")).booleanValue();


        pForm.setApplication(application);
        pForm.setCluster(cluster);
        pForm.setPlatform(platform);
        pForm.setServer(server);
        pForm.setService(service);

        pForm.setGroupMixed(groupMixed);
        pForm.setGroupGroups(groupGroups);            
        pForm.setGroupPlatServerService(groupPlatServerService);
        pForm.setGroupApplication(groupApplication);

        String[] applicationTypes = getStringArray(".dashContent.summaryCounts.applicationTypes", dashPrefs);            
        String[] platformTypes =    getStringArray(".dashContent.summaryCounts.platformTypes", dashPrefs);            
        String[] serverTypes =      getStringArray(".dashContent.summaryCounts.serverTypes", dashPrefs);
        String[] serviceTypes =     getStringArray(".dashContent.summaryCounts.serviceTypes", dashPrefs);
        String[] clusterTypes =     getStringArray(".dashContent.summaryCounts.group.clusterTypes", dashPrefs);


        pForm.setApplicationTypes(applicationTypes);
        pForm.setClusterTypes(clusterTypes);
        pForm.setPlatformTypes(platformTypes);
        pForm.setServerTypes(serverTypes);
        pForm.setServiceTypes(serviceTypes);

        AppdefInventorySummary summary = appdefBoss.getInventorySummary( 
                                            user.getSessionId().intValue(), true );

        request.setAttribute("summary", summary);

        return null;

    }
    
    private String[] getStringArray(String preference, ConfigResponse config) throws Exception{
        List preferences = StringUtil.explode(config.getValue(preference), "," );
        
        int element;
        Iterator i;
        
        String[] array = new String[preferences.size()];
        
        for(i = preferences.iterator(), element = 0; i.hasNext(); element++){
            array[element] =  (String) i.next();            
        
        }           
        
        return array;
    
    }
}
