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

package org.hyperic.hq.appdef.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AppdefStatManagerImpl provides summary and aggregate statistical information
 * for appdef related entities.
 * <p>
 * 
 * </p>
 * 
 */
@org.springframework.stereotype.Service
public class AppdefStatManagerImpl implements AppdefStatManager {

    private static final String LOG_CTX = AppdefStatManagerImpl.class.getName();

    private final Log log = LogFactory.getLog(LOG_CTX);
    private static int DB_TYPE = -1;

    private static final String TBL_PLATFORM = "EAM_PLATFORM";
    private static final String TBL_SERVICE = "EAM_SERVICE";
    private static final String TBL_SERVER = "EAM_SERVER";

    private static final String TBL_RES = "EAM_RESOURCE";

    private static final String PLATFORM_RES_TYPE = AuthzConstants.platformResType;

    private static final String SERVER_RES_TYPE = AuthzConstants.serverResType;
    private static final String SERVICE_RES_TYPE = AuthzConstants.serviceResType;

    private static final String PLATFORM_OP_VIEW_PLATFORM = AuthzConstants.platformOpViewPlatform;

    private static final String SERVER_OP_VIEW_SERVER = AuthzConstants.serverOpViewServer;
    private static final String SERVICE_OP_VIEW_SERVICE = AuthzConstants.serviceOpViewService;

    private static final int APPDEF_TYPE_PLATFORM = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    private static final int APPDEF_TYPE_SERVER = AppdefEntityConstants.APPDEF_TYPE_SERVER;
    private static final int APPDEF_TYPE_SERVICE = AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    private static final int APPDEF_TYPE_GROUP = AppdefEntityConstants.APPDEF_TYPE_GROUP;

    private PermissionManager permissionManager;

    private ApplicationManager applicationManager;

    private PlatformManager platformManager;

    private ServerManager serverManager;

    private ServiceManager serviceManager;

    private ResourceGroupManager resourceGroupManager;

    private AppdefStatDAO appdefStatDAO;

    @Autowired
    public AppdefStatManagerImpl(PermissionManager permissionManager,
                                 ApplicationManager applicationManager,
                                 PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager,
                                 ResourceGroupManager resourceGroupManager,
                                 AppdefStatDAO appdefStatDAO) {
        this.permissionManager = permissionManager;
        this.applicationManager = applicationManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.appdefStatDAO = appdefStatDAO;
    }

    /**
     * <p>
     * Return map of platform counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getPlatformCountsByTypeMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding Platforms by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return platforms count.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getPlatformsCount(AuthzSubject subject) {
        try {
            return appdefStatDAO.getPlatformsCount(subject);
        } catch (Exception e) {
            log.error("Caught Exception counting Platforms: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of server counts.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getServerCountsByTypeMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getServerCountsByTypeMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return servers count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getServersCount(AuthzSubject subject) {
        try {
            return appdefStatDAO.getServersCount(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of service counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getServiceCountsByTypeMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getServiceCountsByTypeMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return services count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getServicesCount(AuthzSubject subject) {
        try {
            return appdefStatDAO.getServicesCount(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of app counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getApplicationCountsByTypeMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getApplicationCountsByTypeMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return apps count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getApplicationsCount(AuthzSubject subject) {
        try {
            return appdefStatDAO.getApplicationsCount(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of grp counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<Integer, Integer> getGroupCountsMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getGroupCountsMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding groups by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return directly connected resource tree for node level platform
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, Integer platformId)
        throws PlatformNotFoundException, PermissionException {
        try {
            Platform plat = platformManager.findPlatformById(platformId);
            ResourceTreeNode[] retVal = appdefStatDAO.getNavMapDataForPlatform(subject, plat);
            if (log.isDebugEnabled()) {
                log.debug(mapToString(retVal));
            }
            return retVal;
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return directly connected resource tree for node level server
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject, Integer serverId)
        throws ServerNotFoundException, PermissionException {
        Server server = serverManager.findServerById(serverId);

        try {
            return appdefStatDAO.getNavMapDataForServer(subject, server);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject, Integer serviceId)
        throws ServiceNotFoundException, PermissionException {
        Service service = serviceManager.findServiceById(serviceId);
        try {
            return appdefStatDAO.getNavMapDataForService(subject, service);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private String mapToString(ResourceTreeNode[] node) {
        StringBuffer sb = new StringBuffer();
        if (node == null) {
            sb.append("MAP IS NULL!\n");
            return sb.toString();
        }
        int height = node.length;
        for (int x = 0; x < height; x++) {
            if (node[x] == null) {
                sb.append("MAP[" + x + "] IS NULL!\n");
            } else {
                sb.append("MAP[" + x + "] NOT NULL \n");
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject, Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        Application app = applicationManager.findApplicationById(subject, appId);
        try {
            return appdefStatDAO.getNavMapDataForApplication(subject, app);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return resources for autogroups
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject,
                                                        AppdefEntityID[] parents, Integer resType)
        throws AppdefEntityNotFoundException, PermissionException {
        try {
            // platform auto-groups do not have parent resource types
            int entType = (parents != null) ? getChildEntityType(parents[0].getType())
                                           : APPDEF_TYPE_PLATFORM;

            AppdefResourceType type = getResourceTypeValue(entType, resType);
            return getNavMapDataForAutoGroup(subject, parents, type);
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private AppdefResourceType getResourceTypeValue(int entityType, Integer resType)
        throws AppdefEntityNotFoundException {
        switch (entityType) {
            case APPDEF_TYPE_PLATFORM:
                return platformManager.findPlatformType(resType);
            case APPDEF_TYPE_SERVER:
                return serverManager.findServerType(resType);
            case APPDEF_TYPE_SERVICE:
                return serviceManager.findServiceType(resType);
            default:
                return null;
        }
    }

    private ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject,
                                                         AppdefEntityID[] parents,
                                                         AppdefResourceType type)
        throws AppdefEntityNotFoundException, PermissionException, SQLException {
        ResourceTreeNode[] retVal = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int pEntityType;
        int cEntityType;
        String sqlStmt;
        String bindMarkerStr = "";
        String authzResName;
        String authzOpName;
        final int APPDEF_TYPE_UNDEFINED = -1;
        List<ResourceTreeNode> parentNodes = null;

        Integer subjectId = subject.getId();
        // derive parent and child entity types
        pEntityType = (parents != null) ? parents[0].getType() : APPDEF_TYPE_UNDEFINED;
        cEntityType = (pEntityType != APPDEF_TYPE_UNDEFINED) ? getChildEntityType(pEntityType)
                                                            : APPDEF_TYPE_PLATFORM;

        try {

            // If the auto-group has parents, fetch the resources
            if (parents != null) {
                parentNodes = new ArrayList<ResourceTreeNode>(parents.length);
                for (int x = 0; x < parents.length; x++) {
                    AppdefEntityValue av = new AppdefEntityValue(parents[x], subject);
                    parentNodes.add(new ResourceTreeNode(av.getName(), getAppdefTypeLabel(
                        pEntityType, av.getTypeName()), parents[x], ResourceTreeNode.RESOURCE));
                }
            }

            // Platforms don't have a auto-group parents
            if (pEntityType != APPDEF_TYPE_UNDEFINED) {
                for (int x = 0; x < parents.length; x++) {
                    bindMarkerStr += (x < parents.length - 1) ? "?," : "?";
                }
            }
            Connection conn = getDBConn();

            final String res_join = " JOIN " + TBL_RES + " res on resource_id = res.id ";
            final String platAGSql = "SELECT p.id as platform_id, res.name as platform_name, " +
                                     "       pt.id as platform_type_id, pt.name as platform_type_name " +
                                     "FROM " +
                                     TBL_PLATFORM +
                                     "_TYPE pt, " +
                                     TBL_PLATFORM +
                                     " p " +
                                     res_join +
                                     " WHERE p.platform_type_id=pt.id AND platform_type_id=" +
                                     type.getId() +
                                     " AND " +
                                     "EXISTS (" +
                                     permissionManager.getResourceTypeSQL("p.id", subjectId,
                                         PLATFORM_RES_TYPE, PLATFORM_OP_VIEW_PLATFORM) + ") ";

            final String svrAGSql = "SELECT s.id as server_id, res.name as server_name, " +
                                    "       st.id as server_type_id, st.name as server_type_name " +
                                    "FROM " +
                                    TBL_SERVER +
                                    "_TYPE st, " +
                                    TBL_SERVER +
                                    " s " +
                                    res_join +
                                    " WHERE s.server_type_id=st.id AND platform_id in ( " +
                                    bindMarkerStr +
                                    " ) " +
                                    "   AND server_type_id=" +
                                    type.getId() +
                                    "   AND EXISTS (" +
                                    permissionManager.getResourceTypeSQL("s.id", subjectId,
                                        SERVER_RES_TYPE, SERVER_OP_VIEW_SERVER) + ") ";

            final String svcAGSql = "SELECT s.id as service_id, res.name as service_name, " +
                                    "       st.id as service_type_id, st.name as service_type_name " +
                                    "FROM " +
                                    TBL_SERVICE +
                                    "_TYPE st, " +
                                    TBL_SERVICE +
                                    " s " +
                                    res_join +
                                    " WHERE s.service_type_id=st.id AND s.server_id in ( " +
                                    bindMarkerStr +
                                    " ) AND " +
                                    "s.service_type_id=" +
                                    type.getId() +
                                    "   AND EXISTS (" +
                                    permissionManager.getResourceTypeSQL("s.id", subjectId,
                                        SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE) + ") ";

            final String appSvcAGSql = "SELECT s.id as service_id, res.name as service_name, " +
                                       "       st.id as service_type_id, st.name as service_type_name " +
                                       "FROM " +
                                       TBL_SERVICE +
                                       "_TYPE st, EAM_APP_SERVICE aps, " +
                                       TBL_SERVICE +
                                       " s " +
                                       res_join +
                                       " WHERE s.service_type_id=st.id and s.id=aps.service_id AND " +
                                       "aps.application_id in ( " +
                                       bindMarkerStr +
                                       " ) AND " +
                                       "s.service_type_id=" +
                                       type.getId() +
                                       "   AND EXISTS (" +
                                       permissionManager.getResourceTypeSQL("s.id", subjectId,
                                           SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE) + ") ";

            switch (pEntityType) {
                case APPDEF_TYPE_PLATFORM:
                    sqlStmt = svrAGSql;
                    authzResName = AuthzConstants.serverResType;
                    authzOpName = AuthzConstants.serverOpViewServer;
                    break;
                case APPDEF_TYPE_SERVER:
                    sqlStmt = svcAGSql;
                    authzResName = AuthzConstants.serviceResType;
                    authzOpName = AuthzConstants.serviceOpViewService;
                    break;
                case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION):
                    sqlStmt = appSvcAGSql;
                    authzResName = AuthzConstants.serviceResType;
                    authzOpName = AuthzConstants.serviceOpViewService;
                    break;
                case (APPDEF_TYPE_UNDEFINED):
                    sqlStmt = platAGSql;
                    authzResName = AuthzConstants.platformResType;
                    authzOpName = AuthzConstants.platformOpViewPlatform;
                    break;
                default:
                    throw new IllegalArgumentException("No auto-group support "
                                                       + "for specified type");
            }

            if (log.isDebugEnabled())
                log.debug(sqlStmt);

            ResourceTreeNode agNode = new ResourceTreeNode(type.getName(), getAppdefTypeLabel(
                cEntityType, type.getName()), parents, type.getId().intValue(),
                ResourceTreeNode.AUTO_GROUP);
            Set<ResourceTreeNode> entitySet = new HashSet<ResourceTreeNode>();
            int x = 0;
            try {
                stmt = conn.prepareStatement(sqlStmt);

                if (pEntityType != APPDEF_TYPE_UNDEFINED) {
                    for (; x < parents.length; x++) {
                        stmt.setInt(x + 1, parents[x].getID());
                    }
                }

                StopWatch timer = new StopWatch();

                rs = stmt.executeQuery();

                if (log.isDebugEnabled()) {
                    log.debug("getNavMapDataForAutoGroup() executed in: " + timer);
                    log.debug("SQL: " + sqlStmt);
                    int i;
                    for (i = 0; i < parents.length; i++) {
                        log.debug("Arg " + (i + 1) + ": " + parents[x].getID());
                    }
                    i = 1;
                    log.debug("Arg " + (i++) + ": " + type.getId());
                    log.debug("Arg " + (i++) + ": " + subject.getId());
                    log.debug("Arg " + (i++) + ": " + subject.getId());
                    log.debug("Arg " + (i++) + ": " + authzResName);
                    log.debug("Arg " + (i++) + ": " + authzOpName);
                }

                while (rs.next()) {
                    int thisEntityId = rs.getInt(1);
                    String thisEntityName = rs.getString(2);
                    String thisEntityTypeName = rs.getString(4);

                    entitySet.add(new ResourceTreeNode(thisEntityName, getAppdefTypeLabel(
                        cEntityType, thisEntityTypeName), new AppdefEntityID(cEntityType,
                        thisEntityId), ResourceTreeNode.RESOURCE));
                }

                agNode.setSelected(true);
                if (parentNodes != null) {
                    ResourceTreeNode[] parNodeArr = (ResourceTreeNode[]) parentNodes
                        .toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(parNodeArr, true);
                    agNode.addUpChildren(parNodeArr);
                }

                ResourceTreeNode[] members = (ResourceTreeNode[]) entitySet
                    .toArray(new ResourceTreeNode[0]);

                ResourceTreeNode.alphaSortNodes(members);
                agNode.addDownChildren(members);

                retVal = new ResourceTreeNode[] { agNode };

            } finally {
                DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
            }
        } catch (SQLException e) {
            throw e;
        }
        return retVal;
    }

    /**
     * <p>
     * Return resources for groups (not autogroups)
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, Integer groupId)
        throws PermissionException {

        ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, groupId);
        AppdefGroupValue groupVal = resourceGroupManager.getGroupConvert(subject, group);
        try {
            return getNavMapDataForGroup(subject, groupVal);
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, AppdefGroupValue groupVo)
        throws PermissionException, SQLException {
        ResourceTreeNode grpNode = new ResourceTreeNode(groupVo.getName(), getAppdefTypeLabel(
            APPDEF_TYPE_GROUP, groupVo.getAppdefResourceTypeValue().getName()), groupVo
            .getEntityId(), ResourceTreeNode.CLUSTER);
        final List<AppdefEntityID> agEntries = groupVo.getAppdefGroupEntries();
        if (agEntries.size() == 0) {
            return new ResourceTreeNode[] { grpNode };
        }
        ResourceTreeNode[] retVal = null;
        final StringBuilder grpSqlStmt = new StringBuilder();
        final boolean debug = log.isDebugEnabled();

        int entityType = groupVo.getGroupEntType();

        final Connection conn = getDBConn();

        final String resJoin = new StringBuilder().append(" JOIN ").append(TBL_RES).append(
            " res on resource_id = res.id ").toString();

        switch (entityType) {
            case APPDEF_TYPE_PLATFORM:
                grpSqlStmt.append("SELECT p.id as platform_id, res.name as platform_name ").append(
                    " FROM ").append(TBL_PLATFORM).append(" p ").append(resJoin).append(
                    "WHERE p.id IN (");
                break;
            case APPDEF_TYPE_SERVER:
                grpSqlStmt.append("SELECT s.id as server_id, res.name as server_name ").append(
                    "FROM ").append(TBL_SERVER).append(" s ").append(resJoin).append(
                    "WHERE s.id IN (");
                break;
            case APPDEF_TYPE_SERVICE:
                grpSqlStmt.append("SELECT s.id as service_id, res.name as service_name ").append(
                    "FROM ").append(TBL_SERVICE).append(" s  ").append(resJoin).append(
                    "WHERE s.id IN (");
                break;
            default:
                throw new IllegalArgumentException("No group support " + "for specified type");
        }

        if (debug) {
            log.debug(grpSqlStmt);
        }
        Set<ResourceTreeNode> entitySet = new HashSet<ResourceTreeNode>(agEntries.size());

        Map<Integer, String> entNameMap = new HashMap<Integer, String>();
        if (groupVo.getTotalSize() > 0) {
            final int max = Util.getHQDialect().getMaxExpressions();
            final int batchSize = (max < 0) ? Integer.MAX_VALUE : max;
            for (int ii = 0; ii < agEntries.size(); ii += batchSize) {
                int end = Math.min(ii + batchSize, agEntries.size());
                List<AppdefEntityID> list = agEntries.subList(ii, end);
                setEntNameMap(entNameMap, list, conn, grpSqlStmt);
            }

            // Let group member order drive node creation (not db order).
            for (AppdefEntityID id : groupVo.getAppdefGroupEntries()) {
                entitySet
                    .add(new ResourceTreeNode((String) entNameMap.get(id.getId()),
                        getAppdefTypeLabel(id.getType(), groupVo.getAppdefResourceTypeValue()
                            .getName()), new AppdefEntityID(entityType, id.getId()),
                        ResourceTreeNode.RESOURCE));
            }
        }

        ResourceTreeNode[] memberNodes = entitySet.toArray(new ResourceTreeNode[0]);

        grpNode.setSelected(true);
        ResourceTreeNode.alphaSortNodes(memberNodes);
        grpNode.addDownChildren(memberNodes);

        retVal = new ResourceTreeNode[] { grpNode };

        return retVal;
    }

    private void setEntNameMap(Map<Integer, String> entNameMap, List<AppdefEntityID> list,
                               Connection conn, StringBuilder grpSqlStmt) throws SQLException {
        if (list.size() == 0) {
            return;
        }
        final boolean debug = log.isDebugEnabled();
        // don't overwrite the caller's object
        grpSqlStmt = new StringBuilder(grpSqlStmt);
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            int x = 1;
            for (AppdefEntityID mem : list) {
                if (debug)
                    log.debug("Arg " + x + ": " + mem.getID());
                grpSqlStmt.append((x == 1 ? "" : ",")).append(mem.getID());
            }
            grpSqlStmt.append(")");
            StopWatch timer = new StopWatch();
            if (debug)
                log.debug("SQL: " + grpSqlStmt);
            rs = stmt.executeQuery(grpSqlStmt.toString());
            if (debug) {
                log.debug("getNavMapDataForGroup() executed in: " + timer);
            }
            while (rs.next()) {
                int thisEntityId = rs.getInt(1);
                String thisEntityName = rs.getString(2);
                entNameMap.put(new Integer(thisEntityId), thisEntityName);
            }
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
        }
    }

    // The methods in this class should call getDBConn() to obtain a connection,
    // because it also initializes the private database-related variables
    private Connection getDBConn() throws SQLException {
        Connection conn = Util.getConnection();

        if (DB_TYPE == -1) {
            DB_TYPE = DBUtil.getDBType(conn);
        }

        return conn;
    }

    private int getChildEntityType(int type) {
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return APPDEF_TYPE_SERVER;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return APPDEF_TYPE_SERVICE;
            default:
                return type;
        }
    }

    private String getAppdefTypeLabel(int typeId, String desc) {
        String typeLabel = AppdefEntityConstants.typeToString(typeId);
        if (desc == null) {
            desc = typeLabel;
        } else if (desc.toLowerCase().indexOf(typeLabel.toLowerCase()) == -1) {
            desc += " " + typeLabel;
        }
        return desc;
    }
}
