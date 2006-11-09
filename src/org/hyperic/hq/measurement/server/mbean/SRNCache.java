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

package org.hyperic.hq.measurement.server.mbean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.ScheduleRevNumDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.measurement.SRN;
import org.hyperic.hq.measurement.SRNCreateException;
import org.hyperic.hq.measurement.ScheduleRevNum;
import org.hyperic.hq.measurement.SrnId;
import org.hyperic.hq.measurement.shared.ScheduleRevNumValue;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.util.jdbc.DBUtil;

/**
 * The cache class that keeps track of resources and their SRNs
 */
public class SRNCache {
    protected Log log = LogFactory.getLog(SRNCache.class.getName());

    private static SRNCache _singleton = null;
    private HashMap _cache = null;
    private InitialContext _ic = null;

    protected SRNCache() {}

    protected ScheduleRevNumDAO getScheduleRevNumDAO() {
        return DAOFactory.getDAOFactory().getScheduleRevNumDAO();
    }

    private Connection getConnection() throws SQLException {
        try {
            if (_ic == null) {
                _ic = new InitialContext();
            }

            return DBUtil.getConnByContext(_ic, HQConstants.DATASOURCE);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Return singleton instance and initialize if necessary
     */    
    public static SRNCache getInstance() {
        if (_singleton == null) {
            _singleton = (SRNCache) ProductProperties
                .getPropertyInstance("hyperic.hq.metric.srncache");
            
            if (_singleton == null)
                _singleton = new SRNCache();
            
            _singleton.init();
        }
        return _singleton;
    }

    /**
     * Only return singleton instance only if it's already initialized
     */    
    public static SRNCache getInitializedInstance() {
        return _singleton;
    }

    private PreparedStatement getMinIntervalsStmt(Connection conn)
        throws SQLException {
        return conn.prepareStatement(
            "SELECT appdef_type, instance_id, min(coll_interval)" +
            " FROM EAM_MEASUREMENT m, EAM_MONITORABLE_TYPE mt," +
            "      EAM_MEASUREMENT_TEMPL t" +
            " WHERE enabled = " + DBUtil.getBooleanValue(true, conn) + " AND " +
            "      template_id = t.id AND monitorable_type_id = mt.id" +
            " GROUP BY appdef_type, instance_id");
    }
    
    private PreparedStatement getMinIntervalStmt(Connection conn)
        throws SQLException {
        return conn.prepareStatement(
            "SELECT min(coll_interval)" +
            " FROM EAM_MEASUREMENT m, EAM_MONITORABLE_TYPE mt," +
            "      EAM_MEASUREMENT_TEMPL t" +
            " WHERE enabled = " + DBUtil.getBooleanValue(true, conn) + " AND " +
            "      instance_id = ? AND template_id = t.id AND" +
            "      monitorable_type_id = mt.id AND appdef_type = ?" +
            " GROUP BY appdef_type, instance_id");
    }

    private PreparedStatement getEnabledEntitiesStmt(Connection conn)
        throws SQLException {
        return conn.prepareStatement(
            "SELECT appdef_type, instance_id" +
            " FROM EAM_MEASUREMENT m, EAM_MONITORABLE_TYPE mt," +
            "      EAM_MEASUREMENT_TEMPL t" +
            " WHERE enabled = " + DBUtil.getBooleanValue(true, conn) + " AND " +
            "      template_id = t.id AND monitorable_type_id = mt.id" +
            " GROUP BY appdef_type, instance_id");
    }

    private PreparedStatement getSRNStmt(Connection conn) throws SQLException {
        return conn.prepareStatement(
            "SELECT appdef_type, instance_id, srn FROM EAM_SRN");
    }

    private PreparedStatement setSRNStmt(Connection conn) throws SQLException {
        return conn.prepareStatement(
            "UPDATE EAM_SRN SET srn = ? WHERE " +
            "appdef_type = ? AND instance_id = ?");
    }

    private void setMinInterval(ScheduleRevNumValue srn, Connection conn,
                                PreparedStatement stmt, AppdefEntityID eid)
        throws SQLException {
        ResultSet rs = null;
        try {
            int i = 1;
            stmt.setInt(i++, eid.getID());
            stmt.setInt(i++, eid.getType());

            rs = stmt.executeQuery();

            if (rs.next()) {
                srn.setMinInterval(rs.getLong(1));
            } else {
                // Set to something sufficiently large so that we won't try to
                // reschedule
                srn.setMinInterval(System.currentTimeMillis());
            }
        } finally {
            DBUtil.closeResultSet(this, rs);
        }
    }

    private synchronized boolean init() {
        if (_cache != null)
            return true;

        log.debug("Initializing SRN Cache");
        
        // Create the new cache
        _cache = new HashMap();
        
        synchronized (_cache) {
            long current = System.currentTimeMillis();
            
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            // Look up the minimum intervals from the database            
            try {                    
                conn = getConnection();
                
                try {
                    stmt = this.getSRNStmt(conn);

                    log.debug("Fetch existing SRN values from database");
                    
                    rs = stmt.executeQuery();
                    
                    while(rs.next()) {
                        ScheduleRevNumValue srnVal =
                            new ScheduleRevNumValue(rs.getInt(1),
                                                    rs.getInt(2),
                                                    rs.getInt(3),
                                                    0, 0, false);
                        AppdefEntityID eid =
                            new AppdefEntityID(srnVal.getAppdefType(),
                                               srnVal.getInstanceId());

                        // Set to something sufficiently large so that we
                        // won't try to reschedule
                        srnVal.setMinInterval(current);

                        _cache.put(eid, srnVal);
                    }
                } finally {
                    DBUtil.closeJDBCObjects(this, null, stmt, rs);
                }
    
                if (_cache.size() == 0) {
                    log.debug("Create SRN values from entities");

                    // Create SRNs from the database
                    try {
                        stmt = this.getEnabledEntitiesStmt(conn);
                        rs = stmt.executeQuery();
                        
                        while (rs.next()) {
                            ScheduleRevNum srn =
                                getScheduleRevNumDAO().create(rs.getInt(1),
                                                              rs.getInt(2));
                            
                            ScheduleRevNumValue srnVal =
                                srn.getScheduleRevNumValue();
                            
                            // Set to something sufficiently large so that we
                            // won't try to reschedule
                            srnVal.setMinInterval(current);

                            AppdefEntityID eid =
                                new AppdefEntityID(srnVal.getAppdefType(),
                                                   srnVal.getInstanceId());
                            
                            _cache.put(eid, srnVal);
                        }
                    } finally {
                        DBUtil.closeJDBCObjects(this, null, stmt, rs);
                    }

                    log.debug("SRN values created from entities");
                }
                
                log.debug("Existing SRN values fetched from database");
                log.debug("Fetch minimum metric collection intervals");

                stmt = this.getMinIntervalsStmt(conn);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    // Set minInterval
                    AppdefEntityID eid =
                        new AppdefEntityID(rs.getInt(1), rs.getInt(2));
                    
                    if (_cache.containsKey(eid)) {
                        ScheduleRevNumValue srn =
                            (ScheduleRevNumValue) _cache.get(eid);
                        srn.setMinInterval(rs.getLong(3));
                    }
                }

                log.debug("Minimum metric collection intervals fetched");
            } catch (SQLException e) {
                throw new SystemException(e);
            } finally {
                DBUtil.closeJDBCObjects(this, conn, stmt, rs);
            }
        }

        log.info("SRN Cache initialized");
        return false;
    }

    /**
     * Called prior to contacting the agent to unschedule an entity
     * @param eid the entity ID
     * @return the new version number
     * @throws FinderException
     * @throws SRNCreateException
     */
    public ScheduleRevNumValue removeSRN(AppdefEntityID eid)
        throws FinderException, RemoveException {
        ScheduleRevNumValue srnVal;
        synchronized (_cache) {
            srnVal = (ScheduleRevNumValue) _cache.remove(eid);
        }

        if (srnVal != null) {
            // Update the local EJB
            try {
                MeasurementProcessorLocal mlocal =
                    MeasurementProcessorUtil.getLocalHome().create();
                mlocal.removeSRN(eid);
            } catch (CreateException e) {
                throw new RuntimeException(e);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
            srnVal.setSRN(0);
        }
        
        return srnVal;
    }

    /**
     * Called prior to contacting the agent
     * @param eid the entity ID
     * @return the new version number
     * @throws FinderException
     * @throws SRNCreateException
     */
    public int beginIncrementSRN(AppdefEntityID eid, long newMin)
        throws FinderException, SRNCreateException
    {
        ScheduleRevNumValue srnVal;

        boolean update = false;
        synchronized (_cache) {
            srnVal = getSRN(eid);
            if (srnVal == null) {
                ScheduleRevNum srn = 
                    getScheduleRevNumDAO().create(eid.getType(), eid.getID());
                srnVal = srn.getScheduleRevNumValue();
                _cache.put(eid, srnVal);
            }
            else {
                int srn = srnVal.getSRN();
                srnVal.setSRN(++srn);
                update = true;
                
                if (log.isDebugEnabled())
                    log.debug("Update " + eid + " SRN to " + srn);
            }
        }
        
        // Use direct SQL to bypass EJB complexity
        if (update) {
            Connection conn = null;
            PreparedStatement stmt = null;
            
            try {
                conn = getConnection();
                stmt = setSRNStmt(conn);
                
                int i = 1;
                stmt.setInt(i++, srnVal.getSRN());
                stmt.setInt(i++, srnVal.getAppdefType());
                stmt.setInt(i++, srnVal.getInstanceId());
                
                stmt.execute();
            } catch (SQLException e) {
                // This is not the end of the world, SRNs generally work
                // themselves out in the end
                log.error("SQLException updating SRN", e);
            } finally {
                DBUtil.closeJDBCObjects(this, conn, stmt, null);
            }
        }
    
        synchronized (srnVal) {
            // Get the current time
            long current = System.currentTimeMillis();
    
            if (newMin > 0 && newMin < srnVal.getMinInterval()) {
                // Go ahead and set to the new low
                srnVal.setMinInterval(newMin);
            }
            else {
                // Look up the min interval
                Connection conn = null;
                PreparedStatement stmt = null;
                try {            
                    conn = getConnection();
                    stmt = this.getMinIntervalStmt(conn);
    
                    // Set minInterval
                    this.setMinInterval(srnVal, conn, stmt, eid);
                } catch (SQLException e) {
                    // Prevent rescheduling
                    this.log.debug("SQLException, setting min interval to " +
                                   "current");
                    srnVal.setMinInterval(current);
                } finally {
                    DBUtil.closeJDBCObjects(this, conn, stmt, null);
                }
            }
    
            // srnVal.setLastReported(current);
            srnVal.setPending(true);
        }
    
        return srnVal.getSRN();
    }
    
    public ScheduleRevNumValue endIncrementSRN(AppdefEntityID eid) {
        ScheduleRevNumValue srnVal = getSRN(eid);

        if (srnVal == null) {
            // TODO: Throw exception
        }
        else {
            synchronized (srnVal) {
                srnVal.setPending(false);
                srnVal.setLastReported(0);
            }
        }
        
        return srnVal;
    }
    
    public Collection reportAgentSRNs(SRN[] srns) {
        HashSet nonEntities = new HashSet();
        
        for (int i = 0; i < srns.length; i++) {
            ScheduleRevNumValue srnVal = getSRN(srns[i].getEntity());

            if (srnVal == null) {
                log.error("Agent's reporting for non-existing entity: "
                          + srns[i].getEntity());
                nonEntities.add(srns[i].getEntity());
                continue;
            }

            if (!srnVal.getPending()) {
                synchronized (srnVal) {
                    long current = System.currentTimeMillis();

                    if (srns[i].getRevisionNumber() != srnVal.getSRN()) {
                        if (srnVal.getLastReported() >
                            current - srnVal.getMinInterval()) {
                            // If the last reported time is less than an
                            // interval ago it could be that we just rescheduled
                            // the agent, so let's not panic yet
                            this.log.debug(
                                "Ignore out-of-date SRN for grace period of "
                                    + srnVal.getMinInterval());
                            break;
                        }

                        // Skip setting last reported time, then
                        // getOutOfSyncEntities() will be able to return this
                        if (log.isDebugEnabled()) {
                            log.debug("SRN value for " + srns[i].getEntity() +
                                      " is out of date, agent reports " + 
                                      srns[i].getRevisionNumber() +
                                      " but cached is " + srnVal.getSRN() +
                                      " do not set last reported time");                            
                        }
                    }
                    else {
                        srnVal.setLastReported(current);
                    }
                }
            }
        }
        
        return nonEntities;
    }
    
    public void reportDatabaseSRNs(ScheduleRevNumValue[] srnVals) {
        for (int i = 0; i < srnVals.length; i++) {
            AppdefEntityID eid =
                new AppdefEntityID(srnVals[i].getAppdefType(),
                                   srnVals[i].getInstanceId());
            ScheduleRevNumValue srnVal = getSRN(eid);
            srnVal.setLastReported(Math.max(srnVal.getLastReported(),
                                            srnVals[i].getLastReported()));
        }
    }
        
    public List getOutOfSyncEntities() {
        List srns = getOutOfSyncSRNs(3);
        ArrayList toReschedule = new ArrayList(srns.size());
        
        for (Iterator it = srns.iterator(); it.hasNext(); ) {
            ScheduleRevNumValue srnVal = (ScheduleRevNumValue) it.next();
            
            AppdefEntityID eid =
                new AppdefEntityID(srnVal.getAppdefType(),
                                   srnVal.getInstanceId());
            this.log.trace("getOutOfSyncEntities() lastReported at " +
                           srnVal.getLastReported() + " for " + eid);
            toReschedule.add(eid);
        }
        
        return toReschedule;
    }

    public List getOutOfSyncSRNs(int intervals) {
        long current = System.currentTimeMillis();
        log.debug("Current is " + current);
        
        // Convert values to array to avoid synchronization problem
        Object[] values = null;
        synchronized(_cache) {
            values = _cache.values().toArray();
        }
        
        ArrayList toReschedule = new ArrayList();
        
        if (values == null)
            return toReschedule;
        
        for (int i = 0; i < values.length; i++) {
            ScheduleRevNumValue srnVal = (ScheduleRevNumValue) values[i];
            log.debug("Checking " + srnVal.getAppdefType() + ":" +
                      srnVal.getInstanceId() + " whose last reported time " +
                      "was " + srnVal.getLastReported());
            
            // If it's currently pending, then don't check it
            if (srnVal.getPending()) {
                log.debug("SRNValue is pending");
                continue;
            }
                
            if (srnVal.getLastReported() <
                current - intervals * srnVal.getMinInterval()) {
                toReschedule.add(srnVal);
            }
        }
        
        return toReschedule;
    }
    
    public ScheduleRevNumValue refreshSRN(AppdefEntityID eid) {
        ScheduleRevNumValue srnVal =
            new ScheduleRevNumValue(eid.getType(), eid.getID(), 0, 0, 0, false);

        // Look it up then
        Connection conn = null;
        PreparedStatement stmt = null;
        try {            
            conn = getConnection();
            stmt = getMinIntervalStmt(conn);

            // Set minInterval
            setMinInterval(srnVal, conn, stmt, eid);
        } catch (SQLException e) {
            // Prevent rescheduling
            log.error("SQLException, abort handleMessage", e);
            return srnVal;
        } finally {
            DBUtil.closeJDBCObjects(this, conn, stmt, null);
        }
        
        _cache.put(eid, srnVal);
        return srnVal;
    }
    
    public ScheduleRevNumValue getSRN(AppdefEntityID eid) {
        return (ScheduleRevNumValue) _cache.get(eid);
    }
}
