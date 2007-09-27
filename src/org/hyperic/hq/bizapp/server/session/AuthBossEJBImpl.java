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
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
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

    private SessionContext sessionCtx = null;

    private SessionManager manager = SessionManager.getInstance();

    /** Creates a new instance of AuthBoss */
    public AuthBossEJBImpl() {}

    /**
     * Login a user.
     * @param username The name of the user.
     * @param password The password.
     * @return An integer representing the session ID of the logged-in user.
     * @exception SecurityException If we can't set the authentication config
     * @exception LoginException If we are passed bad credentials
     * @exception NamingException If we can't find AuthManagerLocalHome
     * @exception CreateException If we can't create an AuthManager
     * @ejb:interface-method
     */
    public int login ( String username,
                       String password ) 
        throws SecurityException, LoginException, ApplicationException,
               ConfigPropertyException {
        try {
            return getAuthManager().getSessionId(username, password);
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
     * @exception NamingException If we can't find the PrinipalsLocalHome
     * @exception CreateExceptoin If we try to add a duplicate user
     *
     * @ejb:interface-method
     */
    public void addUser(int sessionID, String username, String password)
        throws SessionTimeoutException, SessionNotFoundException,
               CreateException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        try {
            getAuthManager().addUser(subject, username, password);
        } catch (NamingException e) {
            throw new SystemException("NamingException in addUser", e);
        }
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
        throws FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        getAuthManager().changePassword(subject, username, password);
    }

    /**
     * Check existence of a user
     *
     * @exception NamingException If we can't find PrincipalsLocalHome
     * @exception FinderException
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
     * @exception NamingException If we can't find PrincipalsLocalHome
     * @exception FinderException
     *
     * @ejb:interface-method
     */
    public Collection getAllUsers(int sessionID)
        throws FinderException, 
               SessionTimeoutException, SessionNotFoundException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        try {
            return getAuthManager().getAllUsers(subject);
        } catch (NamingException e) {
            throw new SystemException("NamingException in getAllUsers", e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {
        sessionCtx = ctx;
    }
}
