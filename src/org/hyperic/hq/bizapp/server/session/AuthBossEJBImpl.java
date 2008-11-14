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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;

import javax.ejb.AccessLocalException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginException;

import org.hyperic.hq.auth.server.session.UserAudit;
import org.hyperic.hq.auth.server.session.UserLoginZevent;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.bizapp.shared.AuthBossUtil;
import org.hyperic.hq.bizapp.shared.AuthBossLocal;
import org.hyperic.util.ConfigPropertyException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/** 
 * The BizApp's interface to the Auth Subsystem
 *
 * @ejb:bean name="AuthBoss"
 *      jndi-name="ejb/bizapp/AuthBoss"
 *      local-jndi-name="LocalAuthBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class AuthBossEJBImpl extends BizappSessionEJB implements SessionBean {
    private SessionManager manager = SessionManager.getInstance();

    private static Log _log = LogFactory.getLog(AuthBossEJBImpl.class);

    public AuthBossEJBImpl() {}

    public class UserZeventListener implements ZeventListener {

        public void processEvents(final List events) {
            // Process events needs to occur within a session due to
            // UserAudit accessing pojo's outside of an EJBImpl.
            try {
                org.hyperic.hq.hibernate.SessionManager.runInSession(
                    new org.hyperic.hq.hibernate.SessionManager.SessionRunner() {

                    public String getName() {
                        return "UserLoginListener";
                    }

                    public void run() throws Exception {
                        for (Iterator i = events.iterator(); i.hasNext();) {

                            UserLoginZevent z = (UserLoginZevent) i.next();
                            UserLoginZevent.UserLoginZeventPayload p =
                                    (UserLoginZevent.UserLoginZeventPayload) z.getPayload();
                            Integer subjectId = p.getSubjectId();
                            // Re-look up subject
                            AuthzSubject sub =
                                    getAuthzSubjectManager().getSubjectById(subjectId);
                            UserAudit.loginAudit(sub);
                        }
                    }
                });
            } catch (Exception e) {
                _log.error("Exception running login audit", e);
            }
        }

        public String toString() {
            return "UserLoginListener";
        }
    }

    /**
     * Add buffered listener to register login audits post commit.  This
     * allows for read-only operations to succeed properly when accessed
     * via HQU
     *
     * @ejb:interface-method
     */
    public void startup() {
        HashSet events = new HashSet();
        events.add(UserLoginZevent.class);
        ZeventManager.getInstance().addBufferedListener(events,
                                                        new UserZeventListener());
    }

    /**
     * Login a user.
     * @param username The name of the user.
     * @param password The password.
     * @return An integer representing the session ID of the logged-in user.
     * @ejb:interface-method
     */
    public int login ( String username, String password ) 
        throws SecurityException, LoginException, ApplicationException,
               ConfigPropertyException 
    {
        try {
            int res = getAuthManager().getSessionId(username, password);
            AuthzSubject s = manager.getSubject(res);
            UserLoginZevent evt = new UserLoginZevent(s.getId());
            ZeventManager.getInstance().enqueueEventAfterCommit(evt);

            return res;
        } catch (AccessLocalException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Login a guest.
     *
     * @return An integer representing the session ID of the logged-in user.
     * @ejb:interface-method
     */
    public int loginGuest () 
        throws SecurityException, LoginException, ApplicationException,
               ConfigPropertyException 
    {
        try {
            AuthzSubject guest =
                getAuthzSubjectManager().getSubjectById(AuthzConstants.guestId);
            
            if (guest != null && guest.getActive()) {
                return manager.put(guest);
            }
            throw new LoginException("Guest account not enabled");
        } catch (AccessLocalException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Logout a user.
     * @param sessionID The session id for the current user
     * @ejb:interface-method
     */
    public void logout (int sessionID) {
        try {
            UserAudit.logoutAudit(manager.getSubject(sessionID));
        } catch(SessionException e) {
        }
        manager.invalidate(sessionID);
    }

    /**
     * Check if a user is logged in.
     * @param username The name of the user.
     * @return a boolean| true if logged in and false if not.
     * @ejb:interface-method
     */
    public boolean isLoggedIn(String username) {
        boolean loggedIn = false;
        try {
            if (manager.getIdFromUsername(username) > 0)
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
     * @ejb:interface-method
     */
    public void addUser(int sessionID, String username, String password)
        throws SessionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        getAuthManager().addUser(subject, username, password);
    }

    /**
     * Change a password for a user
     * @param sessionID The session id for the current user
     * @param username The user whose password should be updated
     * @param password The new password for the user
     *
     * @ejb:interface-method
     */
    public void changePassword(int sessionID, String username, String password) 
        throws FinderException, PermissionException, SessionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        getAuthManager().changePassword(subject, username, password);
    }

    /**
     * Check existence of a user
     *
     * @ejb:interface-method
     */
    public boolean isUser(int sessionID, String username)
        throws SessionTimeoutException, SessionNotFoundException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getAuthManager().isUser(subject, username);
    }

    public static AuthBossLocal getOne() {
        try {
            return AuthBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
