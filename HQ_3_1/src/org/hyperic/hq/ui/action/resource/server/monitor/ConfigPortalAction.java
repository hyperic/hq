/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.resource.server.monitor;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.monitor.config.ResourceConfigPortalAction;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;


/**
 * This action prepares the portal for configuring the server monitoring pages.
 */
public class ConfigPortalAction extends ResourceConfigPortalAction {

    private static final String CONFIG_METRICS_PORTAL
        = ".resource.server.monitor.config.ConfigMetrics";
    private static final String CONFIG_METRICS_TITLE
        = "resource.server.monitor.visibility.config.ConfigureVisibility.Title";
 
    /* (non javadoc)
     * @see org.hyperic.hq.ui.action.BaseDispatchAction#getKeyMethodMap()
     */
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.put(Constants.MODE_CONFIGURE, "configMetrics");
        map.put(Constants.MODE_LIST, "configMetrics");
        return map;
    }

    /** mode=configure || mode=view */
    public ActionForward configMetrics(ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        
        setResource(request);
               
        super.configMetrics(mapping,form,request,response);
        
        Portal portal = Portal.createPortal( CONFIG_METRICS_TITLE, 
            CONFIG_METRICS_PORTAL);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    
    }
  
}
