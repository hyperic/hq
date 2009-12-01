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

package org.hyperic.hq.ui;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.servlet.RenditServlet;

public class RenditFilter extends BaseFilter {
    private static final Log _log = LogFactory.getLog(RenditFilter.class);
    
	public void doFilter(ServletRequest request, ServletResponse response,
	                     FilterChain chain) 
        throws IOException, ServletException 
    {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest hreq = (HttpServletRequest)request;
            if (_log.isDebugEnabled()) {
                _log.debug("Filtering request: " + hreq.getRequestURI() + 
                           " valid=" + RenditServlet.requestIsValid(hreq));
            }
            if (!RenditServlet.requestIsValid(hreq)) { 
                throw new ServletException("Invalid request");
            }
        }
        chain.doFilter(request, response);
    }
}
