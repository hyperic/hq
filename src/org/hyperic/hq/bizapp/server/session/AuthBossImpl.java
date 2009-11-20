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

import java.util.HashSet;
import java.util.List;

import javax.ejb.AccessLocalException;
import javax.ejb.FinderException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.server.session.UserAudit;
import org.hyperic.hq.auth.server.session.UserLoginZevent;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Auth Subsystem
 * 
 */
@Service
@Transactional
public class AuthBossImpl implements AuthBoss {
    private SessionManager sessionManager;

    private Log log = LogFactory.getLog(AuthBossImpl.class);

    private AuthManager authManager;

    private AuthzSubjectManager authzSubjectManager;

    private ZeventEnqueuer zEventManager;

    @Autowired
    public AuthBossImpl(SessionManager sessionManager, AuthManager authManager,
                        AuthzSubjectManager authzSubjectManager, ZeventEnqueuer zEventManager) {
        this.sessionManager = sessionManager;
        this.authManager = authManager;
        this.authzSubjectManager = authzSubjectManager;
        this.zEventManager = zEventManager;
    }

    public class UserZeventListener implements ZeventListener<UserLoginZevent> {

        public void processEvents(final List<UserLoginZevent> events) {
            // Process events needs to occur within a session due to
            // UserAudit accessing pojo's outside of a Transactional Impl.
            try {
                org.hyperic.hq.hibernate.SessionManager
                    .runInSession(new org.hyperic.hq.hibernate.SessionManager.SessionRunner() {

                        public String getName() {
                            return "UserLoginListener";
                        }

                        public void run() throws Exception {
                            for (UserLoginZevent z : events) {
                                UserLoginZevent.UserLoginZeventPayload p = (UserLoginZevent.UserLoginZeventPayload) z
                                    .getPayload();
                                Integer subjectId = p.getSubjectId();
                                // Re-look up subject
                                AuthzSubject sub = authzSubjectManager.getSubjectById(subjectId);
                                UserAudit.loginAudit(sub);
                            }
                        }
                    });
            } catch (Exception e) {
                log.error("Exception running login audit", e);
            }
        }

        public String toString() {
            return "UserLoginListener";
        }
    }

    /**
     * Add buffered listener to register login audits post commit. This allows
     * for read-only operations to succeed properly when accessed via HQU
     * 
     * 
     */
    public void startup() {
        HashSet<Class<UserLoginZevent>> events = new HashSet<Class<UserLoginZevent>>();
        events.add(UserLoginZevent.class);
        zEventManager.addBufferedListener(events, new UserZeventListener());
    }

    /**
     * Login a user.
     * @param username The name of the user.
     * @param password The password.
     * @return An integer representing the session ID of the logged-in user.
     * 
     */
    public int login(String username, String password) throws SecurityException, LoginException, ApplicationException,
        ConfigPropertyException {
        try {
            int res = authManager.getSessionId(username, password);
            AuthzSubject s = sessionManager.getSubject(res);
            UserLoginZevent evt = new UserLoginZevent(s.getId());
            zEventManager.enqueueEventAfterCommit(evt);
            return res;
        } catch (AccessLocalException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Login a guest.
     * 
     * @return An integer representing the session ID of the logged-in user.
     * 
     */
    public int loginGuest() throws SecurityException, LoginException, ApplicationException, ConfigPropertyException {
        try {
            AuthzSubject guest = authzSubjectManager.getSubjectById(AuthzConstants.guestId);

            if (guest != null && guest.getActive()) {
                return sessionManager.put(guest);
            }
            throw new LoginException("Guest account not enabled");
        } catch (AccessLocalException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Logout a user.
     * @param sessionID The session id for the current user
     * 
     */
    public void logout(int sessionID) {
        try {
            UserAudit.logoutAudit(sessionManager.getSubject(sessionID));
        } catch (SessionException e) {
        }
        sessionManager.invalidate(sessionID);
    }

    /**
     * Check if a user is logged in.
     * @param username The name of the user.
     * @return a boolean| true if logged in and false if not.
     * 
     */
    public boolean isLoggedIn(String username) {
        boolean loggedIn = false;
        try {
            if (sessionManager.getIdFromUsername(username) > 0)
                loggedIn = true;
        } catch (SessionNotFoundException e) {
            // safely ignore
        } catch (SessionTimeoutException e) {
            // safely ignore
        }
        return loggedIn;
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
    public void changePassword(int sessionID, String username, String password) throws FinderException,
        PermissionException, SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        authManager.changePassword(subject, username, password);
    }

    /**
     * Check existence of a user
     * 
     * 
     */
    public boolean isUser(int sessionID, String username) throws SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return authManager.isUser(subject, username);
    }

    public static AuthBoss getOne() {
        return Bootstrap.getBean(AuthBoss.class);
    }
}
