/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.ui.tapestry.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.PageRedirectException;
import org.apache.tapestry.engine.EngineMessages;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.services.LinkFactory;
import org.hyperic.ui.tapestry.page.PageListing;

/**
 * Logout and restarts the Tapestry application.
 */
public class SignOutService implements IEngineService {
    
    public static final String SERVICE_NAME = "ssnTapUI.signout";

    private HttpServletRequest _request;

    private HttpServletResponse _response;

    private LinkFactory _linkFactory;

    private String _servletPath;

    public ILink getLink(boolean post, Object parameter) {
	if (parameter != null)
	    throw new IllegalArgumentException(EngineMessages
		    .serviceNoParameter(this));

	Map parameters = new HashMap();

	return _linkFactory.constructLink(this, post, parameters, true);
    }

    public void service(IRequestCycle cycle) throws IOException {

	HttpSession session = _request.getSession(false);

	if (session != null) {
	    try {
		session.invalidate();
	    } catch (IllegalStateException ex) {
	    }
	}

	throw new PageRedirectException(PageListing.SIGN_IN);

    }

    public String getName() {
	return SERVICE_NAME;
    }

    public void setRequest(HttpServletRequest request) {
	_request = request;
    }

    public void setResponse(HttpServletResponse response) {
	_response = response;
    }

    public void setLinkFactory(LinkFactory linkFactory) {
	_linkFactory = linkFactory;
    }

    public void setServletPath(String servletPath) {
	_servletPath = servletPath;
    }
}