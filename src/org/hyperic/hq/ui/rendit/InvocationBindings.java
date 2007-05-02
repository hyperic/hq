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
package org.hyperic.hq.ui.rendit;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class encapsulates the data which is passed from RenditServer into 
 * the groovy dispatcher.
 */
public class InvocationBindings {
    private File                _pluginDir;
    private HttpServletRequest  _request;
    private HttpServletResponse _response;
    private ServletContext      _context;
    private String              _type;
    
    private InvocationBindings(String type) {
        _type = type;
    }
    
    public File getPluginDir() {
        return _pluginDir;
    }
    
    public HttpServletRequest getRequest() {
        return _request;
    }
    
    public HttpServletResponse getResponse() {
        return _response;
    }
    
    public ServletContext getContext() {
        return _context;
    }
    
    public String getType() {
        return _type;
    }
    
    public static InvocationBindings newLoad(File pluginDir) {
        InvocationBindings res = new InvocationBindings("load");
        
        res._pluginDir = pluginDir;
        return res;
    }
    
    public static InvocationBindings newRequest(File pluginDir, 
                                                HttpServletRequest request,
                                                HttpServletResponse response,
                                                ServletContext context)
    {
        InvocationBindings res = new InvocationBindings("request");
        
        res._pluginDir = pluginDir;
        res._request   = request;
        res._response  = response;
        res._context   = context;
        return res;
    }
}
