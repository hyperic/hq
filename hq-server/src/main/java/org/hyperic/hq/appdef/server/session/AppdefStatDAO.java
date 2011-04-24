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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppdefConverter;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
    
    protected AppdefConverter appdefConverter;

    protected final Log log = LogFactory.getLog(AppdefStatDAO.class);
    
    protected AppdefStatDAO() {
        super();
    }

    @Autowired
    public AppdefStatDAO(JdbcTemplate jdbcTemplate, AppdefConverter appdefConverter) {
        this.jdbcTemplate = jdbcTemplate;
        this.appdefConverter = appdefConverter;
    }

    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject)
        throws SQLException {
        final Map<String, Integer> platMap = new HashMap<String, Integer>();

        String sql = "SELECT PLATT.NAME, COUNT(PLAT.ID) " +
                     "FROM " +
                     TBL_PLATFORM +
                     "_TYPE PLATT, " +
                     TBL_PLATFORM +
                     " PLAT " +
                     "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID AND EXISTS (" +
                     getResourceTypeSQL("PLAT.ID", subject.getId(), PLATFORM_RES_TYPE,
                         PLATFORM_OP_VIEW_PLATFORM) + ") " +
                     "GROUP BY PLATT.NAME ORDER BY PLATT.NAME";

        if (log.isDebugEnabled()) {
            log.debug(sql);
        }

        this.jdbcTemplate.query(sql, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
               platMap.put(rs.getString(1), rs.getInt(2));
            }            
        });
 
        return platMap;
    }

    public int getPlatformsCount(AuthzSubject subject) throws SQLException {
        String sql = "SELECT COUNT(PLAT.ID) " +
                     "FROM " +
                     TBL_PLATFORM +
                     "_TYPE PLATT, " +
                     TBL_PLATFORM +
                     " PLAT " +
                     "WHERE PLAT.PLATFORM_TYPE_ID = PLATT.ID AND EXISTS (" +
                     getResourceTypeSQL("PLAT.ID", subject.getId(), PLATFORM_RES_TYPE,
                         PLATFORM_OP_VIEW_PLATFORM) + ")";

        if (log.isDebugEnabled()) {
            log.debug(sql);
        }
        return jdbcTemplate.queryForInt(sql);
    }

    public Map<String, Integer> getServerCountsByTypeMap(AuthzSubject subject) throws SQLException {
        final Map<String, Integer> servMap = new HashMap<String, Integer>();
        String sql = "SELECT SERVT.NAME, COUNT(SERV.ID) " +
                     "FROM " +
                     TBL_SERVER +
                     "_TYPE SERVT, " +
                     TBL_SERVER +
                     " SERV " +
                     "WHERE SERV.SERVER_TYPE_ID = SERVT.ID AND EXISTS (" +
                     getResourceTypeSQL("SERV.ID", subject.getId(), SERVER_RES_TYPE,
                         SERVER_OP_VIEW_SERVER) + ") " + "GROUP BY SERVT.NAME ORDER BY SERVT.NAME";

        this.jdbcTemplate.query(sql, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
              servMap.put(rs.getString(1), rs.getInt(2));
            }
        });
      
        return servMap;
    }

    public int getServersCount(AuthzSubject subject) throws SQLException {
        String sql = "SELECT COUNT(SERV.ID) " +
                     "FROM " +
                     TBL_SERVER +
                     "_TYPE SERVT, " +
                     TBL_SERVER +
                     " SERV " +
                     "WHERE SERV.SERVER_TYPE_ID = SERVT.ID AND EXISTS (" +
                     getResourceTypeSQL("SERV.ID", subject.getId(), SERVER_RES_TYPE,
                         SERVER_OP_VIEW_SERVER) + ") ";
        return jdbcTemplate.queryForInt(sql);
    }

    public Map<String, Integer> getServiceCountsByTypeMap(AuthzSubject subject) throws SQLException {
        final Map<String, Integer> servMap = new HashMap<String, Integer>();
        String sql = "SELECT SVCT.NAME, COUNT(SVC.ID) " +
                     "FROM " +
                     TBL_SERVICE +
                     "_TYPE SVCT, " +
                     TBL_SERVICE +
                     " SVC " +
                     "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID AND EXISTS (" +
                     getResourceTypeSQL("SVC.ID", subject.getId(), SERVICE_RES_TYPE,
                         SERVICE_OP_VIEW_SERVICE) + ") " + "GROUP BY SVCT.NAME ORDER BY SVCT.NAME";

        this.jdbcTemplate.query(sql, new RowCallbackHandler() {

            public void processRow(ResultSet rs) throws SQLException {
                servMap.put(rs.getString(1), rs.getInt(2));
            }
            
        });
        return servMap;
    }

    public int getServicesCount(AuthzSubject subject) throws SQLException {
        String sql = "SELECT COUNT(SVC.ID) " +
                     "FROM " +
                     TBL_SERVICE +
                     "_TYPE SVCT, " +
                     TBL_SERVICE +
                     " SVC " +
                     "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID AND EXISTS (" +
                     getResourceTypeSQL("SVC.ID", subject.getId(), SERVICE_RES_TYPE,
                         SERVICE_OP_VIEW_SERVICE) + ") ";
        return jdbcTemplate.queryForInt(sql);
    }

    public Map<String, Integer> getApplicationCountsByTypeMap(AuthzSubject subject)
        throws SQLException {
        final Map<String, Integer> appMap = new HashMap<String, Integer>();
        String sql = "SELECT APPT.NAME, COUNT(APP.ID) " +
                     "FROM " +
                     TBL_APP +
                     "_TYPE APPT, " +
                     TBL_APP +
                     " APP " +
                     "WHERE APP.APPLICATION_TYPE_ID = APPT.ID AND EXISTS (" +
                     getResourceTypeSQL("APP.ID", subject.getId(), APPLICATION_RES_TYPE,
                         APPLICATION_OP_VIEW_APPLICATION) + ") " +
                     "GROUP BY APPT.NAME ORDER BY APPT.NAME";
        this.jdbcTemplate.query(sql, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                appMap.put(rs.getString(1), rs.getInt(2));
            }
        });
      
        return appMap;
    }

    public int getApplicationsCount(AuthzSubject subject) throws SQLException {
        String sql = "SELECT COUNT(APP.ID) FROM " +
                     TBL_APP +
                     "_TYPE APPT, " +
                     TBL_APP +
                     " APP " +
                     "WHERE APP.APPLICATION_TYPE_ID = APPT.ID AND EXISTS (" +
                     getResourceTypeSQL("APP.ID", subject.getId(), APPLICATION_RES_TYPE,
                         APPLICATION_OP_VIEW_APPLICATION) + ") ";
        return jdbcTemplate.queryForInt(sql);
    }

    public Map<Integer, Integer> getGroupCountsMap(AuthzSubject subject) throws SQLException {
        Map<Integer, Integer> grpMap = new HashMap<Integer, Integer>();
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();

        for (int x = 0; x < groupTypes.length; x++) {
            String sql = "SELECT COUNT(*) FROM " +
                         TBL_GROUP +
                         " GRP " +
                         "WHERE GRP.GROUPTYPE = " +
                         groupTypes[x] +
                         " AND EXISTS (" +
                         getResourceTypeSQL("GRP.ID", subject.getId(), GROUP_RES_TYPE,
                             GROUP_OP_VIEW_RESOURCE_GROUP) + ")";
            int total = jdbcTemplate.queryForInt(sql);
            grpMap.put(new Integer(groupTypes[x]), total);
        }
        return grpMap;
    }

    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject,
                                                          final Application app) {
        
        StopWatch timer = new StopWatch();
        ResourceTreeNode appNode = new ResourceTreeNode(app.getName(),
            getAppdefTypeLabel(app.getEntityId().getType(), app.getAppdefResourceType()
                .getName()), app.getEntityId(), ResourceTreeNode.RESOURCE);
        
        Map<String, ResourceTreeNode> svcMap = new HashMap<String, ResourceTreeNode>();
        for(AppService svc : app.getAppServices()) {
            String key = APPDEF_TYPE_SERVICE + "-" + svc.getId();
            svcMap.put(key, new ResourceTreeNode(svc.getService().getName(), getAppdefTypeLabel(
                APPDEF_TYPE_SERVICE, svc.getService().getServiceType().getName()), AppdefEntityID
                .newServiceID(svc.getId()), app.getEntityId(),
                svc.getService().getServiceType().getId()));
        }
        appNode.setSelected(true);
        ResourceTreeNode[] svcNodes = (ResourceTreeNode[]) svcMap.values().toArray(
            new ResourceTreeNode[0]);
        ResourceTreeNode.alphaSortNodes(svcNodes);
        appNode.addDownChildren(svcNodes);

        ResourceTreeNode[] appNodes = new ResourceTreeNode[] { appNode };

        if (log.isDebugEnabled()) {
            log.debug("getNavMapDataForApplication() executed in: " + timer);
        }
        return appNodes;
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
                    ResourceTreeNode[] parNodeArr = (ResourceTreeNode[]) parentNodeList
                        .toArray(new ResourceTreeNode[0]);
                    ResourceTreeNode.alphaSortNodes(parNodeArr, true);
                    agNode.addUpChildren(parNodeArr);
                }

                ResourceTreeNode[] members = (ResourceTreeNode[]) entitySet
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

    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, ResourceGroup group, AppdefGroupValue groupVo)
        throws PermissionException {
        ResourceTreeNode grpNode = new ResourceTreeNode(groupVo.getName(), getAppdefTypeLabel(
            APPDEF_TYPE_GROUP, groupVo.getAppdefResourceTypeValue().getName()), groupVo
            .getEntityId(), ResourceTreeNode.CLUSTER);
        
        if (group.getMembers().size() == 0) {
            return new ResourceTreeNode[] { grpNode };
        }
        int entityType = groupVo.getGroupEntType();

        Set<ResourceTreeNode> entitySet = new HashSet<ResourceTreeNode>(group.getMembers().size());
        for (Resource member: group.getMembers()) {
            AppdefEntityID resourceId = appdefConverter.newAppdefEntityId(member);
            entitySet
                .add(new ResourceTreeNode(member.getName(),
                    getAppdefTypeLabel(resourceId.getType(), groupVo.getAppdefResourceTypeValue()
                        .getName()), new AppdefEntityID(entityType, resourceId.getId()),
                    ResourceTreeNode.RESOURCE));
        }

        ResourceTreeNode[] memberNodes = entitySet.toArray(new ResourceTreeNode[0]);

        grpNode.setSelected(true);
        ResourceTreeNode.alphaSortNodes(memberNodes);
        grpNode.addDownChildren(memberNodes);

        ResourceTreeNode[] retVal = new ResourceTreeNode[] { grpNode };

        return retVal;
    }
   
    protected String getResourceTypeSQL(String instanceId, Integer subjectId, String resType,
                                      String op) throws SQLException {
        return "SELECT RES.ID FROM EAM_RESOURCE RES, " + " EAM_RESOURCE_TYPE RT " + "WHERE " +
               instanceId + " = RES.INSTANCE_ID " + "  AND RES.FSYSTEM = " +
               DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource().getConnection()) +
               "  AND RES.RESOURCE_TYPE_ID = RT.ID " + "  AND RT.NAME = '" + resType + "'";
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
