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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleDAO;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.HierarchicalAlertingManager;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.hyperic.util.IntegerTransformer;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("permissionManager")
public class PermissionManagerImpl extends PermissionManager {
    private static final Log _log = LogFactory.getLog(PermissionManagerImpl.class.getName());

    private final String _falseToken;

    private DBUtil dbUtil;

    private OperationDAO operationDAO;

    private static final String VIEWABLE_SELECT = "SELECT instance_id, EAM_RESOURCE.sort_name, EAM_RESOURCE.id, "
                                                  + "EAM_RESOURCE.resource_type_id "
                                                  + "FROM EAM_RESOURCE ";

    private static final String VIEWABLE_CLAUSE = " EAM_RESOURCE.fsystem = DB_FALSE_TOKEN AND "
                                                  + "EAM_RESOURCE.resource_type_id = "
                                                  + "(SELECT rt.id FROM EAM_RESOURCE_TYPE rt WHERE rt.name = ?) ";

    private static final String VIEWABLE_BYNAME_SQL = " AND (lower(EAM_RESOURCE.name) like lower('%$$resName$$%') OR "
                                                      + " EAM_RESOURCE.instance_id in (SELECT appdef_id FROM EAM_CPROP, "
                                                      + " EAM_CPROP_KEY WHERE keyid = EAM_CPROP_KEY.id AND "
                                                      + " appdef_type = ? AND lower(propvalue) like lower('%$$resName$$%'))) ";

    private static final String ALL_RESOURCE_SQL = "SELECT res.instance_id FROM EAM_RESOURCE res, EAM_OPERATION o "
                                                   + "WHERE o.resource_type_id = res.resource_type_id and o.id = ?";

    private static final String VIEWABLE_SEARCH = "WHERE EAM_RESOURCE.fsystem = DB_FALSE_TOKEN AND "
                                                  + "RESOURCE_TYPE_ID IN (3, 301, 303, 305, 308)  AND "
                                                  + "(SORT_NAME LIKE UPPER(?) OR "
                                                  + "PROTO_ID IN (SELECT ID FROM EAM_RESOURCE "
                                                  + "WHERE PROTO_ID = 0 AND SORT_NAME LIKE UPPER(?))) ";

    private Connection getConnection() throws SQLException {
        return dbUtil.getConnection();
    }

    @Autowired
    public PermissionManagerImpl(DBUtil dbUtil, OperationDAO operationDAO) {
        Connection conn = null;
        this.dbUtil = dbUtil;
        this.operationDAO = operationDAO;
        try {
            conn = getConnection();
            _falseToken = DBUtil.getBooleanValue(false, conn);
        } catch (Exception e) {
            throw new SystemException("Unable to initialize " + "PermissionManager:" + e, e);
        } finally {
            DBUtil.closeConnection(PermissionManagerImpl.class, conn);
        }
    }

    public void check(Integer subject, ResourceType type, Integer instanceId, String operation)
        throws PermissionException {
    }

    public void check(Integer subjectId, Integer typeId, Integer instanceId, Integer operationId)
        throws PermissionException {
    }

    public void check(Integer subjectId, String resType, Integer instanceId, String operation)
        throws PermissionException {
    }
    
    public void check(Integer subjectId, Integer typeId, Integer operationId) 
            throws PermissionException{
    }
    

    public boolean hasAdminPermission(Integer who) {
        return true;
    }

    public Collection<Integer> findOperationScopeBySubject(AuthzSubject subj, String opName,
                                                           String resType)
    throws NotFoundException,
        PermissionException {
        if (_log.isDebugEnabled()) {
            _log.debug("Checking Scope for Operation: " + opName + " subject: " + subj);
        }

        ResourceType resTypeBean = getResourceTypeDAO().findByName(resType);

        if (resTypeBean != null) {
            Operation op = getOperationDAO().findByTypeAndName(resTypeBean, opName);

            if (op != null) {
                return findOperationScopeBySubject(subj, op.getId());
            }
        }

        return new ArrayList<Integer>();
    }

    public Collection<Integer> findOperationScopeBySubject(AuthzSubject subj, Integer opId)
    throws NotFoundException, PermissionException {
        if (_log.isDebugEnabled()) {
            _log.debug("Checking Scope for Operation: " + opId + " subject: " + subj);
        }

        List<Integer> scope = findScopeBySQL(subj, opId);

        if (_log.isDebugEnabled()) {
            _log.debug("Scope check returned a size of : " + scope.size() + " items");
        }
        return scope;
    }

    public Resource[] findOperationScopeBySubjectBatch(AuthzSubject whoami, ResourceValue[] resArr,
                                                       String[] opArr) throws ApplicationException {
        if (resArr == null) {
            throw new IllegalArgumentException("At least one resource required");
        }

        Set resLocArr = toPojos(resArr);

        return (Resource[]) resLocArr.toArray(new Resource[resLocArr.size()]);
    }

    protected RoleDAO getRoleDAO() {
        return Bootstrap.getBean(RoleDAO.class);
    }

    protected ResourceGroupDAO getResourceGroupDAO() {
        return Bootstrap.getBean(ResourceGroupDAO.class);
    }

    private Resource lookupResource(ResourceValue resource) {
        if (resource.getId() == null) {
            ResourceType type = resource.getResourceType();
            return getResourceDAO().findByInstanceId(type, resource.getInstanceId());
        }
        return getResourceDAO().findById(resource.getId());
    }

    private Set toPojos(Object[] vals) {
        Set ret = new HashSet();
        if (vals == null || vals.length == 0) {
            return ret;
        }

        RoleDAO roleDao = null;
        ResourceGroupDAO resGrpDao = null;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof Operation) {
                ret.add(vals[i]);
            } else if (vals[i] instanceof ResourceValue) {
                ret.add(lookupResource((ResourceValue) vals[i]));
            } else if (vals[i] instanceof RoleValue) {
                if (roleDao == null) {
                    roleDao = getRoleDAO();
                }
                ret.add(roleDao.findById(((RoleValue) vals[i]).getId()));
            } else if (vals[i] instanceof ResourceGroupValue) {
                if (resGrpDao == null) {
                    resGrpDao = getResourceGroupDAO();
                }
                ret.add(resGrpDao.findById(((ResourceGroupValue) vals[i]).getId()));
            } else {
                _log.error("Invalid type.");
            }

        }

        return ret;
    }

    public List<Integer> findViewableResources(AuthzSubject subj, String searchFor, PageControl pc) {
        List<Integer> viewableInstances = new ArrayList<Integer>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            String sql = VIEWABLE_SELECT + VIEWABLE_SEARCH;

            // TODO: change sort by
            sql += "ORDER BY EAM_RESOURCE.resource_type_id, " + "EAM_RESOURCE.sort_name ";

            if (!pc.isAscending()) {
                sql = sql + "DESC";
            }
            sql = StringUtil.replace(sql, "DB_FALSE_TOKEN", _falseToken);

            if (searchFor == null) {
                searchFor = "%";
            } else {
                // Support wildcards
                searchFor = '%' + searchFor.replace('*', '%') + '%';
            }

            stmt = conn.prepareStatement(sql);
            int i = 1;

            stmt.setString(i++, searchFor);
            stmt.setString(i++, searchFor);

            if (_log.isDebugEnabled())
                _log.debug("Viewable search for (" + searchFor + ") SQL: " + sql);

            rs = stmt.executeQuery();

            for (i = 1; rs.next(); i++) {
                viewableInstances.add(new Integer(rs.getInt(3)));
            }
            return viewableInstances;
        } catch (SQLException e) {
            _log.error("Error search by SQL", e);
            throw new SystemException("SQL Error search: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(PermissionManagerImpl.class, conn, stmt, rs);
        }
    }

    private List<Integer> findScopeBySQL(AuthzSubject subj, Integer opId) throws NotFoundException,
        PermissionException {
        Pager defaultPager = Pager.getDefaultPager();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Integer> instanceIds = null;
        try {
            conn = getConnection();
            // Always return all resources
            stmt = conn.prepareStatement(ALL_RESOURCE_SQL);
            stmt.setInt(1, opId.intValue());
            rs = stmt.executeQuery();
            // now build the list
            instanceIds = new ArrayList<Integer>();
            for (int i = 1; rs.next(); i++) {
                instanceIds.add(new Integer(rs.getInt(1)));
            }
            return instanceIds;
        } catch (SQLException e) {
            _log.error("Error getting scope by SQL", e);
            throw new NotFoundException("Error getting scope: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(PermissionManagerImpl.class, conn, stmt, rs);
        }
    }

    public List getAllOperations(AuthzSubject subject, PageControl pc) throws PermissionException {
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

    public Collection<Resource> getGroupResources(Integer subjectId, Integer groupId,
                                                  Boolean fsystem) {
        return getResourceDAO().findInGroup_orderName(groupId, fsystem);
    }

    public Collection<Resource> findServiceResources(AuthzSubject subj, Boolean fsystem) {
        return getResourceDAO().findSvcRes_orderName(fsystem);
    }

    public RolePermNativeSQL getRolePermissionNativeSQL(String resourceVar, String eventLogVar, String subjectParam,
                                                        String opListViewResourcesParam,
                                                        String opListManageAlertsParam) {
        return new RolePermNativeSQL() {
            public String getSQL() {
                return "";
            }

            public Query bindParams(Query q, AuthzSubject subject, List opsViewResources, List opsManageAlerts) {
                return q;
            }
        };
    }

    public String getAlertsHQL(boolean inEscalation, boolean notFixed, Integer groupId,
                               Integer resourceId, Integer alertDefId, boolean count) {
        // Join with Resource for sorting
        return "select " +
               (count ? "count(a)" : "a") +
               " from " +
               (inEscalation ? "EscalationState es, " : "") +
               "Alert a " +
               "join a.alertDefinition d " +
               "join d.resource r " +
               "where r.resourceType is not null and " +
               (groupId == null ? "" : "exists (select rg from r.groupBag rg " +
                                       "where rg.group.id = " + groupId + ") and ") +
               (resourceId == null ? "" : "r.id = " + resourceId + " and ") +
               "a.ctime between :begin and :end and " + (notFixed ? " a.fixed = false and " : "") +
               (alertDefId == null ? "" : "d.id = " + alertDefId + " and ") +
               "d.priority >= :priority " +
               (inEscalation ? "and a.id = es.alertId and " + "es.alertDefinitionId = d.id " : "");
    }

    public String getAlertDefsHQL() {
        return "select d from AlertDefinition d " + "join d.resource r "
               + "where r.resourceType is not null and d.priority >= :priority";
    }

    public String getGroupAlertsHQL(boolean inEscalation, boolean notFixed, Integer groupId,
                                    Integer galertDefId) {
        return "select a from " + (inEscalation ? "EscalationState es, " : "") + "GalertLog a " +
               "join a.alertDef d " + "where " +
               (groupId != null ? " g.id = " + groupId + " and " : "") +
               "a.timestamp between :begin and :end " + (notFixed ? " and a.fixed = false " : "") +
               (galertDefId == null ? "" : "and d.id = " + galertDefId + " ") +
               "and d.severityEnum >= :priority " +
               (inEscalation ? "and a.id = es.alertId and " + "es.alertDefinitionId = d.id " : "");
    }

    public String getGroupAlertDefsHQL() {
        return "select d from GalertDef d " + "join d.group g " + "join d.escalation e "
               + "where d.severityEnum >= :priority ";
    }

    public boolean hasGuestRole() {
        return false;
    }

    public EdgePermCheck makePermCheckSql(String subjectParam, String resVar, String resParam,
                                          String distanceParam, String opsParam,
                                          boolean includeDescendants) {
        final Integer cId = AuthzConstants.RELATION_CONTAINMENT_ID;
        final String oper = (includeDescendants) ? ">=" : "=";
        final String sql = new StringBuilder().append(" JOIN EAM_RESOURCE_EDGE edge")
            .append(" ON ").append(resVar).append(".id = edge.TO_ID").append(" AND ")
            .append(resVar).append(".id = edge.FROM_ID").append(" WHERE edge.distance ").append(
                oper).append(" :").append(distanceParam).append(" AND edge.rel_id = ").append(cId)
            .append(" AND ").append(resVar).append(".id = :").append(resParam).append(" ")
            .toString();

        return new EdgePermCheck(sql, subjectParam, resVar, resParam, distanceParam, opsParam) {
            public Query addQueryParameters(Query q, AuthzSubject subject, Resource r,
                                            int distance, List ops) {
                return q.setInteger(getDistanceParam(), distance).setInteger(getResourceParam(),
                    r.getId().intValue());
            }
        };
    }

    public EdgePermCheck makePermCheckHql(String subjectParam, String resourceVar,
                                          String resourceParam, String distanceParam,
                                          String opsParam, boolean includeDescendants) {
        final Integer cId = AuthzConstants.RELATION_CONTAINMENT_ID;
        final String oper = (includeDescendants) ? ">=" : "=";
        final String sql = new StringBuilder().append("join ").append(resourceVar).append(
            ".toEdges _e ").append("join _e.from _fromResource ").append("where ").append(
            " _fromResource = :").append(resourceParam).append(" AND _e.distance ").append(oper)
            .append(" :").append(distanceParam).append(" AND _e.relation.id = ").append(cId)
            .append(' ').toString();

        return new EdgePermCheck(sql, subjectParam, resourceVar, resourceParam, distanceParam,
            opsParam) {
            public Query addQueryParameters(Query q, AuthzSubject subject, Resource r,
                                            int distance, List ops) {
                return q.setInteger(getDistanceParam(), distance).setParameter(getResourceParam(),
                    r);
            }
        };
    }

    public String getOperableGroupsHQL(AuthzSubject subject, String alias, String oper) {
        return "";
    }

    public String getSQLWhere(Integer subjectId) {
        return "";
    }

    public MaintenanceEventManager getMaintenanceEventManager() {
        return (MaintenanceEventManager) Bootstrap.getBean("MaintenanceEventManager");
    }

    public HierarchicalAlertingManager getHierarchicalAlertingManager() {
        return (HierarchicalAlertingManager) Bootstrap.getBean("HierarchicalAlertingManager");
    }

    @Override
    public Set<Integer> findViewablePSSResources(AuthzSubject subj) {
        final Collection<ResourceType> types = new ArrayList<ResourceType>();
        final ResourceManager resourceManager = Bootstrap.getBean(ResourceManager.class);
        types.add(resourceManager.getResourceTypeById(AuthzConstants.authzPlatform));
        types.add(resourceManager.getResourceTypeById(AuthzConstants.authzServer));
        types.add(resourceManager.getResourceTypeById(AuthzConstants.authzService));
        return findViewableResources(subj, types);
    }

    public List<AppdefEntityID> findViewableInstances(AuthzSubject subj,
                                                      Collection<ResourceType> resourceTypes) {
        if (resourceTypes.isEmpty()) {
            return Collections.emptyList();
        }
        final ResourceDAO resourceDAO = getResourceDAO();
        final Collection<Resource> resources = (subj.getId().equals(1)) ?
            resourceDAO.findAll() : resourceDAO.findByOwner(subj, PageControl.SORT_UNSORTED);
        final List<AppdefEntityID> rtn = new ArrayList<AppdefEntityID>(resources.size());
        final Set<Integer> typeIds = new HashSet<Integer>();
        for (final ResourceType type : resourceTypes) {
            typeIds.add(type.getId());
        }
        for (final Resource r : resources) {
            if (r == null || r.isInAsyncDeleteState() || r.isSystem()) {
                continue;
            }
            if (typeIds.contains(r.getResourceType().getId())) {
                rtn.add(AppdefUtil.newAppdefEntityId(r));
            }
        }
        return rtn;
    }

    public Set<Integer> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes) {
        return findViewableResources(subj, resourceTypes, new IntegerTransformer<Integer>() {
            public Integer transform(Integer id) {
                return id;
            }
        });
    }

    public <T> Set<T> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes,
                                            IntegerTransformer<T> transformer) {
        return findViewableResources(subj, resourceTypes, PageControl.SORT_UNSORTED, transformer);
    }

    public <T> Set<T> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes,
                                            int sortName, IntegerTransformer<T> transformer) {
        return findViewableResources(subj, resourceTypes, sortName, transformer, null);
    }

    @Override
    public <T> Set<T> findViewableResources(AuthzSubject subj, Collection<ResourceType> resourceTypes,
                                            int sortName, IntegerTransformer<T> transformer,
                                            Comparator<T> comparator) {
        if (resourceTypes.isEmpty()) {
            return Collections.emptySet();
        }
        final ResourceDAO resourceDAO = getResourceDAO();
        final Collection<Resource> resources = (subj.getId().equals(1)) ?
            resourceDAO.findAllOrderByName() : resourceDAO.findByOwner(subj, sortName);
        final Set<Integer> typeIds = new HashSet<Integer>();
        final Set<T> rtn = (comparator != null) ? new TreeSet<T>(comparator) : new HashSet<T>();
        for (final ResourceType type : resourceTypes) {
            typeIds.add(type.getId());
        }
        for (final Resource r : resources) {
            if (r == null || r.isInAsyncDeleteState() || r.isSystem()) {
                continue;
            }
            final Integer id = r.getId();
            final int typeId = r.getResourceType().getId();
            if (typeIds.contains(typeId)) {
                T val = transformer.transform(id);
                if (val != null) {
                    rtn.add(val);
                }
            }
        }
        return rtn;
    }
    
    @Override
    public <T> Set<T> findViewableResources(AuthzSubject subj, Collection<Role> roles,
            Collection<ResourceType> resourceTypes, IntegerTransformer<T> transformer) {
        Set<T> resourcesViewableByOwner = findViewableResources(subj, resourceTypes, PageControl.SORT_UNSORTED, transformer);        
        
        return resourcesViewableByOwner;                
    }    

    public TypeCounts findViewableInstanceCounts(AuthzSubject subj, Collection<ResourceType> types) {
        final TypeCounts rtn = new TypeCounts();
        if (types.isEmpty()) {
            return rtn;
        }
        final ResourceDAO resourceDAO = getResourceDAO();
        final Collection<Resource> resources = (subj.getId().equals(1)) ?
            resourceDAO.findAll() : resourceDAO.findByOwner(subj, PageControl.SORT_UNSORTED);
        final Set<Integer> typeIds = new HashSet<Integer>();
        for (final ResourceType type : types) {
            typeIds.add(type.getId());
        }
        ResourceGroupManager resourceGroupManager = Bootstrap.getBean(ResourceGroupManager.class);
        for (final Resource r : resources) {
            if (r == null || r.isInAsyncDeleteState() || r.isSystem() || !typeIds.contains(r.getResourceType().getId())) {
                continue;
            }
            final int protoType = r.getPrototype().getId();
            final AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(r);
            int appdefType = -1;
            if (r.getResourceType().getId().equals(AuthzConstants.authzGroup)) {
                ResourceGroup group = resourceGroupManager.getResourceGroupById(r.getInstanceId());
                if (group == null) {
                    continue;
                }
                appdefType = group.getGroupType();
            } else {
                appdefType = aeid.getType();
            }
            rtn.incrementAppdefTypeCount(appdefType);
            rtn.incrementProtoTypeCount(appdefType, protoType);
        }
        return rtn;
    }

    @Override
    public <T> Set<T> findResourcesByOperationIds(AuthzSubject subj, Collection<Integer> operationIds,
                                                  IntegerTransformer<T> transformer) {
        final List<ResourceType> resourceTypes = new ArrayList<ResourceType>();
        for (final Integer opId : operationIds) {
            final Operation op = operationDAO.findById(opId);
            resourceTypes.add(op.getResourceType());
        }
        return findViewableResources(subj, resourceTypes, transformer);
    }

}
