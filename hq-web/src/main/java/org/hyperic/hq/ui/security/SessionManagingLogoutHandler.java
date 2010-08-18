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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.server.session.UserAuditFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
/**
 * Extension of {@link SecurityContextLogoutHandler} that updates the internal SessionManager
 * TODO remove this in favor of just using the SecurityContext (upon removal of SessionManager)
 * @author jhickey
 *
 */
public class SessionManagingLogoutHandler extends SecurityContextLogoutHandler {
    
    private SessionManager sessionManager;
    private UserAuditFactory userAuditFactory;
    
    private final Log log = LogFactory.getLog(SessionManagingLogoutHandler.class);
    
    @Autowired
    public SessionManagingLogoutHandler(SessionManager sessionManager,
    		                            UserAuditFactory userAuditFactory) {
        super();
        this.sessionManager = sessionManager;
        this.userAuditFactory = userAuditFactory;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
    		           Authentication authentication) {
        try {
            int sessId = RequestUtils.getSessionIdInt(request);
            userAuditFactory.logoutAudit(sessionManager.getSubject(sessId));
            sessionManager.invalidate(sessId);
        } catch (Exception e) {
            log.warn("Error invalidating the user associated with this session: " + e.getMessage());
        }
        super.logout(request, response, authentication);
      
    }

}
