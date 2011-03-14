/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.auth.server.session;

import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.data.PrincipalRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AuthManger
 */
@Service
@Transactional(noRollbackFor=AuthenticationException.class)
public class AuthManagerImpl implements AuthManager {

    private PrincipalRepository principalRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public AuthManagerImpl(PrincipalRepository principalRepository, AuthzSubjectManager authzSubjectManager,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager) {
        this.principalRepository = principalRepository;
        this.authzSubjectManager = authzSubjectManager;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            username, password);
        authenticationManager.authenticate(authentication);
    }

    /**
     * Add a user to the internal database
     * 
     * @param subject The subject of the currently logged in user
     * @param username The username to add
     * @param password The password for this user XXX: Shouldn't this check
     *        permissions?
     */
    public void addUser(AuthzSubject subject, String username, String password) {
        // All passwords are stored encrypted
        String passwordHash = passwordEncoder.encodePassword(password, null);
        Principal principal = new Principal();
        principal.setPrincipal(username);
        principal.setPassword(passwordHash);
        principalRepository.save(principal);
    }

    /**
     * Change the password for a user.
     * 
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     */
    public void changePassword(AuthzSubject subject, String username, String password)
        throws PermissionException {
        // AUTHZ check
        if (!subject.getName().equals(username)) {
            // users can change their own passwords... only
            // peeps with modifyUsers can modify other
            authzSubjectManager.checkModifyUsers(subject);
        }
        Principal local = principalRepository.findByPrincipal(username);
        String hash = passwordEncoder.encodePassword(password, null);
        local.setPassword(hash);
    }

    /**
     * Change the hashed password for a user.
     * 
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     */
    public void changePasswordHash(AuthzSubject subject, String username, String hash)
        throws PermissionException {
        // AUTHZ check
        if (!subject.getName().equals(username)) {
            // users can change their own passwords... only
            // peeps with modifyUsers can modify other
            authzSubjectManager.checkModifyUsers(subject);
        }
        Principal local = principalRepository.findByPrincipal(username);
        if (local != null) {
            local.setPassword(hash);
            principalRepository.save(local);
        } else {
            Principal principal = new Principal();
            principal.setPrincipal(username);
            principal.setPassword(hash);
            principalRepository.save(principal);
        }
    }

    /**
     * Delete a user from the internal database
     * 
     * @param subject The subject of the currently logged in user
     * @param username The user to delete XXX: Shouldn't this check permissions?
     */
    public void deleteUser(AuthzSubject subject, String username) {
        Principal local = principalRepository.findByPrincipal(username);

        // Principal does not exist for users authenticated by other JAAS
        // providers
        if (local != null) {
            principalRepository.delete(local);
        }
    }

    /**
     * Check existence of a user
     * 
     * @param subject The subject of the currently logged in user
     * @param username The username of the user to get
     */
    @Transactional(readOnly = true)
    public boolean isUser(AuthzSubject subject, String username) {
        return principalRepository.findByPrincipal(username) != null;
    }

    /**
     * Get the principle of a user
     * 
     * @param subject The subject for whom to return the principle
     */
    @Transactional(readOnly = true)
    public Principal getPrincipal(AuthzSubject subject) {
        return principalRepository.findByPrincipal(subject.getName());
    }
}
