/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.ui.security;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;


public class ApiFilterSecurityInterceptor extends FilterSecurityInterceptor 
{
	private final SessionManager _sessionManager;
	 private final Log log = LogFactory.getLog(ApiFilterSecurityInterceptor.class);
	 
	@Autowired
	public ApiFilterSecurityInterceptor(SessionManager sessionManager)
	{
		_sessionManager = sessionManager;
	}

	@Override
	public void invoke(FilterInvocation fi) throws IOException, ServletException
	{
		super.invoke(fi);
		//For API calls, invalidate the session after the request ends
		try {
			int sessId = RequestUtils.getSessionIdInt(fi.getHttpRequest());
			_sessionManager.invalidate(sessId);
		} catch (Exception e) {
			log.warn("Error invalidating the user associated with this session: " + e.getMessage());
		}
	}

}
