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
package org.hyperic.hq.security;

public class HQAuthenticationDetails {

	private String remoteAddress;
	private String sessionId;
	private boolean usingExternalAuth;

	public HQAuthenticationDetails() {}
	/**
	 * @param remoteAddress - the client IP address
	 * @param sessionId - the session id 
	 * @param usingExternalAuth - true if the user is using ldap\kerberos credentials
	 */
	public HQAuthenticationDetails(String remoteAddress, String sessionId, boolean usingExternalAuth) {
		this.remoteAddress = remoteAddress;
		this.sessionId = sessionId;
		this.usingExternalAuth = usingExternalAuth;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isUsingExternalAuth() {
		return usingExternalAuth;
	}

	public void setUsingExternalAuth(boolean usingExternalAuth) {
		this.usingExternalAuth = usingExternalAuth;
	}

}
