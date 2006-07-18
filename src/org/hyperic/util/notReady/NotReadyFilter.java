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

package org.hyperic.util.notReady;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** 
 * A simple filter that will return 503 ( Service unavailable ) until the 
 * setReady(true) is called.
 * 
 * It can be used by CAM or any other app to reject requests until all 
 * initialization is completed.
 * 
 * To use it, just insert it in the web.xml, and map all the URIs of interest 
 * through this filter. 
 * 
 * When all subsystems are initialized and started, call setReady().
 * 
 * Another option is to use the AuthenticationFilter ( for the CAM UI ) and 
 * the AgentCallbackBossEJBImpl ( for agent callbacks ). 
 * 
 * In any case - the critical step is figuring when all things are started 
 * correctly, and calling the method that will unlock the filter.  
 */
public class NotReadyFilter 
    implements Filter
{
    static boolean isReady = false;

    private ServletContext servletCtx;

    public NotReadyFilter(){
    }

    public void init(FilterConfig filterConfig) 
        throws ServletException 
    {
        this.servletCtx = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest servletRequest, 
                         ServletResponse servletResponse, 
                         FilterChain filterChain) 
        throws IOException, ServletException 
    {
        HttpServletResponse hResp;
        HttpServletRequest hReq;

        if(isReady){
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            hResp = (HttpServletResponse)servletResponse;
            hReq  = (HttpServletRequest)servletRequest;

            this.servletCtx.log("Not ready - received request for " + 
                                hReq.getRequestURI());
                    
            hResp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                            "Server is still starting");
        }
    }

    public void destroy() {
    }

    static boolean getReady() {
        return isReady;
    }

    static void setReady(boolean ready) {
        isReady = ready;
    }
}
