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

package org.hyperic.hq.bizapp.server.session;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.HQConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Auth Subsystem TODO this layer just exists to
 * deal directly with the session ID (since service layer should not be aware of
 * HTTP sessions). We may be able to remove this once we properly integrate
 * Spring Security context holder and possibly get rid of SessionManager
 * 
 */
@Service
@Transactional
public class AuthBossImpl implements AuthBoss {
    private SessionManager sessionManager;

    private AuthManager authManager;

    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public AuthBossImpl(SessionManager sessionManager, AuthManager authManager, AuthzSubjectManager authzSubjectManager) {
        this.sessionManager = sessionManager;
        this.authManager = authManager;
        this.authzSubjectManager = authzSubjectManager;
    }

    /**
     * Get a session ID based on username only
     * @param user The user to authenticate
     * @return session id that is associated with the user
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public int getUnauthSessionId(String user) throws ApplicationException {
        try {
            SessionManager mgr = SessionManager.getInstance();
            try {
                int sessionId = mgr.getIdFromUsername(user);
                if (sessionId > 0)
                    return sessionId;
            } catch (SessionNotFoundException e) {
                // Continue
            }

            // Get the id from the authz system and return an id from the
            // Session Manager
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(user, HQConstants.ApplicationName);
            if (!subject.getActive()) {
                throw new SessionNotFoundException("User account has been disabled.");
            }
            return mgr.put(subject, 30000); // 30 seconds only
        } catch (SubjectNotFoundException e) {
            throw new SessionNotFoundException("Unable to find user " + user + " to create session");
        }
    }

    /**
     * Authenticate a user.
     * @param username The name of the user.
     * @param password The password.
     * 
     */
    public void authenticate(String username, String password) {
        authManager.authenticate(username, password);
    }

    /**
     * Add a user to the internal database
     * 
     * @param sessionID The session id for the current user
     * @param username The username to add
     * @param password The password for this user
     * 
     * 
     */
    public void addUser(int sessionID, String username, String password) throws SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        authManager.addUser(subject, username, password);
    }

    /**
     * Change a password for a user
     * @param sessionID The session id for the current user
     * @param username The user whose password should be updated
     * @param password The new password for the user
     * 
     * 
     */
    public void changePassword(int sessionID, String username, String password) throws PermissionException,
        SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        authManager.changePassword(subject, username, password);
    }

    /**
     * Check existence of a user
     * 
     * 
     */
    @Transactional(readOnly=true)
    public boolean isUser(int sessionID, String username) throws SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return authManager.isUser(subject, username);
    }

}
