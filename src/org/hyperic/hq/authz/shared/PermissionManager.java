/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hyperic.hq.appdef.shared.CloningBossInterface;
import org.hyperic.hq.authz.server.session.AuthzSession;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.PagerProcessor_operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.shared.HierarchicalAlertingManagerInterface;
import org.hyperic.hq.events.shared.MaintenanceEventManagerInterface;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;

public abstract class PermissionManager extends AuthzSession {

    private static final Log _log = LogFactory.getLog(PermissionManager.class);
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
     * @return List of integer instance ids
     */
    public abstract List 
        findOperationScopeBySubject(AuthzSubject subj, String opName,
                                    String resType) 
        throws FinderException, PermissionException;

    /**
     * Find the list of instance ids for which a given subject id
     * has a given operation.
     * @return List of integer instance ids
     */
    public abstract List 
        findOperationScopeBySubject(AuthzSubject subj, Integer opId)
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
    public abstract Resource[]
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

    public abstract String getOperableGroupsHQL(AuthzSubject subject,
                                                String alias, 
                                                String oper);
    
    public abstract Collection 
        getGroupResources(Integer subjectId, Integer groupId, Boolean fsystem);
                                                 
    public abstract Collection 
        findServiceResources(AuthzSubject subj, Boolean fsystem);

    public interface RolePermNativeSQL { 
        String getSQL();
        Query bindParams(Query q, AuthzSubject subject, List viewResourcesOperations, List manageAlertOperations);
    }
          
    public abstract RolePermNativeSQL 
        getRolePermissionNativeSQL(String resourceVar, String eventLogVar,
                                   String subjectParam,
                                   String opListViewResourcesParam,
                                   String opListManageAlertsParam); 
         
    
    public abstract String getAlertsHQL(boolean inEscalation, boolean notFixed,
                                        Integer groupId, Integer alertDefId,
                                        boolean count);
        
    public abstract String getAlertDefsHQL();

    public abstract String getGroupAlertsHQL(boolean inEscalation, boolean notFixed, 
                                             Integer groupId, Integer galertDefId);

    public abstract String getGroupAlertDefsHQL();    

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters.  Used for a SQL query.
     * @param includeDescendants - include the resource's descendants in the query
     */
    public EdgePermCheck makePermCheckSql(String resourceVar,
                                          boolean includeDescendants) {
        return makePermCheckSql("subject", resourceVar, "resource",
                                "distance", "ops", includeDescendants);
    }

    /**
     * Creates an edge perm check with default names of the replacement
     * variables and parameters.  Used for a HQL query.
     * @param includeDescendants - include the resource's descendants in the query
     */
    public EdgePermCheck makePermCheckHql(String resourceVar,
                                          boolean includeDescendants) {
        return makePermCheckHql("subject", resourceVar, "resource",
                                "distance", "ops", includeDescendants);
    }

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking SQL.  This is the SQL version of makePermCheckHql
     * 
     * This method spits out a piece of SQL, like:
     *  JOIN EAM_RESOURCE_EDGE edge ON edge.TO_ID = resId edge.FROM_ID = resId
     *  WHERE (resId = :resParam
     *  AND edge.distance >= :distParam
     *  AND resSubjId = :subjParam
     *  AND ...
     *  AND ...)
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
     * @param includeDescendants - include the resource's descendants in the query
     */
    public abstract EdgePermCheck makePermCheckSql(String subjectParam, 
                                                   String resourceVar,
                                                   String resourceParam,
                                                   String distanceParam,
                                                   String opsParam,
                                                   boolean includeDescendants); 

    /**
     * Generates an object which aids in the creation of hierarchical,
     * permission checking HQL.
     * 
     * This method spits out a piece of HQL, like:
     *   join r.toEdges _e 
     *  ... 
     *  where _e.fromDistance >= :distance (could be '=' based on includeDescendants)
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
     * @param includeDescendants - include the resource's descendants in the query
     */
    public abstract EdgePermCheck makePermCheckHql(String subjectParam, 
                                                   String resourceVar,
                                                   String resourceParam,
                                                   String distanceParam,
                                                   String opsParam,
                                                   boolean includeDescendants);
    
    /**
     * Return the MaintenanceEventManager implementation
     */
    public abstract MaintenanceEventManagerInterface getMaintenanceEventManager();
    
    /**
     * Return the CloningBoss implementation
     */
    public abstract CloningBossInterface getCloningBoss();
    
    /**
     * Return the HierarchicalAlertingManager implementation
     */
    public abstract HierarchicalAlertingManagerInterface getHierarchicalAlertingManager();

    /**
     * @param subj {@link AuthzSubject}
     * @param platformResType The resource_type associated with the EAM_RESOURCE and
     * EAM_RESOURCE_TYPE tables. e.g. "covalentEAMPlatform" from {@link AuthzConstants} class.
     * @param operation The associated operation from {@link AuthzConstants}.  e.g. "viewPlatform"
     * @param excludes {@link Collection} of {@link Integer}s that represent the {@link Resource}Id
     * of the protoTypes to exclude
     * @return a count of viewable resources that the subj is able view
     */
    public int findResourceCount(AuthzSubject subj, String resourceType,
                                 String operation, Collection excludes) {
        // want to exclude top level resource prototypes (protoTypeId = rootResourceId)
        if (excludes == null) {
            excludes = Collections.singletonList(AuthzConstants.rootResourceId);
        } else {
            excludes = new HashSet(excludes);
            excludes.add(AuthzConstants.rootResourceId);
        }
        Connection conn = null;
        Statement stmt  = null;
        ResultSet rs    = null;
        try {
            conn = getConnection();
            String sql = new StringBuilder()
                .append("SELECT COUNT(r.ID) ")
                .append("FROM EAM_RESOURCE r ")
                .append("JOIN EAM_RESOURCE_TYPE rtype on rtype.id = r.resource_type_id ")
                .append("WHERE rtype.name = '").append(resourceType).append("' AND ")
                .append(getExcludes(excludes))
                .append("EXISTS (")
                .append(getResourceTypeSQL("r.INSTANCE_ID", subj.getId(), resourceType, operation))
                .append(")")
                .toString();
            stmt = conn.createStatement();
            if (_log.isDebugEnabled()) _log.debug(sql);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            _log.error("Caught SQL Exception counting Platforms: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        } 
        return 0;
    }

    private String getExcludes(Collection excludes) {
        if (excludes == null || excludes.size() == 0) {
            return "";
        }
        StringBuilder rtn = new StringBuilder(" r.proto_id not in (");
        for (Iterator it=excludes.iterator(); it.hasNext(); ) {
            Integer protoId = (Integer) it.next();
            if (protoId == null) {
                continue;
            }
            rtn.append(protoId).append(",");
        }
        if (rtn.length() == 0) {
            return "";
        }
        rtn.deleteCharAt(rtn.length()-1);
        return rtn.append(") AND ").toString();
    }
    
    protected Connection getConnection() throws SQLException {
        try {
            return DBUtil.getConnByContext(getInitialContext(), HQConstants.DATASOURCE);            
        } catch (NamingException e) {
            throw new SystemException("Failed to retrieve datasource: " + e, e);
        }
    }

}
