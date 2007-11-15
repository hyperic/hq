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

package org.hyperic.hq.ui.action.portlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Base RSSAction class to extend.  Provides utility methods.
 */
public abstract class BaseRSSAction extends BaseAction {
    private static final Log log =
        LogFactory.getLog(BaseRSSAction.class.getName());

    private Properties configProps = null;
    
    protected String getUsername(HttpServletRequest request) {
        return RequestUtils.getStringParameter(request, "user");
    }

    protected ConfigResponse getUserPreferences(HttpServletRequest request,
                                                String user)
        throws LoginException, RemoteException, ConfigPropertyException {
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);

        // Let's make sure that the rss auth token matches
        String rssToken = RequestUtils.getStringParameter(request, "token");

        // Get user preferences
        ConfigResponse preferences = DashboardManagerEJBImpl.getOne()
            .getRssUserPreferences(user, rssToken);

        ConfigResponse defaultPreferences =
            (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_PREFS);

        preferences.merge(defaultPreferences, false);
        
        return preferences;
    }
    
    protected void setManagingEditor(HttpServletRequest request)
        throws RemoteException, ConfigPropertyException {
        Properties props = getConfigProps(request);
        
        // Get "from" sender for managingEditor field
        request.setAttribute("managingEditor",
                             props.getProperty(HQConstants.EmailSender));
    }
    
    private String getBaseURL(HttpServletRequest request)
        throws RemoteException, ConfigPropertyException {
        Properties props = getConfigProps(request);
        return props.getProperty(HQConstants.BaseURL);
    }
    
    private Properties getConfigProps(HttpServletRequest request)
        throws RemoteException, ConfigPropertyException {
        if (configProps == null) {
            synchronized(this) {
                ServletContext ctx = getServlet().getServletContext();
                ConfigBoss cboss = ContextUtils.getConfigBoss(ctx);
                configProps = cboss.getConfig();
            }
        }
        
        return configProps;
    }
    
    protected RSSFeed getNewRSSFeed(HttpServletRequest request)
        throws RemoteException, ConfigPropertyException {
        return new RSSFeed(getBaseURL(request));
    }
}
