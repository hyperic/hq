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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.auth.shared.AuthManagerLocal;
import org.hyperic.hq.auth.shared.AuthManagerUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.PrincipalDAO;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.util.ConfigPropertyException;
import org.jboss.security.Util;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

/** The AuthManger
 *
 * @ejb:bean name="AuthManager"
 *      jndi-name="ejb/auth/AuthManager"
 *      local-jndi-name="LocalAuthManager"
 *      view-type="local"
 *      type="Stateless"
 */

public class AuthManagerEJBImpl implements SessionBean {

    // Always authenticate against the HQ application realm
    private static final String appName = HQConstants.ApplicationName;

    public AuthManagerEJBImpl() {}


    private boolean isReady() {
        return ProductManagerEJBImpl.getOne().isReady();
    }

    /**
     * Authenticates the user using the given password
     * @param user The user to authenticate
     * @param password The password for the user
     * @return session id that is associated with the user
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public int getSessionId(String user, String password)
        throws SecurityException, LoginException, ConfigPropertyException,
               ApplicationException 
    {
        if(password == null)
            throw new LoginException("No password was given");

        if (!isReady()) {
            throw new LoginException("Server still starting");
        }

        UsernamePasswordHandler handler =
            new UsernamePasswordHandler(user, password.toCharArray());

        LoginContext loginContext = new LoginContext(appName, handler);
        loginContext.login();
        loginContext.logout();

        // User is authenticated, get the id from the authz system
        // and return an id from the Session Manager
        AuthzSubjectManagerLocal subjMan = 
            AuthzSubjectManagerEJBImpl.getOne();
        
        AuthzSubject subject;
        try {
            subject = subjMan.findSubjectByAuth(user, appName);
            if (!subject.getActive()) {
                throw new LoginException("User account has been disabled.");
            }
        } catch (SubjectNotFoundException fe) {
            // User not found in the authz system.  Create it.
            try {
                AuthzSubject overlord = subjMan.getOverlordPojo();
                subject = subjMan.createSubject(overlord, user, true, appName,
                                                "", "", "", "", "", "", false);
            } catch (CreateException e) {
                throw new ApplicationException("Unable to add user to " +
                                               "authorization system", e);
            }
        }

        return SessionManager.getInstance().put(subject);
    }

    /**
     * Get a session ID based on username only
     * @param user The user to authenticate
     * @return session id that is associated with the user
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public int getUnauthSessionId(String user)
        throws ApplicationException 
    {
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
            AuthzSubjectManagerLocal subjMgrLocal = 
                AuthzSubjectManagerEJBImpl.getOne(); 
        
            AuthzSubject subject =
                subjMgrLocal.findSubjectByAuth(user, appName);
            if (!subject.getActive()) {
                throw new SessionNotFoundException(
                    "User account has been disabled.");
            }

            return mgr.put(subject, 30000);     // 30 seconds only
        } catch (SubjectNotFoundException e) {
            throw new SessionNotFoundException("Unable to find user " + user +
                                               " to create session");
        }
    }

    /**
     * Add a user to the internal database
     *
     * @param subject The subject of the currently logged in user
     * @param username The username to add
     * @param password The password for this user
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * XXX: Shouldn't this check permissions?
     */
    public void addUser(AuthzSubject subject,
                        String username, String password)
    {
        PrincipalDAO lhome = DAOFactory.getDAOFactory().getPrincipalDAO();
        lhome.create(username, password);
    }
    
    /**
     * Change the password for a user.
     *
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void changePassword(AuthzSubject subject,
                               String username, String password)
        throws PermissionException
    {
        // AUTHZ check
        if(!subject.getName().equals(username)) {
            // users can change their own passwords... only
            // peeps with modifyUsers can modify other 
            AuthzSubjectManagerEJBImpl.getOne().checkModifyUsers(subject);
        }
        PrincipalDAO lhome = DAOFactory.getDAOFactory().getPrincipalDAO();
        Principal local = lhome.findByUsername(username);
        // hash the password as is done in ejbCreate. Fixes 4661
        String hash = Util.createPasswordHash("MD5", "base64",
                                              null, null, password);
        local.setPassword(hash);
    }

    /**
     * Delete a user from the internal database
     *
     * @param subject The subject of the currently logged in user
     * @param username The user to delete
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * XXX: Shouldn't this check permissions?
     */
    public void deleteUser(AuthzSubject subject, String username) {
        PrincipalDAO lhome = DAOFactory.getDAOFactory().getPrincipalDAO();
        Principal local = lhome.findByUsername(username);

        // Principal does not exist for users authenticated by other JAAS
        // providers
        if (local != null) {
            lhome.remove(local);
        }
    }

    /**
     * Check existence of a user
     *
     * @param subject The subject of the currently logged in user
     * @param username The username of the user to get
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public boolean isUser(AuthzSubject subject, String username) {
        PrincipalDAO lhome = DAOFactory.getDAOFactory().getPrincipalDAO();
        return lhome.findByUsername(username) != null;
    }

    /**
     * Get a collection of all users
     *
     * @param subject The subject of the currently logged in user
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Collection getAllUsers(AuthzSubject subject) {
        PrincipalDAO lhome = DAOFactory.getDAOFactory().getPrincipalDAO();
        
        Collection principals = lhome.findAllUsers();
        Collection users = new ArrayList();
        
        for (Iterator i = principals.iterator(); i.hasNext();) {
            Principal p = (Principal)i.next();
            users.add(p.getPrincipal());
        }
        
        return users;
    }

    public static AuthManagerLocal getOne() {
        try {
            return AuthManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
