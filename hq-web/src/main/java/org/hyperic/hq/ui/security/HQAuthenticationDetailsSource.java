/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2012], VMware, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.security.HQAuthenticationDetails;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.codec.Base64;


public class HQAuthenticationDetailsSource implements AuthenticationDetailsSource{

    private final Log log = LogFactory.getLog(HQAuthenticationDetailsSource.class.getName());

	/* (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationDetailsSource#buildDetails(java.lang.Object)
	 */
	public Object buildDetails(Object context) {
		if (!(context instanceof HttpServletRequest)) {
			log.warn("Can only create authentication details from an HttpServletRequest");
			return null;
		}
        String username = "";
		HttpServletRequest request = (HttpServletRequest) context;
		//Extract the user name from the request 
		String header = request.getHeader("Authorization");
		 try{
	        if ((header != null) && header.startsWith("Basic ")) {
	            byte[] base64Token = header.substring(6).getBytes("UTF-8");
	            String token = new String(Base64.decode(base64Token), "UTF-8");
	            int delim = token.indexOf(":");
	            if (delim != -1) {
	                username = token.substring(0, delim);
	            }

	        }
		 }
		 catch (Exception e) {
		}
		HttpSession session = request.getSession(false);
	    String sessionId = (session != null) ? session.getId() : null;
	    boolean usingExternalAuth = false;
	    //If the user checked the login checkbox of 'use my organization authentication'
	    if (null != request.getParameter(HQConstants.ORGANIZATION_AUTHENTICATION) && 
	    		request.getParameter(HQConstants.ORGANIZATION_AUTHENTICATION).equalsIgnoreCase("on")) {
	    	usingExternalAuth = true;
	    }
	    //Used for hqapi to allow api users to use their ldap\kerberos credentials 
	    if (username.toLowerCase().startsWith(HQConstants.ORGANIZATION_AUTHENTICATION)) {
	    	usingExternalAuth = true;
	    }
	    return new HQAuthenticationDetails(request.getRemoteAddr(), sessionId, usingExternalAuth);
	}

	
	
}