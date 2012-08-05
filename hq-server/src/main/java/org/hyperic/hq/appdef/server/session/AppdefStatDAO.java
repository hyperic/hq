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

package org.hyperic.hq.appdef.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class AppdefStatDAO {

    private static final String TBL_GROUP = "EAM_RESOURCE_GROUP";
    private static final String TBL_PLATFORM = "EAM_PLATFORM";
    private static final String TBL_SERVICE = "EAM_SERVICE";
    private static final String TBL_SERVER = "EAM_SERVER";
    private static final String TBL_APP = "EAM_APPLICATION";
    private static final String TBL_RES = "EAM_RESOURCE";
    private static final String PLATFORM_RES_TYPE = AuthzConstants.platformResType;
    private static final String PLATFORM_OP_VIEW_PLATFORM = AuthzConstants.platformOpViewPlatform;
    private static final String SERVER_RES_TYPE = AuthzConstants.serverResType;
    private static final String SERVER_OP_VIEW_SERVER = AuthzConstants.serverOpViewServer;
    private static final String SERVICE_RES_TYPE = AuthzConstants.serviceResType;
    private static final String SERVICE_OP_VIEW_SERVICE = AuthzConstants.serviceOpViewService;
    private static final String APPLICATION_RES_TYPE = AuthzConstants.applicationResType;
    private static final String APPLICATION_OP_VIEW_APPLICATION = AuthzConstants.appOpViewApplication;
    private static final String GROUP_RES_TYPE = AuthzConstants.groupResType;
    private static final String GROUP_OP_VIEW_RESOURCE_GROUP = AuthzConstants.groupOpViewResourceGroup;
    private static final int APPDEF_TYPE_SERVER = AppdefEntityConstants.APPDEF_TYPE_SERVER;
    private static final int APPDEF_TYPE_SERVICE = AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    private static final int APPDEF_TYPE_PLATFORM = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    private static final int APPDEF_TYPE_GROUP = AppdefEntityConstants.APPDEF_TYPE_GROUP;

    protected JdbcTemplate jdbcTemplate;
    
    private SessionFactory sessionFactory;

    protected final Log log = LogFactory.getLog(AppdefStatDAO.class);

    @Autowired
    public AppdefStatDAO(JdbcTemplate jdbcTemplate, SessionFactory sessionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionFactory = sessionFactory;
    }
    
    
    @PreDestroy 
    public final void destroy() { 
        this.jdbcTemplate = null ; 
        this.sessionFactory = null ;
    }//EOM 

    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, final Platform plat)
        throws SQLException {
        final ResourceTreeNode aPlatformNode = new ResourceTreeNode(plat.getName(),
            getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, plat.getAppdefResourceType().getName()), plat
                .getEntityId(), ResourceTreeNode.RESOURCE);
        String falseStr = DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource()
            .getConnection());
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT svr_svrt_svc_svct.server_id, ")
            .append("svr_svrt_svc_svct.server_name, ").append(
                "       svr_svrt_svc_svct.server_type_id, ").append(
                "svr_svrt_svc_svct.server_type_name, ").append(
                "       svr_svrt_svc_svct.service_id, ").append("svr_svrt_svc_svct.service_name, ")
            .append("       svr_svrt_svc_svct.service_type_id, ").append(
                "svr_svrt_svc_svct.service_type_name ").append(
                "FROM (SELECT app.id as application_id, ").append(
                "appsvc.service_id as service_id ").append("      FROM EAM_APP_SERVICE appsvc ");
        if (isOracle8()) {
            buf.append(", ").append(TBL_APP).append(" app ").append(
                "WHERE app.id=appsvc.application_id(+) AND EXISTS (").append(
                getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                    APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ");
        } else {
            buf.append("RIGHT JOIN ").append(TBL_APP).append(
                " app ON app.id=appsvc.application_id ").append("WHERE EXISTS (").append(
                getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                    APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ");
        }
        buf.append("(SELECT svr_svrt.server_id, ").append("svr_svrt.server_name, ").append(
            "        svr_svrt.server_type_id, ").append("svr_svrt.server_type_name, ").append(
            "        svc_svct.service_id, ").append("svc_svct.service_name, ").append(
            "        svc_svct.service_type_id, ").append("svc_svct.service_type_name ").append(
            " FROM ( SELECT svc.id as service_id, ").append(
            "               res2.name  as service_name, ").append(
            "               svct.id   as service_type_id, ").append(
            "               svct.name as service_type_name,").append(
            "               svc.server_id as server_id ").append("          FROM ").append(
            TBL_SERVICE).append("_TYPE svct, ").append(TBL_SERVICE).append(" svc ").append(
            " JOIN " + TBL_RES).append(" res2 ON svc.resource_id = res2.id ").append(
            "         WHERE svc.service_type_id=svct.id ").append("           AND EXISTS (")
            .append(
                getResourceTypeSQL("svc.id", subject.getId(), SERVICE_RES_TYPE,
                    SERVICE_OP_VIEW_SERVICE)).append(") ) svc_svct ");
        if (isOracle8()) {
            buf.append(",");
        } else {
            buf.append("     RIGHT JOIN");
        }
        buf.append("       ( SELECT svr.id    as server_id, ").append(
            "                res1.name as server_name, ").append(
            "                svrt.id   as server_type_id,").append(
            "                svrt.name as server_type_name ").append("         FROM ").append(
            TBL_SERVER).append("_TYPE svrt, ").append(TBL_SERVER).append(" svr ").append(
            " JOIN " + TBL_RES).append(" res1 ON svr.resource_id = res1.id ").append(
            "         WHERE  svr.platform_id=").append(plat.getId())
        // exclude virtual server types from the navMap
            .append("                    AND svrt.fvirtual = " + falseStr).append(
                "                    AND svrt.id=svr.server_type_id ").append(
                "                    AND EXISTS (").append(
                getResourceTypeSQL("svr.id", subject.getId(), SERVER_RES_TYPE,
                    SERVER_OP_VIEW_SERVER)).append(") ) svr_svrt ");
        if (isOracle8()) {
            buf.append(" WHERE svr_svrt.server_id=svc_svct.server_id(+)").append(
                "  ) svr_svrt_svc_svct ").append(
                "WHERE svr_svrt_svc_svct.service_id=app_appsvc.service_id(+)");
        } else {
            buf.append("   ON svr_svrt.server_id=svc_svct.server_id ").append(
                "  ) svr_svrt_svc_svct ").append(
                "ON svr_svrt_svc_svct.service_id=app_appsvc.service_id ");
        }
        buf.append(" ORDER BY svr_svrt_svc_svct.server_id, ").append(
            "svr_svrt_svc_svct.server_type_id, ")
            .append("          svr_svrt_svc_svct.service_id, ").append(
                "svr_svrt_svc_svct.service_type_id ");
        if (log.isDebugEnabled()) {
            log.debug(buf.toString());
        }

        ResourceTreeNode[] platformNode = this.jdbcTemplate.query(buf.toString(),
            new ResultSetExtractor<ResourceTreeNode[]>() {
                public ResourceTreeNode[] extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                    final Set<ResourceTreeNode> servers = new HashSet<ResourceTreeNode>();
                    final Set<ResourceTreeNode> services = new HashSet<ResourceTreeNode>();
                    while (rs.next()) {
                        int thisSvrId = rs.getInt(1);
                        String thisServerName = rs.getString(2);
                        int thisServerTypeId = rs.getInt(3);
                        String thisServerTypeName = rs.getString(4);
                        int thisSvcId = rs.getInt(5);
                        String thisServiceName = rs.getString(6);
                        int thisServiceTypeId = rs.getInt(7);
                        String thisServiceTypeName = rs.getString(8);

                        if (thisServerTypeName != null) {
                            servers.add(new ResourceTreeNode(thisServerName, getAppdefTypeLabel(
                                APPDEF_TYPE_SERVER, thisServerTypeName), AppdefEntityID
                                .newServerID(new Integer(thisSvrId)), plat.getEntityId(),
                                thisServerTypeId));
                        }

                        if (thisServiceTypeName != null) {
                            services.add(new ResourceTreeNode(thisServiceName, getAppdefTypeLabel(
                                APPDEF_TYPE_SERVICE, thisServiceTypeName), AppdefEntityID
                                .newServiceID(new Integer(thisSvcId)), AppdefEntityID
                                .newServerID(new Integer(thisSvrId)), thisServiceTypeId));
                        }
                    }
                    // XXX Leave out service data No current way to represent it
                    // (ResourceTreeNode[]) serviceMap.values()
                    // .toArray(new ResourceTreeNode[0]);
                    aPlatformNode.setSelected(true);
                    ResourceTreeNode[] svrNodes = servers
                        .toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(svrNodes, true);
                    aPlatformNode.addUpChildren(svrNodes);
                    return new ResourceTreeNode[] { aPlatformNode };
                }
            });
        return platformNode;
    }

    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject, final Server server)
        throws SQLException {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT svc_svct_svr_plat.platform_id, ").append(
            "svc_svct_svr_plat.platform_name, ").append(
            "       svc_svct_svr_plat.platform_type_id, ").append(
            "svc_svct_svr_plat.platform_type_name, ").append(
            "       svc_svct_svr_plat.service_id, ").append("svc_svct_svr_plat.service_name, ")
            .append("       svc_svct_svr_plat.service_type_id, ").append(
                "svc_svct_svr_plat.service_type_name ").append(
                "FROM (SELECT app.id as application_id, ").append(
                "appsvc.service_id as service_id ").append("        FROM EAM_APP_SERVICE appsvc ");
        if (isOracle()) {
            buf.append(" , ").append(TBL_APP).append(" app ").append(
                "WHERE app.id=appsvc.application_id(+) AND EXISTS (").append(
                getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                    APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ");
        } else {
            buf.append("  RIGHT JOIN ").append(TBL_APP).append(
                " app ON app.id=appsvc.application_id ").append(" WHERE EXISTS (").append(
                getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                    APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ");
        }
        buf.append(" (SELECT svc_svct.service_id, ").append("svc_svct.service_name, ").append(
            "         svc_svct.service_type_id, ").append("svc_svct.service_type_name, ").append(
            "         plat.id as platform_id, ").append("res0.name as platform_name, ").append(
            "         platt.id as platform_type_id, ").append("platt.name as platform_type_name ")
            .append("  FROM (SELECT svc.id    as service_id, ").append(
                "               res2.name  as service_name, ").append(
                "               svct.id   as service_type_id,").append(
                "               svct.name as service_type_name,").append(
                "               svc.server_id as server_id ").append("        FROM ").append(
                TBL_SERVICE).append("_TYPE svct, ").append(TBL_SERVICE).append(" svc ").append(
                " JOIN " + TBL_RES).append(" res2 ON svc.resource_id = res2.id ").append(
                "        WHERE svc.service_type_id=svct.id AND EXISTS (").append(
                getResourceTypeSQL("svc.id", subject.getId(), SERVICE_RES_TYPE,
                    SERVICE_OP_VIEW_SERVICE)).append(") ) svc_svct ");
        if (isOracle()) {
            buf.append(" ," + TBL_SERVER + " svr, ");
        } else {
            buf.append(" RIGHT JOIN " + TBL_SERVER + " svr ").append(
                "ON svc_svct.server_id=svr.id, ");
        }
        buf.append(TBL_PLATFORM).append("_TYPE platt, ").append(TBL_PLATFORM).append(" plat JOIN ")
            .append(TBL_RES).append(" res0 ON plat.resource_id = res0.id").append(" WHERE svr.id=")
            .append(server.getId()).append("   AND platt.id=plat.platform_type_id ").append(
                "   AND plat.id=svr.platform_id AND EXISTS (").append(
                getResourceTypeSQL("plat.id", subject.getId(), PLATFORM_RES_TYPE,
                    PLATFORM_OP_VIEW_PLATFORM)).append(") ");

        if (isOracle()) {
            buf.append(" AND svr.id=svc_svct.server_id(+) ").append("       ) svc_svct_svr_plat ")
                .append(" WHERE svc_svct_svr_plat.service_id=app_appsvc.service_id(+)");
        } else {
            buf.append(" ) svc_svct_svr_plat ").append(
                " ON svc_svct_svr_plat.service_id=app_appsvc.service_id ");
        }
        buf.append("order by service_type_id ");

        StopWatch timer = new StopWatch();
        final Map<Integer, ResourceTreeNode> serviceMap = new HashMap<Integer, ResourceTreeNode>();

        final ResourceTreeNode aServerNode = new ResourceTreeNode(server.getName(),
            getAppdefTypeLabel(server.getEntityId().getType(), server.getAppdefResourceType()
                .getName()), server.getEntityId(), ResourceTreeNode.RESOURCE);

        ResourceTreeNode[] serverNode = this.jdbcTemplate.query(buf.toString(),
            new ResultSetExtractor<ResourceTreeNode[]>() {
                public ResourceTreeNode[] extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                    int thisPlatId = 0;
                    ResourceTreeNode aPlatformNode = null;
                    while (rs.next()) {
                        if (thisPlatId == 0) {
                            thisPlatId = rs.getInt(1);
                            String thisPlatformName = rs.getString(2);
                            int thisPlatformTypeId = rs.getInt(3);
                            String thisPlatformTypeName = rs.getString(4);
                            aPlatformNode = new ResourceTreeNode(thisPlatformName,
                                getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, thisPlatformTypeName),
                                AppdefEntityID.newPlatformID(new Integer(thisPlatId)),
                                (AppdefEntityID) null, thisPlatformTypeId);
                        }

                        int thisSvcId = rs.getInt(5);
                        String thisServiceName = rs.getString(6);
                        int thisServiceTypeId = rs.getInt(7);
                        String thisServiceTypeName = rs.getString(8);

                        if (thisServiceName != null) {
                            serviceMap.put(new Integer(thisSvcId), new ResourceTreeNode(
                                thisServiceName, getAppdefTypeLabel(APPDEF_TYPE_SERVICE,
                                    thisServiceTypeName), AppdefEntityID.newServiceID(new Integer(
                                    thisSvcId)), server.getEntityId(), thisServiceTypeId));
                        }
                    }
                    aServerNode.setSelected(true);
                    ResourceTreeNode[] services = serviceMap.values().toArray(
                        new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(services, true);
                    aServerNode.addUpChildren(services);
                    // aPlatformNode can be null if user is unauthz
                    if (aPlatformNode != null) {
                        aServerNode.addDownChild(aPlatformNode);
                    }
                    return new ResourceTreeNode[] { aServerNode };
                }
            });

        if (log.isDebugEnabled()) {
            log.debug("getNavMapDataForServer() executed in: " + timer);
            log.debug("SQL: " + buf);
        }

        return serverNode;
    }

    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject, final Service service)
        throws SQLException {
        final String trueStr = DBUtil.getBooleanValue(true, jdbcTemplate.getDataSource()
            .getConnection());
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT plat.platform_id, ").append("platform_name, ").append(
            "       platform_type_name, ").append("asvc_svr.server_id, ").append(
            "       asvc_svr.server_name, ").append("asvc_svr.server_type_name, ").append(
            "       asvc_svr.application_id, ").append("asvc_svr.application_name, ").append(
            "       asvc_svr.application_type_name, ").append("fvirtual ").append(
            "FROM (SELECT plat.id as platform_id, " + "res0.name as platform_name, " +
                "platt.name as platform_type_name " + "FROM " + TBL_PLATFORM + "_TYPE platt, " +
                TBL_PLATFORM + " plat JOIN " + TBL_RES + " res0 ON plat.resource_id = res0.id " +
                "WHERE plat.platform_type_id=platt.id AND " + " EXISTS (").append(
            getResourceTypeSQL("plat.id", subject.getId(), PLATFORM_RES_TYPE,
                PLATFORM_OP_VIEW_PLATFORM)).append(")) plat ");

        if (isOracle8()) {
            buf.append(", ");
        } else {
            buf.append("RIGHT JOIN ");
        }
        buf.append("( SELECT asvc.application_id, ").append("asvc.application_name, ").append(
            "         asvc.application_type_name, ").append("svr.id as server_id, ").append(
            "         res1.name as server_name, ").append(
            "         svrt.name as server_type_name, ").append(
            "         svr.platform_id, fvirtual ").append(" FROM ").append(TBL_RES).append(
            " res1 JOIN ").append(TBL_SERVER).append(" svr ON res1.id = svr.resource_id ");
        if (isOracle8()) {
            buf.append(" , ");
        } else {
            buf.append(" RIGHT JOIN ");
        }
        buf.append(" (SELECT app_appsvc.application_id, ").append("app_appsvc.application_name, ")
            .append("         app_appsvc.application_type_name, ").append(
                "svc.server_id as server_id ")
            .append("    FROM (SELECT app.id as application_id, ").append(
                "r.name as application_name, ").append(
                "                 EAM_APPLICATION_TYPE.name as application_type_name, ").append(
                "                 appsvc.service_id as service_id ").append(
                "          FROM EAM_APP_SERVICE appsvc ");
        if (isOracle8()) {
            buf.append(" , ").append(TBL_APP).append(" app, EAM_APPLICATION_TYPE, ")
                .append(TBL_RES).append(" r ").append(" WHERE app.id=appsvc.application_id(+) ")
                .append("   AND EAM_APPLICATION_TYPE.id=app.application_type_id ").append(
                    "   AND app.resource_id = r.id AND EXISTS (").append(
                    getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc, ").append(
                    TBL_SERVICE).append(" svc WHERE svc.id=app_appsvc.service_id(+) AND svc.id=")
                .append(service.getId()).append(") asvc ");
        } else {
            buf.append(" RIGHT JOIN ").append(TBL_APP).append(
                " app ON app.id=appsvc.application_id ").append(" RIGHT JOIN ").append(TBL_RES)
                .append(" r ON app.resource_id = r.id, ").append(" EAM_APPLICATION_TYPE  ").append(
                    " WHERE EAM_APPLICATION_TYPE.id=app.application_type_id ").append(
                    "   AND EXISTS (").append(
                    getResourceTypeSQL("app.id", subject.getId(), APPLICATION_RES_TYPE,
                        APPLICATION_OP_VIEW_APPLICATION)).append(") ) app_appsvc RIGHT JOIN ")
                .append(TBL_SERVICE).append(" svc ON svc.id=app_appsvc.service_id ").append(
                    " WHERE svc.id=").append(service.getId()).append(") asvc ");
        }
        if (isOracle8()) {
            buf.append(" , ").append(TBL_SERVER).append("_TYPE svrt ").append(
                " WHERE svr.server_type_id=svrt.id ").append("   AND asvc.server_id=svr.id(+) ")
                .append("   AND (fvirtual = ").append(trueStr).append("    OR EXISTS (").append(
                    getResourceTypeSQL("svr.id", subject.getId(), SERVER_RES_TYPE,
                        SERVER_OP_VIEW_SERVER)).append(")) ) asvc_svr, ").append(
                    TBL_PLATFORM + "_TYPE platt ").append("WHERE plat.platform_type_id=platt.id ")
                .append("  AND asvc_svr.platform_id=plat.id(+) AND EXISTS (").append(
                    getResourceTypeSQL("plat.id", subject.getId(), PLATFORM_RES_TYPE,
                        PLATFORM_OP_VIEW_PLATFORM)).append(") ");
        } else {
            buf.append(" ON asvc.server_id=svr.id, ").append(TBL_SERVER).append("_TYPE svrt ")
                .append(" WHERE svr.server_type_id=svrt.id ").append("   AND (fvirtual = ").append(
                    trueStr).append("    OR EXISTS (").append(
                    getResourceTypeSQL("svr.id", subject.getId(), SERVER_RES_TYPE,
                        SERVER_OP_VIEW_SERVER)).append(")) ) asvc_svr ").append(
                    "     ON asvc_svr.platform_id = plat.platform_id");
        }

        StopWatch timer = new StopWatch();
        ResourceTreeNode[] serviceNode = this.jdbcTemplate.query(buf.toString(),
            new ResultSetExtractor<ResourceTreeNode[]>() {
                public ResourceTreeNode[] extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                    ResourceTreeNode aPlatformNode = null;
                    ResourceTreeNode aServerNode = null;
                    ResourceTreeNode aServiceNode = new ResourceTreeNode(service.getName(),
                        getAppdefTypeLabel(service.getEntityId().getType(), service
                            .getAppdefResourceType().getName()), service.getEntityId(),
                        ResourceTreeNode.RESOURCE);
                    Map<Integer, ResourceTreeNode> appMap = new HashMap<Integer, ResourceTreeNode>();
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
                            aPlatformNode = new ResourceTreeNode(thisPlatformName,
                                getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, thisPlatformTypeName),
                                AppdefEntityID.newPlatformID(new Integer(thisPlatId)),
                                ResourceTreeNode.RESOURCE);
                        }

                        if (thisServerName != null && !trueStr.startsWith(virtualServer)) {
                            aServerNode = new ResourceTreeNode(thisServerName, getAppdefTypeLabel(
                                APPDEF_TYPE_SERVER, thisServerTypeName), AppdefEntityID
                                .newServerID(new Integer(thisSvrId)), ResourceTreeNode.RESOURCE);
                        }

                        if (thisApplicationName != null) {
                            appMap.put(new Integer(thisAppId), new ResourceTreeNode(
                                thisApplicationName, getAppdefTypeLabel(
                                    AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                                    thisApplicationDesc), AppdefEntityID.newAppID(new Integer(
                                    thisAppId)), ResourceTreeNode.RESOURCE));
                        }
                    }
                    aServiceNode.setSelected(true);

                    // server nodes and platform nodes can be null if user is
                    // unauthz
                    if (aServerNode != null) {
                        if (aPlatformNode != null) {
                            aServerNode.addDownChild(aPlatformNode);
                        }
                        aServiceNode.addDownChild(aServerNode);
                    } else if (aPlatformNode != null) {
                        aServiceNode.addDownChild(aPlatformNode);
                    }

                    ResourceTreeNode[] appNodes = appMap.values().toArray(
                        new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(appNodes, true);
                    aServiceNode.addUpChildren(appNodes);
                    return new ResourceTreeNode[] { aServiceNode };
                }

            });

        if (log.isDebugEnabled()) {
            log.debug("getNavMapDataForService() executed in: " + timer);
            log.debug("SQL: " + buf);
        }
        return serviceNode;
    }

    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject,
                                                          final Application app)
        throws SQLException {
        StringBuffer buf = new StringBuffer().append("SELECT appsvc.service_id, pm.name,").append(
            " appsvc.service_type_id,").append(" svct.name as service_type_name,").append(
            " appsvc.application_id, appsvc.group_id").append(" FROM EAM_APP_SERVICE appsvc, ")
            .append(TBL_SERVICE).append("_TYPE svct, ").append(TBL_GROUP).append(" grp, (").append(
                getPermGroupSQL(subject.getId())).append(") pm").append(
                " WHERE svct.id = appsvc.service_type_id AND ").append(
                " grp.id = appsvc.group_id AND pm.group_id = grp.id").append(
                " AND appsvc.application_id = ").append(app.getId()).append(" UNION ALL ").append(
                "SELECT appsvc.service_id, res2.name,").append(" appsvc.service_type_id,").append(
                " svct.name as service_type_name,").append(
                " appsvc.application_id, appsvc.group_id").append(" FROM EAM_APP_SERVICE appsvc, ")
            .append(TBL_SERVICE).append("_TYPE svct, (").append(getPermServiceSQL(subject.getId()))
            .append(") pm, ").append(TBL_SERVICE).append(" svc JOIN ").append(TBL_RES).append(
                " res2 ON svc.resource_id = res2.id ").append(
                " WHERE svct.id = appsvc.service_type_id AND ").append(
                " svc.id = appsvc.service_id AND ").append(" pm.service_id = svc.id AND ").append(
                " appsvc.application_id = ").append(app.getId()).append(
                " ORDER BY service_type_id, service_id");

        if (log.isDebugEnabled()) {
            log.debug(buf.toString());
        }

        StopWatch timer = new StopWatch();
        ResourceTreeNode[] appNode = this.jdbcTemplate.query(buf.toString(),
            new ResultSetExtractor<ResourceTreeNode[]>() {
                public ResourceTreeNode[] extractData(ResultSet rs) throws SQLException,
                    DataAccessException {
                    Map<String, ResourceTreeNode> svcMap = new HashMap<String, ResourceTreeNode>();

                    ResourceTreeNode appNode = new ResourceTreeNode(app.getName(),
                        getAppdefTypeLabel(app.getEntityId().getType(), app.getAppdefResourceType()
                            .getName()), app.getEntityId(), ResourceTreeNode.RESOURCE);

                    int svc_id_col = rs.findColumn("service_id"), name_col = rs.findColumn("name"), service_type_col = rs
                        .findColumn("service_type_id"), type_name_col = rs
                        .findColumn("service_type_name"), group_id_col = rs.findColumn("group_id");

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
                            svcMap.put(key, new ResourceTreeNode(thisGroupName, getAppdefTypeLabel(
                                APPDEF_TYPE_GROUP, serviceTypeName), AppdefEntityID
                                .newGroupID(new Integer(groupId)), ResourceTreeNode.CLUSTER));
                        } else if (serviceName != null) {
                            String key = APPDEF_TYPE_SERVICE + "-" + serviceId;
                            svcMap.put(key, new ResourceTreeNode(serviceName, getAppdefTypeLabel(
                                APPDEF_TYPE_SERVICE, serviceTypeName), AppdefEntityID
                                .newServiceID(new Integer(serviceId)), app.getEntityId(),
                                serviceTypeId));
                        }
                    }

                    appNode.setSelected(true);
                    ResourceTreeNode[] svcNodes = svcMap.values().toArray(
                        new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(svcNodes);
                    appNode.addDownChildren(svcNodes);

                    return new ResourceTreeNode[] { appNode };
                }
            });

        if (log.isDebugEnabled()) {
            log.debug("getNavMapDataForApplication() executed in: " + timer);
            log.debug("SQL: " + buf);
        }
        return appNode;
    }

    public ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject,
                                                        final AppdefEntityID[] parents,
                                                        AppdefResourceType type, final int pEntityType, final int cEntityType)
        throws AppdefEntityNotFoundException, PermissionException, SQLException {
        final String sqlStmt;
        String bindMarkerStr = "";
        String authzResName;
        String authzOpName;
        final int appdefTypeUndefined = -1;
        List<ResourceTreeNode> parentNodes = null;
       
        // If the auto-group has parents, fetch the resources
        if (parents != null) {
            parentNodes = new ArrayList<ResourceTreeNode>(parents.length);
            for (int x = 0; x < parents.length; x++) {
                AppdefEntityValue av = new AppdefEntityValue(parents[x], subject);
                parentNodes.add(new ResourceTreeNode(av.getName(), getAppdefTypeLabel(pEntityType,
                    av.getTypeName()), parents[x], ResourceTreeNode.RESOURCE));
            }
        }

        // Platforms don't have a auto-group parents
        if (pEntityType != appdefTypeUndefined) {
            for (int x = 0; x < parents.length; x++) {
                bindMarkerStr += (x < parents.length - 1) ? "?," : "?";
            }
        }

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
                                 getResourceTypeSQL("p.id", subject.getId(), PLATFORM_RES_TYPE,
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
                                getResourceTypeSQL("s.id", subject.getId(), SERVER_RES_TYPE,
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
                                getResourceTypeSQL("s.id", subject.getId(), SERVICE_RES_TYPE,
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
                                   getResourceTypeSQL("s.id", subject.getId(), SERVICE_RES_TYPE,
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
            case (appdefTypeUndefined):
                sqlStmt = platAGSql;
                authzResName = AuthzConstants.platformResType;
                authzOpName = AuthzConstants.platformOpViewPlatform;
                break;
            default:
                throw new IllegalArgumentException("No auto-group support " + "for specified type");
        }

        if (log.isDebugEnabled()) {
            log.debug(sqlStmt);
        }

        final ResourceTreeNode agNode = new ResourceTreeNode(type.getName(), getAppdefTypeLabel(
            cEntityType, type.getName()), parents, type.getId().intValue(),
            ResourceTreeNode.AUTO_GROUP);
        final Set<ResourceTreeNode> entitySet = new HashSet<ResourceTreeNode>();
        final List<ResourceTreeNode> parentNodeList = parentNodes;
        StopWatch timer = new StopWatch();
        ResourceTreeNode[] groupNode = jdbcTemplate.query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement stmt = con.prepareStatement(sqlStmt);
                if (pEntityType != appdefTypeUndefined) {
                    for (int x = 0; x < parents.length; x++) {
                        stmt.setInt(x + 1, parents[x].getID());
                    }
                }
                return stmt;
            }
        }, new ResultSetExtractor<ResourceTreeNode[]>() {
            public ResourceTreeNode[] extractData(ResultSet rs) throws SQLException,
                DataAccessException {
                while (rs.next()) {
                    int thisEntityId = rs.getInt(1);
                    String thisEntityName = rs.getString(2);
                    String thisEntityTypeName = rs.getString(4);

                    entitySet.add(new ResourceTreeNode(thisEntityName, getAppdefTypeLabel(
                        cEntityType, thisEntityTypeName), new AppdefEntityID(cEntityType,
                        thisEntityId), ResourceTreeNode.RESOURCE));
                }

                agNode.setSelected(true);
                if (parentNodeList != null) {
                    ResourceTreeNode[] parNodeArr = parentNodeList
                        .toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(parNodeArr, true);
                    agNode.addUpChildren(parNodeArr);
                }

                ResourceTreeNode[] members = entitySet
                    .toArray(new ResourceTreeNode[0]);

                ResourceTreeNode.alphaSortNodes(members);
                agNode.addDownChildren(members);

                return new ResourceTreeNode[] { agNode };
            }

        });

        if (log.isDebugEnabled()) {
            log.debug("getNavMapDataForAutoGroup() executed in: " + timer);
            log.debug("SQL: " + sqlStmt);
            int i;
            for (i = 0; i < parents.length; i++) {
                log.debug("Arg " + (i + 1) + ": " + parents[i].getID());
            }
            i = 1;
            log.debug("Arg " + (i++) + ": " + type.getId());
            log.debug("Arg " + (i++) + ": " + subject.getId());
            log.debug("Arg " + (i++) + ": " + subject.getId());
            log.debug("Arg " + (i++) + ": " + authzResName);
            log.debug("Arg " + (i++) + ": " + authzOpName);
        }

        return groupNode;
    }

    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, AppdefGroupValue groupVo)
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
            final int max = getHQDialect().getMaxExpressions();
            final int batchSize = (max < 0) ? Integer.MAX_VALUE : max;
            for (int ii = 0; ii < agEntries.size(); ii += batchSize) {
                int end = Math.min(ii + batchSize, agEntries.size());
                List<AppdefEntityID> list = agEntries.subList(ii, end);
                setEntNameMap(entNameMap, list, grpSqlStmt);
            }

            // Let group member order drive node creation (not db order).
            for (AppdefEntityID id : groupVo.getAppdefGroupEntries()) {
                entitySet
                    .add(new ResourceTreeNode(entNameMap.get(id.getId()),
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
    
    private HQDialect getHQDialect() {
        return (HQDialect) ((SessionFactoryImplementor) sessionFactory)
            .getDialect();
    }

    private void setEntNameMap(final Map<Integer, String> entNameMap, List<AppdefEntityID> list,
                               StringBuilder grpSqlStmt) throws SQLException {
        if (list.size() == 0) {
            return;
        }
        final boolean debug = log.isDebugEnabled();
        // don't overwrite the caller's object
        grpSqlStmt = new StringBuilder(grpSqlStmt);

        int x = 1;
        for (AppdefEntityID mem : list) {
            if (debug) {
                log.debug("Arg " + x + ": " + mem.getID());
            }
            grpSqlStmt.append((x == 1 ? "" : ",")).append(mem.getID());
            x++;
        }
        grpSqlStmt.append(")");
        StopWatch timer = new StopWatch();
        if (debug) {
            log.debug("SQL: " + grpSqlStmt);
        }
        jdbcTemplate.query(grpSqlStmt.toString(), new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                entNameMap.put(rs.getInt(1), rs.getString(2));
            } 
        });   
     
        if (debug) {
            log.debug("getNavMapDataForGroup() executed in: " + timer);
        }
    }

    protected String getResourceTypeSQL(String instanceId, Integer subjectId, String resType,
                                      String op) throws SQLException {
        return "SELECT RES.ID FROM EAM_RESOURCE RES, " + " EAM_RESOURCE_TYPE RT " + "WHERE " +
               instanceId + " = RES.INSTANCE_ID " + "  AND RES.FSYSTEM = " +
               DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource().getConnection()) +
               "  AND RES.RESOURCE_TYPE_ID = RT.ID " + "  AND RT.NAME = '" + resType + "'";
    }

    private boolean isOracle8() throws SQLException {
        return DBUtil.getDBType(jdbcTemplate.getDataSource().getConnection()) == DBUtil.DATABASE_ORACLE_8;
    }

    private boolean isOracle() throws SQLException {
        return isOracle8() ||
               DBUtil.getDBType(jdbcTemplate.getDataSource().getConnection()) == DBUtil.DATABASE_ORACLE_9;
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

    private final String getPermGroupSQL(Integer subjectId) throws SQLException {
        StringBuffer rtn = new StringBuffer().append(
            "SELECT grp.id as group_id, res.name, cluster_id ").append(" FROM ").append(TBL_GROUP)
            .append(" grp, ").append(TBL_RES).append(" res ").append(
                " WHERE grp.resource_id = res.id AND EXISTS (").append(
                getResourceTypeSQL("grp.id", subjectId, GROUP_RES_TYPE,
                    GROUP_OP_VIEW_RESOURCE_GROUP)).append(")");
        return rtn.toString();
    }

    private final String getPermServiceSQL(Integer subjectId) throws SQLException {
        StringBuffer rtn = new StringBuffer().append(
            "SELECT svc.id as service_id, res.name as service_name,").append(" server_id").append(
            " FROM  " + TBL_SERVICE + " svc JOIN ").append(TBL_RES).append(
            " res ON resource_id = svc.id ").append(" WHERE EXISTS (").append(
            getResourceTypeSQL("svc.id", subjectId, SERVICE_RES_TYPE, SERVICE_OP_VIEW_SERVICE))
            .append(")");
        return rtn.toString();
    }

}
