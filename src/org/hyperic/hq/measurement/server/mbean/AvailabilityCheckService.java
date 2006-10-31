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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycle;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener;
import org.hyperic.hq.ha.shared.Mode;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.ScheduleRevNumValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

import javax.ejb.CreateException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This job is responsible for filling in missing availabilty
 * metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AvailabilityCheck"
 * 
 */
public class AvailabilityCheckService
    implements AvailabilityCheckServiceMBean, MBeanRegistration,
               EjbModuleLifecycleListener {

    private MBeanServer server = null;
    private EjbModuleLifecycle camListener = null;
    private boolean started = false;
    private Boolean fill = null;
        
    private static final String ENABLED_AVAIL_METRICS_SQL = 
        "SELECT m.id, m.mtime, m.coll_interval, mt.appdef_type, m.instance_id " +
        "FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, " +
             "EAM_MEASUREMENT_CAT cat, EAM_MONITORABLE_TYPE mt " +
        "WHERE m.enabled = DB_TRUE_TOKEN AND m.coll_interval IS NOT NULL AND " +
             "m.template_id = t.id AND t.monitorable_type_id = mt.id AND " +
             "t.category_id = cat.id AND cat.name = '" +
             MeasurementConstants.CAT_AVAILABILITY + "'";
    
    private static final String ENABLED_METRICS_SQL =
        "SELECT m.id FROM EAM_MEASUREMENT m, EAM_MEASUREMENT_TEMPL t, " +
                         "EAM_MONITORABLE_TYPE mt " +
        "WHERE m.enabled = DB_TRUE_TOKEN AND m.coll_interval IS NOT NULL AND " +
              "m.instance_id = ? AND m.template_id = t.id AND " +
              "t.monitorable_type_id = mt.id AND mt.appdef_type = ?";    
    
    private static final String DATASOURCE = HQConstants.DATASOURCE;    
    
    private final String logCtx = AvailabilityCheckService.class.getName();
    private Log log = LogFactory.getLog(logCtx);

    private long interval = 0;
    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    
    private DataManagerLocal dataMan = null;
    private DataManagerLocal getDataMan() {
        try {
            if (dataMan == null)
                dataMan = DataManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        return dataMan;
    }

    //---------------------------------------------------------------------
    //-- managed operations
    //---------------------------------------------------------------------
    /**
     * AvailCheck service is only active on master and standalone servers.
     * @jmx:managed-operation
     */
    public boolean isActive () {
        return Mode.getInstance().isActivated();
    }

    /**
     * Send the message.
     *
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        if (!started) {
            this.log.debug("HQ Services have not been started for " + logCtx);
            return;
        }
        
        if (!isActive())
            return;

        long current = lDate.getTime();

        // Don't start backfilling until 10 minutes after app has started
        if (startTime == 0) {
            startTime = current;
            return;
        }
        else if (startTime + wait > current) {
                return;
        }

        if (log.isDebugEnabled())
            log.debug("AvailabilityCheckService.hit() at " + lDate + "(" +
                      current + ")");
        
        if (fill == null) {
            try {
                Properties conf =
                    ServerConfigManagerUtil.getLocalHome().create().getConfig();
                
                String storeAllString =
                    conf.getProperty(HQConstants.DataStoreAll);

                if (storeAllString != null) {
                    this.fill = new Boolean(storeAllString);
                }
                else {
                    this.fill = Boolean.TRUE;
                }
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (ConfigPropertyException e) {
                // Continue to fill
                this.fill = Boolean.TRUE;
            }
        }
        
        if (fill.booleanValue() == false)
            return;
        
        SRNCache srnCache = SRNCache.getInstance();
        
        // Fetch all derived availablity measurements
        List dmList = this.getEnabledAvailabilityMetrics();
        MetricDataCache metCache = MetricDataCache.getInstance();
        
        if (log.isDebugEnabled())
            log.debug("Total of " + dmList.size() +
                      " availability metrics to check");
            
        // First check every platform derived measurement
        HashMap availMap = new HashMap();
        ArrayList downPlatforms = new ArrayList();
        
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
                theMissing = this.getDataMan().getMissingDataTimestamps(
                        dmVo.getId(), dmVo.getInterval(), begin, end);
            } catch (DataNotAvailableException e) {
                log.error("Failed in AvailabilityCheckService", e);
                continue;
            }
            
            // Go through the data and add missing data points
            MetricValue mval;
            for (int i = 0; i < theMissing.length; i++) {
                if (log.isDebugEnabled())
                    log.debug("Metric ID: " + dmVo.getId() +
                              " missing data at " + theMissing[i]);

                // Insert the missing data point
                mval = new MetricValue(
                    MeasurementConstants.AVAIL_DOWN, theMissing[i]);
                this.getDataMan().addData(dmVo.getId(), mval, false);
            }
            
            // Check SRN to see if somehow the agent lost the schedule
            if (theMissing.length > 0) {
                // First see if it was reported recently
                if (metCache.get(dmVo.getId(),
                    theMissing[theMissing.length - 1] + 1) != null)
                    continue;
                
                downPlatforms.add(aeid);
                
                ScheduleRevNumValue srn = srnCache.getSRN(aeid);
                
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
        
        // Now check the servers and services
        try {
            PlatformManagerLocal platMan =
                PlatformManagerUtil.getLocalHome().create();

            ServerManagerLocal svrMan =
                ServerManagerUtil.getLocalHome().create();

            AuthzSubjectManagerLocal authzMan =
                AuthzSubjectManagerUtil.getLocalHome().create();
            AuthzSubjectValue overlord = authzMan.getOverlord();
            ArrayList metrics = new ArrayList();
            for (Iterator it = downPlatforms.iterator(); it.hasNext(); ) {
                AppdefEntityID platId = (AppdefEntityID) it.next();
                
                try {
                    PlatformValue platform =
                        platMan.getPlatformById(overlord, platId.getId());
                    
                    // Go through the servers and services
                    ServerLightValue[] servers = platform.getServerValues();
                    for (int svrIdx = 0; svrIdx < servers.length; svrIdx++) {
                        Object dmv =
                            availMap.remove(servers[svrIdx].getEntityId());
                        if (dmv != null)
                            metrics.add(dmv);
                        
                        // Find the services
                        ServerValue server = svrMan.getServerById(overlord,
                                                 servers[svrIdx].getId());
                        
                        ServiceLightValue[] services =
                            server.getServiceValues();
                        for (int svcIdx = 0; svcIdx < services.length;
                             svcIdx++) {
                            dmv =
                                availMap.remove(services[svcIdx].getEntityId());
                            
                            if (dmv != null)
                                metrics.add(dmv);
                        }
                    }
                } catch (PlatformNotFoundException e) {
                    log.error("Unable to find plaform " + platId);
                } catch (ServerNotFoundException e) {
                    log.error("Unable to find server for platform " + platId);
                }
                
            }
            
            // Go through the server and service metrics and backfill them
            for (Iterator it = metrics.iterator(); it.hasNext(); ) {
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
                    theMissing = this.getDataMan().getMissingDataTimestamps(
                            dmVo.getId(), dmVo.getInterval(), begin, end);
                } catch (DataNotAvailableException e) {
                    log.error("Failed in AvailabilityCheckService", e);
                    continue;
                }
                
                // Go through the data and add missing data points
                MetricValue mval;
                for (int i = 0; i < theMissing.length; i++) {
                    if (log.isDebugEnabled())
                        log.debug("Metric ID: " + dmVo.getId() +
                                  " missing data at " + theMissing[i]);

                    // Insert the missing data point
                    mval = new MetricValue(
                        MeasurementConstants.AVAIL_DOWN, theMissing[i]);
                    this.getDataMan().addData(dmVo.getId(), mval, false);
                }
            }
        } catch (CreateException e) {
            log.error("Unable to create PlatformManager");
        } catch (NamingException e) {
            log.error("Unable to lookup PlatformManager");
        } catch (PermissionException e) {
            log.error("The overlord does not have permission to lookup " +
                      "platform", e);
        }
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
            // gulp?
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
    
    //---------------------------------------------------------------------
    //-- mbean control methods
    //---------------------------------------------------------------------
    /**
     * @jmx:managed-operation
     */
    public void init() {
        // Do nothing
    }

    /**
     * @jmx:managed-operation
     */
    public void start() throws Exception {
        camListener =
            new EjbModuleLifecycle(this.server, this,
                                   HQConstants.EJB_MODULE_PATTERN);
        camListener.start();
    }

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        log.info("Stopping " + this.getClass().getName());
        camListener.stop();
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {
        // do nothing
    }
    
    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
        this.server = server;
        return name;
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean arg0) {
        // do nothing
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        // do nothing
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        // do nothing
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener#ejbModuleStarted()
     */
    public void ejbModuleStarted() {
        log.info("Starting " + this.getClass().getName());
        this.started = true;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener#ejbModuleStopped()
     */
    public void ejbModuleStopped() {
        // do nothing
    }
}

