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
package org.hyperic.hq.bizapp.shared;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;

/**
 * Local interface for AuthBoss.
 */
public interface AuthBoss {
    /**
     * Get a session ID based on username only
     * @param user The user to authenticate
     * @return session id that is associated with the user
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     */
    public int getUnauthSessionId(String user) throws ApplicationException;

    /**
     * Authenticate a user
     * @param username The username
     * @param password The password
     */
    void authenticate(String username, String password);

    /**
     * Add a user to the internal database
     * @param sessionID The session id for the current user
     * @param username The username to add
     * @param password The password for this user
     */
    public void addUser(int sessionID, String username, String password) throws SessionException;

    /**
     * Change a password for a user
     * @param sessionID The session id for the current user
     * @param username The user whose password should be updated
     * @param password The new password for the user
     */
    public void changePassword(int sessionID, String username, String password) throws PermissionException,
        SessionException;

    /**
     * Check existence of a user
     */
    public boolean isUser(int sessionID, String username) throws SessionTimeoutException, SessionNotFoundException;

}
