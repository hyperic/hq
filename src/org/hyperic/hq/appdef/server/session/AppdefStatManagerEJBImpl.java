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

package org.hyperic.hq.appdef.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefStatManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefStatManagerUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.PlatformDAO;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;

/** 
 * AppdefStatManagerEJB provides summary and aggregate statistical
 * information for appdef related entities.
 * <p>
 *
 * </p>
 * @ejb:bean name="AppdefStatManager"
 *      jndi-name="ejb/appdef/AppdefStatManager"
 *      local-jndi-name="LocalAppdefStatManager"
 *      view-type="local"
 *      type="Stateless"
 */

public class AppdefStatManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {
    private final String logCtx  = AppdefStatManagerEJBImpl.class.getName();
    private final Log    log     = LogFactory.getLog(logCtx);
    private int          DB_TYPE = -1;
    
    private static final PermissionManager pm =
        PermissionManagerFactory.getInstance();

    /**
     * <p>Return map of platform counts.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public Map getPlatformCountsByTypeMap (AuthzSubjectValue subject)
    {
        Map               platMap = new HashMap();
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        int               subjectId = subject.getId().intValue();
    
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT PLATT.NAME, COUNT(PLAT.ID) " +
                "FROM EAM_PLATFORM_TYPE PLATT, EAM_PLATFORM PLAT " +
                "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID " +
                "  AND PLAT.ID IN (" + getResourceTypeSQL("EAM_PLATFORM") + ") " + 
                "GROUP BY PLATT.NAME ORDER BY PLATT.NAME";
            stmt = conn.prepareStatement(sql);
    
            if (log.isDebugEnabled())
                log.debug(sql);
    
            int total = 0;
            pm.prepareResourceTypeSQL(stmt, 0, subjectId,
                                      AuthzConstants.platformResType,
                                      AuthzConstants.platformOpViewPlatform);
            rs = stmt.executeQuery();
    
            String platTypeName = null;
            while (rs.next()) {
                platTypeName = rs.getString(1);
                total   = rs.getInt(2);
                platMap.put(platTypeName,new Integer(total));
            }
    
        } catch (SQLException e) {
            log.error("Caught SQL Exception finding Platforms by type: " + 
                      e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return platMap;
    }

    /**
     * <p>Return platforms count.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public int getPlatformsCount (AuthzSubjectValue subject) {
        PreparedStatement stmt = null;
        ResultSet         rs = null;

        try {
            Connection conn = getDBConn();
            
            String sql =
                "SELECT COUNT(PLAT.ID) " +
                "FROM EAM_PLATFORM_TYPE PLATT, EAM_PLATFORM PLAT " +
                "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID " +
                "  AND PLAT.ID IN (" + getResourceTypeSQL("EAM_PLATFORM") + ")";
            stmt = conn.prepareStatement(sql);

            if (log.isDebugEnabled())
                log.debug(sql);
    
            pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                      AuthzConstants.platformResType,
                                      AuthzConstants.platformOpViewPlatform);
            rs = stmt.executeQuery();
    
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Caught SQL Exception counting Platforms: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return 0;
    }

    /**
     * <p>Return map of server counts.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public Map getServerCountsByTypeMap (AuthzSubjectValue subject)
    {
        Map               servMap = new HashMap();
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        int               subjectId = subject.getId().intValue();
    
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT SERVT.NAME, COUNT(SERV.ID) " +
                "FROM EAM_SERVER_TYPE SERVT, EAM_SERVER SERV " +
                "WHERE SERV.SERVER_TYPE_ID = SERVT.ID " +
                "  AND SERV.ID IN (" + getResourceTypeSQL("EAM_SERVER") + ") " +
                "GROUP BY SERVT.NAME ORDER BY SERVT.NAME";
            stmt = conn.prepareStatement(sql);
    
            int total = 0;
            pm.prepareResourceTypeSQL(stmt, 0, subjectId,
                                      AuthzConstants.serverResType,
                                      AuthzConstants.serverOpViewServer);
            rs = stmt.executeQuery();
    
            String servTypeName = null;
            while (rs.next()) {
                servTypeName = rs.getString(1);
                total   = rs.getInt(2);
                servMap.put(servTypeName,new Integer(total));
            }
    
        } catch (SQLException e) {
             log.error ("Caught SQL Exception finding Servers by type: " +
                        e, e);
             throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return servMap;
    }

    /**
     * <p>Return servers count.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public int getServersCount (AuthzSubjectValue subject) {
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT COUNT(SERV.ID) " +
                "FROM EAM_SERVER_TYPE SERVT, EAM_SERVER SERV " +
                "WHERE SERV.SERVER_TYPE_ID = SERVT.ID " +
                "  AND SERV.ID IN (" + getResourceTypeSQL("EAM_SERVER") + ") ";
            stmt = conn.prepareStatement(sql);
    
            pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                      AuthzConstants.serverResType,
                                      AuthzConstants.serverOpViewServer);
            rs = stmt.executeQuery();
    
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
             log.error ("Caught SQL Exception finding Servers by type: " +
                        e, e);
             throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return 0;
    }

    /**<p>Return map of service counts.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public Map getServiceCountsByTypeMap (AuthzSubjectValue subject)
    {
        Map               svcMap = new HashMap();
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        int               subjectId = subject.getId().intValue();
    
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT SVCT.NAME, COUNT(SVC.ID) " +
                "FROM EAM_SERVICE_TYPE SVCT, EAM_SERVICE SVC " +
                "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID " +
                "  AND SVC.ID IN (" + getResourceTypeSQL("EAM_SERVICE") + ") " +
                "GROUP BY SVCT.NAME ORDER BY SVCT.NAME";
            stmt = conn.prepareStatement(sql);
    
            int total = 0;
            pm.prepareResourceTypeSQL(stmt, 0, subjectId, 
                                      AuthzConstants.serviceResType,
                                      AuthzConstants.serviceOpViewService);
            rs = stmt.executeQuery();
    
            String serviceTypeName = null;
            while (rs.next()) {
                serviceTypeName = rs.getString(1);
                total   = rs.getInt(2);
                svcMap.put(serviceTypeName,new Integer(total));
            }
    
        } catch (SQLException e) {
            log.error ("Caught SQL Exception finding Services by type: " +
                       e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return svcMap;
    }

    /**<p>Return services count.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public int getServicesCount (AuthzSubjectValue subject) {
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT COUNT(SVC.ID) " +
                "FROM EAM_SERVICE_TYPE SVCT, EAM_SERVICE SVC " +
                "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID " +
                "  AND SVC.ID IN (" + getResourceTypeSQL("EAM_SERVICE") + ") ";
            stmt = conn.prepareStatement(sql);
    
            pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(), 
                                      AuthzConstants.serviceResType,
                                      AuthzConstants.serviceOpViewService);
            rs = stmt.executeQuery();
    
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error ("Caught SQL Exception finding Services by type: " +
                       e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return 0;
    }

    /**<p>Return map of app counts.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public Map getApplicationCountsByTypeMap (AuthzSubjectValue subject)
    {
        Map               appMap = new HashMap();
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        int               subjectId = subject.getId().intValue();
    
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT APPT.NAME, COUNT(APP.ID) " +
                "FROM EAM_APPLICATION_TYPE APPT, EAM_APPLICATION APP " +
                "WHERE APP.APPLICATION_TYPE_ID = APPT.ID " +
                " AND APP.ID IN (" + getResourceTypeSQL("EAM_APPLICATION") + ") " +
                "GROUP BY APPT.NAME ORDER BY APPT.NAME";
            stmt = conn.prepareStatement(sql);
    
            int total = 0;
            pm.prepareResourceTypeSQL(stmt, 0, subjectId, 
                                      AuthzConstants.applicationResType,
                                      AuthzConstants.appOpViewApplication);
            rs = stmt.executeQuery();
    
            String appTypeName = null;
            while (rs.next()) {
                appTypeName = rs.getString(1);
                total   = rs.getInt(2);
                appMap.put(appTypeName,new Integer(total));
            }
    
        } catch (SQLException e) {
            log.error ("Caught SQL Exception finding applications by type: " +
                       e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return appMap;
    }

    /**<p>Return apps count.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public int getApplicationsCount(AuthzSubjectValue subject) {
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        
        try {
            Connection conn = getDBConn();
            String sql =
                "SELECT COUNT(APP.ID) " +
                "FROM EAM_APPLICATION_TYPE APPT, EAM_APPLICATION APP " +
                "WHERE APP.APPLICATION_TYPE_ID = APPT.ID " +
                " AND APP.ID IN (" + getResourceTypeSQL("EAM_APPLICATION") +
                ") ";
            stmt = conn.prepareStatement(sql);
    
            pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(), 
                                      AuthzConstants.applicationResType,
                                      AuthzConstants.appOpViewApplication);
            rs = stmt.executeQuery();
    
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error ("Caught SQL Exception finding applications by type: " +
                       e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        } 
        return 0;
    }

    /**<p>Return map of grp counts.</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public Map getGroupCountsMap (AuthzSubjectValue subject)
    {
        Map               grpMap = new HashMap();
        PreparedStatement stmt = null;
        ResultSet         rs = null;
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        int  subjectId = subject.getId().intValue();
    
        try {
            Connection conn = getDBConn();
    
            for (int x=0;x< groupTypes.length; x++) {
                String sql =
                    "SELECT COUNT(*) FROM EAM_RESOURCE_GROUP GRP " +
                    "WHERE GRP.GROUPTYPE = " + groupTypes[x] + " " +
                    " AND GRP.ID IN (" + getResourceTypeSQL("EAM_RESOURCE_GROUP") + ")";
                
                try {
                    stmt = conn.prepareStatement(sql);
    
                    int total = 0;
                    pm.prepareResourceTypeSQL(stmt, 0, subjectId, 
                                              AuthzConstants.groupResType,
                                              AuthzConstants.groupOpViewResourceGroup);
                    rs = stmt.executeQuery();
        
                    if (rs.next()) {
                        total = rs.getInt(1);
                        grpMap.put(new Integer(groupTypes[x]),
                                   new Integer(total));
                    }
                } finally {
                    DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
                }
            }
        } catch (SQLException e) {
            log.error ("Caught SQL Exception finding groups by type: " +
                       e, e);
            throw new SystemException(e);
        } finally {
            disconnect();
        } 
        return grpMap;
    }

    private final String getResourceTypeSQL(String table) {
        return pm.getResourceTypeSQL(table);
    }

    /**
     * Method for determining whether or not to show a nav map
     * (this is a temporary method)
     * @ejb:interface-method
     */
    public boolean isNavMapSupported () { 
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
        } finally {
            disconnect();
        }
    }

    /**<p>Return directly connected resource tree for node level platform</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject,
                                                       Integer platformId) 
        throws PlatformNotFoundException, PermissionException {
        try {
            Platform platVo = 
                getPlatformMgrLocal().findPlatformById(platformId);
            ResourceTreeNode[] retVal;
            retVal = getNavMapDataForPlatform(subject, platVo);
            return retVal;
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject,
                                                        Platform platVo)
        throws PermissionException, SQLException {
        ResourceTreeNode[] retVal;
        PreparedStatement    stmt;
        ResultSet            rs;
        StringBuffer         buf;

        retVal = null;
        stmt = null;
        rs = null;
        try {
            Connection conn = getDBConn();
            String falseStr = DBUtil.getBooleanValue(false, conn);
            buf = new StringBuffer();
            buf.append("SELECT svr_svrt_svc_svct.server_id, svr_svrt_svc_svct.server_name,   ")
               .append("        svr_svrt_svc_svct.server_type_id, svr_svrt_svc_svct.server_type_name,")
               .append("        svr_svrt_svc_svct.service_id, svr_svrt_svc_svct.service_name,")
               .append("        svr_svrt_svc_svct.service_type_id, svr_svrt_svc_svct.service_type_name ")
               .append("FROM   (SELECT app.id as application_id, app.name as application_name,")
               .append("               app.description as application_desc, ")
               .append("               appsvc.service_id as service_id ")
               .append("        FROM   EAM_APP_SERVICE appsvc ");
            if (DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append(", EAM_APPLICATION app ")
                   .append("WHERE app.id=appsvc.application_id(+) AND ")
                   .append("app.id IN (").append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc, ");
            }
            else {
                buf.append("RIGHT JOIN EAM_APPLICATION app ")
                   .append("ON app.id=appsvc.application_id ")
                   .append("WHERE app.id IN (")
                   .append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc RIGHT JOIN ");
            }
            buf.append("(SELECT svr_svrt.server_id, svr_svrt.server_name,")
               .append("        svr_svrt.server_type_id, svr_svrt.server_type_name,")
               .append("        svc_svct.service_id, svc_svct.service_name,")
               .append("        svc_svct.service_type_id, svc_svct.service_type_name ")
               .append("        FROM ( SELECT svc.id    as service_id, ")
               .append("                      svc.name  as service_name, ")
               .append("                      svct.id   as service_type_id, ")
               .append("                      svct.name as service_type_name,")
               .append("                      svc.server_id as server_id ")
               .append("               FROM   EAM_SERVICE svc, ")
               .append("                      EAM_SERVICE_TYPE svct ")
               .append("               WHERE  svc.service_type_id=svct.id ") 
               .append("                  AND svc.id IN (")
               .append(getResourceTypeSQL("EAM_SERVICE"))
               .append(") ) svc_svct ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append(",");
            } 
            else {
                buf.append("     RIGHT JOIN");
            }
            buf.append("           ( SELECT svr.id    as server_id, ")
               .append("                    svr.name  as server_name, ")
               .append("                    svrt.id   as server_type_id,")
               .append("                              svrt.name as server_type_name ")
               .append("             FROM   EAM_SERVER       svr, ")
               .append("                    EAM_SERVER_TYPE  svrt ")
               .append("             WHERE      svr.platform_id=? ")
               // exclude virtual server types from the navMap
               .append("                    AND svrt.fvirtual = " + falseStr + " ")
               .append("                    AND svrt.id=svr.server_type_id ")
               .append("                    AND svr.id IN (")
               .append(getResourceTypeSQL("EAM_SERVER"))
               .append(") ) svr_svrt ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append(" WHERE svr_svrt.server_id=svc_svct.server_id(+)")
                   .append("  ) svr_svrt_svc_svct ")
                   .append("WHERE svr_svrt_svc_svct.service_id=app_appsvc.service_id(+)");
            } 
            else {
                buf.append("   ON svr_svrt.server_id=svc_svct.server_id ")
                   .append("  ) svr_svrt_svc_svct ")
                   .append("ON svr_svrt_svc_svct.service_id=app_appsvc.service_id ");
            }
            buf.append(" ORDER BY svr_svrt_svc_svct.server_id, svr_svrt_svc_svct.server_type_id, ")
               .append("          svr_svrt_svc_svct.service_id, svr_svrt_svc_svct.service_type_id ");
    
            if (log.isDebugEnabled())
                log.debug(buf.toString());

            Set servers       = new HashSet();
            Set services      = new HashSet();
            
            ResourceTreeNode aPlatformNode 
                = new ResourceTreeNode(
                      platVo.getName(), 
                      getAppdefTypeLabel(AppdefEntityConstants
                          .APPDEF_TYPE_PLATFORM,
                          platVo.getAppdefResourceType().getName()),
                      platVo.getEntityId(),
                      ResourceTreeNode.RESOURCE);

            int    thisServerId               = 0;
            String thisServerName             = null;
            int    thisServerTypeId           = 0;
            String thisServerTypeName         = null;
            int    thisServiceId              = 0;
            String thisServiceName            = null;
            int    thisServiceTypeId          = 0;
            String thisServiceTypeName        = null;

            stmt = conn.prepareStatement(buf.toString());
            int pos = 
                pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                          AuthzConstants.applicationResType,
                                          AuthzConstants.appOpViewApplication);
            
            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.serviceResType,
                                          AuthzConstants.serviceOpViewService);

            stmt.setInt(++pos, platVo.getId().intValue());

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.serverResType,
                                          AuthzConstants.serverOpViewServer);
            rs = stmt.executeQuery();

            while (rs.next()) {
                thisServerId        = rs.getInt(1); 
                thisServerName      = rs.getString(2);
                thisServerTypeId    = rs.getInt(3);
                thisServerTypeName  = rs.getString(4);
                thisServiceId       = rs.getInt(5);
                thisServiceName     = rs.getString(6);
                thisServiceTypeId   = rs.getInt(7);
                thisServiceTypeName = rs.getString(8);

                if (thisServerTypeName != null) {
                    servers.add( new ResourceTreeNode (
                            thisServerName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_SERVER,
                                thisServerTypeName),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                thisServerId),
                                platVo.getEntityId(),thisServerTypeId ));
                }

                if (thisServiceTypeName != null){
                    services.add(
                        new ResourceTreeNode (
                            thisServiceName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_SERVICE,
                                thisServiceTypeName),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                                thisServiceId),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                thisServerId),thisServiceTypeId));
                }
            }
            // XXX Leave out service data No current way to represent it
            // (ResourceTreeNode[]) serviceMap.values()
            // .toArray(new ResourceTreeNode[0]);
            aPlatformNode.setSelected(true);
            ResourceTreeNode[] svrNodes = (ResourceTreeNode[])servers
                .toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(svrNodes,true);
            aPlatformNode.addUpChildren(svrNodes);

            retVal = new ResourceTreeNode[] { aPlatformNode }; 
        } catch (SQLException e) {
            throw e;
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        }
        if (log.isDebugEnabled())
            log.debug(mapToString(retVal));
        return retVal;
    }

    /**<p>Return directly connected resource tree for node level server</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject,
                                                     Integer serverId) 
        throws ServerNotFoundException, PermissionException {
        Server serverVo = getServerMgrLocal().findServerById(serverId);

        ResourceTreeNode[] retVal;
        PreparedStatement    stmt;
        ResultSet            rs;
        StringBuffer         buf;

        stmt = null;
        rs = null;
        retVal = null;
        try {
            Connection conn = getDBConn();

            buf = new StringBuffer();
            buf.append("SELECT svc_svct_svr_plat.platform_id, svc_svct_svr_plat.platform_name,")
               .append("       svc_svct_svr_plat.platform_type_id, svc_svct_svr_plat.platform_type_name, ")
               .append("       svc_svct_svr_plat.service_id, svc_svct_svr_plat.service_name, ")
               .append("       svc_svct_svr_plat.service_type_id, svc_svct_svr_plat.service_type_name ")
               .append("FROM   (SELECT app.id as application_id, app.name as application_name,")
               .append("               app.description as application_desc,")
               .append("               appsvc.service_id as service_id ")
               .append("        FROM   EAM_APP_SERVICE appsvc ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8 || DB_TYPE == DBUtil.DATABASE_ORACLE_9 ||
                    DB_TYPE == DBUtil.DATABASE_ORACLE_10) {
                buf.append("                 , EAM_APPLICATION app ")
                   .append("                 WHERE     app.id=appsvc.application_id(+) ")
                   .append("                       AND app.id IN (")
                   .append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc, ");
            } else {
                buf.append("                 RIGHT JOIN EAM_APPLICATION app ")
                   .append("                 ON app.id=appsvc.application_id ")
                   .append("        WHERE app.id IN (")
                   .append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc RIGHT JOIN ");
            }
            buf.append("       (SELECT svc_svct.service_id, svc_svct.service_name, ")
               .append("               svc_svct.service_type_id, svc_svct.service_type_name, ")
               .append("               plat.id as platform_id, plat.name as platform_name, ")
               .append("               platt.id as platform_type_id, platt.name as platform_type_name ")
               .append("               FROM ( SELECT svc.id    as service_id, ")
               .append("                             svc.name  as service_name, ")
               .append("                             svct.id   as service_type_id,")
               .append("                             svct.name as service_type_name,")
               .append("                             svc.server_id as server_id ")
               .append("                      FROM   EAM_SERVICE svc, ")
               .append("                             EAM_SERVICE_TYPE svct ")
               .append("                      WHERE  svc.service_type_id=svct.id ")
               .append("                             AND svc.id IN (")
               .append(getResourceTypeSQL("EAM_SERVICE"))
               .append(") ) svc_svct ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8 || DB_TYPE == DBUtil.DATABASE_ORACLE_9
                    || DB_TYPE == DBUtil.DATABASE_ORACLE_10) {
                buf.append(" ,EAM_SERVER svr, ");
            } else {
                buf.append(" RIGHT JOIN EAM_SERVER svr ON svc_svct.server_id=svr.id, ");
            }
            buf.append("                    EAM_PLATFORM plat, EAM_PLATFORM_TYPE platt ")
               .append("                    WHERE     svr.id= ? AND platt.id=plat.platform_type_id ")
               .append("                          AND plat.id=svr.platform_id ")
               .append("                          AND plat.id IN (")
               .append(getResourceTypeSQL("EAM_PLATFORM"))
               .append(") ");
            
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8 || DB_TYPE == DBUtil.DATABASE_ORACLE_9
                    || DB_TYPE == DBUtil.DATABASE_ORACLE_10) {
                buf.append(" AND svr.id=svc_svct.server_id(+) ")
                   .append("       ) svc_svct_svr_plat ")
                   .append("   WHERE svc_svct_svr_plat.service_id=app_appsvc.service_id(+)");
            } else {
               buf.append("       ) svc_svct_svr_plat ")
                  .append("   ON svc_svct_svr_plat.service_id=app_appsvc.service_id ");
            } 
            buf.append("order by service_type_id ");

            stmt = conn.prepareStatement(buf.toString());
            int pos =
                pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                          AuthzConstants.applicationResType,
                                          AuthzConstants.appOpViewApplication);

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.serviceResType,
                                          AuthzConstants.serviceOpViewService);

            stmt.setInt    (++pos, serverVo.getId().intValue());

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.platformResType,
                                          AuthzConstants.platformOpViewPlatform);

            StopWatch timer = new StopWatch();
            
            rs = stmt.executeQuery();
            
            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForServer() executed in: " +
                          timer);
                log.debug("SQL: " + buf);
                int i = 1;
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + AuthzConstants.applicationResType);
                log.debug("Arg " + (i++) + ": " + AuthzConstants.appOpViewApplication);
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + AuthzConstants.serviceResType);
                log.debug("Arg " + (i++) + ": " + AuthzConstants.serviceOpViewService);
                log.debug("Arg " + (i++) + ": " + serverVo.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + AuthzConstants.platformResType);
                log.debug("Arg " + (i++) + ": " + AuthzConstants.platformOpViewPlatform);
            }

            Map serviceMap       = new HashMap();
            ResourceTreeNode aServerNode;
            ResourceTreeNode aPlatformNode;

            aPlatformNode = null;
            aServerNode = new ResourceTreeNode (
                              serverVo.getName(),
                              getAppdefTypeLabel(AppdefEntityConstants
                                  .APPDEF_TYPE_SERVER,
                                  serverVo.getAppdefResourceType().getName()),
                              new AppdefEntityID(
                                  AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                  serverVo.getId().intValue()),
                              ResourceTreeNode.RESOURCE);

            int    thisPlatformId        = 0;
            String thisPlatformName      = null;
            int    thisPlatformTypeId    = 0;
            String thisPlatformTypeName  = null;
            int    thisServiceId         = 0;
            String thisServiceName       = null;
            int    thisServiceTypeId     = 0;
            String thisServiceTypeName   = null;

            while (rs.next()) {

                if (thisPlatformId == 0) {
                    thisPlatformId        = rs.getInt(1); 
                    thisPlatformName      = rs.getString(2);
                    thisPlatformTypeId    = rs.getInt(3);
                    thisPlatformTypeName  = rs.getString(4);
                    aPlatformNode = 
                        new ResourceTreeNode (
                            thisPlatformName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_PLATFORM,
                                thisPlatformTypeName),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                                thisPlatformId), (AppdefEntityID)null, 
                            thisPlatformTypeId );
                }

                thisServiceId       = rs.getInt(5);
                thisServiceName     = rs.getString(6);
                thisServiceTypeId   = rs.getInt(7);
                thisServiceTypeName = rs.getString(8);

                if (thisServiceName != null) {
                    serviceMap.put(new Integer(thisServiceId),
                        new ResourceTreeNode (
                            thisServiceName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_SERVICE,
                                thisServiceTypeName),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                                thisServiceId), serverVo.getEntityId(),
                            thisServiceTypeId ));
                }
            }

            aServerNode.setSelected(true);
            ResourceTreeNode[] services = (ResourceTreeNode[])serviceMap.values()
                .toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(services,true);
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
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        }
        return retVal;
    }


    /**<p>Return directly connected resource tree for node level service</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject,
                                                      Integer serviceId) 
        throws ServiceNotFoundException, PermissionException {
        Service serviceVo = getServiceMgrLocal().findServiceById(serviceId);

        ResourceTreeNode[] retVal;
        PreparedStatement    stmt;
        ResultSet            rs;
        StringBuffer         buf;

        stmt = null;
        rs = null;
        retVal = null;
        try {
            Connection conn = getDBConn();
            
            String trueStr = DBUtil.getBooleanValue(true, conn);
            buf = new StringBuffer();
            buf.append("SELECT plat.platform_id, platform_name, ")
               .append("       platform_type_name, asvc_svr.server_id, ")
               .append("       asvc_svr.server_name, asvc_svr.server_type_name, ")
               .append("       asvc_svr.application_id, asvc_svr.application_name, ")
               .append("       asvc_svr.application_type_name, fvirtual ")
               .append("FROM (SELECT plat.id as platform_id, " +
                                    "plat.name as platform_name, " +
                                    "platt.name as platform_type_name " +
                             "FROM EAM_PLATFORM_TYPE platt, EAM_PLATFORM plat "+
                             "WHERE plat.platform_type_id=platt.id AND " +
                                   "plat.id IN (")
               .append(getResourceTypeSQL("EAM_PLATFORM"))
               .append(")) plat ");
            
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append(", ");
            } else {
                buf.append("RIGHT JOIN ");
            }
            buf.append("( SELECT asvc.application_id, asvc.application_name, ")
               .append("         asvc.application_type_name, svr.id as server_id, ")
               .append("              svr.name as server_name, ")
               .append("              svrt.name as server_type_name, ")
               .append("              svr.platform_id, fvirtual ")
               .append("       FROM   EAM_SERVER svr ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append(" , ");
            } else {
                buf.append(" RIGHT JOIN ");
            }
            buf.append("              (  SELECT app_appsvc.application_id, app_appsvc.application_name, ")
               .append("                        app_appsvc.application_type_name, svc.server_id as server_id ")
               .append("                 FROM   (SELECT app.id as application_id, app.name as application_name, ")
               .append("                                EAM_APPLICATION_TYPE.name as application_type_name, ")
               .append("                                appsvc.service_id as service_id ")
               .append("                         FROM   EAM_APP_SERVICE appsvc ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append("                            , EAM_APPLICATION app, EAM_APPLICATION_TYPE ")
                   .append("                              WHERE     app.id=appsvc.application_id(+) ")
                   .append("                                    AND EAM_APPLICATION_TYPE.id=app.application_type_id ")
                   .append("                                    AND app.id IN (")
                   .append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc, EAM_SERVICE svc ")
                   .append("                        WHERE svc.id=app_appsvc.service_id(+) AND svc.id=? ) asvc ");
            } else {
                buf.append("                            RIGHT JOIN EAM_APPLICATION app ")
                   .append("                                    ON app.id=appsvc.application_id, ")
                   .append("                                    EAM_APPLICATION_TYPE  ")
                   .append("                            WHERE EAM_APPLICATION_TYPE.id=app.application_type_id AND ")
                   .append("                                  app.id IN (")
                   .append(getResourceTypeSQL("EAM_APPLICATION"))
                   .append(") ) app_appsvc RIGHT JOIN EAM_SERVICE svc ")
                   .append("                            ON svc.id=app_appsvc.service_id ")
                   .append("                        WHERE     svc.id=? ) asvc ");
            }
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append("            ,EAM_SERVER_TYPE svrt ")
                   .append("             WHERE svr.server_type_id=svrt.id ")
                   .append("               AND asvc.server_id=svr.id(+) ")
                   .append("               AND (fvirtual = ").append(trueStr)
                   .append("                OR svr.id IN (")
                   .append(getResourceTypeSQL("EAM_SERVER"))
                   .append(")) ) asvc_svr, ")
                   .append("      EAM_PLATFORM_TYPE platt ")
                   .append("WHERE plat.platform_type_id=platt.id ")
                   .append("  AND asvc_svr.platform_id=plat.id(+) ")
                   .append("  AND plat.id IN (")
                   .append(getResourceTypeSQL("EAM_PLATFORM"))
                   .append(") ");
            } else {
                buf.append("      ON asvc.server_id=svr.id, ")
                   .append("            EAM_SERVER_TYPE svrt ")
                   .append("             WHERE svr.server_type_id=svrt.id ")
                   .append("               AND (fvirtual = ").append(trueStr)
                   .append("                OR svr.id IN (")
                   .append(getResourceTypeSQL("EAM_SERVER"))
                   .append(")) ) asvc_svr ")
                   .append("     ON asvc_svr.platform_id = plat.platform_id");
            }

            stmt = conn.prepareStatement(buf.toString());
            int pos =
                pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                          AuthzConstants.platformResType,
                                          AuthzConstants.platformOpViewPlatform);

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.applicationResType,
                                          AuthzConstants.appOpViewApplication);

            stmt.setInt(++pos, serviceVo.getId().intValue());

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.serverResType,
                                          AuthzConstants.serverOpViewServer);

            StopWatch timer = new StopWatch();
            
            rs = stmt.executeQuery();
            
            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForService() executed in: " + timer);
                log.debug("SQL: " + buf);
                pos = 0;
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " + AuthzConstants.platformResType);
                log.debug("ARG " + ++pos + ": " +
                          AuthzConstants.platformOpViewPlatform);
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " +
                          AuthzConstants.applicationResType);
                log.debug("ARG " + ++pos + ": " +
                          AuthzConstants.appOpViewApplication);
                log.debug("ARG " + ++pos + ": " + serviceVo.getId());
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " + subject.getId());
                log.debug("ARG " + ++pos + ": " + AuthzConstants.serverResType);
                log.debug("ARG " + ++pos + ": " +
                          AuthzConstants.serverOpViewServer);
            }
            
            ResourceTreeNode aPlatformNode = null;
            ResourceTreeNode aServerNode   = null;
            ResourceTreeNode aServiceNode  = null;
            Map              appMap        = new HashMap();

            aServiceNode = new ResourceTreeNode (
                  serviceVo.getName(),
                  getAppdefTypeLabel(AppdefEntityConstants
                      .APPDEF_TYPE_SERVICE,
                      serviceVo.getAppdefResourceType().getName()), 
                  new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                                     serviceVo.getId().intValue()),
                  ResourceTreeNode.RESOURCE);

            while (rs.next()) {

                int i = 1;
                int    thisPlatformId       = rs.getInt(i++); 
                String thisPlatformName     = rs.getString(i++);
                String thisPlatformTypeName = rs.getString(i++);
                int    thisServerId         = rs.getInt(i++); 
                String thisServerName       = rs.getString(i++);
                String thisServerTypeName   = rs.getString(i++);
                int thisApplicationId       = rs.getInt(i++);
                String thisApplicationName  = rs.getString(i++);
                String thisApplicationDesc  = rs.getString(i++);
                String virtualServer        = rs.getString(i++);
                
                if (thisPlatformName != null) {
                    aPlatformNode = new ResourceTreeNode (
                            thisPlatformName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                  .APPDEF_TYPE_PLATFORM,
                                  thisPlatformTypeName), 
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                                thisPlatformId),
                            ResourceTreeNode.RESOURCE);
                }

                if (thisServerName != null &&
                    !trueStr.startsWith(virtualServer)) {
                    aServerNode = new ResourceTreeNode (
                            thisServerName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                  .APPDEF_TYPE_SERVER,
                                  thisServerTypeName), 
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                thisServerId),
                            ResourceTreeNode.RESOURCE);
                }

                if (thisApplicationName != null) {
                    appMap.put( new Integer(thisApplicationId), 
                        new ResourceTreeNode (
                            thisApplicationName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_APPLICATION,
                                thisApplicationDesc),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                                thisApplicationId),
                            ResourceTreeNode.RESOURCE));
                }
            }
            aServiceNode.setSelected(true);

            // server nodes and platform nodes can be null if user is unauthz
            if (aServerNode != null) {
                if (aPlatformNode != null) {
                    aServerNode.addDownChild(aPlatformNode);
                }
                aServiceNode.addDownChild(aServerNode);
            }
            else if (aPlatformNode != null) {
                aServiceNode.addDownChild(aPlatformNode);
            }
            
            ResourceTreeNode[] appNodes = (ResourceTreeNode[])appMap.values()
                                          .toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(appNodes,true);
            aServiceNode.addUpChildren(appNodes);

            retVal = new ResourceTreeNode[] { aServiceNode }; 

        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        }
        return retVal;
    }

    private String mapToString (ResourceTreeNode[] node) {
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

    /**<p>Return directly connected resource tree for node level service</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject,
                                                          Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        ApplicationValue appVo =
            getApplicationMgrLocal().getApplicationById(subject,appId);

        ResourceTreeNode[] retVal;
        PreparedStatement  stmt;
        ResultSet          rs;
        StringBuffer       buf;

        stmt = null;
        rs = null;
        retVal = null;

        try {
            Connection conn = getDBConn();

            buf = new StringBuffer();
            buf.append("SELECT asvclust.service_id, asvclust.service_name, ")
               .append("       asvclust.service_type_id, asvclust.service_type_name, ")
               .append("       group_id, group_name ")
               .append("FROM (SELECT grp.id as group_id, grp.name as group_name, cluster_id ")
               .append("      FROM   EAM_RESOURCE_GROUP grp ")
               .append("      WHERE grp.id IN (")
               .append(getResourceTypeSQL("EAM_RESOURCE_GROUP"))
               .append(") ) grp ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append("     ,( ");
            } else {
                buf.append("     RIGHT JOIN ( ");
            }
            buf.append("       SELECT asvc.service_id, asvc.service_name, ")
               .append("              svct.id as service_type_id, svct.name as service_type_name, ")
               .append("              asvc.server_id, clust.id as cluster_id ")
               .append("       FROM EAM_SVC_CLUSTER clust ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append("    , "); 
            } else {
                buf.append("    RIGHT JOIN ");
            }
            buf.append("         ( SELECT svc.service_id, service_name, server_id, cluster_id, ")
               .append("                               service_type_id ")
               .append("                        FROM  (SELECT  svc.id as service_id, svc.name as service_name, ")
               .append("                               server_id ")
               .append("                               FROM  EAM_SERVICE svc ")
               .append("                               WHERE svc.id IN (")
               .append(getResourceTypeSQL("EAM_SERVICE"))
               .append(") ) svc ");
            if(DB_TYPE == DBUtil.DATABASE_ORACLE_8) {
                buf.append("                  , EAM_APP_SERVICE appsvc ")
                   .append("                    WHERE     appsvc.service_id=svc.service_id (+) ")
                   .append("                          AND appsvc.application_id = ? ")
                   .append("                      ) asvc, ")
                   .append("             EAM_SERVICE_TYPE svct ")
                   .append("            WHERE     svct.id=asvc.service_type_id ")
                   .append("                  AND asvc.cluster_id=clust.id (+) ")
                   .append("     ) asvclust ")
                   .append("   WHERE asvclust.cluster_id = grp.cluster_id (+)");
            } else {
                buf.append("                  RIGHT JOIN EAM_APP_SERVICE appsvc ")
                   .append("                               ON appsvc.service_id=svc.service_id ")
                   .append("                        WHERE appsvc.application_id = ? ")
                   .append("                      ) asvc ")
                   .append("             ON asvc.cluster_id=clust.id, ")
                   .append("             EAM_SERVICE_TYPE svct ")
                   .append("            WHERE svct.id=asvc.service_type_id ")
                   .append("     ) asvclust ")
                   .append("   ON asvclust.cluster_id = grp.cluster_id ");
            }
            buf.append("ORDER BY asvclust.service_type_id,asvclust.service_id ");

            if (log.isDebugEnabled())
                log.debug(buf.toString());
            stmt = conn.prepareStatement(buf.toString());
            int pos =
                pm.prepareResourceTypeSQL(stmt, 0, subject.getId().intValue(),
                                          AuthzConstants.groupResType,
                                          AuthzConstants.groupOpViewResourceGroup);

            pos =
                pm.prepareResourceTypeSQL(stmt, pos, subject.getId().intValue(),
                                          AuthzConstants.serviceResType,
                                          AuthzConstants.serviceOpViewService);

            stmt.setInt(++pos, appVo.getId().intValue());

            StopWatch timer = new StopWatch();
            
            rs = stmt.executeQuery();
            
            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForApplication() executed in: " +
                          timer);
                log.debug("SQL: " + buf);
                int i = 1;
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + AuthzConstants.groupResType);
                log.debug("Arg " + (i++) + ": " + AuthzConstants.groupOpViewResourceGroup);
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + subject.getId());
                log.debug("Arg " + (i++) + ": " + AuthzConstants.serviceResType);
                log.debug("Arg " + (i++) + ": " + AuthzConstants.serviceOpViewService);
                log.debug("Arg " + (i++) + ": " + appVo.getId());
            }

            Map svcMap = new HashMap();

            int    thisServiceId       = 0;
            String thisServiceName     = null;
            int    thisServiceTypeId   = 0;
            String thisServiceTypeName = null;
            int    thisGroupId         = 0;
            String thisGroupName       = null;

            ResourceTreeNode appNode = new ResourceTreeNode (
                appVo.getName(),
                getAppdefTypeLabel(AppdefEntityConstants
                    .APPDEF_TYPE_APPLICATION,
                    appVo.getAppdefResourceTypeValue().getName()), 
                new AppdefEntityID(
                    AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                    appVo.getId().intValue()),
                ResourceTreeNode.RESOURCE);

            while (rs.next()) {
                thisServiceId       = rs.getInt(1);
                thisServiceName     = rs.getString(2);
                thisServiceTypeId   = rs.getInt(3);
                thisServiceTypeName = rs.getString(4);
                thisGroupId         = rs.getInt(5);
                thisGroupName       = rs.getString(6);

                if (thisGroupName != null) {
                    String key = AppdefEntityConstants.APPDEF_TYPE_GROUP+
                                 "-"+thisGroupId;
                    svcMap.put(key, 
                        new ResourceTreeNode (
                            thisGroupName, 
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_GROUP,
                                thisServiceTypeName), 
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                thisGroupId),
                            ResourceTreeNode.CLUSTER));
                } else if (thisServiceName != null) {
                    String key = AppdefEntityConstants.APPDEF_TYPE_SERVICE+
                                 "-"+thisServiceId;
                    svcMap.put(key, 
                        new ResourceTreeNode (
                            thisServiceName,
                            getAppdefTypeLabel(AppdefEntityConstants
                                .APPDEF_TYPE_SERVICE,
                                thisServiceTypeName),
                            new AppdefEntityID(
                                AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                                thisServiceId), 
                            appVo.getEntityId(),
                            thisServiceTypeId));
                }
            }

            appNode.setSelected(true);
            ResourceTreeNode[] svcNodes = (ResourceTreeNode[])
                svcMap.values().toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(svcNodes);
            appNode.addDownChildren(svcNodes);

            retVal = new ResourceTreeNode[] { appNode }; 

        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            disconnect();
        }
        return retVal;
    }

    /**<p>Return resources for autogroups</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForAutoGroup (AuthzSubject subject,
                                                     AppdefEntityID[] parents,
                                                     Integer resType)
        throws AppdefEntityNotFoundException, PermissionException {
        try {
            // platform auto-groups do not have parent resource types
            int entType = (parents!=null) ? 
                getChildEntityType(parents[0].getType()) : 
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM;

            AppdefResourceType artVo = getResourceTypeValue(entType, resType);
            return getNavMapDataForAutoGroup(subject,parents,artVo);
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private AppdefResourceType getResourceTypeValue(int entityType,
                                                    Integer resType)
        throws AppdefEntityNotFoundException {
        switch (entityType) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM):
                return getPlatformMgrLocal().findPlatformType(resType);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER):
                return getServerMgrLocal().findServerType(resType);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVICE):
                return getServiceMgrLocal().findServiceType(resType);
            default:
                return null;
        }
    }

    private ResourceTreeNode[] getNavMapDataForAutoGroup (AuthzSubject subject,
                                                      AppdefEntityID[] parents,
                                                      AppdefResourceType artVo)
        throws AppdefEntityNotFoundException, PermissionException, 
               SQLException {
        ResourceTreeNode[] retVal;
        PreparedStatement  stmt;
        ResultSet          rs;
        int                pEntityType;
        int                cEntityType;
        String             sqlStmt;
        String             bindMarkerStr;
        String             authzResName;
        String             authzOpName;
        final int          APPDEF_TYPE_UNDEFINED = -1;
        List               parentNodes = null;

        stmt = null;
        rs = null;
        retVal = null;
        bindMarkerStr = "";
        // derive parent and child entity types
		pEntityType = (parents != null) ? 
            parents[0].getType() : APPDEF_TYPE_UNDEFINED;
		cEntityType = (pEntityType != APPDEF_TYPE_UNDEFINED) ? 
            getChildEntityType(pEntityType) : 
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM;

        try {

            // If the auto-group has parents, fetch the resources
            if (parents != null) {
                parentNodes = new ArrayList(parents.length);
                for (int x=0;x<parents.length;x++) {
                    AppdefEntityValue av = 
                        new AppdefEntityValue(parents[x],subject);
                    parentNodes.add(
                        new ResourceTreeNode(
                            av.getName(),
                            getAppdefTypeLabel(pEntityType, av.getTypeName()),
                            parents[x],
                            ResourceTreeNode.RESOURCE));
                }
            }

            // Platforms don't have a auto-group parents
            if (pEntityType != APPDEF_TYPE_UNDEFINED) {
                for (int x=0;x<parents.length;x++){
                    bindMarkerStr += (x<parents.length-1) ? "?," : "?";
                }
            }
            Connection conn = getDBConn();

            final String platAGSql =
                "SELECT    p.id as platform_id, p.name as platform_name,            "+
                "          pt.id as platform_type_id, pt.name as platform_type_name "+
                "FROM      EAM_PLATFORM p, EAM_PLATFORM_TYPE pt                     "+
                "WHERE     p.platform_type_id=pt.id AND platform_type_id=?          "+
                "      AND p.id IN (" + getResourceTypeSQL("EAM_PLATFORM") + ") ";

            final String svrAGSql =
                "SELECT    s.id as server_id, s.name as server_name,                "+
                "          st.id as server_type_id, st.name as server_type_name     "+
                "FROM      EAM_SERVER s, EAM_SERVER_TYPE st                         "+
                "WHERE     s.server_type_id=st.id AND platform_id in                "+
                "          ( "+bindMarkerStr+" )                                    "+
                "      AND server_type_id=?                                         "+
                "      AND s.id IN (" + getResourceTypeSQL("EAM_SERVER") + ") ";

            final String svcAGSql = 
                "SELECT    s.id as service_id, s.name as service_name,              "+
                "          st.id as service_type_id, st.name as service_type_name   "+
                "FROM      EAM_SERVICE s, EAM_SERVICE_TYPE st                       "+
                "WHERE     s.service_type_id=st.id AND s.server_id in               "+
                "          ( "+bindMarkerStr+" )                                    "+
                "      AND s.service_type_id=?                                      "+
                "      AND s.id IN (" + getResourceTypeSQL("EAM_SERVICE") + ") ";

            final String appSvcAGSql = 
                "SELECT    s.id as service_id, s.name as service_name,              "+
                "          st.id as service_type_id, st.name as service_type_name   "+
                "FROM      EAM_SERVICE s, EAM_SERVICE_TYPE st, EAM_APP_SERVICE aps  "+
                "WHERE     s.service_type_id=st.id and s.id=aps.service_id          "+
                "          and aps.application_id in ( "+bindMarkerStr+" )          "+
                "      AND s.service_type_id=?                                      "+
                "      AND s.id IN (" + getResourceTypeSQL("EAM_SERVICE") + ") ";

            switch (pEntityType) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM) :
                sqlStmt = svrAGSql;
                authzResName = AuthzConstants.serverResType;
                authzOpName = AuthzConstants.serverOpViewServer;
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER) :
                sqlStmt = svcAGSql;
                authzResName = AuthzConstants.serviceResType;
                authzOpName = AuthzConstants.serviceOpViewService;
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION) :
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
                throw new IllegalArgumentException("No auto-group support "+
                                                   "for specified type");
            }

            if (log.isDebugEnabled())
                log.debug(sqlStmt);

            ResourceTreeNode agNode = 
                new ResourceTreeNode ( artVo.getName(),
                                       getAppdefTypeLabel(cEntityType,
                                           artVo.getName()),
                                       parents,
                                       artVo.getId().intValue(),
                                       ResourceTreeNode.AUTO_GROUP);
            Set entitySet = new HashSet();
            int x=0;
            try {
                stmt = conn.prepareStatement(sqlStmt);
            
                if (pEntityType != APPDEF_TYPE_UNDEFINED) {
                    for (; x < parents.length; x++) {
                        stmt.setInt(x + 1, parents[x].getID());
                    }
                }
                stmt.setInt (++x, artVo.getId().intValue());

                pm.prepareResourceTypeSQL(stmt, x,
                                          subject.getId().intValue(),
                                          authzResName,
                                          authzOpName);
                
                StopWatch timer = new StopWatch();
                
                rs = stmt.executeQuery();
                
                if (log.isDebugEnabled()) {
                    log.debug("getNavMapDataForAutoGroup() executed in: " +
                              timer);
                    log.debug("SQL: " + sqlStmt);
                    int i;
                    for (i = 0; i < parents.length; i++) {
                        log.debug("Arg " + (i+1) + ": " + parents[x].getID());
                    }
                    log.debug("Arg " + (i++) + ": " + artVo.getId());
                    log.debug("Arg " + (i++) + ": " + subject.getId());
                    log.debug("Arg " + (i++) + ": " + subject.getId());
                    log.debug("Arg " + (i++) + ": " + authzResName);
                    log.debug("Arg " + (i++) + ": " + authzOpName);
                }
                
                while (rs.next()) {
                    int     thisEntityId       = rs.getInt(1);
                    String  thisEntityName     = rs.getString(2);
                    int     thisEntityTypeId   = rs.getInt(3);
                    String  thisEntityTypeName = rs.getString(4);

                    entitySet.add(
                        new ResourceTreeNode (
                            thisEntityName,
                            getAppdefTypeLabel(cEntityType,
                                thisEntityTypeName),
                            new AppdefEntityID(cEntityType,thisEntityId),
                            ResourceTreeNode.RESOURCE));
                }

                agNode.setSelected(true);
                if (parentNodes != null) {
                    ResourceTreeNode[] parNodeArr = (ResourceTreeNode[])
                        parentNodes.toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(parNodeArr,true);
                    agNode.addUpChildren(parNodeArr);
                }

                ResourceTreeNode[] members = (ResourceTreeNode[])
                    entitySet.toArray(new ResourceTreeNode[0]);

                ResourceTreeNode.alphaSortNodes(members);
                agNode.addDownChildren(members);

                retVal = new ResourceTreeNode[] { agNode }; 

            } finally {
                DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            disconnect();
        }
        return retVal;
    }

    /**<p>Return resources for groups (not autogroups)</p>
     * @ejb:interface-method
     * @ejb:transaction type="Supports"
     */
    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject,
                                                    Integer groupId)
        throws AppdefEntityNotFoundException, PermissionException {
        try {
            AppdefGroupValue groupVo = 
                getAppdefGroupManagerLocal().findGroup(subject,groupId);

            return getNavMapDataForGroup(subject.getAuthzSubjectValue(),
                                         groupVo);
        } catch (SQLException e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private ResourceTreeNode[] getNavMapDataForGroup (
        AuthzSubjectValue subject, AppdefGroupValue groupVo)
        throws PermissionException, SQLException {
        ResourceTreeNode[] retVal;
        PreparedStatement    stmt;
        ResultSet            rs;
        String               grpSqlStmt;
        int                  entityType;
        String               bindMarkerStr;
        ResourceTreeNode     grpNode;
        Set                  entitySet;
        
        stmt = null;
        rs = null;
        retVal = null;
        bindMarkerStr = "";
        entityType = groupVo.getGroupEntType();

        try {
            int size = groupVo.getTotalSize();
            for (int x=0;x<size;x++){
                bindMarkerStr += (x<size-1) ? "?," : "?";
            }

            Connection conn = getDBConn();

            final String grpPlatSql = 
                "SELECT    p.id as platform_id, p.name as platform_name      "+
                "FROM      EAM_PLATFORM p                                    "+       
                "WHERE     p.id IN ("+bindMarkerStr+")                       ";

            final String grpSvrSql = 
                "SELECT    s.id as server_id, s.name as server_name           "+
                "FROM      EAM_SERVER s                                       "+       
                "WHERE     s.id IN ("+bindMarkerStr+")                        ";

            final String grpSvcSql = 
                "SELECT    s.id as service_id, s.name as service_name         "+
                "FROM      EAM_SERVICE s                                      "+       
                "WHERE     s.id IN ("+bindMarkerStr+")                        ";

            switch (entityType) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM) :
                grpSqlStmt = grpPlatSql;
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER) :
                grpSqlStmt = grpSvrSql;
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_SERVICE):
                grpSqlStmt = grpSvcSql;
                break;
            default:
                throw new IllegalArgumentException("No group support "+
                                                   "for specified type");
            }

            if (log.isDebugEnabled())
                log.debug(grpSqlStmt);
            grpNode = new ResourceTreeNode (groupVo.getName(),
                                            getAppdefTypeLabel(AppdefEntityConstants
                                                .APPDEF_TYPE_GROUP,
                                                groupVo.getAppdefResourceTypeValue()
                                                   .getName()),
                                            groupVo.getEntityId(),
                                            ResourceTreeNode.CLUSTER);
            entitySet = new HashSet();

            int x;
            Iterator i;
            Map entNameMap = new HashMap();
            if (groupVo.getTotalSize() > 0) {
                try {
                    stmt = conn.prepareStatement(grpSqlStmt);
                    
                    if (log.isDebugEnabled())
                        log.debug("SQL: " + grpSqlStmt);
                    
                    for (x=1,i=groupVo.getAppdefGroupEntries().iterator();
                         i.hasNext();x++) {
                        AppdefEntityID mem = (AppdefEntityID) i.next();
                        stmt.setInt (x, mem.getID());

                        if (log.isDebugEnabled())
                            log.debug("Arg " + x + ": " + mem.getID());
                    }

                    StopWatch timer = new StopWatch();
                    
                    rs = stmt.executeQuery();
                    
                    if (log.isDebugEnabled())
                        log.debug("getNavMapDataForGroup() executed in: " +
                                  timer);

                    while (rs.next()) {
                        int     thisEntityId       = rs.getInt(1);
                        String  thisEntityName     = rs.getString(2);
                        entNameMap.put(new Integer(thisEntityId),
                                       thisEntityName);
                    }
                } finally {
                    DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
                }

                // Let group member order drive node creation (not db order).
                for (i=groupVo.getAppdefGroupEntries().iterator();i.hasNext();) {
                    AppdefEntityID id = (AppdefEntityID)i.next();
                    entitySet.add (
                        new ResourceTreeNode (
                            (String)entNameMap.get(id.getId()),
                            getAppdefTypeLabel(id.getType(),
                                groupVo.getAppdefResourceTypeValue().getName()),
                            new AppdefEntityID(entityType,id.getID()),
                            ResourceTreeNode.RESOURCE));
                }
            }

            ResourceTreeNode[] memberNodes = (ResourceTreeNode[])
                entitySet.toArray(new ResourceTreeNode[0]);

            grpNode.setSelected(true);
            ResourceTreeNode.alphaSortNodes(memberNodes);
            grpNode.addDownChildren(memberNodes);

            retVal = new ResourceTreeNode[] { grpNode }; 

        } catch (SQLException e) {
            throw e;
        } finally {
            disconnect();
        }
        return retVal;
    }

    // The methods in this class should call getDBConn() to obtain a connection,
    // because it also initializes the private database-related variables
    private Connection getDBConn() throws SQLException {
        Connection conn = new PlatformDAO(DAOFactory.getDAOFactory())
            .getSession().connection();
        
        if (DB_TYPE == -1) {
            DB_TYPE = DBUtil.getDBType(conn);
        }
        
        return conn;
    }
    
    private void disconnect() {
        new PlatformDAO(DAOFactory.getDAOFactory()).getSession().disconnect();
    }

    private int getChildEntityType (int type) {
        switch (type) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        default:
            return type;
        }
    }

    private String getAppdefTypeLabel(int typeId, String desc) {
        String typeLabel = AppdefEntityConstants.typeToString(typeId);
        if (desc == null) {
            desc = typeLabel;
        }
        else if (desc.toLowerCase().indexOf(typeLabel.toLowerCase()) == -1) {
            desc += " " + typeLabel;
        }
        return desc;
    }

    public static AppdefStatManagerLocal getOne() {
        try {
            return AppdefStatManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void setSessionContext(javax.ejb.SessionContext ctx) {}
    public void ejbCreate() throws CreateException {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
} 
