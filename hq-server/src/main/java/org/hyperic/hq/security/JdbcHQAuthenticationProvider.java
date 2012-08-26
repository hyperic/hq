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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.PrincipalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link HQAuthenticationProvider} that authenticates users
 * from HQ's database store
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Component
public class JdbcHQAuthenticationProvider implements HQAuthenticationProvider {

    private final Log log = LogFactory.getLog(JdbcHQAuthenticationProvider.class.getName());
    private PrincipalDAO principalDao;
    private AuthzSubjectManager authzSubjectManager;
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    public JdbcHQAuthenticationProvider(PrincipalDAO principalDao,
                                        AuthzSubjectManager authzSubjectManager,
                                        PasswordEncoder passwordEncoder) {
        this.principalDao = principalDao;
        this.authzSubjectManager = authzSubjectManager;
        this.passwordEncoder = passwordEncoder;
    }

    public Authentication authenticate(String username, String password) {
        final boolean debug = log.isDebugEnabled();
        try {
            // TODO We shouldn't have to make two calls here...
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(username,
                HQConstants.ApplicationName);
            Principal principal = principalDao.findByUsername(username);

            if (password == null || principal == null) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed because password being null.");
                }

                throw new BadCredentialsException("login.error.login");
            } else if (!subject.getActive()) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed because account is disabled.");
                }

                throw new UserDisabledException();
            } else if (!passwordEncoder.isPasswordValid(principal.getPassword(), password, null)) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed due to a login error.");
                }

                throw new BadCredentialsException("login.error.login");
            }

            if (debug) {
                log.debug("Logged in as [" + username + "]");
            }
        } catch (SubjectNotFoundException e) {
            if (debug) {
                log.debug("Authentication of user {" + username + "} failed due to a login error.",
                    e);
            }
            throw new BadCredentialsException("login.error.login");
        }
        // ...finally, we need to create a list grant authorities for Spring
        // Security...
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

        // ...TODO right now, every user is given the "ROLE HQ USER" grant
        // authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_HQ_USER"));

        return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities);
    }

    public boolean supports(Properties serverConfigProps, Object authDetails) {
    	if ((null == authDetails) || (!(authDetails instanceof HQAuthenticationDetails))) {
    		return true;
    	}
    	HQAuthenticationDetails hqAuthDetails = (HQAuthenticationDetails)authDetails;
    	if (hqAuthDetails.isUsingExternalAuth()) {
    		return false;
    	}
        return true;
    }

    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
