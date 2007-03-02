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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.common.SessionMBeanBase;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.server.session.SRNCache;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

/**
 * This job is responsible for filling in missing availabilty metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AvailabilityCheck"
 */
public class AvailabilityCheckService
    extends SessionMBeanBase
    implements AvailabilityCheckServiceMBean
{
    private static final String ENABLED_AVAIL_METRICS_SQL = 
        "SELECT m.id, m.mtime, m.coll_interval, mt.appdef_type, m.instance_id " +
        "FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, " +
             "EAM_MEASUREMENT_CAT cat, EAM_MONITORABLE_TYPE mt " +
        "WHERE m.enabled = DB_TRUE_TOKEN AND m.coll_interval IS NOT NULL AND " +
             "m.template_id = t.id AND t.monitorable_type_id = mt.id AND " +
             "t.category_id = cat.id AND cat.name = '" +
             MeasurementConstants.CAT_AVAILABILITY + "'";
       
    private static final String DATASOURCE = HQConstants.DATASOURCE;    
    
    private final String logCtx = AvailabilityCheckService.class.getName();
    private Log log = LogFactory.getLog(logCtx);

    private long interval = 0;
    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    
    private DataManagerLocal dataMan;
    private DataManagerLocal getDataMan() {
        if (dataMan == null)
            dataMan = DataManagerEJBImpl.getOne();
        
        return dataMan;
    }

    /**
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        super.hit(lDate);
    }
    
    protected void hitInSession(Date lDate) {

        long current = lDate.getTime();

        // Don't start backfilling immediately
        if (startTime == 0) {
            startTime = current;
            return;
        } else if (startTime + wait > current) {
            return;
        }

        SRNCache srnCache = SRNCache.getInstance();
        
        // Fetch all derived availablity measurements
        List dmList = getEnabledAvailabilityMetrics();
        MetricDataCache metCache = MetricDataCache.getInstance();
        
        if (log.isDebugEnabled())
            log.debug("Total of " + dmList.size() +
                      " availability metrics to check");
            
        // First check every platform derived measurement
        HashMap availMap = new HashMap();
        ArrayList downPlatforms = new ArrayList();
        
        // List of DataPoints to add at the end of all this mayhem
        List addData = new ArrayList();
        // Let's be safe and reset the time to current
        current = System.currentTimeMillis();
        for (Iterator it = dmList.iterator(); it.hasNext(); ) {
            DerivedMeasurementValue dmVo =
                (DerivedMeasurementValue) it.next();
            
            AppdefEntityID aeid =
                new AppdefEntityID(dmVo.getAppdefType(), dmVo.getInstanceId());
                                                 
            if (dmVo.getAppdefType() !=
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                availMap.put(aeid, dmVo);
                continue;
            }

            // End is at least more than 1/2 interval away
            long end = TimingVoodoo.closestTime(
                current - dmVo.getInterval(), dmVo.getInterval());

            // We have to get at least the measurement interval
            long maxInterval = Math.max(this.interval, dmVo.getInterval());
            
            // Begin is maximum of mbean interval or measurement create time
            long begin = Math.max(end - maxInterval,
                                  dmVo.getMtime() + dmVo.getInterval());
            begin = TimingVoodoo.roundDownTime(begin, dmVo.getInterval());

            if (log.isDebugEnabled())
                log.debug("Check metric ID: " + dmVo.getId() +
                          " from " + begin + " to " + end);

            // If our time range is negative, then we just wait until next
            if (end < begin)
                continue;
                                                   
            long[] theMissing;
            try {
                theMissing = getDataMan().getMissingDataTimestamps(
                        dmVo.getId(), dmVo.getInterval(), begin, end);
            } catch (DataNotAvailableException e) {
                log.error("Failed in AvailabilityCheckService", e);
                continue;
            }
            
            // Go through the data and add missing data points
            MetricValue mval;
            for (int i = theMissing.length - 1; i >= 0; i--) {
                if (log.isDebugEnabled())
                    log.debug("Metric ID: " + dmVo.getId() +
                              " missing data at " + theMissing[i]);

                // Insert the missing data point
                mval = new MetricValue(MeasurementConstants.AVAIL_DOWN, 
                                       theMissing[i]);
                addData.add(new DataPoint(dmVo.getId(), mval));
            }
            
            // Check SRN to see if somehow the agent lost the schedule
            if (theMissing.length > 0) {
                // First see if it was reported recently
                if (metCache.get(dmVo.getId(),
                    theMissing[theMissing.length - 1] + 1) != null)
                    continue;
                
                downPlatforms.add(aeid);
                
                ScheduleRevNum srn = srnCache.get(aeid);
                
                if (srn == null)
                    continue;
                
                if (log.isDebugEnabled())
                    log.debug("Compare missing " + theMissing[0] +
                              " to last reported " + srn.getLastReported());
                
                // That's odd, why is there no data, then?
                if (srn.getLastReported() > theMissing[0]) {
                    if (log.isDebugEnabled())
                        log.debug("Reset report time for " + aeid);
                    
                    // Let ScheduleVerification reschedule
                    srn.setLastReported(theMissing[0]);
                }
            }
        }
        
        // Check the servers and services.
        PlatformManagerLocal platMan = PlatformManagerEJBImpl.getOne();
        ArrayList metrics = new ArrayList();

        for (Iterator i = downPlatforms.iterator(); i.hasNext();) {
            AppdefEntityID platId = (AppdefEntityID)i.next();
            // Go through the servers and services
            Platform platform;
            try {
                platform = platMan.findPlatformById(platId.getId());
            } catch (Exception e) {
                log.error("Unable to find platform=" + platId.getId(), e);
                continue;
            }

            Collection servers = platform.getServers();
            for (Iterator it = servers.iterator(); it.hasNext();) {
                Server server = (Server)it.next();
                Object dmv =
                    availMap.remove(server.getEntityId());
                if (dmv != null)
                    metrics.add(dmv);
                Collection services = server.getServices();
                for (Iterator serviceItr = services.iterator();
                     serviceItr.hasNext();) {
                    Service service = (Service) serviceItr.next();
                    dmv = availMap.remove(service.getEntityId());
                    if (dmv != null)
                        metrics.add(dmv);
                }
            }
        }

        // Go through the server and service metrics and backfill them
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            DerivedMeasurementValue dmVo =
                (DerivedMeasurementValue) it.next();

            // End is at least more than 1/2 interval away
            long end = TimingVoodoo.closestTime(
                current - dmVo.getInterval(), dmVo.getInterval());

            // We have to get at least the measurement interval
            long maxInterval = Math.max(this.interval, dmVo.getInterval());

            // Begin is maximum of mbean interval or measurement create time
            long begin = Math.max(end - maxInterval,
                                  dmVo.getMtime() + dmVo.getInterval());
            begin = TimingVoodoo.roundDownTime(begin, dmVo.getInterval());

            if (log.isDebugEnabled())
                log.debug("Check metric ID: " + dmVo.getId() +
                    " from " + begin + " to " + end);

            // If our time range is negative, then we just wait until next
            if (end < begin)
                continue;

            long[] theMissing;
            try {
                theMissing = getDataMan().getMissingDataTimestamps(
                    dmVo.getId(), dmVo.getInterval(), begin, end);
            } catch (DataNotAvailableException e) {
                log.error("Failed in AvailabilityCheckService", e);
                continue;
            }

            // Go through the data and add missing data points
            MetricValue mval;
            for (int i = theMissing.length - 1; i >= 0; i--) {
                if (log.isDebugEnabled())
                    log.debug("Metric ID: " + dmVo.getId() +
                        " missing data at " + theMissing[i]);

                // Insert the missing data point
                mval = new MetricValue(MeasurementConstants.AVAIL_DOWN, 
                                       theMissing[i]);
                addData.add(new DataPoint(dmVo.getId(), mval));
            }
        }
        getDataMan().addData(addData, false);
    }
    
    /**
     * Optimized method of retrieving enabled availability measurements
     * This is done because retrieving this every 5 mintues via entity beans
     * results in massive overhead for returning 3 values per measurement
     * @return List of DerivedMeasurementValue which ONLY have id, mtime, and
     *         interval enabled. These DMVs are NOT fully initialized.
     *         DO NOT USE THEM ANYWHERE ELSE
     */
    private List getEnabledAvailabilityMetrics() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List dms = new ArrayList();
        try {
            conn = DBUtil.getConnByContext(new InitialContext(), DATASOURCE);
            stmt = conn.createStatement();
            String query =
                StringUtil.replace(ENABLED_AVAIL_METRICS_SQL, "DB_TRUE_TOKEN", 
                                   DBUtil.getBooleanValue(true, conn));
            
            if (log.isDebugEnabled())
                log.debug("Executing query: " + query);
            
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                DerivedMeasurementValue dmv = new DerivedMeasurementValue();
                dmv.setId(new Integer(rs.getInt(1)));
                dmv.setMtime(rs.getLong(2));
                dmv.setInterval(rs.getLong(3));
                dmv.setAppdefType(rs.getInt(4));
                dmv.setInstanceId(new Integer(rs.getInt(5)));
                dms.add(dmv);
            }
        } catch (Exception e) {
            log.error("Unable to get enabled availability measurements", e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
        return dms;
    }

    /**
     * Get the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public long getInterval() {
        return this.interval;
    }

    /**
     * Set the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }
    
    /**
     * Get the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public long getWait() {
        return this.wait;
    }

    /**
     * Set the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public void setWait(long wait) {
        this.wait = wait;
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void start() throws Exception {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        log.info("Stopping " + this.getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}
}

