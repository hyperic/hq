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

package org.hyperic.hq.authz.shared;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.ejb.FinderException;

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.PagerProcessor_operation;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

public interface PermissionManager {

    final String OPERATION_PAGER = PagerProcessor_operation.class.getName();

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
    public void check(Integer subject, ResourceType type,
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
    public void check(Integer subjectId, Integer typeId, Integer instanceId,
                      Integer operationId)
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
    public void check(Integer subjectId, String resType, Integer instanceId,
                      String operation)
        throws PermissionException;

    /**
     * Check whether a user has permission to access the admin component. 
     *
     * @return true - if user has administerCAM operation false otherwise
     */
    public boolean hasAdminPermission(Integer who);

    /**
     * Check to see if user can see role dashboards
     */
    public boolean hasGuestRole();
    
    /**
     * Find the list of instance ids for which a given subject id 
     * has the named operation in one of their roles or owns a resource
     * for which the operation is valid
     * @return List of integer instnace ids
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public PageList findOperationScopeBySubject(AuthzSubjectValue subj,
                                                String opName, String resType,
                                                PageControl pc) 
        throws FinderException, PermissionException;

    /**
     * Find the list of instance ids for which a given subject id
     * has a given operation.
     * @return List of integer instance ids
     */
    public PageList findOperationScopeBySubject(AuthzSubjectValue subj,
                                                Integer opId,
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
    public ResourceValue[]
        findOperationScopeBySubjectBatch(AuthzSubjectValue whoami,
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
    public List findViewableResources(AuthzSubjectValue subj,
                                      String resType, 
                                      String resName, 
                                      String appdefTypeStr,
                                      Integer typeId,
                                      PageControl pc);

    /**
     * Get all operations for a given subject
     *
     * @return a list of Integers representing instance ids
     */
    public List getAllOperations(AuthzSubjectValue subject, PageControl pc)
        throws PermissionException, FinderException;


    /**
     * Utilities for generation of custom authz-aware SQL.
     */
    public static final String AUTHZ_FROM = ", EAM_RESOURCE authz_r ";

    /**
     * Get a WHERE clause that you can append to an existing WHERE clause
     * to make it authz-aware.  Note that your WHERE clause must include
     * at least 1 condition, as the value returned from this method begins
     * with 'AND'.  If you don't have any existing conditions, you can just
     * add "WHERE 1=1" and then append the return value of this method to that.
     *
     * @param resIdKey The value to join to the resource table.  If
     * the operationColumnName parameter is null, then this field will
     * be joined against the instance_id column.  If operationColumnName
     * is non-null, then this field will be joined against the id column.
     * This can be an absolute value like a number or a column name if you 
     * want to do a join.
     *
     * @return a WHERE clause that can be used to query against authz data.
     */
    public String getSQLWhere(Integer subjectId, String resId);

    /**
     * Populate a prepared statement with the appropriate authz parameters.
     *
     * @param ps The prepared statement.  The SQL that this statement is based
     * on must include the return value from getSQLWhere method.
     * @param ps_idx The parameter index for the prepared statement where we'll 
     * start setting parameter values. The caller has to take care to ensure 
     * that this index lines up with the placement of the authz where clause in
     * the original SQL that the prepared statement is based on.
     * @param subjectId The subject to evaluate permissions against.
     * @param resType The resource type to restrict the query to.
     * @param operationId The operation to check permissions for.
     * @return The new ps_idx value, in case the caller has more parameters
     * to set in the prepared statement.
     */
    public int prepareSQL(PreparedStatement ps, int ps_idx, Integer subjectId, 
                          Integer resType, Integer operationId) 
        throws SQLException;

    public String getResourceTypeSQL(String table);

    public int prepareResourceTypeSQL(PreparedStatement ps,
                                      int ps_idx,
                                      int subjectId,
                                      String resType,
                                      String op)
        throws SQLException;

    public Collection getGroupResources(Integer subjectId,
                                        Integer groupId, Boolean fsystem);

    public Collection findServiceResources(AuthzSubject subj, Boolean fsystem);

    public interface RolePermNativeSQL { 
        String getSQL();
        Query bindParams(Query q, AuthzSubject subject, List operations);
    }
    
    public RolePermNativeSQL getRolePermissionNativeSQL(String resourceVar,
                                                        String subjectParam,
                                                        String opListParam); 
    
    public String getAlertsHQL(boolean inEscalation);
    
    public String getAlertDefsHQL();

    public String getGroupAlertsHQL(boolean inEscalation);
    
    public String getGroupAlertDefsHQL();    
    
}
