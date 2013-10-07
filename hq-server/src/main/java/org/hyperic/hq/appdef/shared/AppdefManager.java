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
package org.hyperic.hq.appdef.shared;

import java.util.Map;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;

/**
 * Local interface for AppdefManager.
 */
public interface AppdefManager {
    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityID> getControllablePlatformTypes(AuthzSubject subject) throws PermissionException;

    /**
     * Get controllable platform types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityID> getControllablePlatformNames(AuthzSubject subject, int tid)
        throws PermissionException;

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityTypeID> getControllableServerTypes(AuthzSubject subject) throws PermissionException;

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityID> getControllableServerNames(AuthzSubject subject, int tid)
        throws PermissionException;

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityTypeID> getControllableServiceTypes(AuthzSubject subject) throws PermissionException;

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     */
    public Map<String, AppdefEntityID> getControllableServiceNames(AuthzSubject subject, int tid)
        throws PermissionException;

    /**
     * Change appdef entity owner
     */
    public void changeOwner(AuthzSubject who, AppdefResource res, AuthzSubject newOwner) throws PermissionException,
        ServerNotFoundException;
    
    
    /**
     * Removes an appdef entity by nulling out any reference from its children
     * and then deleting it synchronously. The children are then cleaned up in
     * the zevent queue by issuing a {@link ResourcesCleanupZevent}
     * @param aeid {@link AppdefEntityID} resource to be removed.
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     */
    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid) 
        throws SessionNotFoundException, SessionTimeoutException, ApplicationException, VetoException;

    /**
     * Removes an appdef entity by nulling out any reference from its children
     * and then deleting it synchronously. The children are then cleaned up in
     * the zevent queue by issuing a {@link ResourcesCleanupZevent}
     * @param aeid {@link AppdefEntityID} resource to be removed.
     * @param removeAllVirtual tells the method to remove all resources, including
     *        associated platforms, under the virtual resource hierarchy
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     */
    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid,
                                               boolean removeAllVirtual) 
        throws SessionNotFoundException, SessionTimeoutException, ApplicationException, VetoException;
    
    public  AppdefEntityID[] removeAppdefEntity(final Resource res,  Integer id,  int type, final AuthzSubject subject, boolean removeAllVirtual ) throws VetoException, ApplicationException;
    
    public void removeServer(AuthzSubject subj, Integer serverId)
            throws ServerNotFoundException, SessionNotFoundException, SessionTimeoutException, PermissionException,
                   SessionException, VetoException;
    
    public void removePlatform(AuthzSubject subject, Integer platformId)
            throws ApplicationException, VetoException;
    
    public void removeService(AuthzSubject subject, Integer serviceId)
            throws VetoException, PermissionException, ServiceNotFoundException;
    
    public Agent findResourceAgent(AppdefEntityID entityId) throws AppdefEntityNotFoundException,
    SessionTimeoutException, SessionNotFoundException, PermissionException, AgentNotFoundException;



}
