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
package org.hyperic.hq.auth.shared;

import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;

/**
 * Local interface for AuthManager.
 */
public interface AuthManager {

    
    /**
     * Authenticate a user.
     * @param username The name of the user.
     * @param password The password.
     */
    public void authenticate(String username, String password);
    
    
    /**
     * Add a user to the internal database
     * @param subject The subject of the currently logged in user
     * @param username The username to add
     * @param password The password for this user
     */
    public void addUser(AuthzSubject subject, String username, String password);

    /**
     * Change the password for a user.
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     */
    public void changePassword(AuthzSubject subject, String username, String password) throws PermissionException;

    /**
     * Change the hashed password for a user.
     * @param subject The subject of the currently logged in user
     * @param username The username whose password will be changed.
     * @param password The new password for this user
     */
    public void changePasswordHash(AuthzSubject subject, String username, String hash) throws PermissionException;

    /**
     * Delete a user from the internal database
     * @param subject The subject of the currently logged in user
     * @param username The user to delete
     */
    public void deleteUser(AuthzSubject subject, String username);

    /**
     * Check existence of a user
     * @param subject The subject of the currently logged in user
     * @param username The username of the user to get
     */
    public boolean isUser(AuthzSubject subject, String username);

    /**
     * Get the principle of a user
     * @param subject The subject for whom to return the principle
     */
    public Principal getPrincipal(AuthzSubject subject);

}
