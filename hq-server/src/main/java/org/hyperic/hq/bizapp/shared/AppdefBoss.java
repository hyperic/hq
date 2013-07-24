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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;



import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.CpropKey;
import org.hyperic.hq.appdef.server.session.DownResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AppdefBoss.
 */
public interface AppdefBoss {
    /**
     * Find a common appdef resource type among the appdef entities
     * @param sessionID
     * @param aeids the array of appdef entity IDs
     * @return AppdefResourceTypeValue if they are of same type, null otherwise
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @throws SessionNotFoundException
     * @throws SessionTimeoutException
     */
    public AppdefResourceType findCommonResourceType(int sessionID, String[] aeids)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException, SessionTimeoutException;

    /**
     * Find all the platform types defined in the system.
     * @return A list of PlatformTypeValue objects.
     */
    public PageList<PlatformTypeValue> findAllPlatformTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Find all the viewable platform types defined in the system.
     * @return A list of PlatformTypeValue objects.
     */
    public PageList<PlatformTypeValue> findViewablePlatformTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException;

    /**
     * Find all the server types defined in the system.
     * @return A list of ServerTypeValue objects.
     */
    public PageList<ServerTypeValue> findAllServerTypes(int sessionID, PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException;

    /**
     * Find all viewable server types defined in the system.
     * @return A list of ServerTypeValue objects.
     */
    public PageList<ServerTypeValue> findViewableServerTypes(int sessionID, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException;

    public List<AppdefResourceTypeValue> findAllApplicationTypes(int sessionID) throws ApplicationException;

    public ApplicationType findApplicationTypeById(int sessionId, Integer id) throws ApplicationException;

    public PageList<ServiceTypeValue> findAllServiceTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    public PageList<ServiceTypeValue> findViewableServiceTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException;

    public PageList<ServiceTypeValue> findViewablePlatformServiceTypes(int sessionID, Integer platId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    public ApplicationValue findApplicationById(int sessionID, Integer id) throws AppdefEntityNotFoundException,
        PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * <p>
     * Get first-level child resources of a given resource based on the child
     * resource type.
     * </p>
     * <p>
     * For example:
     * <ul>
     * <li><b>platform -</b> list of servers</li>
     * <li><b>server -</b> list of services</li>
     * <li><b>service -</b> <i>not supported</i></li>
     * <li><b>application -</b> list of services</li>
     * <li><b>group -</b> <i>list of members if the group is compatible</i></li>
     * </ul>
     * </p>
     * @param parent the resource whose children we want
     * @param childResourceType the type of child resource
     * @return list of <code>{@link AppdefResourceValue}</code> objects
     */
    public PageList<? extends AppdefResourceValue> findChildResources(int sessionID, AppdefEntityID parent,
                                                                      AppdefEntityTypeID childResourceType,
                                                                      PageControl pc) throws SessionException,
        PermissionException, AppdefEntityNotFoundException;

    public PageList<ApplicationValue> findApplications(int sessionID, AppdefEntityID id, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    public PageList<ServiceValue> findPlatformServices(int sessionID, Integer platformId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    public PageList<ServiceValue> findPlatformServices(int sessionID, Integer platformId, Integer typeId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Find service inventory by application - including services and clusters
     */
    public PageList<AppdefResourceValue> findServiceInventoryByApplication(int sessionID, Integer appId, PageControl pc)
        throws AppdefEntityNotFoundException, SessionException, PermissionException;

    /**
     * Find all services on a server
     * @return A list of ServiceValue objects.
     */
    public PageList<AppdefResourceValue> findServicesByServer(int sessionID, Integer serverId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException;

    /**
     * Find the platform by service.
     */
    public PlatformValue findPlatformByDependentID(int sessionID, AppdefEntityID entityId)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException, PermissionException;

    public ServerValue findServerByService(int sessionID, Integer serviceID) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    public PageList<ServerValue> findServersByTypeAndPlatform(int sessionId, Integer platformId, int adResTypeId,
                                                              PageControl pc) throws AppdefEntityNotFoundException,
        PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Get the virtual server for a given platform and service type
     */
    public ServerValue findVirtualServerByPlatformServiceType(int sessionID, Integer platId, Integer svcTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException, SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Find all servers on a given platform
     * @return A list of ServerValue objects
     */
    public PageList<ServerValue> findServersByPlatform(int sessionID, Integer platformId, PageControl pc)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Get the virtual servers for a given platform
     */
    public PageList<ServerValue> findViewableServersByPlatform(int sessionID, Integer platformId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    public PageList<ServerTypeValue> findServerTypesByPlatform(int sessionID, Integer platformId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    public PageList<ServerTypeValue> findServerTypesByPlatformType(int sessionID, Integer platformId, PageControl pc)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Get all platforms in the inventory.
     * @param sessionID The current session token.
     * @param pc a PageControl object which determines the size of the page and
     *        the sorting, if any.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view.
     */
    public PageList<PlatformValue> findAllPlatforms(int sessionID, PageControl pc) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, NotFoundException;

    /**
     * Get recently created platforms in the inventory.
     * @param sessionID The current session token.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view that was created in the
     *         past time range specified.
     */
    public PageList<PlatformValue> findRecentPlatforms(int sessionID, long range, int size)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException;

    /**
     * Looks up and returns a list of value objects corresponding to the list of
     * appdef entity represented by the instance ids passed in. The method does
     * not require the caller to know the instance-id's corresponding type.
     * Similarly, the return value is upcasted.
     * @return list of appdefResourceValue
     */
    public PageList<AppdefResourceValue> findByIds(int sessionId, AppdefEntityID[] entities, PageControl pc)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Looks up and returns a value object corresponding to the appdef entity
     * represented by the instance id passed in. The method does not require the
     * caller to know the instance-id's corresponding type. Similarly, the
     * return value is upcasted.
     */
    public AppdefResourceValue findById(int sessionId, AppdefEntityID entityId) throws AppdefEntityNotFoundException,
        PermissionException, SessionTimeoutException, SessionNotFoundException;
    /**
     * Looks up and returns a value object corresponding to the appdef entity
     * represented by the instance id passed in. The method does not require the
     * caller to know the instance-id's corresponding type. Similarly, the
     * return value is upcasted.
     */
    public AppdefResourceValue findById(AuthzSubject subject, AppdefEntityID entityId)
            throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException ;

    public PlatformValue findPlatformById(int sessionID, Integer id) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    public Agent findResourceAgent(AppdefEntityID entityId) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException, AgentNotFoundException;

    public ServerValue findServerById(int sessionID, Integer id) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    public ServiceValue findServiceById(int sessionID, Integer id) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    public PageList<AppdefResourceTypeValue> findAllResourceTypes(int sessionId, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    public PageList<AppdefResourceTypeValue> findAllResourceTypes(int sessionId, int entType, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    public Platform createPlatform(int sessionID, PlatformValue platformVal, Integer platTypePK, Integer agent)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException, ApplicationException;

    public AppdefResourceTypeValue findResourceTypeById(int sessionID, AppdefEntityTypeID id)
        throws SessionTimeoutException, SessionNotFoundException;

    public PlatformType findPlatformTypeById(int sessionID, Integer id) throws PlatformNotFoundException,
        SessionTimeoutException, SessionNotFoundException;

    public PlatformType findPlatformTypeByName(int sessionID, String name) throws PlatformNotFoundException,
        SessionTimeoutException, SessionNotFoundException;

    public ServiceType findServiceTypeById(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException;

    public PageList<ServiceTypeValue> findServiceTypesByServerType(int sessionID, int serverTypeId)
        throws SessionTimeoutException, SessionNotFoundException;

    public ServerType findServerTypeById(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException;

    /**
     * Create a server with CProps
     * @param platformPK - the pk of the host platform
     * @param serverTypePK - the type of server
     * @param cProps - the map with Custom Properties for the server
     * @return ServerValue - the saved server
     */
    public ServerValue createServer(int sessionID, ServerValue serverVal, Integer platformPK, Integer serverTypePK,
                                    Map<String, String> cProps) throws ValidationException, SessionTimeoutException,
        SessionNotFoundException, PermissionException, AppdefDuplicateNameException, CPropKeyNotFoundException,
        NotFoundException;

    /**
     * Create an application
     * @return ApplicationValue - the saved application
     */
    public ApplicationValue createApplication(int sessionID, ApplicationValue appVal,
                                              ConfigResponse protoProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
        AppdefDuplicateNameException, NotFoundException;

    public ServiceValue createService(int sessionID, ServiceValue serviceVal, Integer serviceTypePK, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException, ServerNotFoundException, PlatformNotFoundException,
        PermissionException, AppdefDuplicateNameException, ValidationException;

    /**
     * Create a service with CProps
     * @param serviceTypePK - the type of service
     * @param serverPK - the server host
     * @param cProps - the map with Custom Properties for the service
     * @return Service - the saved Service
     */
    public Service createService(AuthzSubject subject, ServiceValue serviceVal, Integer serviceTypePK,
                                 Integer serverPK, Map<String, String> cProps) throws SessionNotFoundException,
        SessionTimeoutException, AppdefDuplicateNameException, ValidationException, PermissionException,
        CPropKeyNotFoundException;

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

    public void removePlatform(AuthzSubject subject, Integer platformId) throws ApplicationException, VetoException;

    public ServerValue updateServer(int sessionId, ServerValue aServer) throws PermissionException,
        ValidationException, SessionTimeoutException, SessionNotFoundException, UpdateException,
        AppdefDuplicateNameException;

    /**
     * Update a server with cprops.
     * @param cProps - the map with Custom Properties for the server
     */
    public ServerValue updateServer(int sessionId, ServerValue aServer, Map<String, String> cProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
        UpdateException, AppdefDuplicateNameException, CPropKeyNotFoundException;
    
    public ServerValue updateServer(AuthzSubject subject, ServerValue aServer, Map<String, String> cProps)
            throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
            UpdateException, AppdefDuplicateNameException, CPropKeyNotFoundException;

    public ServiceValue updateService(int sessionId, ServiceValue aService) throws PermissionException,
        ValidationException, SessionTimeoutException, SessionNotFoundException, UpdateException,
        AppdefDuplicateNameException, NotFoundException;

    /**
     * Update a service with cProps.
     * @param cProps - the map with Custom Properties for the service
     */
    public ServiceValue updateService(int sessionId, ServiceValue aService, Map<String, String> cProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
        UpdateException, AppdefDuplicateNameException, CPropKeyNotFoundException, NotFoundException;

    /**
     * Update a service with cProps.
     * @param cProps - the map with Custom Properties for the service
     */
    public ServiceValue updateService(AuthzSubject subject, ServiceValue aService, Map<String, String> cProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException, PermissionException,
        UpdateException, AppdefDuplicateNameException, CPropKeyNotFoundException, NotFoundException;

    public PlatformValue updatePlatform(int sessionId, PlatformValue aPlatform) throws ValidationException,
        PermissionException, SessionTimeoutException, SessionNotFoundException, UpdateException, ApplicationException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException;

    public PlatformValue updatePlatform(AuthzSubject subject, PlatformValue aPlatform) throws ValidationException,
        PermissionException, SessionTimeoutException, SessionNotFoundException, UpdateException, ApplicationException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException;

    public ApplicationValue updateApplication(int sessionId, ApplicationValue app) throws ApplicationException,
        PermissionException;

    /**
     * Set the services used by an application indicate whether the service is
     * an entry point
     */
    public void setApplicationServices(int sessionId, Integer appId, List<AppdefEntityID> entityIds)
        throws ApplicationException, PermissionException;

    /**
     * Get the dependency tree for a given application
     */
    public DependencyTree getAppDependencyTree(int sessionId, Integer appId) throws ApplicationException,
        PermissionException;

    public void setAppDependencyTree(int sessionId, DependencyTree depTree) throws ApplicationException,
        PermissionException;

    public void removeServer(AuthzSubject subj, Integer serverId) throws ServerNotFoundException,
        SessionNotFoundException, SessionTimeoutException, PermissionException, SessionException, VetoException;
    
    void removeService(AuthzSubject subject, Integer serviceId)
    throws VetoException, PermissionException, ServiceNotFoundException;

    /**
     * Remove an application service.
     * @param appId - The application identifier.
     */
    public void removeAppService(int sessionId, Integer appId, Integer serviceId) throws ApplicationException,
        ApplicationNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException;

    public AppdefResourceValue changeResourceOwner(int sessionId, AppdefEntityID eid, Integer newOwnerId)
        throws ApplicationException, PermissionException;

    /**
     * Create and return a new mixed group value object. This group can contain
     * mixed resources of any entity/resource type combination including
     * platform, server and service.
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     * @return AppdefGroupValue object
     */
    public ResourceGroup createGroup(int sessionId, String name, String description, String location,
                                     java.lang.String[] resources, boolean privGrp) throws GroupCreationException,
        org.hyperic.hq.grouping.shared.GroupDuplicateNameException, SessionException;

    /**
     * Create and return a new strict mixed group value object. This type of
     * group can contain either applications or other groups. However, the
     * choice between between the two is mutually exclusive because all group
     * members must be of the same entity type. Additionally, groups that
     * contain groups are limited to containing either "application groups" or
     * "platform,server&service groups".
     * @param adType - The appdef entity type (groups or applications)
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     * @return AppdefGroupValue object
     */
    public ResourceGroup createGroup(int sessionId, int adType, String name, String description, String location,
                                     java.lang.String[] resources, boolean privGrp) throws GroupCreationException,
        SessionException, org.hyperic.hq.grouping.shared.GroupDuplicateNameException;

    /**
     * Create and return a new compatible group type object. This group type can
     * contain any type of platform, server or service. Compatible groups are
     * strict which means that all members must be of the same type. Compatible
     * group members must also be compatible which means that all group members
     * must have the same resource type. Compatible groups of services have an
     * additional designation of being of type "Cluster".
     * @param adType - The type of entity this group is compatible with.
     * @param adResType - The resource type this group is compatible with.
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     */
    public ResourceGroup createGroup(int sessionId, int adType, int adResType, String name, String description,
                                     String location, java.lang.String[] resources, boolean privGrp)
        throws GroupCreationException, org.hyperic.hq.grouping.shared.GroupDuplicateNameException, SessionException;

    /**
     * Remove resources from the group's contents.
     */
    public void removeResourcesFromGroup(int sessionId, ResourceGroup group, Collection<Resource> resources)
        throws SessionException, PermissionException, VetoException;

    public ResourceGroup findGroupById(int sessionId, Integer groupId) throws PermissionException, SessionException;

    public Map<String, Number> getResourceTypeCountMap(int sessionId, Integer groupId) throws PermissionException,
        SessionException;

    public AppdefGroupValue findGroup(int sessionId, Integer id) throws PermissionException, SessionException;

    public Collection<ResourceGroup> getGroupsForResource(int sessionId, Resource r) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Lookup and return a list of group value objects by their identifiers.
     * @return PageList of AppdefGroupValue objects
     * @throws AppdefGroupNotFoundException when group cannot be found.
     * @throws InvalidAppdefTypeException if group is compat and the appdef type
     *         id is incorrect.
     */
    public PageList<AppdefGroupValue> findGroups(int sessionId, java.lang.Integer[] groupIds, PageControl pc)
        throws PermissionException, SessionException;

    /**
     * Produce list of all groups where caller is authorized to modify. Include
     * just those groups that contain the specified appdef entity.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    public PageList<AppdefGroupValue> findAllGroupsMemberInclusive(int sessionId, PageControl pc, AppdefEntityID entity)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException, ApplicationException;

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc, AppdefEntityID entity)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity, java.lang.Integer[] removeIds)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id. Filter out any unwanted
     * groups specified by groupId array.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity,
                                                                   java.lang.Integer[] removeIds, Resource resourceType)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id. Filter out any unwanted
     * groups specified by groupId array.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID[] entities)
        throws PermissionException, SessionException;

    /**
     * Produce list of all group pojos where caller is authorized
     * @return List containing AppdefGroup.
     */
    public Collection<ResourceGroup> findAllGroupPojos(int sessionId) throws PermissionException,
        SessionTimeoutException, SessionNotFoundException;

    /**
     * Add entities to a resource group
     */
    public void addResourcesToGroup(int sessionID, ResourceGroup group, List<AppdefEntityID> aeids)
        throws SessionException, PermissionException, VetoException;

    /**
     * Update properties of a group.
     * @see ResourceGroupManagerImpl.updateGroup
     */
    public void updateGroup(int sessionId, ResourceGroup group, String name, String description, String location)
        throws SessionException, PermissionException, org.hyperic.hq.grouping.shared.GroupDuplicateNameException;

    /**
     * Produce list of compatible, viewable inventory items. The returned list
     * of value objects will consist only of group inventory compatible with the
     * the specified group type. NOTE: This method returns an empty page list
     * when no compatible inventory is found.
     * @param groupType - the optional group type
     * @param appdefTypeId - the id correponding to the type of entity. example:
     *        group, platform, server, service NOTE: A valid entity type id is
     *        now MANDATORY!
     * @param appdefResTypeId - the id corresponding to the type of resource
     *        example: linux, jboss, vhost
     * @param resourceName - resource name (or name substring) to search for.
     * @return page list of value objects that extend AppdefResourceValue
     */
    public PageList<AppdefResourceValue> findCompatInventory(int sessionId, int groupType, int appdefTypeId,
                                                             int groupEntTypeId, int appdefResTypeId,
                                                             String resourceName, AppdefEntityID[] pendingEntities,
                                                             PageControl pc) throws AppdefEntityNotFoundException,
        PermissionException, SessionException;

    /**
     * Produce list of compatible, viewable inventory items. The returned list
     * of value objects will be filtered on AppdefGroupValue -- if the group
     * contains the entity, then then the entity will not be included in the
     * returned set. NOTE: This method returns an empty page list when no
     * compatible inventory is found.
     * @param appdefTypeId - the id correponding to the type of entity example:
     *        platform, server, service NOTE: A valid entity type id is now
     *        MANDATORY!
     * @param appdefResTypeId - the id corresponding to the type of resource
     *        example: linux, jboss, vhost
     * @param groupEntity - the appdef entity of a group value who's members are
     *        to be filtered out of result set.
     * @param resourceName - resource name (or name substring) to search for.
     * @return page list of value objects that extend AppdefResourceValue
     */
    public PageList<AppdefResourceValue> findCompatInventory(int sessionId, int appdefTypeId, int appdefResTypeId,
                                                             AppdefEntityID groupEntity,
                                                             AppdefEntityID[] pendingEntities, String resourceName,
                                                             PageControl pc) throws AppdefEntityNotFoundException,
        PermissionException, SessionException;

    /**
     * Perform a search for resources from the resource hub
     */
    public PageList<AppdefResourceValue> search(int sessionId, int appdefTypeId, String searchFor,
                                                AppdefEntityTypeID appdefResType, Integer groupId, int[] groupSubType,
                                                boolean matchAny, boolean matchOwn, boolean matchUnavail, PageControl pc)
        throws PermissionException, SessionException, java.util.regex.PatternSyntaxException;

    /**
     * Perform a search for resources
     */
    public PageList<SearchResult> search(int sessionId, String searchFor, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Find SERVICE compatible inventory. Specifically, find all viewable
     * services and service clusters. Services that are assigned to clusters are
     * not returned by this method. Value objects returned by this method
     * include ServiceValue and/or AppdefGroupValue. An array of pending
     * AppdefEntityID can also be specified for filtering. NOTE: This method
     * returns an empty page list when no compatible inventory is found.
     * @param sessionId - valid auth token
     * @return page list of value objects that extend AppdefResourceValue
     */
    public PageList<AppdefResourceValue> findAvailableServicesForApplication(int sessionId, Integer appId,
                                                                             AppdefEntityID[] pendingEntities,
                                                                             String resourceName, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException;

    /**
     * Add an appdef entity to a batch of groups.
     * @param sessionId representing session identifier
     * @param entityId object to be added.
     * @param groupIds identifier array
     */
    public void batchGroupAdd(int sessionId, AppdefEntityID entityId, java.lang.Integer[] groupIds)
        throws SessionException, PermissionException, VetoException;

    /**
     * Update all the appdef resources owned by this user to be owned by the
     * root user. This is done to prevent resources from being orphaned in the
     * UI due to its display restrictions. This method should only get called
     * before a user is about to be deleted
     */
    public void resetResourceOwnership(int sessionId, AuthzSubject currentOwner) throws UpdateException,
        PermissionException, AppdefEntityNotFoundException;

    /**
     * Remove an appdef entity from a batch of groups.
     * @param entityId object to be removed
     * @param groupIds identifier array
     */
    public void batchGroupRemove(int sessionId, AppdefEntityID entityId, java.lang.Integer[] groupIds)
        throws PermissionException, SessionException, VetoException;

    public AppdefResourcePermissions getResourcePermissions(int sessionId, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException;

    public int getAgentCount(int sessionId) throws SessionNotFoundException, SessionTimeoutException;

    public List<Agent> findAllAgents(int sessionId) throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Get the value of one agent based on the IP and Port on which the agent is
     * listening
     */
    public Agent findAgentByIpAndPort(int sessionId, String ip, int port) throws SessionNotFoundException,
        SessionTimeoutException, AgentNotFoundException;

    /**
     * Set (or delete) a custom property for a resource. If the property already
     * exists, it will be overwritten.
     * @param id Appdef entity to set the value for
     * @param key Key to associate the value with
     * @param val Value to assicate with the key. If the value is null, then the
     *        value will simply be removed.
     */
    public void setCPropValue(int sessionId, AppdefEntityID id, String key, String val)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException,
        CPropKeyNotFoundException;

    /**
     * Get a map which holds the descriptions & their associated values for an
     * appdef entity.
     * @param id Appdef entity to get the custom entities for
     * @return The properties stored for a specific entity ID
     */
    public Properties getCPropDescEntries(int sessionId, AppdefEntityID id) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException, AppdefEntityNotFoundException;

    /**
     * Get all the keys associated with an appdef resource type.
     * @param appdefType One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the appdef resource type
     * @return a List of CPropKeyValue objects
     */
    public List<CpropKey> getCPropKeys(int sessionId, int appdefType, int appdefTypeId)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Get all the keys associated with an appdef type of a resource.
     * @param aeid The ID of the appdef resource
     * @return a List of CPropKeyValue objects
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     */
    public List<CpropKey> getCPropKeys(int sessionId, AppdefEntityID aeid) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the appdef inventory summary visible to a user
     */
    public AppdefInventorySummary getInventorySummary(int sessionId, boolean countTypes)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Returns a 2x2 array mapping "appdef type id" to its corresponding label.
     * Suitable for populating an HTML selector.
     */
    public java.lang.String[][] getAppdefTypeStrArrMap();

    /**
     * A method to set ALL the configs of a resource. This includes the
     * resourceConfig, metricConfig, rtConfig and controlCThis also includes the
     * enabling/disabling of rtMetrics for both service and enduser. NOTE: This
     * method should ONLY be called when a user manually configures a resource.
     * @param allConfigs The full configuation information.
     * @param allConfigsRollback The configuation to rollback to if an error
     *        occurs.
     */
    public void setAllConfigResponses(int sessionInt, AllConfigResponses allConfigs,
                                      AllConfigResponses allConfigsRollback) throws PermissionException,
        EncodingException, org.hyperic.hq.product.PluginException, ApplicationException,
        org.hyperic.hq.autoinventory.AutoinventoryException, org.hyperic.hq.scheduler.ScheduleWillNeverFireException,
        org.hyperic.hq.agent.AgentConnectionException;

    /**
     * Get the navigation map data for a given Appdef entity.
     * @return all navigable resources for the given appdef entity
     */
    public org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode[] getNavMapData(int sessionId, AppdefEntityID adeId)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException;

    /**
     * Get the navigation map data for a an auto-group.
     * @param adeIds the appdef entity ids of the "parents" of the groupd
     *        children
     * @param ctype the child resource type
     * @return all navigable resources for the given appdef entities and child
     *         resource type
     */
    public org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode[] getNavMapData(int sessionId,
                                                                                 AppdefEntityID[] adeIds, int ctype)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException;

    /**
     * Get the list of resources that are unavailable
     */
    public Collection<DownResource> getUnavailableResources(AuthzSubject user, String typeId, PageInfo info)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the map of unavailable resource counts by type
     */
    public Map<String, List<AppdefResourceType>> getUnavailableResourcesCount(AuthzSubject user)
        throws AppdefEntityNotFoundException, PermissionException;
    
    /**
     * Check whether or not a given resource exists in the virtual hierarchy
     * 
     */
    public boolean hasVirtualResourceRelation(Resource resource);

    void setAllConfigResponses(AuthzSubject subject,
        AllConfigResponses allConfigs, AllConfigResponses allConfigsRollback)
        throws PermissionException, EncodingException, PluginException,
        ApplicationException, AutoinventoryException,
        ScheduleWillNeverFireException, AgentConnectionException;

    void setAllConfigResponses(AuthzSubject subject, AllConfigResponses allConfigs, 
    						   AllConfigResponses allConfigsRollback, Boolean isUserManaged)
    	throws PermissionException, EncodingException, PluginException,
               ApplicationException, AutoinventoryException,
               ScheduleWillNeverFireException, AgentConnectionException;

    AppdefResourceTypeValue findResourceTypeByResId(int sessionID, Integer resourceId)
    throws SessionTimeoutException, SessionNotFoundException;
}
