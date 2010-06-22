package org.hyperic.hq.appdef.server.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private String getResourceTypeSQL(String instanceId, Integer subjectId, String resType,
                                      String op) throws SQLException {
        return "SELECT RES.ID FROM EAM_RESOURCE RES, " + " EAM_RESOURCE_TYPE RT " + "WHERE " +
               instanceId + " = RES.INSTANCE_ID " + "  AND RES.FSYSTEM = " +
               DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource().getConnection()) +
               "  AND RES.RESOURCE_TYPE_ID = RT.ID " + "  AND RT.NAME = '" + resType + "'";
    }
}
