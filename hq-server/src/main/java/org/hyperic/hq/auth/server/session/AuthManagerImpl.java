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

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.dao.PrincipalDAO;
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

    private final Log log = LogFactory.getLog(AuthManagerImpl.class);
    private final PrincipalDAO principalDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthzSubjectManager authzSubjectManager;

    @Autowired
    public AuthManagerImpl(PrincipalDAO principalDao, AuthzSubjectManager authzSubjectManager,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager) {
        this.principalDao = principalDao;
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
        principalDao.create(username, passwordHash);
    }

    /**
     * Change the password for a user.
     * 
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     */
    @Transactional(readOnly = false)
    public void changePassword(AuthzSubject subject, String username, String password)
        throws PermissionException {
        // AUTHZ check
        checkChangePassword(subject, username);
        if (!subject.getName().equals(username)) {
            // users can change their own passwords... only
            // peeps with modifyUsers can modify other
            authzSubjectManager.checkModifyUsers(subject);
        }
        Principal local = principalDao.findByUsername(username);
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
    @Transactional(readOnly = false)
    public void changePasswordHash(AuthzSubject subject, String username, String hash)
        throws PermissionException {
        // AUTHZ check
        checkChangePassword(subject, username);
        if (!subject.getName().equals(username)) {
            // users can change their own passwords... only
            // peeps with modifyUsers can modify other
            authzSubjectManager.checkModifyUsers(subject);
        }
        Principal local = principalDao.findByUsername(username);
        if (local != null) {
            local.setPassword(hash);
        } else {
            principalDao.create(username, hash);
        }
    }

    private void checkChangePassword(AuthzSubject subject, String username) throws PermissionException {
        AuthzSubject target = authzSubjectManager.findSubjectByName(username);
        AuthzSubject user = authzSubjectManager.findSubjectById(subject.getId());
        final boolean isTargetAdmin = isSuperUser(target);
        final boolean isUserAdmin = isSuperUser(user);
        if (log.isDebugEnabled()) {
            log.debug("[changePassword] target user isSuperUser()=" + isTargetAdmin);
            log.debug("[changePassword] loged  user isSuperUser()=" + isSuperUser(user));
        }

        if (isTargetAdmin && !isUserAdmin) {
            PermissionException ex = new PermissionException("Only admin user can change admin user passwords");
            log.error(ex);
            throw ex;
        }
    }
    
    /**
     * Delete a user from the internal database
     * 
     * @param subject The subject of the currently logged in user
     * @param username The user to delete XXX: Shouldn't this check permissions?
     */
    public void deleteUser(AuthzSubject subject, String username) {
        Principal local = principalDao.findByUsername(username);

        // Principal does not exist for users authenticated by other JAAS
        // providers
        if (local != null) {
            principalDao.remove(local);
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
        return principalDao.findByUsername(username) != null;
    }

    /**
     * Get the principle of a user
     * 
     * @param subject The subject for whom to return the principle
     */
    @Transactional(readOnly = true)
    public Principal getPrincipal(AuthzSubject subject) {
        return principalDao.findByUsername(subject.getName());
    }

    private boolean isSuperUser(AuthzSubject subject) {
        if (subject.getId().equals(AuthzConstants.overlordId)) {
            return true;
        }
        final Collection<Role> roles = subject.getRoles();
        for (final Role role : roles) {
            if (role.getId().equals(AuthzConstants.rootRoleId)) {
                return true;
            }
        }
        return false;
    }
}
