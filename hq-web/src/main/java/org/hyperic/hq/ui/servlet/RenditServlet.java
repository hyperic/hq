/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
package org.hyperic.hq.ui.servlet;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.hqu.rendit.RequestInvocationBindings;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.WebUtils;

public class RenditServlet 
    extends HttpServlet
{
    private static final Log _log = LogFactory.getLog(RenditServlet.class);

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException 
    {
        handleRequest(req, resp);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException 
    {
        handleRequest(req, resp);
    }
    
    public static boolean requestIsValid(HttpServletRequest req) {
        String reqUri = (String)
            req.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        boolean useInclude = false;
        
        if (reqUri != null)
            useInclude = true;
        if (reqUri == null && !useInclude)
            reqUri = req.getRequestURI();
        
        List fullPath = StringUtil.explode(reqUri, "/");
        int pathSize = fullPath.size();
        
        Iterator<String> it = fullPath.iterator();
        while(it.hasNext()){
            if("..".equals(it.next())){
                return false;
            }
        }
        
        if (_log.isDebugEnabled()) {
            _log.debug("Examining path: " + fullPath);
        }

        if (pathSize < 3) { 
            _log.warn("Illegal request path [" + fullPath + "]");
            return false;
        }
        String elem1 = (String)fullPath.get(1);
        String elem2 = (String)fullPath.get(2);

        if (elem1.equals("public") || elem2.equals("public"))
            return true;
        
        if (pathSize < 4 || !fullPath.get(pathSize - 4).equals("hqu")) {
            _log.warn("Illegal request path [" + fullPath + "]");
            return false;
        }

        String lastElem = (String)fullPath.get(pathSize - 1);
        if (lastElem.endsWith(".hqu") == false) {
            _log.warn("non .hqu file requested [" + fullPath + "]");
            return false;
        }
        
        return true;
    }
    
    protected void handleRequest(HttpServletRequest req, 
                                 HttpServletResponse resp)
        throws ServletException, IOException
    {
        boolean useInclude = false;

        // Since we may be processing via an internal RequestDispatcher 
        // include(), we need to investigate the subrequest URIs, etc.
        // and use those, as Tomcat won't set them up in subrequest objects
        String reqUri = (String)
            req.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (reqUri != null)
            useInclude = true;
        if (reqUri == null && !useInclude)
            reqUri = req.getRequestURI();
        
        String ctxPath = (String)
            req.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
        if (ctxPath != null)
            useInclude = true;
        if (ctxPath == null && !useInclude)
            ctxPath = req.getContextPath();
        
        String pathInfo = (String)
            req.getAttribute(WebUtils.INCLUDE_PATH_INFO_ATTRIBUTE);
        if (pathInfo != null)
            useInclude = true;
        if (pathInfo == null && !useInclude) 
            pathInfo = req.getPathInfo();
        
        String servletPath = (String)
            req.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
        if (servletPath != null)
            useInclude = true;
        if (servletPath == null && !useInclude)
            servletPath = req.getServletPath();
        
        String queryStr = (String)
            req.getAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE);
        if (queryStr != null)
            useInclude = true;
        if (queryStr == null && !useInclude)
            queryStr = req.getQueryString();

        List fullPath = StringUtil.explode(reqUri, "/");
        int pathSize = fullPath.size();
        
        if (_log.isDebugEnabled()) {
            _log.debug("Request path [" + fullPath + "]");
        }

        if (((String)fullPath.get(pathSize - 1)).endsWith(".groovy")) {
            _log.warn(".groovy file requested [" + fullPath + "]");
            throw new ServletException("Illegal request path [" + fullPath + 
                                       "]");
        }
        
        if (!requestIsValid(req)) {
            throw new ServletException("Illegal request path [" + fullPath + 
                                       "]");
        }
        
        List subPath  = fullPath.subList(pathSize - 3, fullPath.size());
        
        String plugin = (String)subPath.get(0);
        if (_log.isDebugEnabled()) {
            _log.debug("Request for [" + plugin + "]: " + reqUri + 
                       (queryStr == null ? "" : ("?" + queryStr)));
        }

        int sessId = RequestUtils.getSessionIdInt(req);
        WebApplicationContext springContext =
            WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        AuthzBoss authzBoss = springContext.getBean(AuthzBoss.class);

        AuthzSubject user;
        
        try {
            user = authzBoss.getCurrentSubject(sessId);
        } catch(SessionException e) {
            // Could not get the current user.  We should default to a 'nobody'
            // user here.
            _log.warn("Unable to get current user.  Bailing", e);
            throw new ServletException(e);
        }

        RequestInvocationBindings b = 
            new RequestInvocationBindings(reqUri, ctxPath, pathInfo,
                                          servletPath, queryStr, user,
                                          req, resp, getServletContext());
        long start = System.currentTimeMillis();
        try {
            Bootstrap.getBean(RenditServer.class).handleRequest(plugin, b); 
        } catch(Exception e) {
            throw new ServletException(e);
        } finally {
            _log.debug("Processed request for [" + plugin + "] in " +
                       (System.currentTimeMillis() - start) + " ms");
        }
    }
    
    public void init() throws ServletException {
        super.init();
    }
}
