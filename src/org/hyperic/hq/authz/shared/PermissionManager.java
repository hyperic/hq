/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.authz.shared;

import java.util.Collection;
import java.util.List;

import javax.ejb.FinderException;

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.AuthzSession;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.PagerProcessor_operation;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

public abstract class PermissionManager extends AuthzSession {

    public static final String OPERATION_PAGER = 
        PagerProcessor_operation.class.getName();

    /**
     * Check permission.
     *
     * @param subject The subject.
     * @param type The type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operation The operation (as a String) that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to 
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subject, ResourceType type,
                      Integer instanceId, String operation)
        throws PermissionException;

    /**
     * Check permission.
     *
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to 
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subjectId, Integer typeId, 
                               Integer instanceId, Integer operationId)
        throws PermissionException;

    /**
     * Check permission.
     *
     * @param subjectId ID of the subject.
     * @param typeId ID of the type of the resource.
     * @param instanceId The consumer's ID for the resource in question.
     * @param operationId ID of the operation that the subject may want
     * to perform.
     * @exception PermissionException If subject is not authorized to 
     * perform the given operation on the resource of the given type whose
     * id is instanceId.
     */
    public abstract void check(Integer subjectId, String resType, 
                               Integer instanceId, String operation)
        throws PermissionException;

    /**
     * Check whether a user has permission to access the admin component. 
     *
     * @return true - if user has administerCAM operation false otherwise
     */
    public abstract boolean hasAdminPermission(Integer who);

    /**
     * Check to see if user can see role dashboards
     */
    public abstract boolean hasGuestRole();
    
    /**
     * Find the list of instance ids for which a given subject id 
     * has the named operation in one of their roles or owns a resource
     * for which the operation is valid
     * @return List of integer instnace ids
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public abstract PageList 
        findOperationScopeBySubject(AuthzSubject subj, String opName,
                                    String resType, PageControl pc) 
        throws FinderException, PermissionException;

    /**
     * Find the list of instance ids for which a given subject id
     * has a given operation.
     * @return List of integer instance ids
     */
    public abstract PageList 
        findOperationScopeBySubject(AuthzSubject subj, Integer opId,
                                    PageControl pc)
        throws FinderException, PermissionException;

    /**
     * Find the list of resources for which a given subject id can perform
     * specified operation. This method operates on a batch of resources and 
     * their corresponding operations. Unlike, other findOperScopeBySubj
     * methods, this one operates on any type of resource and thus the
     * "resource and operation" tuple should be expressed by common index.
     *
     * @param whoami - subject
     * @param resArr - batch of resources to verify
     * @param opArr  - corresponding batch of operations
     * @return array of authz Resources
     * @exception FinderException
     */
    public abstract ResourceValue[]
        findOperationScopeBySubjectBatch(AuthzSubject whoami,
                                         ResourceValue[] resArr,
                                         String[] opArr)
        throws FinderException;

    /**
     * Get viewable resources of a specific type
     * @param resType the authz resource type name
     * @param resName if result should filter by resource name
     * @param appdefTypeStr the Appdef type name, like 'platform', 'server', etc
     * @param typeId the appdef type ID, e.g. the platform_type_id
     *
     * @return a list of Integers representing instance ids
     */
    public abstract List findViewableResources(AuthzSubject subj,
                                               String resType, 
                                               String resName, 
                                               String appdefTypeStr,
                                               Integer typeId,
                                               PageControl pc);
    
    /**
     * Search viewable resources of any type
     * @return a list of Integers representing instance ids
     */
    public abstract List findViewableResources(AuthzSubject subj,
                                               String searchFor, 
                                               PageControl pc);
    
    /**
     * Get a clause that you can append to an existing WHERE clause to make it
     * authz-aware.  Note that your WHERE clause must include at least 1
     * condition, as the value returned from this method begins with 'AND'.
     * Also, the alias of the EAM_RESOURCE table is assumed to be 'res'.
     *
     * @return a clause that can be appended to a WHERE clause to query against
     *  authz data.
     */
    public abstract String getSQLWhere(Integer subjectId);

    /**
     * Get all operations for a given subject
     *
     * @return a list of Integers representing instance ids
     */
    public abstract List 
        getAllOperations(AuthzSubject subject, PageControl pc)
        throws PermissionException, FinderException;

    public abstract String getResourceTypeSQL(String instanceId,
                                              Integer subjectId,
                                              String resType,
                                              String op);

    public abstract String getOperableGroupsHQL(String alias, String oper);
    
    public abstract Collection 
        getGroupResources(Integer subjectId, Integer groupId, Boolean fsystem);
                                                 
    public abstract Collection 
        findServiceResources(AuthzSubject subj, Boolean fsystem);

    public interface RolePermNativeSQL { 
        String getSQL();
        Query bindParams(Query q, AuthzSubject subject, List operations);
    }
          
    public abstract RolePermNativeSQL 
        getRolePermissionNativeSQL(String resourceVar, String subjectParam,
                                   String opListParam); 
         
    
    public abstract String getAlertsHQL(boolean inEscalation);
        
    public abstract String getAlertDefsHQL();

    public abstract String getGroupAlertsHQL(boolean inEscalation);

    public abstract String getGroupAlertDefsHQL();    

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters.
     */
    public EdgePermCheck makePermCheckSql(String resourceVar) {
        return makePermCheckSql("subject", resourceVar, "resource",
                                "distance", "ops");
    }

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking SQL.
     * 
     * This method spits out a piece of SQL, like:
     *   join r.toEdges _e 
     *  ... 
     *  where _e.fromDistance > :distance
     *   and ...
     *   and ...
     *   
     * Therefore, it must used between the select and last parts of the
     * where clause, preceded by an 'and'
     * 
     * The arguments ending with 'Param' are used to identify names of
     * Query parameters which will later passed in. 
     *   (e.g. query.setParameter("subject", s)
     *   
     * The arguments ending in 'Var' are the SQL variable names used
     * straight in the SQL text.  
     *   (e.g.  "select rez from Resource rez "... , you would specify
     *    the name of your resourceVar as 'rez')
     */
    public abstract EdgePermCheck
        makePermCheckSql(String subjectParam, 
                         String resourceVar, String resourceParam,
                         String distanceParam, String opsParam); 
}
