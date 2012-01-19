/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ApplicationManager.
 */
public interface ApplicationManager {
    /**
     * Get all Application types
     * @return list of ApplicationTypeValue objects
     */
    public List<AppdefResourceTypeValue> getAllApplicationTypes(AuthzSubject who);

    /**
     * Get ApplicationType by ID
     */
    public ApplicationType findApplicationType(Integer id);

    /**
     * Create a Application of a specified type
     * @param subject - who
     * @param newApp - the new application to create
     * @param services - A collection of ServiceValue objects that will be the
     *        initial set of services for the application. This can be null if
     *        you are creating an empty application.
     */
    public Application createApplication(AuthzSubject subject, ApplicationValue newApp) throws ValidationException,
        PermissionException, AppdefDuplicateNameException, NotFoundException;

    /**
     * Update the basic properties of an application. Will NOT update service
     * dependencies, etc.
     */
    public ApplicationValue updateApplication(AuthzSubject subject, ApplicationValue newValue)
        throws ApplicationNotFoundException, PermissionException, UpdateException,
        org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;

    /**
     * Remove an application
     */
    public void removeApplication(AuthzSubject subject, Integer id) throws ApplicationNotFoundException,
        PermissionException, VetoException;

    /**
     * Remove an application service.
     * @param caller - Valid spider subject of caller.
     * @param appId - The application identifier.
     * @param appServiceId - The service identifier
     * @throws ApplicationException when unable to perform remove
     * @throws ApplicationNotFoundException - when the app can't be found
     * @throws PermissionException - when caller is not authorized to remove.
     */
    public void removeAppService(AuthzSubject caller, Integer appId, Integer appServiceId) throws ApplicationException,
        ApplicationNotFoundException, PermissionException;

    public void handleResourceDelete(Resource resource);

    /**
     * Get the service dependency map for an application
     * @param subject
     * @param appId
     */
    public DependencyTree getServiceDepsForApp(AuthzSubject subject, Integer pk)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, PermissionException;

    /**
     * Get the # of applications within HQ inventory
     */
    public Number getApplicationCount();

    /**
     * Set the dependency map for an application
     * @param depTree
     * @param subject
     */
    public void setServiceDepsForApp(AuthzSubject subject, DependencyTree depTree) throws ApplicationNotFoundException,
        PermissionException;
    
    /**
     * Find application by name
     * @param subject - who
     * @param name - name of app
     */
    public Application findApplicationByName(AuthzSubject subject, String name)
        throws ApplicationNotFoundException, PermissionException;

    /**
     * Get application pojo by id.
     */
    public Application findApplicationById(AuthzSubject subject, Integer id) throws ApplicationNotFoundException,
        PermissionException;

    public java.util.Collection<Application> findDeletedApplications();

    /**
     * Get all applications.
     * @param subject The subject trying to list applications.
     * @return A List of ApplicationValue objects representing all of the
     *         applications that the given subject is allowed to view.
     */
    public PageList<ApplicationValue> getAllApplications(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException;
    
    /**
     * @return {@link List} of {@link Resource}
     *
     */
    List<Resource> getApplicationResources(AuthzSubject subject, Integer appId) 
    throws ApplicationNotFoundException, PermissionException;

    /**
     * Get all the application services for this application
     * @param subject
     * @param appId
     * @retur list of AppServiceValue objects
     */
    public List<AppServiceValue> getApplicationServices(AuthzSubject subject, Integer appId)
        throws org.hyperic.hq.appdef.shared.ApplicationNotFoundException, PermissionException;

    /**
     * Set the application services for this application
     * @param subject
     * @param map key: Integer service ID value: Boolean indicating that the
     *        service is an entry point
     */
    public void setApplicationServices(AuthzSubject subject, Integer appId, java.util.List<AppdefEntityID> entityIds)
        throws ApplicationNotFoundException, AppdefGroupNotFoundException, PermissionException;

    /**
     * Get all applications for a resource.
     */
    public org.hyperic.util.pager.PageList<ApplicationValue> getApplicationsByResource(AuthzSubject subject,
                                                                                       AppdefEntityID resource,
                                                                                       PageControl pc)
        throws ApplicationNotFoundException, PermissionException;

    /**
     * Get all application IDs that use the specified resource.
     * @param subject The subject trying to get the app list
     * @param resource Server ID.
     * @param pagenum The page number to start listing. First page is zero.
     * @param pagesize The size of the page (the number of items to return).
     * @param sort The sort order.
     * @return A List of ApplicationValue objects which use the specified
     *         resource.
     */
    public java.lang.Integer[] getApplicationIDsByResource(AppdefEntityID resource) throws ApplicationNotFoundException;

    public boolean isApplicationMember(AppdefEntityID application, AppdefEntityID service);

    /**
     * Generate a resource tree based on the root resources and the traversal
     * (one of ResourceTreeGenerator.TRAVERSE_*)
     */
    public ResourceTree getResourceTree(AuthzSubject subject, AppdefEntityID[] resources, int traversal)
        throws AppdefEntityNotFoundException, PermissionException;

    public Application getApplicationById(Integer id);

}
