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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

public class PermissionManagerImpl 
    extends PermissionManager
{
    private static Log _log =
        LogFactory.getLog(PermissionManagerImpl.class.getName());

    private String _falseToken = null;

    private static final String VIEWABLE_SELECT =
        "SELECT instance_id, EAM_RESOURCE.sort_name, EAM_RESOURCE.id, " +
                            "EAM_RESOURCE.resource_type_id " +
        "FROM EAM_RESOURCE ";

    private static final String VIEWABLE_CLAUSE =
        " EAM_RESOURCE.fsystem = DB_FALSE_TOKEN AND " +
        "EAM_RESOURCE.resource_type_id = " +
        "(SELECT rt.id FROM EAM_RESOURCE_TYPE rt WHERE rt.name = ?) ";

    private static final String VIEWABLE_BYNAME_SQL =
        " AND (lower(EAM_RESOURCE.name) like lower('%$$resName$$%') OR " +
        " EAM_RESOURCE.instance_id in (SELECT appdef_id FROM EAM_CPROP, " +
        " EAM_CPROP_KEY WHERE keyid = EAM_CPROP_KEY.id AND " +
        " appdef_type = ? AND lower(propvalue) like lower('%$$resName$$%'))) ";

    private static final String ALL_RESOURCE_SQL = 
        "SELECT res.instance_id FROM EAM_RESOURCE res, EAM_OPERATION o " +
        "WHERE o.resource_type_id = res.resource_type_id and o.id = ?";

    private static final String VIEWABLE_SEARCH =
        "WHERE EAM_RESOURCE.fsystem = DB_FALSE_TOKEN AND " +
              "RESOURCE_TYPE_ID IN (3, 301, 303, 305, 308)  AND "+
              "(SORT_NAME LIKE UPPER(?) OR " +
               "PROTO_ID IN (SELECT ID FROM EAM_RESOURCE " +
                            "WHERE PROTO_ID = 0 AND SORT_NAME LIKE UPPER(?))) ";

    private Connection getConnection() throws SQLException {
        try {
            return DBUtil.getConnByContext(getInitialContext(),
                                           HQConstants.DATASOURCE);            
        } catch (NamingException e) {
            throw new SQLException("Failed to retrieve datasource: "+e);
        }
    }

    public PermissionManagerImpl() { 
        Connection conn = null;
        
        try {
            conn = getConnection();
            _falseToken = DBUtil.getBooleanValue(false, conn);                
        } catch (Exception e) {
            throw new SystemException("Unable to initialize " +
                                      "PermissionManager:" + e, e);
        } finally {
            DBUtil.closeConnection(ctx, conn);
        }
    }

    public void check(Integer subject, ResourceType type, Integer instanceId,
                      String operation)
        throws PermissionException {}

    public void check(Integer subjectId, Integer typeId, Integer instanceId,
                      Integer operationId)
        throws PermissionException {}

    public void check(Integer subjectId, String resType, Integer instanceId,
                      String operation)
        throws PermissionException {}

    public boolean hasAdminPermission(Integer who) {
        return true;
    }
    
    public PageList findOperationScopeBySubject(AuthzSubject subj,
                                                String opName, String resType,
                                                PageControl pc) 
        throws FinderException, PermissionException
    {
        if (_log.isDebugEnabled()) { 
            _log.debug("Checking Scope for Operation: " + opName +
                       " subject: " + subj);
        }
        ResourceType resTypeBean = getResourceTypeDAO().findByName(resType);
        if (resTypeBean != null) {
            Operation opEJB =
                getOperationDAO().findByTypeAndName(resTypeBean, opName);
            if (opEJB != null) {
                return findOperationScopeBySubject(subj, opEJB.getId(), pc);
            }
        }
        return new PageList();
    }

    public PageList findOperationScopeBySubject(AuthzSubject subj,
                                                Integer opId, PageControl pc) 
        throws FinderException, PermissionException
    {
        if (_log.isDebugEnabled()) {
            _log.debug("Checking Scope for Operation: " + opId + " subject: " + 
                       subj);
        }
        PageList scope = findScopeBySQL(subj, opId, pc);

        if (_log.isDebugEnabled()) {
            _log.debug("Scope check returned a page of : " + scope.size() +
                       " of " + scope.getTotalSize() + " items");
        }
        return scope;
    }

    public ResourceValue[]
        findOperationScopeBySubjectBatch(AuthzSubject whoami,
                                         ResourceValue[] resArr, 
                                         String[] opArr)
        throws FinderException
    {
        if (resArr == null) {
            throw new IllegalArgumentException("At least one resource required");
        }

        ArrayList resLocArr = new ArrayList(resArr.length);

        for (int x = 0; x < resArr.length; x++) {
            resLocArr.add(lookupResource(resArr[x]));
        }
        
        return (ResourceValue[]) fromLocals(resLocArr, ResourceValue.class);
    }

    public List findViewableResources(AuthzSubject subj, String resType,
                                      String resName, String appdefTypeStr,
                                      Integer typeId, PageControl pc) {
        List viewableInstances = new ArrayList();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            String sql = VIEWABLE_SELECT;
            if (appdefTypeStr != null && typeId != null) {
                sql += ", EAM_" + appdefTypeStr.toUpperCase() +
                " appdef WHERE EAM_RESOURCE.instance_id = appdef.id AND " +
                " appdef." + appdefTypeStr + "_type_id = ? AND ";
            }
            else {
                sql += " WHERE ";
            }
            sql += VIEWABLE_CLAUSE;
            
            if (resName != null)
            {
                // Support wildcards
                resName = resName.replace('*', '%');
                resName = resName.replace('?', '_');

                sql += VIEWABLE_BYNAME_SQL;
                sql = StringUtil.replace(sql, "$$resName$$", resName);
            }
            
            sql += "ORDER BY EAM_RESOURCE.sort_name ";

            if(!pc.isAscending()) {
                sql = sql + "DESC";
            }
            sql = StringUtil.replace(sql, "DB_FALSE_TOKEN", _falseToken);
 
            stmt = conn.prepareStatement(sql);
            int i = 1;
            
            if (appdefTypeStr != null && typeId != null) {
                stmt.setInt(i++, typeId.intValue());
            }
            stmt.setString(i++, resType);
            
            if (resName != null) {
                stmt.setInt(i++, AppdefUtil.resNameToAppdefTypeId(resType));
            }
            
            _log.debug("Viewable SQL: " + sql);
            rs = stmt.executeQuery();

            for(i = 1; rs.next(); i++) {
                viewableInstances.add(new Integer(rs.getInt(1)));
            }
            return viewableInstances;
        } catch (SQLException e) {
            _log.error("Error getting scope by SQL", e);
            throw new SystemException("SQL Error getting scope: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        }
    }

    public List findViewableResources(AuthzSubject subj, String searchFor,
                                      PageControl pc) {
        List viewableInstances = new ArrayList();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;        
        try {
            conn = getConnection();
            String sql = VIEWABLE_SELECT + VIEWABLE_SEARCH;
            
            // TODO: change sort by
            sql += "ORDER BY EAM_RESOURCE.resource_type_id, " +
            		        "EAM_RESOURCE.sort_name ";
            
            if(!pc.isAscending()) {
                sql = sql + "DESC";
            }
            sql = StringUtil.replace(sql, "DB_FALSE_TOKEN", _falseToken);
 
            if (searchFor == null) {
                searchFor = "%";
            }
            else {
                // Support wildcards
                searchFor = '%' + searchFor.replace('*', '%') + '%';
            }
            
            stmt = conn.prepareStatement(sql);
            int i = 1;
            
            stmt.setString(i++, searchFor);
            stmt.setString(i++, searchFor);
            
            if (_log.isDebugEnabled())
                _log.debug("Viewable search for (" + searchFor + ") SQL: " +
                           sql);
            
            rs = stmt.executeQuery();

            for(i = 1; rs.next(); i++) {
                viewableInstances.add(new Integer(rs.getInt(3)));
            }
            return viewableInstances;
        } catch (SQLException e) {
            _log.error("Error search by SQL", e);
            throw new SystemException("SQL Error search: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        }
    }

    private PageList findScopeBySQL(AuthzSubject subj, Integer opId, 
                                    PageControl pc)
        throws FinderException, PermissionException 
    {
        Pager defaultPager = Pager.getDefaultPager();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List instanceIds = null;
        try {
            conn = getConnection();
            // Always return all resources
            stmt = conn.prepareStatement(ALL_RESOURCE_SQL);
            stmt.setInt(1, opId.intValue());
            rs = stmt.executeQuery();
            // now build the list
            instanceIds = new ArrayList();
            for(int i = 1; rs.next(); i++) {
                instanceIds.add(new Integer(rs.getInt(1)));
            }
            return defaultPager.seek(instanceIds, pc.getPagenum(), pc.getPagesize());
        } catch (SQLException e) {
            _log.error("Error getting scope by SQL", e);
            throw new FinderException("Error getting scope: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        }
    }

    public String getResourceTypeSQL(String instanceId, Integer subjectId,
                                     String resType, String op) {
        return
            "SELECT RES.ID FROM EAM_RESOURCE RES, " +
            " EAM_RESOURCE_TYPE RT " +
            "WHERE " + instanceId + " = RES.INSTANCE_ID " +
            "  AND RES.FSYSTEM = " + _falseToken + 
            "  AND RES.RESOURCE_TYPE_ID = RT.ID " +
            "  AND RT.NAME = '" + resType + "'";
    }

    public List getAllOperations(AuthzSubject subject, PageControl pc)
        throws PermissionException, FinderException 
    {
        Role rootRole = getRoleDAO().findById(AuthzConstants.rootRoleId);
        Set ops = new HashSet();
        ops.addAll(rootRole.getOperations());
        pc = PageControl.initDefaults(pc, SortAttribute.OPERATION_NAME);
        Pager operationPager;
        try {
            operationPager = Pager.getPager(OPERATION_PAGER);
        } catch (Exception e) {
            return null;
        }
        return operationPager.seek(ops, pc.getPagenum(), pc.getPagesize());
    }

    public Collection getGroupResources(Integer subjectId,
                                        Integer groupId, Boolean fsystem) {
        return getResourceDAO().findInGroup_orderName(groupId, fsystem);
    }

    public Collection findServiceResources(AuthzSubject subj, Boolean fsystem) {
        return getResourceDAO().findSvcRes_orderName(fsystem);
    }

    public RolePermNativeSQL getRolePermissionNativeSQL(String resourceVar,
                                                        String subjectParam,
                                                        String opListParam) 
    {
        return new RolePermNativeSQL() {
            public String getSQL() {
                return "";
            }
                                                                           
            public Query bindParams(Query q, AuthzSubject subject, List ops) {
                return q;
            }
        };
    }
                                                                   
    
    public String getAlertsHQL(boolean inEscalation, boolean notFixed,
                               Integer groupId) {
        // Join with Resource for sorting
        return "select a from " + (inEscalation ? "EscalationState es, " : "") +
        		"Alert a " +
                "join a.alertDefinition d " +
                "join d.resource r " +
          "where " +
                (groupId == null ? "" :
                    "exists (select rg from r.resourceGroups rg " +
                             "where rg.id = " + groupId + ") and ") +
               "a.ctime between :begin and :end and " +
                (notFixed ? " a.fixed = false and " : "") +
                "d.priority >= :priority " +
                (inEscalation ? "and a.id = es.alertId and " +
                		            "es.alertDefinitionId = d.id " : "");
    }

    public String getAlertDefsHQL() {
        return "select d from AlertDefinition d " +
               "join d.resource r " +
         "where d.priority >= :priority";
    }

    public String getGroupAlertsHQL(boolean inEscalation, boolean notFixed,
                                    Integer groupId) {
        return "select a from " + (inEscalation ? "EscalationState es, " : "") +
                "GalertLog a " +
               "join a.alertDef d " +
         "where " +
          (groupId != null ? " g.id = " + groupId + " and " : "") + 
          "a.timestamp between :begin and :end " + 
           (notFixed ? " and a.fixed = false " : "") +
           "and d.severityEnum >= :priority " +
                (inEscalation ? "and a.id = es.alertId and " +
                                    "es.alertDefinitionId = d.id " : "");
    }

    public String getGroupAlertDefsHQL() {
        return "select d from GalertDef d " +
               "join d.group g " +
               "join d.escalation e " +
         "where d.severityEnum >= :priority ";
    }

    public boolean hasGuestRole() {
        return false;
    }

    public EdgePermCheck
        makePermCheckSql(String subjectParam, 
                         String resourceVar, String resourceParam,
                         String distanceParam, String opsParam)
    {
        String sql = 
            "join " + resourceVar+ ".toEdges _e " + 
            "join _e.from _fromResource " +  
            "where " + 
            "  _fromResource = :" + resourceParam + 
            "  and _e.distance >= :" + distanceParam + " "; 

        return new EdgePermCheck(sql, subjectParam, resourceVar,
                                 resourceParam, distanceParam, opsParam) 
        {
            public Query addQueryParameters(Query q, AuthzSubject subject,
                                            Resource r, int distance, List ops) 
            {
                return q.setInteger(getDistanceParam(), distance)
                        .setParameter(getResourceParam(), r);
            }  
        };
    }

    public String getOperableGroupsHQL(String alias, String oper) { return ""; }

    public String getSQLWhere(Integer subjectId) {
        return "";
    }
}
