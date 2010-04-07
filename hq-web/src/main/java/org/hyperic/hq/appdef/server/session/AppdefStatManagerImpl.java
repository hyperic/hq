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
    private static final String TBL_GROUP = "EAM_RESOURCE_GROUP";
    private static final String TBL_PLATFORM = "EAM_PLATFORM";
    private static final String TBL_SERVICE = "EAM_SERVICE";
    private static final String TBL_SERVER = "EAM_SERVER";
    private static final String TBL_APP = "EAM_APPLICATION";
    private static final String TBL_RES = "EAM_RESOURCE";
    private static final String LOG_CTX = AppdefStatManagerImpl.class.getName();

    private final Log log = LogFactory.getLog(LOG_CTX);
    private static int DB_TYPE = -1;

    private static final String PLATFORM_RES_TYPE = AuthzConstants.platformResType;
    private static final String APPLICATION_RES_TYPE = AuthzConstants.applicationResType;
    private static final String SERVER_RES_TYPE = AuthzConstants.serverResType;
    private static final String SERVICE_RES_TYPE = AuthzConstants.serviceResType;
    private static final String GROUP_RES_TYPE = AuthzConstants.groupResType;
    private static final String PLATFORM_OP_VIEW_PLATFORM = AuthzConstants.platformOpViewPlatform;
    private static final String APPLICATION_OP_VIEW_APPLICATION = AuthzConstants.appOpViewApplication;
    private static final String SERVER_OP_VIEW_SERVER = AuthzConstants.serverOpViewServer;
    private static final String SERVICE_OP_VIEW_SERVICE = AuthzConstants.serviceOpViewService;
    private static final String GROUP_OP_VIEW_RESOURCE_GROUP = AuthzConstants.groupOpViewResourceGroup;

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

    @Autowired
    public AppdefStatManagerImpl(PermissionManager permissionManager, ApplicationManager applicationManager,
                                 PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager, ResourceGroupManager resourceGroupManager) {
        this.permissionManager = permissionManager;
        this.applicationManager = applicationManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    /**
     * <p>
     * Return map of platform counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject) {
        Map<String, Integer> platMap = new HashMap<String, Integer>();
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String sql = "SELECT PLATT.NAME, COUNT(PLAT.ID) " +
                         "FROM " +
                         TBL_PLATFORM +
                         "_TYPE PLATT, " +
                         TBL_PLATFORM +
                         " PLAT " +
                         "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("PLAT.ID", subjectId, PLATFORM_RES_TYPE,
                             PLATFORM_OP_VIEW_PLATFORM) + ") " + "GROUP BY PLATT.NAME ORDER BY PLATT.NAME";
            stmt = conn.createStatement();

            if (log.isDebugEnabled())
                log.debug(sql);

            int total = 0;
            rs = stmt.executeQuery(sql);

            String platTypeName = null;
            while (rs.next()) {
                platTypeName = rs.getString(1);
                total = rs.getInt(2);
                platMap.put(platTypeName, new Integer(total));
            }

        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Platforms by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
        }
        return platMap;
    }

    /**
     * <p>
     * Return platforms count.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public int getPlatformsCount(AuthzSubject subject) {
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();

            String sql = "SELECT COUNT(PLAT.ID) " +
                         "FROM " +
                         TBL_PLATFORM +
                         "_TYPE PLATT, " +
                         TBL_PLATFORM +
                         " PLAT " +
                         "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("PLAT.ID", subjectId, PLATFORM_RES_TYPE,
                             PLATFORM_OP_VIEW_PLATFORM) + ")";
            stmt = conn.createStatement();

            if (log.isDebugEnabled())
                log.debug(sql);

            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception counting Platforms: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
           
        }
        return 0;
    }

    /**
     * <p>
     * Return map of server counts.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public Map<String, Integer> getServerCountsByTypeMap(AuthzSubject subject) {
        Map<String, Integer> servMap = new HashMap<String, Integer>();
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String sql = "SELECT SERVT.NAME, COUNT(SERV.ID) " +
                         "FROM " +
                         TBL_SERVER +
                         "_TYPE SERVT, " +
                         TBL_SERVER +
                         " SERV " +
                         "WHERE SERV.SERVER_TYPE_ID = SERVT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("SERV.ID", subjectId, SERVER_RES_TYPE,
                             SERVER_OP_VIEW_SERVER) + ") " + "GROUP BY SERVT.NAME ORDER BY SERVT.NAME";
            stmt = conn.createStatement();

            int total = 0;
            rs = stmt.executeQuery(sql);

            String servTypeName = null;
            while (rs.next()) {
                servTypeName = rs.getString(1);
                total = rs.getInt(2);
                servMap.put(servTypeName, new Integer(total));
            }

        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
          
        }
        return servMap;
    }

    /**
     * <p>
     * Return servers count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public int getServersCount(AuthzSubject subject) {
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();
        try {
            Connection conn = getDBConn();
            String sql = "SELECT COUNT(SERV.ID) " +
                         "FROM " +
                         TBL_SERVER +
                         "_TYPE SERVT, " +
                         TBL_SERVER +
                         " SERV " +
                         "WHERE SERV.SERVER_TYPE_ID = SERVT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("SERV.ID", subjectId, SERVER_RES_TYPE,
                             SERVER_OP_VIEW_SERVER) + ") ";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
           
        }
        return 0;
    }

    /**
     * <p>
     * Return map of service counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public Map<String, Integer> getServiceCountsByTypeMap(AuthzSubject subject) {
        Map<String, Integer> svcMap = new HashMap<String, Integer>();
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String sql = "SELECT SVCT.NAME, COUNT(SVC.ID) " +
                         "FROM " +
                         TBL_SERVICE +
                         "_TYPE SVCT, " +
                         TBL_SERVICE +
                         " SVC " +
                         "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("SVC.ID", subjectId, SERVICE_RES_TYPE,
                             SERVICE_OP_VIEW_SERVICE) + ") " + "GROUP BY SVCT.NAME ORDER BY SVCT.NAME";
            stmt = conn.createStatement();

            int total = 0;
            rs = stmt.executeQuery(sql);

            String serviceTypeName = null;
            while (rs.next()) {
                serviceTypeName = rs.getString(1);
                total = rs.getInt(2);
                svcMap.put(serviceTypeName, new Integer(total));
            }

        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
           
        }
        return svcMap;
    }

    /**
     * <p>
     * Return services count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public int getServicesCount(AuthzSubject subject) {
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();
        try {
            Connection conn = getDBConn();
            String sql = "SELECT COUNT(SVC.ID) " +
                         "FROM " +
                         TBL_SERVICE +
                         "_TYPE SVCT, " +
                         TBL_SERVICE +
                         " SVC " +
                         "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("SVC.ID", subjectId, SERVICE_RES_TYPE,
                             SERVICE_OP_VIEW_SERVICE) + ") ";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
          
        }
        return 0;
    }

    /**
     * <p>
     * Return map of app counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public Map<String, Integer> getApplicationCountsByTypeMap(AuthzSubject subject) {
        Map<String, Integer> appMap = new HashMap<String, Integer>();
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String sql = "SELECT APPT.NAME, COUNT(APP.ID) " +
                         "FROM " +
                         TBL_APP +
                         "_TYPE APPT, " +
                         TBL_APP +
                         " APP " +
                         "WHERE APP.APPLICATION_TYPE_ID = APPT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("APP.ID", subjectId, APPLICATION_RES_TYPE,
                             APPLICATION_OP_VIEW_APPLICATION) + ") " + "GROUP BY APPT.NAME ORDER BY APPT.NAME";
            stmt = conn.createStatement();

            int total = 0;
            rs = stmt.executeQuery(sql);

            String appTypeName = null;
            while (rs.next()) {
                appTypeName = rs.getString(1);
                total = rs.getInt(2);
                appMap.put(appTypeName, new Integer(total));
            }

        } catch (SQLException e) {
            log.error("Caught SQL Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
            
        }
        return appMap;
    }

    /**
     * <p>
     * Return apps count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public int getApplicationsCount(AuthzSubject subject) {
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String sql = "SELECT COUNT(APP.ID) FROM " +
                         TBL_APP +
                         "_TYPE APPT, " +
                         TBL_APP +
                         " APP " +
                         "WHERE APP.APPLICATION_TYPE_ID = APPT.ID AND EXISTS (" +
                         permissionManager.getResourceTypeSQL("APP.ID", subjectId, APPLICATION_RES_TYPE,
                             APPLICATION_OP_VIEW_APPLICATION) + ") ";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
           
        }
        return 0;
    }

    /**
     * <p>
     * Return map of grp counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public Map<Integer, Integer> getGroupCountsMap(AuthzSubject subject) {
        Map<Integer, Integer> grpMap = new HashMap<Integer, Integer>();
        Statement stmt = null;
        ResultSet rs = null;
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();

            for (int x = 0; x < groupTypes.length; x++) {
                String sql = "SELECT COUNT(*) FROM " +
                             TBL_GROUP +
                             " GRP " +
                             "WHERE GRP.GROUPTYPE = " +
                             groupTypes[x] +
                             " AND EXISTS (" +
                             permissionManager.getResourceTypeSQL("GRP.ID", subjectId, GROUP_RES_TYPE,
                                 GROUP_OP_VIEW_RESOURCE_GROUP) + ")";

                try {
                    int total = 0;
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);

                    if (rs.next()) {
                        total = rs.getInt(1);
                        grpMap.put(new Integer(groupTypes[x]), new Integer(total));
                    }
                } finally {
                    DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
                }
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception finding groups by type: " + e, e);
            throw new SystemException(e);
        } 
        return grpMap;
    }

    /**
     * Method for determining whether or not to show a nav map (this is a
     * temporary method)
     * 
     */
    public boolean isNavMapSupported() {
        try {
            Connection conn = getDBConn();
            switch (DBUtil.getDBType(conn)) {
                case DBUtil.DATABASE_ORACLE_8:
                case DBUtil.DATABASE_ORACLE_9:
                case DBUtil.DATABASE_ORACLE_10:
                case DBUtil.DATABASE_POSTGRESQL_7:
                case DBUtil.DATABASE_POSTGRESQL_8:
                case DBUtil.DATABASE_MYSQL5:
                    return true;
                default:
                    return false;
            }
        } catch (SQLException e) {
            log.error("Unable to determine navmap capability");
            return false;
        } 
    }

    /**
     * <p>
     * Return directly connected resource tree for node level platform
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, Integer platformId)
        throws PlatformNotFoundException, PermissionException {
        try {
            Platform plat = platformManager.findPlatformById(platformId);
            ResourceTreeNode[] retVal;
            retVal = getNavMapDataForPlatform(subject, plat);
            return retVal;
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, Platform plat)
        throws PermissionException, SQLException {
        ResourceTreeNode[] retVal = null;
        Statement stmt = null;
        ResultSet rs = null;
        StringBuffer buf;
        Integer subjectId = subject.getId();

        try {
            Connection conn = getDBConn();
            String falseStr = DBUtil.getBooleanValue(false, conn);
            buf = new StringBuffer();
            buf.append("SELECT svr_svrt_svc_svct.server_id, ").append("svr_svrt_svc_svct.server_name, ").append(
                "       svr_svrt_svc_svct.server_type_id, ").append("svr_svrt_svc_svct.server_type_name, ").append(
                "       svr_svrt_svc_svct.service_id, ").append("svr_svrt_svc_svct.service_name, ").append(
                "       svr_svrt_svc_svct.service_type_id, ").append("svr_svrt_svc_svct.service_type_name ").append(
                "FROM (SELECT app.id as application_id, ").append("appsvc.service_id as service_id ").append(
                "      FROM EAM_APP_SERVICE appsvc ");
            if (isOracle8()) {
                buf.append(", ").append(TBL_APP).append(" app ").append(
                    "WHERE app.id=appsvc.application_id(+) AND EXISTS (").append(
                    permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ");
            } else {
                buf.append("RIGHT JOIN ").append(TBL_APP).append(" app ON app.id=appsvc.application_id ").append(
                    "WHERE EXISTS (").append(
                    permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ");
            }
            buf.append("(SELECT svr_svrt.server_id, ").append("svr_svrt.server_name, ").append(
                "        svr_svrt.server_type_id, ").append("svr_svrt.server_type_name, ").append(
                "        svc_svct.service_id, ").append("svc_svct.service_name, ").append(
                "        svc_svct.service_type_id, ").append("svc_svct.service_type_name ").append(
                " FROM ( SELECT svc.id as service_id, ").append("               res2.name  as service_name, ").append(
                "               svct.id   as service_type_id, ").append(
                "               svct.name as service_type_name,").append("               svc.server_id as server_id ")
                .append("          FROM ").append(TBL_SERVICE).append("_TYPE svct, ").append(TBL_SERVICE).append(
                    " svc ").append(" JOIN " + TBL_RES).append(" res2 ON svc.resource_id = res2.id ").append(
                    "         WHERE svc.service_type_id=svct.id ").append("           AND EXISTS (").append(
                    permissionManager
                        .getResourceTypeSQL("svc.id", subjectId, SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE)).append(
                    ") ) svc_svct ");
            if (isOracle8()) {
                buf.append(",");
            } else {
                buf.append("     RIGHT JOIN");
            }
            buf.append("       ( SELECT svr.id    as server_id, ").append("                res1.name as server_name, ")
                .append("                svrt.id   as server_type_id,").append(
                    "                svrt.name as server_type_name ").append("         FROM ").append(TBL_SERVER)
                .append("_TYPE svrt, ").append(TBL_SERVER).append(" svr ").append(" JOIN " + TBL_RES).append(
                    " res1 ON svr.resource_id = res1.id ").append("         WHERE  svr.platform_id=")
                .append(plat.getId())
                // exclude virtual server types from the navMap
                .append("                    AND svrt.fvirtual = " + falseStr).append(
                    "                    AND svrt.id=svr.server_type_id ").append("                    AND EXISTS (")
                .append(
                    permissionManager.getResourceTypeSQL("svr.id", subjectId, SERVER_RES_TYPE, SERVER_OP_VIEW_SERVER))
                .append(") ) svr_svrt ");
            if (isOracle8()) {
                buf.append(" WHERE svr_svrt.server_id=svc_svct.server_id(+)").append("  ) svr_svrt_svc_svct ").append(
                    "WHERE svr_svrt_svc_svct.service_id=app_appsvc.service_id(+)");
            } else {
                buf.append("   ON svr_svrt.server_id=svc_svct.server_id ").append("  ) svr_svrt_svc_svct ").append(
                    "ON svr_svrt_svc_svct.service_id=app_appsvc.service_id ");
            }
            buf.append(" ORDER BY svr_svrt_svc_svct.server_id, ").append("svr_svrt_svc_svct.server_type_id, ").append(
                "          svr_svrt_svc_svct.service_id, ").append("svr_svrt_svc_svct.service_type_id ");

            if (log.isDebugEnabled())
                log.debug(buf.toString());

            Set<ResourceTreeNode> servers = new HashSet<ResourceTreeNode>();
            Set<ResourceTreeNode> services = new HashSet<ResourceTreeNode>();

            ResourceTreeNode aPlatformNode = new ResourceTreeNode(plat.getName(), getAppdefTypeLabel(
                APPDEF_TYPE_PLATFORM, plat.getAppdefResourceType().getName()), plat.getEntityId(),
                ResourceTreeNode.RESOURCE);

            int thisSvrId = 0;
            String thisServerName = null;
            int thisServerTypeId = 0;
            String thisServerTypeName = null;
            int thisSvcId = 0;
            String thisServiceName = null;
            int thisServiceTypeId = 0;
            String thisServiceTypeName = null;

            stmt = conn.createStatement();
            rs = stmt.executeQuery(buf.toString());

            while (rs.next()) {
                thisSvrId = rs.getInt(1);
                thisServerName = rs.getString(2);
                thisServerTypeId = rs.getInt(3);
                thisServerTypeName = rs.getString(4);
                thisSvcId = rs.getInt(5);
                thisServiceName = rs.getString(6);
                thisServiceTypeId = rs.getInt(7);
                thisServiceTypeName = rs.getString(8);

                if (thisServerTypeName != null) {
                    servers.add(new ResourceTreeNode(thisServerName, getAppdefTypeLabel(APPDEF_TYPE_SERVER,
                        thisServerTypeName), AppdefEntityID.newServerID(new Integer(thisSvrId)), plat.getEntityId(),
                        thisServerTypeId));
                }

                if (thisServiceTypeName != null) {
                    services.add(new ResourceTreeNode(thisServiceName, getAppdefTypeLabel(APPDEF_TYPE_SERVICE,
                        thisServiceTypeName), AppdefEntityID.newServiceID(new Integer(thisSvcId)), AppdefEntityID
                        .newServerID(new Integer(thisSvrId)), thisServiceTypeId));
                }
            }
            // XXX Leave out service data No current way to represent it
            // (ResourceTreeNode[]) serviceMap.values()
            // .toArray(new ResourceTreeNode[0]);
            aPlatformNode.setSelected(true);
            ResourceTreeNode[] svrNodes = (ResourceTreeNode[]) servers.toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(svrNodes, true);
            aPlatformNode.addUpChildren(svrNodes);

            retVal = new ResourceTreeNode[] { aPlatformNode };
        } catch (SQLException e) {
            throw e;
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
           
        }
        if (log.isDebugEnabled()) {
            log.debug(mapToString(retVal));
        }
        return retVal;
    }

    private boolean isOracle8() {
        return DB_TYPE == DBUtil.DATABASE_ORACLE_8;
    }

    private boolean isOracle() {
        return isOracle8() || DB_TYPE == DBUtil.DATABASE_ORACLE_9;
    }

    /**
     * <p>
     * Return directly connected resource tree for node level server
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject, Integer serverId)
        throws ServerNotFoundException, PermissionException {
        Server server = serverManager.findServerById(serverId);

        ResourceTreeNode[] retVal = null;
        Statement stmt = null;
        ResultSet rs = null;
        StringBuffer buf;
        Integer subjectId = subject.getId();
        try {
            Connection conn = getDBConn();

            buf = new StringBuffer();
            buf.append("SELECT svc_svct_svr_plat.platform_id, ").append("svc_svct_svr_plat.platform_name, ").append(
                "       svc_svct_svr_plat.platform_type_id, ").append("svc_svct_svr_plat.platform_type_name, ").append(
                "       svc_svct_svr_plat.service_id, ").append("svc_svct_svr_plat.service_name, ").append(
                "       svc_svct_svr_plat.service_type_id, ").append("svc_svct_svr_plat.service_type_name ").append(
                "FROM (SELECT app.id as application_id, ").append("appsvc.service_id as service_id ").append(
                "        FROM EAM_APP_SERVICE appsvc ");
            if (isOracle()) {
                buf.append(" , ").append(TBL_APP).append(" app ").append(
                    "WHERE app.id=appsvc.application_id(+) AND EXISTS (").append(
                    permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ");
            } else {
                buf.append("  RIGHT JOIN ").append(TBL_APP).append(" app ON app.id=appsvc.application_id ").append(
                    " WHERE EXISTS (").append(
                    permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ");
            }
            buf.append(" (SELECT svc_svct.service_id, ").append("svc_svct.service_name, ").append(
                "         svc_svct.service_type_id, ").append("svc_svct.service_type_name, ").append(
                "         plat.id as platform_id, ").append("res0.name as platform_name, ").append(
                "         platt.id as platform_type_id, ").append("platt.name as platform_type_name ").append(
                "  FROM (SELECT svc.id    as service_id, ").append("               res2.name  as service_name, ")
                .append("               svct.id   as service_type_id,").append(
                    "               svct.name as service_type_name,").append(
                    "               svc.server_id as server_id ").append("        FROM ").append(TBL_SERVICE).append(
                    "_TYPE svct, ").append(TBL_SERVICE).append(" svc ").append(" JOIN " + TBL_RES).append(
                    " res2 ON svc.resource_id = res2.id ").append(
                    "        WHERE svc.service_type_id=svct.id AND EXISTS (").append(
                    permissionManager
                        .getResourceTypeSQL("svc.id", subjectId, SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE)).append(
                    ") ) svc_svct ");
            if (isOracle()) {
                buf.append(" ," + TBL_SERVER + " svr, ");
            } else {
                buf.append(" RIGHT JOIN " + TBL_SERVER + " svr ").append("ON svc_svct.server_id=svr.id, ");
            }
            buf.append(TBL_PLATFORM).append("_TYPE platt, ").append(TBL_PLATFORM).append(" plat JOIN ").append(TBL_RES)
                .append(" res0 ON plat.resource_id = res0.id").append(" WHERE svr.id=").append(server.getId()).append(
                    "   AND platt.id=plat.platform_type_id ").append("   AND plat.id=svr.platform_id AND EXISTS (")
                .append(
                    permissionManager.getResourceTypeSQL("plat.id", subjectId, PLATFORM_RES_TYPE,
                        PLATFORM_OP_VIEW_PLATFORM)).append(") ");

            if (isOracle()) {
                buf.append(" AND svr.id=svc_svct.server_id(+) ").append("       ) svc_svct_svr_plat ").append(
                    " WHERE svc_svct_svr_plat.service_id=app_appsvc.service_id(+)");
            } else {
                buf.append(" ) svc_svct_svr_plat ").append(" ON svc_svct_svr_plat.service_id=app_appsvc.service_id ");
            }
            buf.append("order by service_type_id ");

            stmt = conn.createStatement();
            StopWatch timer = new StopWatch();

            rs = stmt.executeQuery(buf.toString());

            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForServer() executed in: " + timer);
                log.debug("SQL: " + buf);
            }

            Map<Integer, ResourceTreeNode> serviceMap = new HashMap<Integer, ResourceTreeNode>();
            ResourceTreeNode aServerNode;
            ResourceTreeNode aPlatformNode;

            aPlatformNode = null;
            aServerNode = new ResourceTreeNode(server.getName(), getAppdefTypeLabel(server.getEntityId().getType(),
                server.getAppdefResourceType().getName()), server.getEntityId(), ResourceTreeNode.RESOURCE);

            int thisPlatId = 0;
            String thisPlatformName = null;
            int thisPlatformTypeId = 0;
            String thisPlatformTypeName = null;
            int thisSvcId = 0;
            String thisServiceName = null;
            int thisServiceTypeId = 0;
            String thisServiceTypeName = null;

            while (rs.next()) {

                if (thisPlatId == 0) {
                    thisPlatId = rs.getInt(1);
                    thisPlatformName = rs.getString(2);
                    thisPlatformTypeId = rs.getInt(3);
                    thisPlatformTypeName = rs.getString(4);
                    aPlatformNode = new ResourceTreeNode(thisPlatformName, getAppdefTypeLabel(APPDEF_TYPE_PLATFORM,
                        thisPlatformTypeName), AppdefEntityID.newPlatformID(new Integer(thisPlatId)),
                        (AppdefEntityID) null, thisPlatformTypeId);
                }

                thisSvcId = rs.getInt(5);
                thisServiceName = rs.getString(6);
                thisServiceTypeId = rs.getInt(7);
                thisServiceTypeName = rs.getString(8);

                if (thisServiceName != null) {
                    serviceMap.put(new Integer(thisSvcId), new ResourceTreeNode(thisServiceName, getAppdefTypeLabel(
                        APPDEF_TYPE_SERVICE, thisServiceTypeName), AppdefEntityID.newServiceID(new Integer(thisSvcId)),
                        server.getEntityId(), thisServiceTypeId));
                }
            }

            aServerNode.setSelected(true);
            ResourceTreeNode[] services = (ResourceTreeNode[]) serviceMap.values().toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(services, true);
            aServerNode.addUpChildren(services);
            // aPlatformNode can be null if user is unauthz
            if (aPlatformNode != null) {
                aServerNode.addDownChild(aPlatformNode);
            }
            retVal = new ResourceTreeNode[] { aServerNode };

        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
            
        }
        return retVal;
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject, Integer serviceId)
        throws ServiceNotFoundException, PermissionException {
        Service service = serviceManager.findServiceById(serviceId);

        ResourceTreeNode[] retVal = null;
        Statement stmt = null;
        ResultSet rs = null;
        StringBuffer buf;
        Integer subjectId = subject.getId();
        try {
            Connection conn = getDBConn();

            String trueStr = DBUtil.getBooleanValue(true, conn);
            buf = new StringBuffer();
            buf.append("SELECT plat.platform_id, ").append("platform_name, ").append("       platform_type_name, ")
                .append("asvc_svr.server_id, ").append("       asvc_svr.server_name, ").append(
                    "asvc_svr.server_type_name, ").append("       asvc_svr.application_id, ").append(
                    "asvc_svr.application_name, ").append("       asvc_svr.application_type_name, ")
                .append("fvirtual ").append(
                    "FROM (SELECT plat.id as platform_id, " + "res0.name as platform_name, " +
                        "platt.name as platform_type_name " + "FROM " + TBL_PLATFORM + "_TYPE platt, " + TBL_PLATFORM +
                        " plat JOIN " + TBL_RES + " res0 ON plat.resource_id = res0.id " +
                        "WHERE plat.platform_type_id=platt.id AND " + " EXISTS (").append(
                    permissionManager.getResourceTypeSQL("plat.id", subjectId, PLATFORM_RES_TYPE,
                        PLATFORM_OP_VIEW_PLATFORM)).append(")) plat ");

            if (isOracle8()) {
                buf.append(", ");
            } else {
                buf.append("RIGHT JOIN ");
            }
            buf.append("( SELECT asvc.application_id, ").append("asvc.application_name, ").append(
                "         asvc.application_type_name, ").append("svr.id as server_id, ").append(
                "         res1.name as server_name, ").append("         svrt.name as server_type_name, ").append(
                "         svr.platform_id, fvirtual ").append(" FROM ").append(TBL_RES).append(" res1 JOIN ").append(
                TBL_SERVER).append(" svr ON res1.id = svr.resource_id ");
            if (isOracle8()) {
                buf.append(" , ");
            } else {
                buf.append(" RIGHT JOIN ");
            }
            buf.append(" (SELECT app_appsvc.application_id, ").append("app_appsvc.application_name, ").append(
                "         app_appsvc.application_type_name, ").append("svc.server_id as server_id ").append(
                "    FROM (SELECT app.id as application_id, ").append("r.name as application_name, ").append(
                "                 EAM_APPLICATION_TYPE.name as application_type_name, ").append(
                "                 appsvc.service_id as service_id ").append("          FROM EAM_APP_SERVICE appsvc ");
            if (isOracle8()) {
                buf.append(" , ").append(TBL_APP).append(" app, EAM_APPLICATION_TYPE, ").append(TBL_RES).append(" r ")
                    .append(" WHERE app.id=appsvc.application_id(+) ").append(
                        "   AND EAM_APPLICATION_TYPE.id=app.application_type_id ").append(
                        "   AND app.resource_id = r.id AND EXISTS (").append(
                        permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                            APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ").append(TBL_SERVICE).append(
                        " svc WHERE svc.id=app_appsvc.service_id(+) AND svc.id=").append(service.getId()).append(
                        ") asvc ");
            } else {
                buf.append(" RIGHT JOIN ").append(TBL_APP).append(" app ON app.id=appsvc.application_id ").append(
                    " RIGHT JOIN ").append(TBL_RES).append(" r ON app.resource_id = r.id, ").append(
                    " EAM_APPLICATION_TYPE  ").append(" WHERE EAM_APPLICATION_TYPE.id=app.application_type_id ")
                    .append("   AND EXISTS (").append(
                        permissionManager.getResourceTypeSQL("app.id", subjectId, APPLICATION_RES_TYPE,
                            APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ").append(TBL_SERVICE)
                    .append(" svc ON svc.id=app_appsvc.service_id ").append(" WHERE svc.id=").append(service.getId())
                    .append(") asvc ");
            }
            if (isOracle8()) {
                buf.append(" , ").append(TBL_SERVER).append("_TYPE svrt ").append(" WHERE svr.server_type_id=svrt.id ")
                    .append("   AND asvc.server_id=svr.id(+) ").append("   AND (fvirtual = ").append(trueStr).append(
                        "    OR EXISTS (").append(
                        permissionManager.getResourceTypeSQL("svr.id", subjectId, SERVER_RES_TYPE,
                            SERVER_OP_VIEW_SERVER)).append(")) ) asvc_svr, ").append(TBL_PLATFORM + "_TYPE platt ")
                    .append("WHERE plat.platform_type_id=platt.id ").append(
                        "  AND asvc_svr.platform_id=plat.id(+) AND EXISTS (").append(
                        permissionManager.getResourceTypeSQL("plat.id", subjectId, PLATFORM_RES_TYPE,
                            PLATFORM_OP_VIEW_PLATFORM)).append(") ");
            } else {
                buf.append(" ON asvc.server_id=svr.id, ").append(TBL_SERVER).append("_TYPE svrt ").append(
                    " WHERE svr.server_type_id=svrt.id ").append("   AND (fvirtual = ").append(trueStr).append(
                    "    OR EXISTS (").append(
                    permissionManager.getResourceTypeSQL("svr.id", subjectId, SERVER_RES_TYPE, SERVER_OP_VIEW_SERVER))
                    .append(")) ) asvc_svr ").append("     ON asvc_svr.platform_id = plat.platform_id");
            }

            stmt = conn.createStatement();
            StopWatch timer = new StopWatch();

            rs = stmt.executeQuery(buf.toString());

            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForService() executed in: " + timer);
                log.debug("SQL: " + buf);
            }

            ResourceTreeNode aPlatformNode = null;
            ResourceTreeNode aServerNode = null;
            ResourceTreeNode aServiceNode = null;
            Map<Integer, ResourceTreeNode> appMap = new HashMap<Integer, ResourceTreeNode>();

            aServiceNode = new ResourceTreeNode(service.getName(), getAppdefTypeLabel(service.getEntityId().getType(),
                service.getAppdefResourceType().getName()), service.getEntityId(), ResourceTreeNode.RESOURCE);

            while (rs.next()) {

                int i = 1;
                int thisPlatId = rs.getInt(i++);
                String thisPlatformName = rs.getString(i++);
                String thisPlatformTypeName = rs.getString(i++);
                int thisSvrId = rs.getInt(i++);
                String thisServerName = rs.getString(i++);
                String thisServerTypeName = rs.getString(i++);
                int thisAppId = rs.getInt(i++);
                String thisApplicationName = rs.getString(i++);
                String thisApplicationDesc = rs.getString(i++);
                String virtualServer = rs.getString(i++);

                if (thisPlatformName != null) {
                    aPlatformNode = new ResourceTreeNode(thisPlatformName, getAppdefTypeLabel(APPDEF_TYPE_PLATFORM,
                        thisPlatformTypeName), AppdefEntityID.newPlatformID(new Integer(thisPlatId)),
                        ResourceTreeNode.RESOURCE);
                }

                if (thisServerName != null && !trueStr.startsWith(virtualServer)) {
                    aServerNode = new ResourceTreeNode(thisServerName, getAppdefTypeLabel(APPDEF_TYPE_SERVER,
                        thisServerTypeName), AppdefEntityID.newServerID(new Integer(thisSvrId)),
                        ResourceTreeNode.RESOURCE);
                }

                if (thisApplicationName != null) {
                    appMap.put(new Integer(thisAppId), new ResourceTreeNode(thisApplicationName, getAppdefTypeLabel(
                        AppdefEntityConstants.APPDEF_TYPE_APPLICATION, thisApplicationDesc), AppdefEntityID
                        .newAppID(new Integer(thisAppId)), ResourceTreeNode.RESOURCE));
                }
            }
            aServiceNode.setSelected(true);

            // server nodes and platform nodes can be null if user is unauthz
            if (aServerNode != null) {
                if (aPlatformNode != null) {
                    aServerNode.addDownChild(aPlatformNode);
                }
                aServiceNode.addDownChild(aServerNode);
            } else if (aPlatformNode != null) {
                aServiceNode.addDownChild(aPlatformNode);
            }

            ResourceTreeNode[] appNodes = (ResourceTreeNode[]) appMap.values().toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(appNodes, true);
            aServiceNode.addUpChildren(appNodes);

            retVal = new ResourceTreeNode[] { aServiceNode };

        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
            
        }
        return retVal;
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

    private final String getPermGroupSQL(Integer subjectId) {
        StringBuffer rtn = new StringBuffer().append("SELECT grp.id as group_id, res.name, cluster_id ").append(
            " FROM ").append(TBL_GROUP).append(" grp, ").append(TBL_RES).append(" res ").append(
            " WHERE grp.resource_id = res.id AND EXISTS (").append(
            permissionManager.getResourceTypeSQL("grp.id", subjectId, GROUP_RES_TYPE, GROUP_OP_VIEW_RESOURCE_GROUP))
            .append(")");
        return rtn.toString();
    }

    private final String getPermServiceSQL(Integer subjectId) {
        StringBuffer rtn = new StringBuffer().append("SELECT svc.id as service_id, res.name as service_name,").append(
            " server_id").append(" FROM  " + TBL_SERVICE + " svc JOIN ").append(TBL_RES).append(
            " res ON resource_id = svc.id ").append(" WHERE EXISTS (").append(
            permissionManager.getResourceTypeSQL("svc.id", subjectId, SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE))
            .append(")");
        return rtn.toString();
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject, Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        Application app = applicationManager.findApplicationById(subject, appId);

        ResourceTreeNode[] retVal = null;
        Statement stmt = null;
        ResultSet rs = null;
        Integer subjectId = subject.getId();
        try {
            Connection conn = getDBConn();

            StringBuffer buf = new StringBuffer().append("SELECT appsvc.service_id, pm.name,").append(
                " appsvc.service_type_id,").append(" svct.name as service_type_name,").append(
                " appsvc.application_id, appsvc.group_id").append(" FROM EAM_APP_SERVICE appsvc, ").append(TBL_SERVICE)
                .append("_TYPE svct, ").append(TBL_GROUP).append(" grp, (").append(getPermGroupSQL(subjectId)).append(
                    ") pm").append(" WHERE svct.id = appsvc.service_type_id AND ").append(
                    " grp.id = appsvc.group_id AND pm.group_id = grp.id").append(" AND appsvc.application_id = ")
                .append(app.getId()).append(" UNION ALL ").append("SELECT appsvc.service_id, res2.name,").append(
                    " appsvc.service_type_id,").append(" svct.name as service_type_name,").append(
                    " appsvc.application_id, appsvc.group_id").append(" FROM EAM_APP_SERVICE appsvc, ").append(
                    TBL_SERVICE).append("_TYPE svct, (").append(getPermServiceSQL(subjectId)).append(") pm, ").append(
                    TBL_SERVICE).append(" svc JOIN ").append(TBL_RES).append(" res2 ON svc.resource_id = res2.id ")
                .append(" WHERE svct.id = appsvc.service_type_id AND ").append(" svc.id = appsvc.service_id AND ")
                .append(" pm.service_id = svc.id AND ").append(" appsvc.application_id = ").append(app.getId()).append(
                    " ORDER BY service_type_id, service_id");

            if (log.isDebugEnabled()) {
                log.debug(buf.toString());
            }
            stmt = conn.createStatement();

            StopWatch timer = new StopWatch();

            rs = stmt.executeQuery(buf.toString());

            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForApplication() executed in: " + timer);
                log.debug("SQL: " + buf);
            }

            Map<String, ResourceTreeNode> svcMap = new HashMap<String, ResourceTreeNode>();

            ResourceTreeNode appNode = new ResourceTreeNode(app.getName(), getAppdefTypeLabel(app.getEntityId()
                .getType(), app.getAppdefResourceType().getName()), app.getEntityId(), ResourceTreeNode.RESOURCE);

            int svc_id_col = rs.findColumn("service_id"), name_col = rs.findColumn("name"), service_type_col = rs
                .findColumn("service_type_id"), type_name_col = rs.findColumn("service_type_name"), group_id_col = rs
                .findColumn("group_id");

            while (rs.next()) {
                int serviceId = rs.getInt(svc_id_col);
                String serviceName = rs.getString(name_col);
                int serviceTypeId = rs.getInt(service_type_col);
                String serviceTypeName = rs.getString(type_name_col);
                int groupId = rs.getInt(group_id_col);
                String thisGroupName = rs.getString(name_col);
                // means that column is null, hence row is not a group
                if (groupId == 0) {
                    thisGroupName = null;
                } else {
                    serviceName = null;
                }

                if (thisGroupName != null) {
                    String key = APPDEF_TYPE_GROUP + "-" + groupId;
                    svcMap.put(key, new ResourceTreeNode(thisGroupName, getAppdefTypeLabel(APPDEF_TYPE_GROUP,
                        serviceTypeName), AppdefEntityID.newGroupID(new Integer(groupId)), ResourceTreeNode.CLUSTER));
                } else if (serviceName != null) {
                    String key = APPDEF_TYPE_SERVICE + "-" + serviceId;
                    svcMap.put(key, new ResourceTreeNode(serviceName, getAppdefTypeLabel(APPDEF_TYPE_SERVICE,
                        serviceTypeName), AppdefEntityID.newServiceID(new Integer(serviceId)), app.getEntityId(),
                        serviceTypeId));
                }
            }

            appNode.setSelected(true);
            ResourceTreeNode[] svcNodes = (ResourceTreeNode[]) svcMap.values().toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(svcNodes);
            appNode.addDownChildren(svcNodes);

            retVal = new ResourceTreeNode[] { appNode };

        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
          
        }
        return retVal;
    }

    /**
     * <p>
     * Return resources for autogroups
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject, AppdefEntityID[] parents, Integer resType)
        throws AppdefEntityNotFoundException, PermissionException {
        try {
            // platform auto-groups do not have parent resource types
            int entType = (parents != null) ? getChildEntityType(parents[0].getType()) : APPDEF_TYPE_PLATFORM;

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

    private ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject, AppdefEntityID[] parents,
                                                         AppdefResourceType type) throws AppdefEntityNotFoundException,
        PermissionException, SQLException {
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
        cEntityType = (pEntityType != APPDEF_TYPE_UNDEFINED) ? getChildEntityType(pEntityType) : APPDEF_TYPE_PLATFORM;

        try {

            // If the auto-group has parents, fetch the resources
            if (parents != null) {
                parentNodes = new ArrayList<ResourceTreeNode>(parents.length);
                for (int x = 0; x < parents.length; x++) {
                    AppdefEntityValue av = new AppdefEntityValue(parents[x], subject);
                    parentNodes.add(new ResourceTreeNode(av.getName(),
                        getAppdefTypeLabel(pEntityType, av.getTypeName()), parents[x], ResourceTreeNode.RESOURCE));
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
                                     permissionManager.getResourceTypeSQL("p.id", subjectId, PLATFORM_RES_TYPE,
                                         PLATFORM_OP_VIEW_PLATFORM) + ") ";

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
                                    permissionManager.getResourceTypeSQL("s.id", subjectId, SERVER_RES_TYPE,
                                        SERVER_OP_VIEW_SERVER) + ") ";

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
                                    permissionManager.getResourceTypeSQL("s.id", subjectId, SERVICE_RES_TYPE,
                                        SERVICE_OP_VIEW_SERVICE) + ") ";

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
                                       permissionManager.getResourceTypeSQL("s.id", subjectId, SERVICE_RES_TYPE,
                                           SERVICE_OP_VIEW_SERVICE) + ") ";

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
                    throw new IllegalArgumentException("No auto-group support " + "for specified type");
            }

            if (log.isDebugEnabled())
                log.debug(sqlStmt);

            ResourceTreeNode agNode = new ResourceTreeNode(type.getName(), getAppdefTypeLabel(cEntityType, type
                .getName()), parents, type.getId().intValue(), ResourceTreeNode.AUTO_GROUP);
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

                    entitySet.add(new ResourceTreeNode(thisEntityName, getAppdefTypeLabel(cEntityType,
                        thisEntityTypeName), new AppdefEntityID(cEntityType, thisEntityId), ResourceTreeNode.RESOURCE));
                }

                agNode.setSelected(true);
                if (parentNodes != null) {
                    ResourceTreeNode[] parNodeArr = (ResourceTreeNode[]) parentNodes.toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(parNodeArr, true);
                    agNode.addUpChildren(parNodeArr);
                }

                ResourceTreeNode[] members = (ResourceTreeNode[]) entitySet.toArray(new ResourceTreeNode[0]);

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
    @Transactional(propagation = Propagation.SUPPORTS, readOnly=true)
    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, Integer groupId) throws PermissionException {

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
        ResourceTreeNode grpNode = new ResourceTreeNode(groupVo.getName(), getAppdefTypeLabel(APPDEF_TYPE_GROUP,
            groupVo.getAppdefResourceTypeValue().getName()), groupVo.getEntityId(), ResourceTreeNode.CLUSTER);
        final List<AppdefEntityID> agEntries = groupVo.getAppdefGroupEntries();
        if (agEntries.size() == 0) {
            return new ResourceTreeNode[] { grpNode };
        }
        ResourceTreeNode[] retVal = null;
        final StringBuilder grpSqlStmt = new StringBuilder();
        final boolean debug = log.isDebugEnabled();

        int entityType = groupVo.getGroupEntType();

        final Connection conn = getDBConn();

        final String resJoin = new StringBuilder().append(" JOIN ")
                .append(TBL_RES)
                .append(" res on resource_id = res.id ")
                .toString();

        switch (entityType) {
                case APPDEF_TYPE_PLATFORM:
                    grpSqlStmt.append("SELECT p.id as platform_id, res.name as platform_name ").append(" FROM ")
                        .append(TBL_PLATFORM).append(" p ").append(resJoin).append("WHERE p.id IN (");
                    break;
                case APPDEF_TYPE_SERVER:
                    grpSqlStmt.append("SELECT s.id as server_id, res.name as server_name ").append("FROM ").append(
                        TBL_SERVER).append(" s ").append(resJoin).append("WHERE s.id IN (");
                    break;
                case APPDEF_TYPE_SERVICE:
                    grpSqlStmt.append("SELECT s.id as service_id, res.name as service_name ").append("FROM ").append(
                        TBL_SERVICE).append(" s  ").append(resJoin).append("WHERE s.id IN (");
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
                for (int ii=0; ii<agEntries.size(); ii+=batchSize) {
                    int end = Math.min(ii+batchSize, agEntries.size());
                    List<AppdefEntityID> list = agEntries.subList(ii, end);
                    setEntNameMap(entNameMap, list, conn, grpSqlStmt);
                }

                // Let group member order drive node creation (not db order).
                for (AppdefEntityID id : groupVo.getAppdefGroupEntries()) {
                    entitySet.add(new ResourceTreeNode((String) entNameMap.get(id.getId()), getAppdefTypeLabel(id
                        .getType(), groupVo.getAppdefResourceTypeValue().getName()), new AppdefEntityID(entityType, id
                        .getId()), ResourceTreeNode.RESOURCE));
                }
        }

        ResourceTreeNode[] memberNodes = entitySet.toArray(new ResourceTreeNode[0]);

        grpNode.setSelected(true);
        ResourceTreeNode.alphaSortNodes(memberNodes);
        grpNode.addDownChildren(memberNodes);

        retVal = new ResourceTreeNode[] { grpNode };

       
        return retVal;
    }
    
    private void setEntNameMap(Map<Integer,String> entNameMap, List<AppdefEntityID> list, Connection conn, StringBuilder grpSqlStmt) throws SQLException {
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
             int x=1;
             for (AppdefEntityID mem : list ) {
                 if (debug) log.debug("Arg " + x + ": " +  mem.getID());
                 grpSqlStmt.append((x==1 ? "" : ",")).append(mem.getID());
             }
             grpSqlStmt.append(")");
             StopWatch timer = new StopWatch();
             if (debug) log.debug("SQL: " + grpSqlStmt);
             rs = stmt.executeQuery(grpSqlStmt.toString());
             if (debug) {
                 log.debug("getNavMapDataForGroup() executed in: " + timer);
             }
            while (rs.next()) {
                int    thisEntityId   = rs.getInt(1);
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
