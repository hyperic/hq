package org.hyperic.hq.appdef.server.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
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

    private JdbcTemplate jdbcTemplate;

    private final Log log = LogFactory.getLog(AppdefStatDAO.class);

    @Autowired
    public AppdefStatDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject)
        throws SQLException {
        Map<String, Integer> platMap = new HashMap<String, Integer>();

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

        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : rows) {
            platMap.put((String) row.get("NAME"), new Integer(((Long) row.get("COUNT(PLAT.ID)"))
                .intValue()));
        }

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
        Map<String, Integer> servMap = new HashMap<String, Integer>();
        String sql = "SELECT SERVT.NAME, COUNT(SERV.ID) " +
                     "FROM " +
                     TBL_SERVER +
                     "_TYPE SERVT, " +
                     TBL_SERVER +
                     " SERV " +
                     "WHERE SERV.SERVER_TYPE_ID = SERVT.ID AND EXISTS (" +
                     getResourceTypeSQL("SERV.ID", subject.getId(), SERVER_RES_TYPE,
                         SERVER_OP_VIEW_SERVER) + ") " + "GROUP BY SERVT.NAME ORDER BY SERVT.NAME";

        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : rows) {
            servMap.put((String) row.get("NAME"), new Integer(((Long) row.get("COUNT(SERV.ID)"))
                .intValue()));
        }
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
        Map<String, Integer> servMap = new HashMap<String, Integer>();
        String sql = "SELECT SVCT.NAME, COUNT(SVC.ID) " +
                     "FROM " +
                     TBL_SERVICE +
                     "_TYPE SVCT, " +
                     TBL_SERVICE +
                     " SVC " +
                     "WHERE SVC.SERVICE_TYPE_ID = SVCT.ID AND EXISTS (" +
                     getResourceTypeSQL("SVC.ID", subject.getId(), SERVICE_RES_TYPE,
                         SERVICE_OP_VIEW_SERVICE) + ") " + "GROUP BY SVCT.NAME ORDER BY SVCT.NAME";

        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : rows) {
            servMap.put((String) row.get("NAME"), new Integer(((Long) row.get("COUNT(SVC.ID)"))
                .intValue()));
        }
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
        Map<String, Integer> appMap = new HashMap<String, Integer>();
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
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql);
        for (Map<String, Object> row : rows) {
            appMap.put((String) row.get("NAME"), new Integer(((Long) row.get("COUNT(APP.ID)"))
                .intValue()));
        }
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

    public Set<ResourceTreeNode> foo(AuthzSubject subject, final Platform plat) throws SQLException {
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

        Set<ResourceTreeNode> servers = this.jdbcTemplate.query(buf.toString(),
            new ResultSetExtractor<Set<ResourceTreeNode>>() {
                public Set<ResourceTreeNode> extractData(ResultSet rs) throws SQLException,
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
                    return servers;
                }
            });
        return servers;
    }

    private String getResourceTypeSQL(String instanceId, Integer subjectId, String resType,
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
}
