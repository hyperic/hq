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

import javax.ejb.AccessLocalException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginException;

import org.hyperic.hq.auth.server.session.UserAudit;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;

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

    public AuthBossEJBImpl() {}

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
            UserAudit.loginAudit(manager.getSubjectPojo(res));
            return res;
        } catch (AccessLocalException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * Login a guest.
     * @param username The name of the user.
     * @param password The password.
     * @return An integer representing the session ID of the logged-in user.
     * @ejb:interface-method
     */
    public int loginGuest () 
        throws SecurityException, LoginException, ApplicationException,
               ConfigPropertyException 
    {
        try {
            AuthzSubject guest =
                getAuthzSubjectManager().findSubjectById(AuthzConstants.guestId);
            
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
            UserAudit.logoutAudit(manager.getSubjectPojo(sessionID));
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
        AuthzSubjectValue subject = manager.getSubject(sessionID);
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
        AuthzSubjectValue subject = manager.getSubject(sessionID);
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
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getAuthManager().isUser(subject, username);
    }

    /**
     * Get a collection of all users
     *
     * @ejb:interface-method
     */
    public Collection getAllUsers(int sessionID)
        throws SessionException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getAuthManager().getAllUsers(subject);
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
