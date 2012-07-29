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

package org.hyperic.hq.security;

import java.util.Properties;

import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
/**
 * Internal authenticators for HQ
 * @author jhickey
 *
 */
public interface HQAuthenticationProvider extends Ordered {

    /**
     * 
     * @param serverConfigProps The server configuration properties, should
     *        include authentication-related props
     * @param authDetails 
     * @return true if the server configuration indicates that this type of
     *         authentication should be used
     */
    boolean supports(Properties serverConfigProps, Object authDetails);

    /**
     * 
     * @param username The name of the user to authenticate
     * @param password The password
     * @return The authenticated user's information as retrieved and populated
     *         by the specific implementation. Should not be null.
     * @throws AuthenticationException If user cannot be authenticated or if an
     *         error occurs attempting to authenticate
     */
    Authentication authenticate(String username, String password) throws AuthenticationException;

}
