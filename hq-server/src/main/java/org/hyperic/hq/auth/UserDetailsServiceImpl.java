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

package org.hyperic.hq.auth;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.auth.data.PrincipalRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.HQConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserDetailsServiceImpl implements UserDetailsService {

    private PrincipalRepository principalRepository;

    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public UserDetailsServiceImpl(PrincipalRepository principalRepository, AuthzSubjectManager authzSubjectManager) {
        this.principalRepository = principalRepository;
        this.authzSubjectManager = authzSubjectManager;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        Principal principal = principalRepository.findByPrincipal(username);
        if (principal == null) {
            throw new UsernameNotFoundException("Username " + username + " not found");
        }
        boolean enabled = true;
        try {
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(username, HQConstants.ApplicationName);
            enabled = subject.getActive();
        } catch (SubjectNotFoundException fe) {
            // User not found in the authz system. Create it. This could happen
            // with LDAP and/or Kerberos Authentication the first time a user
            // authenticates
            // TODO does this logic belong elsewhere? Revisit when we add
            // LDAP/Kerberos support
            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
            try {
                authzSubjectManager.createSubject(overlord, username, true, HQConstants.ApplicationName, "", "", "",
                    "", "", "", false);
            } catch (PermissionException e) {
                throw new PermissionDeniedDataAccessException("Error adding new subject", e);
            } catch (ApplicationException e) {
                // This should only be thrown if subject already exists. Do
                // nothing
            }
        }

        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

        // ...TODO right now, every user is given the "ROLE USER" grant
        // authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));

        return new User(username, principal.getPassword(), enabled, true, true, true, grantedAuthorities);
    }

}
