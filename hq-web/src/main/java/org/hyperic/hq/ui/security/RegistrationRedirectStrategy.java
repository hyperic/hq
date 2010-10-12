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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.Constants;
import org.springframework.security.web.RedirectStrategy;

public class RegistrationRedirectStrategy implements RedirectStrategy {
    private String registrationUrl;
    
    public RegistrationRedirectStrategy(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }
    
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        // We determine if the user needs to register with HQ
        HttpSession session = request.getSession();
        Boolean needRegistration = (Boolean) session.getAttribute(Constants.NEEDS_REGISTRATION);
        
        if (Boolean.TRUE.equals(needRegistration)) {
            // We need to go to the user registration page
            session.removeAttribute(Constants.NEEDS_REGISTRATION);
            
            response.sendRedirect(this.registrationUrl);
        } else {
            response.sendRedirect(url);
        }
    }
}
