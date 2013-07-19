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

import java.util.Collection;
import java.util.List;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.vm.VmMapping;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for PlatformManager.
 */
public interface PlatformManager {

    /**
     * Create a PlatformType
     * @param name The name of the platform type
     * @param plugin The name of the plugin managing the platformType
     * @return
     * @throws NotFoundException
     */
    PlatformType createPlatformType(String name, String plugin) throws NotFoundException;

    /**
     * Find a PlatformType by id
     */
    public PlatformType findPlatformType(Integer id) throws ObjectNotFoundException;

    /**
     * Find a platform type by name
     * @param type - name of the platform type
     * @return platformTypeValue
     */
    public PlatformType findPlatformTypeByName(String type) throws PlatformNotFoundException;

    public Collection<PlatformType> findAllPlatformTypes();

    public Collection<PlatformType> findSupportedPlatformTypes();

    public Collection<PlatformType> findUnsupportedPlatformTypes();

    public Resource findResource(PlatformType pt);

    /**
     * Find all platform types
     * @return List of PlatformTypeValues
     */
    public PageList<PlatformTypeValue> getAllPlatformTypes(AuthzSubject subject, PageControl pc);

    /**
     * Find viewable platform types
     * @return List of PlatformTypeValues
     */
    public PageList<PlatformTypeValue> getViewablePlatformTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * Get PlatformPluginName for an entity id. There is no authz in this method
     * because it is not needed.
     * @return name of the plugin for the entity's platform such as
     *         "Apache 2.0 Linux". It is used as to look up plugins via a
     *         generic plugin manager.
     */
    public String getPlatformPluginName(AppdefEntityID id) throws AppdefEntityNotFoundException;

    /**
     * Delete a platform
     * @param subject The user performing the delete operation.
     * @param id - The id of the Platform
     */
    public void removePlatform(AuthzSubject subject, Platform platform)
        throws PlatformNotFoundException, PermissionException, VetoException;

    public void handleResourceDelete(Resource resource);

    /**
     * Create a Platform of a specified type
     */
    public Platform createPlatform(AuthzSubject subject, Integer platformTypeId,
                                   PlatformValue pValue, Integer agentPK)
        throws ValidationException, PermissionException, AppdefDuplicateNameException,
        AppdefDuplicateFQDNException, ApplicationException;

    /**
     * Create a Platform from an AIPlatform
     * @param aipValue the AIPlatform to create as a regular appdef platform.
     */
    public Platform createPlatform(AuthzSubject subject, AIPlatformValue aipValue)
        throws ApplicationException;

    /**
     * Get all platforms.
     * @param subject The subject trying to list platforms.
     * @param pc a PageControl object which determines the size of the page and
     *        the sorting, if any.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view.
     */
    public PageList<PlatformValue> getAllPlatforms(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * Get platforms created within a given time range.
     * @param subject The subject trying to list platforms.
     * @param range The range in milliseconds.
     * @param size The number of platforms to return.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view that were created
     *         within the given range.
     */
    public PageList<PlatformValue> getRecentPlatforms(AuthzSubject subject, long range, int size)
        throws PermissionException, NotFoundException;

    /**
     * Get platform light value by id. Does not check permission.
     */
    public Platform getPlatformById(AuthzSubject subject, Integer id)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Find a Platform by Id.
     * @param id The id to look up.
     * @return A Platform object representing this Platform.
     * @throws PlatformNotFoundException If the given Platform is not found.
     */
    public Platform findPlatformById(Integer id) throws PlatformNotFoundException;

    /**
     * Finds platform by AI platform when it is EXPECTED to be there (when the
     * AIPlatform queuestatus represents a change or removal). Avoids the poor-performing
     * isAgentPorker method used by other AI use cases
     * @param subject The user performing the action
     * @param aiPlatform The AIPlatform that may represent a change to an
     *        existing platform
     * @return The existing Platform
     * @throws PermissionException
     * @throws PlatformNotFoundException If a Platform that matches IP addresses
     *         or FQDN cannot be found
     */
    Platform findPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform)
        throws PermissionException, PlatformNotFoundException;

    /**
     * Get the Platform object based on an AIPlatformValue. Checks against FQDN,
     * CertDN, then checks to see if all IP addresses match. If all of these
     * checks fail null is returned.
     */
    public Platform getPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform)
        throws PermissionException;

    public Platform getPhysPlatformByAgentToken(String agentToken);

    /**
     * Find a platform by name
     * @param subject - who is trying this
     * @param name - the name of the platform
     */
    public PlatformValue getPlatformByName(AuthzSubject subject, String name)
        throws PlatformNotFoundException, PermissionException;

    public Platform getPlatformByName(String name);

    /**
     * Get the Platform that has the specified Fqdn
     */
    public Platform findPlatformByFqdn(AuthzSubject subject, String fqdn)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Get the Collection of platforms that have the specified Ip address
     */
    public Collection<Platform> getPlatformByIpAddr(AuthzSubject subject, String address)
        throws PermissionException;

    /**
     * Get the Collection of platforms that have the specified MAC address
     * 
     * 
     */
    Collection<Platform> getPlatformByMacAddr(AuthzSubject subject, String address)
        throws PermissionException;

    /**
     * Get the associated platform that has the same MAC address as the given
     * resource
     * 
     * 
     */
    Platform getAssociatedPlatformByMacAddress(AuthzSubject subject, Resource r)
        throws PermissionException, PlatformNotFoundException;

    /**
     * Get the platform by agent token
     */
    public Collection<Integer> getPlatformPksByAgentToken(AuthzSubject subject, String agentToken)
        throws PlatformNotFoundException;

    /**
     * Get the platform that hosts the server that provides the specified
     * service.
     * @param subject The subject trying to list services.
     * @param serviceId service ID.
     * @return the Platform
     */
    public PlatformValue getPlatformByService(AuthzSubject subject, Integer serviceId)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Get the platform ID that hosts the server that provides the specified
     * service.
     * @param serviceId service ID.
     * @return the Platform
     */
    public Integer getPlatformIdByService(Integer serviceId) throws PlatformNotFoundException;

    /**
     * Get the platform for a server.
     * @param subject The subject trying to list services.
     * @param serverId Server ID.
     */
    public PlatformValue getPlatformByServer(AuthzSubject subject, Integer serverId)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Get the platform ID for a server.
     * @param serverId Server ID.
     */
    public Integer getPlatformIdByServer(Integer serverId) throws PlatformNotFoundException;

    /**
     * Get the platforms for a list of servers.
     * @param subject The subject trying to list services.
     */
    public PageList<PlatformValue> getPlatformsByServers(AuthzSubject subject,
                                                         List<AppdefEntityID> sIDs)
        throws PlatformNotFoundException, PermissionException;

    /**
     * Get all platforms by application.
     * @param subject The subject trying to list services.
     * @param appId Application ID. but when they are, they should live
     *        somewhere in appdef/shared so that clients can use them too.
     * @return A List of ApplicationValue objects representing all of the
     *         services that the given subject is allowed to view.
     */
    public PageList<PlatformValue> getPlatformsByApplication(AuthzSubject subject, Integer appId,
                                                             PageControl pc)
        throws ApplicationNotFoundException, PlatformNotFoundException, PermissionException;

    /**
     * Get server IDs by server type and platform.
     * @param subject The subject trying to list servers.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    public Integer[] getPlatformIds(AuthzSubject subject, Integer platTypeId)
        throws PermissionException;

    /**
     * Get server IDs by server type and platform.
     * @param subject The subject trying to list servers.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    public List<Platform> getPlatformsByType(AuthzSubject subject, String type)
        throws PermissionException, InvalidAppdefTypeException;

    /**
     * Get the platforms that have an IP with the specified address. If no
     * matches are found, this method DOES NOT throw a
     * PlatformNotFoundException, rather it returns an empty PageList.
     */
    public PageList<PlatformValue> findPlatformsByIpAddr(AuthzSubject subject, String addr,
                                                         PageControl pc) throws PermissionException;

    public List<Platform> findPlatformPojosByTypeAndName(AuthzSubject subj, Integer pType,
                                                         String regEx);

    public List<Platform> findParentPlatformPojosByNetworkRelation(AuthzSubject subj,
                                                                   List<Integer> platformTypeIds,
                                                                   String platformName,
                                                                   Boolean hasChildren);

    public List<Platform> findPlatformPojosByNoNetworkRelation(AuthzSubject subj,
                                                               List<Integer> platformTypeIds,
                                                               String platformName);

    /**
     * Get the platforms that have an IP with the specified address.
     * @return a list of {@link Platform}s
     */
    public Collection<Platform> findPlatformPojosByIpAddr(String addr);

    public Collection<Platform> findDeletedPlatforms();

    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * @param existing - the value object for the platform you want to save
     */
    public Platform updatePlatformImpl(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException;

    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * @param existing - the value object for the platform you want to save
     */
    public Platform updatePlatform(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException;

    /**
     * DevNote: This method was refactored out of updatePlatformTypes. It does
     * not work.
     */
    public void deletePlatformType(PlatformType pt) throws org.hyperic.hq.common.VetoException;

    /**
     * Update platform types
     */
    public void updatePlatformTypes(String plugin, org.hyperic.hq.product.PlatformTypeInfo[] infos)
        throws VetoException, NotFoundException;

    /**
     * Update an existing appdef platform with data from an AI platform.
     * @param aiplatform the AI platform object to use for data
     */
    public void updateWithAI(AIPlatformValue aiplatform, AuthzSubject subj)
        throws PlatformNotFoundException, ApplicationException;

    /**
     * Add an IP to a platform
     */
    public Ip addIp(Platform platform, String address, String netmask, String macAddress);

    /**
     * Update an IP on a platform
     */
    public Ip updateIp(Platform platform, String address, String netmask, String macAddress);

    /**
     * Remove an IP on a platform
     */
    public void removeIp(Platform platform, String address, String netmask, String macAddress);

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * platform type, the second element is the # of platforms of that type in
     * the inventory.
     */
    public List<Object[]> getPlatformTypeCounts();

    public Number getPlatformCount();

    public Number getCpuCount();

    /**
     * Fetches the {@link Platform} that the {@link Agent} is running on or null if none exists.
     * Uses PlatformDetector.isSupportedPlatform() to determine if the agent can run on the platform
     * if the agent has multiple associated platforms.
     * @param agentId
     * @return {@link Platform} associated with the agentId, may return null.
     * @see PlatformDetector#isSupportedPlatform(String)
     */
    public Platform getPlatformByAgentId(Integer agentId);

    /**
     * @param superUser - must be a super user or else this call will fail
     * @return {@link Collection} of {@link Platform}s
     * @throws PermissionException if the authzsubject is not a SuperUser
     */
    public Collection<Platform> findAll(AuthzSubject superUser) throws PermissionException;

    public Platform getPlatformById(Integer id);

    void mapUUIDToPlatforms(AuthzSubject subject, List<VmMapping> mapping) throws PermissionException, CPropKeyNotFoundException;

    void removePlatformVmMapping(AuthzSubject subject, List<String> macAddresses) throws PermissionException;

    Platform getPlatformByResourceId(int id);

    public Collection<Platform> getOrphanedPlatforms();

}
