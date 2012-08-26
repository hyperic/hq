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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

/**
 * This class is responsible for authenticating a user using HQ's internal user
 * store. It can also be configured to enable guest user access as well as
 * override the guest username.
 */
public class InternalAuthenticationProvider implements AuthenticationProvider {

    private final AuthzSubjectManager authzSubjectManager;
    private final Set<HQAuthenticationProvider> hqAuthenticationProviders;
    private final ServerConfigManager serverConfigManager;

    public InternalAuthenticationProvider(AuthzSubjectManager authzSubjectManager,
                                          Set<HQAuthenticationProvider> hqAuthenticationProviders, ServerConfigManager serverConfigManager) {
        this.authzSubjectManager = authzSubjectManager;
        this.hqAuthenticationProviders = hqAuthenticationProviders;
        this.serverConfigManager = serverConfigManager;
    }

    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Check if we're dealing with a guest user...
        AuthzSubject guestUser = authzSubjectManager.getSubjectById(AuthzConstants.guestId);

        if (guestUser == null || !guestUser.getActive() ||
            !guestUser.getName().equalsIgnoreCase(username)) {
            // ...we're not dealing with a guest authentication...
            AuthenticationException lastException = null;
            Authentication result = null;
            Properties serverConfig;
            try {
                serverConfig = serverConfigManager.getConfig();
            } catch (ConfigPropertyException e) {
                throw new AuthenticationServiceException(
                    "Unable to read server configuration to determine authentication type", e);
            }
            for (HQAuthenticationProvider authProvider : hqAuthenticationProviders) {
                if (authProvider.supports(serverConfig, authentication.getDetails())) {
                    try {
                        result = authProvider.authenticate(username, password);
                    }catch (UserDisabledException e) {
                    	throw e;
                    }catch (AuthenticationException e) {
                        lastException = e;
                    } catch (Exception e) {
						lastException = new AuthenticationException(e.getMessage(), e) {
						};
					}
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (lastException != null) {
                throw lastException;
            }
        }
       //Return a token for guest user
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        // ...TODO right now, every user is given the "ROLE HQ USER" grant authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_HQ_USER"));
        return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities);
    }

    public boolean supports(Class<? extends Object> authentication) {
        // ...for now, we only support objects that implement Authentication if
        // we find that we need more information
        // we can add some interfaces to make sure we get that information and
        // check for it here...
        return Authentication.class.getClass().isInstance(authentication);
    }
}
