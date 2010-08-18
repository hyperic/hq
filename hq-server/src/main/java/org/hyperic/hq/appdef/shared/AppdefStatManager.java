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

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;

/**
 * Local interface for AppdefStatManager.
 */
public interface AppdefStatManager {
    /**
     * <p>
     * Return map of platform counts.
     * </p>
     */
    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject);

    /**
     * <p>
     * Return platforms count.
     * </p>
     */
    public int getPlatformsCount(AuthzSubject subject);

    /**
     * <p>
     * Return map of server counts.
     * </p>
     */
    public Map<String, Integer> getServerCountsByTypeMap(AuthzSubject subject);

    /**
     * <p>
     * Return servers count.
     * </p>
     */
    public int getServersCount(AuthzSubject subject);

    /**
     * <p>
     * Return map of service counts.
     * </p>
     */
    public Map<String, Integer> getServiceCountsByTypeMap(AuthzSubject subject);

    /**
     * <p>
     * Return services count.
     * </p>
     */
    public int getServicesCount(AuthzSubject subject);

    /**
     * <p>
     * Return map of app counts.
     * </p>
     */
    public Map<String, Integer> getApplicationCountsByTypeMap(AuthzSubject subject);

    /**
     * <p>
     * Return apps count.
     * </p>
     */
    public int getApplicationsCount(AuthzSubject subject);

    /**
     * <p>
     * Return map of grp counts.
     * </p>
     */
    public Map<Integer, Integer> getGroupCountsMap(AuthzSubject subject);

    /**
     * <p>
     * Return directly connected resource tree for node level platform
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, Integer platformId)
        throws PlatformNotFoundException, PermissionException;

    /**
     * <p>
     * Return directly connected resource tree for node level server
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject, Integer serverId)
        throws ServerNotFoundException, PermissionException;

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject, Integer serviceId)
        throws ServiceNotFoundException, PermissionException;

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject, Integer appId)
        throws ApplicationNotFoundException, PermissionException;

    /**
     * <p>
     * Return resources for autogroups
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject, AppdefEntityID[] parents, Integer resType)
        throws AppdefEntityNotFoundException, PermissionException;

    /**
     * <p>
     * Return resources for groups (not autogroups)
     * </p>
     */
    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, Integer groupId) throws PermissionException;

}
