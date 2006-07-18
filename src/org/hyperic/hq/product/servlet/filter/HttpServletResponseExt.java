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

package org.hyperic.hq.product.servlet.filter;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * This is a very stupid class that is only required because the
 * ServletResponse interface is brain-dead.  You can _set_ the status code
 * using the ServletResponse interface, but you can't retreive the 
 * response from it.  This class extends the HttpServletResponseWrapper
 * (which implements HttpServletResponse and ServletResponse) and 
 * overloads the three methods that can set the status code.  All three
 * overloaded methods save the status code in a private field, so that we
 * can return that value when it is required.  This feature is required
 * for the JMXFilter.
 */
public final class HttpServletResponseExt extends HttpServletResponseWrapper {

    private int status = 200;
    private int contentLength;

    public HttpServletResponseExt(HttpServletResponse hsr) {
        super(hsr);
    }

    public HttpServletResponseExt(ServletResponse sr) {
        super((HttpServletResponse)sr);
    }

    public int getStatus() {
        return status;
    }
    
    public void setContentLength( int l ) {
        super.setContentLength( l );
        contentLength=l;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setStatus(int sc) {
        status = sc;
        super.setStatus(sc);
    }

    public void sendError(int sc) throws IOException
    {
        status = sc;
        super.sendError(sc);
    }

    public void sendError(int sc, String msg) throws IOException
    {
        status = sc;
        super.sendError(sc, msg);
    }
}
