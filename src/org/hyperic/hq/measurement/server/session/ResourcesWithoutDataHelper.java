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

package org.hyperic.hq.measurement.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.MiniResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.math.MathUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used by the "Problem Resources" portlet, aka the "Things to Do" portlet.
 *
 * A singleton class used to retrieve resources that have not 
 * had any metrics collected in a while.  This is in a separate
 * class because it's got some dependencies (appdef, authz) that
 * aren't generally found in this package.  So if we want, we
 * can move this class elsewhere.
 *
 * Update: this class is also used to locate resources that are
 * configured for metric collectiom but have no metrics turned on.
 */
public class ResourcesWithoutDataHelper {
    private final String logCtx = ResourcesWithoutDataHelper.class.getName();
    private final Log log = LogFactory.getLog(logCtx);

    private InitialContext    initialContext = null;
    private static final PermissionManager pm =
        PermissionManagerFactory.getInstance();

    private static final ResourcesWithoutDataHelper _instance
        = new ResourcesWithoutDataHelper();
    private ResourcesWithoutDataHelper () {}
    public static ResourcesWithoutDataHelper instance () { return _instance; }

    // We don't worry about resources that were just recently created, because
    // they probably haven't had any time to get metrics in yet.  Let's give 
    // them a 15-minute window to get their first metrics in.
    public static final long MIN_CREATE_OFFSET = 15 * 60 * 1000;

    // We don't worry about newly created resources that have
    // no default metrics yet -- the MDB may not have enabled them yet.  Give 
    // the MDB 90 seconds to get the job done.  Note that modified resources
    // that have had their config setup but have not yet been default-enabled
    // will still show up in the "things to do" portlet until the enabler-MDB
    // gets to them.  We could query based on MTIME instead of CTIME, but
    // then anything that had been modified in any way would briefly disappear
    // from the portlet, regardless of whether its config had changed or not.
    // It's probably not a very common case, let's not deal with it.
    public static final long MIN_METRIC_CREATE_OFFSET = 90 * 1000;

    private static final String SQL_PLATFORMS_WITHOUT_DATA
        = "SELECT authz_r.ID, p.ID, t.NAME AS TNAME, p.NAME, p.CTIME "
        + "FROM EAM_PLATFORM p, EAM_PLATFORM_TYPE t " + PermissionManager.AUTHZ_FROM
        + " WHERE p.ID = authz_r.INSTANCE_ID "
        +   " AND authz_r.RESOURCE_TYPE_ID = " + AuthzConstants.authzPlatform
        +   " AND p.ID IN ( %%OUTOFSYNC%% ) "
        +   " AND p.PLATFORM_TYPE_ID = t.ID "
        +   " AND p.CTIME < ? "
        +   " AND authz_r.ID NOT IN ( %%IGNORELIST%% ) ";

    private static final String SQL_SERVERS_WITHOUT_DATA
        = "SELECT authz_r.ID, s.ID, t.NAME AS TNAME, s.NAME, s.CTIME "
        + "FROM EAM_PLATFORM p, EAM_SERVER s, EAM_SERVER_TYPE t " 
        +  PermissionManager.AUTHZ_FROM
        + " WHERE s.ID = authz_r.INSTANCE_ID "
        +   " AND authz_r.RESOURCE_TYPE_ID = " + AuthzConstants.authzServer
        +   " AND s.ID IN ( %%OUTOFSYNC%% ) "
        +   " AND s.CTIME < ? "
        +   " AND authz_r.ID NOT IN ( %%IGNORELIST%% ) "
        +   " AND s.PLATFORM_ID = p.ID "
        +   " AND s.SERVER_TYPE_ID = t.ID "
        +   " AND p.ID NOT IN ( %%EXCLUDES1%% ) ";

    private static final String SQL_SERVICES_WITHOUT_DATA
        = "SELECT authz_r.ID, svc.ID, t.NAME AS TNAME, svc.NAME, svc.CTIME "
        + "FROM EAM_PLATFORM p, EAM_SERVER s, "
        +      "EAM_SERVICE svc, EAM_SERVICE_TYPE t " 
        + PermissionManager.AUTHZ_FROM
        + " WHERE svc.ID = authz_r.INSTANCE_ID "
        +   " AND authz_r.RESOURCE_TYPE_ID = " + AuthzConstants.authzService
        +   " AND svc.ID IN ( %%OUTOFSYNC%% ) "
        +   " AND svc.CTIME < ? "
        +   " AND authz_r.ID NOT IN ( %%IGNORELIST%% ) "
        +   " AND svc.SERVER_ID = s.ID "
        +   " AND svc.SERVICE_TYPE_ID = t.ID "
        +   " AND s.PLATFORM_ID = p.ID "
        +   " AND s.ID NOT IN ( %%EXCLUDES1%% ) "
        +   " AND p.ID NOT IN ( %%EXCLUDES2%% ) ";

    /**
     * Get the list of resources that have not collected any metrics in 3
     * times the shortest collection interval for that resource.
     * Note that we roll-up the results according to 
     * platform->server->service relationship.  That is, if a platform
     * is in the return list, none of its servers will be.  And if a 
     * server is in the return list, none of its services will be.
     * @param ignores a List of Integers representing resource IDs to
     * not return in the results.
     * @return a List of MiniResourceValue objects representing the
     * resources.
     */
    public List getResourcesWithoutData(AuthzSubjectValue subject,
                                        List ignores) {
        // First get all the out-of-sync1 resources from the SRNCache
        SRNManagerLocal mgr;
        try {
            mgr = SRNManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            log.error("Unable to lookup SRNManager.", e);
            return new ArrayList();
        }

        List results = mgr.getOutOfSyncEntities();

        // Now, filter these into platforms, servers, and services
        List OOSplatforms = new ArrayList();
        //List OOSservers = new ArrayList();
        //List OOSservices = new ArrayList();
        AppdefEntityID id;
        for (int i=0; i<results.size(); i++) {
            id = (AppdefEntityID) results.get(i);
            switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                OOSplatforms.add(id.getId()); break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                /*OOSservers.add(id.getId());*/ break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                /*OOSservices.add(id.getId());*/ break;
            default:
                log.warn("Unknown out-of-sync entity: " + id); break;
            }
        }
        results.clear();

        if (ignores == null ) ignores = new ArrayList();
        if (ignores.size() == 0) ignores.add(MathUtil.NEGATIVE_ONE);

        boolean doPlatforms = !OOSplatforms.isEmpty();
        boolean doServers   = /*!OOSservers.isEmpty()*/ false;
        boolean doServices  = /*!OOSservices.isEmpty()*/ false;

        String platformSQL = null, serverSQL = null, serviceSQL = null; 
        if (doPlatforms) {
            platformSQL = SQL_PLATFORMS_WITHOUT_DATA
                + pm.getSQLWhere(subject.getId(), "p.ID");
            platformSQL = StringUtil.replace(platformSQL, "%%IGNORELIST%%",
                                             StringUtil.listToString(ignores));
            platformSQL = StringUtil.replace(platformSQL, "%%OUTOFSYNC%%",
                                             StringUtil.listToString(OOSplatforms));
        }
            
        if (doServers) {
            serverSQL = SQL_SERVERS_WITHOUT_DATA
                + pm.getSQLWhere(subject.getId(), "s.ID");
            serverSQL = StringUtil.replace(serverSQL, "%%IGNORELIST%%",
                                           StringUtil.listToString(ignores));
            //serverSQL = StringUtil.replace(serverSQL, "%%OUTOFSYNC%%",
            //                               StringUtil.listToString(OOSservers));
        }

        if (doServices) {
            serviceSQL = SQL_SERVICES_WITHOUT_DATA
                + pm.getSQLWhere(subject.getId(), "svc.ID");
            serviceSQL = StringUtil.replace(serviceSQL, "%%IGNORELIST%%",
                                            StringUtil.listToString(ignores));
            //serviceSQL = StringUtil.replace(serviceSQL, "%%OUTOFSYNC%%",
            //                                StringUtil.listToString(OOSservices));
        }

        StringBuffer platformIds = null;
        StringBuffer serverIds = null;
        Connection conn = null;
        try {
            conn = getDBConn();

            // Lookup platforms - platformIds will get filled out and
            // results will get appended to
            if (doPlatforms) {
                platformIds = new StringBuffer();
                noDataQuery ( platformSQL, conn, subject.getId(),
                              AuthzConstants.authzPlatform, 
                              AuthzConstants.perm_modifyPlatform,
                              platformIds, null, null, results,
                              AppdefEntityConstants.APPDEF_TYPE_PLATFORM );
                // log.info("TRACKER: after platform query, results=" + StringUtil.listToString(results));
            } else {
                platformIds = new StringBuffer("-1");
            }

            // Lookup servers, excluding servers on platforms already found.
            // serverIds will get filled out; more results will be appended
            if (doServers) {
                serverIds = new StringBuffer();
                noDataQuery ( serverSQL, conn, subject.getId(),
                              AuthzConstants.authzServer, 
                              AuthzConstants.perm_modifyServer,
                              serverIds, platformIds, null, results,
                              AppdefEntityConstants.APPDEF_TYPE_SERVER );
                // log.info("TRACKER: after server query, results=" + StringUtil.listToString(results));
            } else {
                serverIds = new StringBuffer("-1");
            }

            // Lookup services, excluding services on servers already found
            if (doServices) {
                noDataQuery ( serviceSQL, conn, subject.getId(),
                              AuthzConstants.authzService, 
                              AuthzConstants.perm_modifyService,
                              null, serverIds, platformIds, results,
                              AppdefEntityConstants.APPDEF_TYPE_SERVICE );
                // log.info("TRACKER: after service query, results=" + StringUtil.listToString(results));
            }

            // log.info("TRACKER: FINAL results=" + StringUtil.listToString(results));
            return results;

        } catch (SQLException e) {
            throw new SystemException("Error in getResourcesWithoutData: "
                                         + e, e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    private void noDataQuery (String sql, Connection conn, Integer subjectId,
                              Integer authzType, Integer authzPerm,
                              StringBuffer ids, 
                              StringBuffer excludeIds1, 
                              StringBuffer excludeIds2, 
                              List results, int appdefType) 
        throws SQLException {

        int col, resId, id, ps_idx = 1;
        ResultSet rs = null;
        PreparedStatement ps = null;
        if (excludeIds1 != null) {
            sql = StringUtil.replace(sql, "%%EXCLUDES1%%",
                                     excludeIds1.toString());
        }
        if (excludeIds2 != null) {
            sql = StringUtil.replace(sql, "%%EXCLUDES2%%",
                                     excludeIds2.toString());
        }
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(ps_idx++,
                       System.currentTimeMillis() - MIN_CREATE_OFFSET);
            ps_idx = pm.prepareSQL(ps, ps_idx, 
                                   subjectId,
                                   authzType,
                                   authzPerm);
            rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                resId = rs.getInt(col++);
                id    = rs.getInt(col++);
                if (ids != null) ids.append(id).append(",");
                results.add(new MiniResourceValue(resId,
                                                  id,
                                                  appdefType,
                                                  rs.getString(col++),
                                                  rs.getString(col++),
                                                  rs.getLong(col++)));
            }
            // Since ids either (a) ends with a comma, or (b) is empty,
            // make sure it will be legal to put in an IN (...) clause later
            // by appending a -1
            if (ids != null) ids.append("-1");

        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, ps);
        }
    }

    public static final String SQL_PLATFORMS_WITHOUT_METRICS
        = "SELECT authz_r.ID, p.ID, t.NAME AS TNAME, p.NAME, p.CTIME "
        + "FROM EAM_PLATFORM p, EAM_PLATFORM_TYPE t, EAM_CONFIG_RESPONSE cr " 
        +  PermissionManager.AUTHZ_FROM
        + "WHERE p.CONFIG_RESPONSE_ID = cr.ID "
        +  " AND cr.PRODUCT_RESPONSE IS NOT NULL "
        +  " AND cr.MEASUREMENT_RESPONSE IS NOT NULL "
        +  " AND authz_r.ID NOT IN ( %%IGNORELIST%% ) "
        +  " AND p.CTIME < ? "
        +  " AND p.PLATFORM_TYPE_ID = t.ID "
        +  " AND p.ID NOT IN "
        +   "(SELECT pl.ID "
        +      "FROM EAM_PLATFORM pl, "
        +           "EAM_MONITORABLE_TYPE mtype, EAM_MEASUREMENT m, "
        +           "EAM_MEASUREMENT_TEMPL mt "
        +     "WHERE m.TEMPLATE_ID = mt.ID "
        +      " AND mt.MONITORABLE_TYPE_ID = mtype.ID "
        +      " AND mtype.APPDEF_TYPE = " + AppdefEntityConstants.APPDEF_TYPE_PLATFORM
        +      " AND m.INSTANCE_ID = pl.ID "
        +      " AND m.ENABLED = ?"
        +      " AND m.INTERVAL IS NOT NULL)";

    public static final String SQL_SERVERS_WITHOUT_METRICS
        = "SELECT authz_r.ID, s.ID, st.NAME AS TNAME, s.NAME, s.CTIME "
        + "FROM EAM_SERVER s, EAM_SERVER_TYPE st, "
        +      "EAM_CONFIG_RESPONSE cr " + PermissionManager.AUTHZ_FROM
        + "WHERE s.CONFIG_RESPONSE_ID = cr.ID "
        +  " AND cr.PRODUCT_RESPONSE IS NOT NULL "
        +  " AND cr.MEASUREMENT_RESPONSE IS NOT NULL "
        +  " AND authz_r.ID NOT IN ( %%IGNORELIST%% ) "
        +  " AND s.SERVER_TYPE_ID = st.ID "
        +  " AND s.CTIME < ? "
        +  " AND st.FVIRTUAL = ? "
        +  " AND s.ID NOT IN "
        +   "(SELECT sv.ID "
        +      "FROM EAM_SERVER sv, "
        +           "EAM_MONITORABLE_TYPE mtype, EAM_MEASUREMENT m, "
        +           "EAM_MEASUREMENT_TEMPL mt "
        +     "WHERE m.TEMPLATE_ID = mt.ID "
        +      " AND mt.MONITORABLE_TYPE_ID = mtype.ID "
        +      " AND mtype.APPDEF_TYPE = " + AppdefEntityConstants.APPDEF_TYPE_SERVER
        +      " AND m.INSTANCE_ID = sv.ID "
        +      " AND m.ENABLED = ?"
        +      " AND m.INTERVAL IS NOT NULL)";
    public List getResourcesWithoutMetrics (AuthzSubjectValue subject,
                                            List ignores) {
        Connection conn = null;
        String platformSQL = SQL_PLATFORMS_WITHOUT_METRICS
            + pm.getSQLWhere(subject.getId(), "p.ID");
        String serverSQL = SQL_SERVERS_WITHOUT_METRICS
            + pm.getSQLWhere(subject.getId(), "s.ID");
        List results = new ArrayList();

        if (ignores == null ) ignores = new ArrayList();
        if (ignores.size() == 0) ignores.add(MathUtil.NEGATIVE_ONE);

        platformSQL = StringUtil.replace(platformSQL, "%%IGNORELIST%%",
                                         StringUtil.listToString(ignores));
        serverSQL = StringUtil.replace(serverSQL, "%%IGNORELIST%%",
                                       StringUtil.listToString(ignores));
        long minCtime = System.currentTimeMillis() - MIN_METRIC_CREATE_OFFSET;
        try {
            conn = getDBConn();

            noMetricsQuery(platformSQL, conn, subject.getId(),
                           AuthzConstants.authzPlatform, 
                           AuthzConstants.perm_modifyPlatform,
                           results, false, minCtime,
                           AppdefEntityConstants.APPDEF_TYPE_PLATFORM);

            noMetricsQuery(serverSQL, conn, subject.getId(),
                           AuthzConstants.authzServer, 
                           AuthzConstants.perm_modifyServer,
                           results, true, minCtime,
                           AppdefEntityConstants.APPDEF_TYPE_SERVER);

            return results;

        } catch (SQLException e) {
            throw new SystemException("Error in getResourcesWithoutMetrics: "
                                         + e, e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    private void noMetricsQuery (String sql, Connection conn, Integer subjectId,
                                 Integer authzType, Integer authzPerm,
                                 List results, boolean setBoolParam,
                                 long minCtime, int appdefType) 
        throws SQLException {

        int col, resId, id, ps_idx = 1;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(ps_idx++, minCtime);
            if (setBoolParam) ps.setBoolean(ps_idx++, false);
            ps.setBoolean(ps_idx++, true);
            ps_idx = pm.prepareSQL(ps, ps_idx, 
                                   subjectId,
                                   authzType,
                                   authzPerm);
            rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                resId = rs.getInt(col++);
                id    = rs.getInt(col++);
                results.add(new MiniResourceValue(resId,
                                                  id,
                                                  appdefType,
                                                  rs.getString(col++),
                                                  rs.getString(col++),
                                                  rs.getLong(col++)));
            }
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, ps);
        }
    }

    private InitialContext getInitialContext(){
        if(this.initialContext == null){
            try {
                this.initialContext = new InitialContext();
            } catch(NamingException exc){
                throw new SystemException(exc);
            }
        }
        return this.initialContext;
    }
    private Connection getDBConn() throws SQLException {
        try {
            return DBUtil.getConnByContext(this.getInitialContext(), 
                                            HQConstants.DATASOURCE);
        } catch(NamingException exc){
            throw new SystemException("Unable to get database context: " +
                                         exc.getMessage(), exc);
        }
    }
}
