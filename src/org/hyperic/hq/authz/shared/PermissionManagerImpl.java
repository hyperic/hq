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
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.server.session.AuthzSession;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

public class PermissionManagerImpl 
    extends AuthzSession implements PermissionManager
{
    private static Log log =
        LogFactory.getLog(PermissionManagerImpl.class.getName());

    private String falseToken = null;

    private static final String VIEWABLE_SQL =
        "SELECT instance_id, sort_name FROM EAM_RESOURCE " +
        "WHERE EAM_RESOURCE.fsystem = DB_FALSE_TOKEN AND " +
        "EAM_RESOURCE.resource_type_id = " +
        "(SELECT rt.id FROM EAM_RESOURCE_TYPE rt WHERE rt.name = ?) ";

    private static final String VIEWABLE_BYNAME_SQL =
        VIEWABLE_SQL +
        "AND (lower(EAM_RESOURCE.name) like '%'||lower(?)||'%' OR " +
        " EAM_RESOURCE.instance_id in (SELECT appdef_id FROM EAM_CPROP, " +
        " EAM_CPROP_KEY WHERE keyid = EAM_CPROP_KEY.id AND " +
        " appdef_type = ? AND lower(propvalue) like '%'||lower(?)||'%'))";

    private static final String ALL_RESOURCE_SQL = 
        "SELECT res.instance_id FROM EAM_RESOURCE res";

    private static final String AUTHZ_WHERE_OVERLORD_INSTANCE
        = " AND authz_r.RESOURCE_TYPE_ID = ? "
        + "AND authz_r.INSTANCE_ID = %%RESID%%";

    public PermissionManagerImpl() {
        Connection conn = null;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);
            this.falseToken = DBUtil.getBooleanValue(false, conn);                
        } catch (Exception e) {
            throw new SystemException("Unable to initialize " +
                                      "PermissionManager:" + e, e);
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, null, null);
        }
    }

    public void check(Integer subject, ResourceTypeLocal type,
                      Integer instanceId, String operation)
        throws PermissionException {}

    public void check(Integer subject, ResourceType type, Integer instanceId,
                      String operation)
        throws PermissionException {}

    public void check(Integer subjectId, Integer typeId, Integer instanceId,
                      Integer operationId)
        throws PermissionException {}

    public void check(Integer subjectId, String resType, Integer instanceId,
                      String operation)
        throws PermissionException {}

    public boolean hasAdminPermission(AuthzSubjectValue who) {
        return true;
    }
    
    public PageList findOperationScopeBySubject(AuthzSubjectValue subj,
                                                String opName, String resType,
                                                PageControl pc) 
        throws FinderException, NamingException, PermissionException
    {
        log.debug("Checking Scope for Operation: " + opName +
                  " subject: " + subj);
        ResourceTypeLocal resTypeBean = getResourceTypeHome()
            .findByName(resType);
        OperationLocal opEJB = getOperationHome().findByTypeAndName( 
            resTypeBean, opName);

        return findOperationScopeBySubject(subj, ((OperationPK)opEJB.
                                                  getPrimaryKey()).getId(),pc);
    }

    public PageList findOperationScopeBySubject(AuthzSubjectValue subj,
                                                Integer opId, PageControl pc) 
        throws FinderException, NamingException, PermissionException
    {
        log.debug("Checking Scope for Operation: " + opId + " subject: " + 
                  subj);
        PageList scope = findScopeBySQL(subj, opId, pc);
        log.debug("Scope check returned a page of : " + scope.size() +
                  " of " + scope.getTotalSize() + " items");
        return scope;
    }

    public ResourceValue[]
        findOperationScopeBySubjectBatch(AuthzSubjectValue whoami,
                                         ResourceValue[] resArr, 
                                         String[] opArr)
        throws FinderException
    {
        ResourceLocal[] resLocArr;

        if (resArr == null || opArr == null ||
            resArr.length != opArr.length) {
            throw new IllegalArgumentException("At least one resource " +
                                               "must have an equal number " +
                                               "of operations");
        }

        resLocArr = new ResourceLocal[resArr.length];

        for (int x = 0; x < resLocArr.length; x++) {
            resLocArr[x] = lookupResource(resArr[x]);
        }
        
        Collection coll = 
            getResourceHome().findScopeByOperationBatch(resLocArr);
        return (ResourceValue[])this.fromLocals(coll, 
            org.hyperic.hq.authz.shared.ResourceValue.class);

    }

    public List findViewableResources(AuthzSubjectValue subj,
                                      String resType, 
                                      String resName, 
                                      PageControl pc)
    {
        List viewableInstances = new ArrayList();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;        
        try {
            conn = 
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE);
            String sql = null;
            if(resName == null) {
                sql = VIEWABLE_SQL + "ORDER BY EAM_RESOURCE.sort_name ";
                if(!pc.isAscending()) {
                    sql = sql + "DESC";
                }
                sql = StringUtil.replace(sql, "DB_FALSE_TOKEN",
                                         this.falseToken);
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, resType);
            } else {
                sql = VIEWABLE_BYNAME_SQL + "ORDER BY EAM_RESOURCE.sort_name ";
                if(!pc.isAscending()) {
                    sql = sql + "DESC";
                }
                sql = StringUtil.replace(sql, "DB_FALSE_TOKEN",
                                         this.falseToken);
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, resType);
                stmt.setString(2, resName);
                stmt.setInt(3, AppdefUtil.resNameToAppdefTypeId(resType));
                stmt.setString(4, resName);
            }
            log.debug("Viewable SQL: " + sql);
            rs = stmt.executeQuery();

            for(int i = 1; rs.next(); i++) {
                viewableInstances.add(new Integer(rs.getInt(1)));
            }
            return viewableInstances;
        } catch (SQLException e) {
            log.error("Error getting scope by SQL", e);
            throw new SystemException("SQL Error getting scope: " + e.getMessage());
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        }
    }

    private PageList findScopeBySQL(AuthzSubjectValue subj, 
                                    Integer opId,
                                    PageControl pc)
        throws FinderException, NamingException, PermissionException {
        Pager defaultPager = Pager.getDefaultPager();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List instanceIds = null;
        try {
            conn = 
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE);
            // Always return all resources
            stmt = conn.prepareStatement(ALL_RESOURCE_SQL);
            rs = stmt.executeQuery();
            // now build the list
            instanceIds = new ArrayList();
            for(int i = 1; rs.next(); i++) {
                instanceIds.add(new Integer(rs.getInt(1)));
            }
            return defaultPager.seek(instanceIds, pc.getPagenum(), pc.getPagesize());
        } catch (SQLException e) {
            log.error("Error getting scope by SQL", e);
            throw new FinderException("Error getting scope: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(ctx, conn, stmt, rs);
        }
    }

    public String getSQLWhere(Integer subjectId,
                              String resId)
    {
        return StringUtil.replace(AUTHZ_WHERE_OVERLORD_INSTANCE,
                                  "%%RESID%%", resId);
    }

    public int prepareSQL(PreparedStatement ps, 
                          int ps_idx, 
                          Integer subjectId, 
                          Integer resType,
                          Integer operationId) 
        throws SQLException
    {
        ps.setInt(ps_idx++, resType.intValue());
        return ps_idx;
    }

    public String getResourceTypeSQL(String col) {
        return
            "SELECT RES.INSTANCE_ID FROM " +
            "  EAM_RESOURCE RES, EAM_RESOURCE_TYPE RT " +
            "WHERE " + col + " = RES.INSTANCE_ID " +
            "  AND RES.FSYSTEM = " + this.falseToken + 
            "  AND RES.RESOURCE_TYPE_ID = RT.ID " +
            "  AND RT.NAME = ? ";
    }

    public int prepareResourceTypeSQL(PreparedStatement ps,
                                      int ps_idx,
                                      int subjectId,
                                      String resType,
                                      String op)
        throws SQLException
    {
        ps.setString(++ps_idx, resType);
        return ps_idx;
    }

    public List getAllOperations(AuthzSubjectValue subject, PageControl pc)
        throws NamingException, PermissionException, FinderException {
        RoleLocal rootRole = RoleUtil.getLocalHome()
            .findByPrimaryKey(new RolePK(AuthzConstants.rootRoleId));
        Set ops = new HashSet();
        ops.addAll(rootRole.getOperations());
        pc = PageControl.initDefaults(pc, SortAttribute.OPERATION_NAME);
        Pager operationPager;
        try {
            operationPager = Pager.getPager(OPERATION_PAGER);
        } catch (Exception e) {
            return null;
        }
        return operationPager.seek((Collection) ops, pc.getPagenum(),
                                   pc.getPagesize());
    }

    public Collection getGroupResources(Integer subjectId,
                                        Integer groupId, Boolean fsystem)
        throws NamingException, FinderException
    {
        return getResourceHome().findInGroup_orderName_asc(groupId, fsystem);
    }

    public Collection findServiceResources(AuthzSubjectLocal subject,
                                           Boolean fsystem)
        throws NamingException, FinderException
    {
        return getResourceHome().findSvcRes_orderName(fsystem);
    }
}
