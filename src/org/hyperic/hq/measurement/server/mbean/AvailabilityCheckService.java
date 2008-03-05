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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.server.session.SRNCache;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.timer.StopWatch;

/**
 * This job is responsible for filling in missing availabilty metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AvailabilityCheck"
 */
public class AvailabilityCheckService
    extends SessionMBeanBase
    implements AvailabilityCheckServiceMBean
{
    private final String logCtx = AvailabilityCheckService.class.getName();
    private Log log = LogFactory.getLog(logCtx);

    private long interval = 0;
    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    
    private DataManagerLocal _dataMan;
    private DataManagerLocal getDataMan() {
        if (_dataMan == null)
            _dataMan = DataManagerEJBImpl.getOne();
        
        return _dataMan;
    }

    private DerivedMeasurementManagerLocal _dmLocal;
    private DerivedMeasurementManagerLocal getDMManager() {
        if (_dmLocal == null) {
            _dmLocal = DerivedMeasurementManagerEJBImpl.getOne();
        }
        return _dmLocal;
    }

    /**
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        super.hit(lDate);
    }
    
    protected void hitInSession(Date lDate) {
        boolean debug = log.isDebugEnabled();
        
        if (debug) {
            log.debug("Availability Check Service started executing: "+lDate);            
        }

        StopWatch watch = new StopWatch();
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
        watch.markTimeBegin("getEnabledAvailabilityMetrics");
        List dmList = getDMManager().
            findMeasurementsByCategory(MeasurementConstants.CAT_AVAILABILITY);
        MetricDataCache cache = MetricDataCache.getInstance();
        watch.markTimeEnd("getEnabledAvailabilityMetrics");
        
        if (debug) {
            log.debug("Total of " + dmList.size() + 
                    " availability metrics to check");            
        }
            
        // First check every platform derived measurement
        HashMap availMap = new HashMap();
        ArrayList downPlatforms = new ArrayList();
        
        // List of DataPoints to add at the end of all this mayhem
        List addData = new ArrayList();
        // Let's be safe and reset the time to current
        current = System.currentTimeMillis();

        for (Iterator it = dmList.iterator(); it.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement) it.next();
            if (!dm.getTemplate().getAlias().toUpperCase()
                    .equals(MeasurementConstants.CAT_AVAILABILITY))
                continue;
            
            cache.setAvailMetric(dm.getId());

            AppdefEntityID aeid =
                new AppdefEntityID(dm.getAppdefType(), dm.getInstanceId());
                                                 
            if (dm.getAppdefType() !=
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                availMap.put(aeid, dm);
                continue;
            }

            // End is at least more than 1 interval away
            long end = TimingVoodoo.roundDownTime(current - dm.getInterval(),
                                                  dm.getInterval());

            // We have to get at least the measurement interval
            long maxInterval =
                Math.max(Math.max(interval, wait), dm.getInterval());
            
            // Begin is maximum of mbean interval or measurement create time
            long begin = Math.max(end - maxInterval,
                                  dm.getMtime() + dm.getInterval());
            begin = TimingVoodoo.roundDownTime(begin, dm.getInterval());

            if (debug) {
                log.debug("Check metric ID: " + dm.getId() +
                        " from " + begin + " to " + end);                
            }

            // If our time range is negative, then we just wait until next
            if (end < begin)
                continue;
                                                   
            long[] theMissing;
            try {
                theMissing = getDataMan().getMissingDataTimestamps(
                        dm.getId(), dm.getInterval(), begin, end);
            } catch (DataNotAvailableException e) {
                log.error("Failed in AvailabilityCheckService", e);
                continue;
            }
            
            // Go through the data and add missing data points
            MetricValue mval;
            for (int i = theMissing.length - 1; i >= 0; i--) {
                if (debug) {
                    log.debug("Metric ID: " + dm.getId() +
                            " missing data at " + theMissing[i]);                    
                }

                // Insert the missing data point
                mval = new MetricValue(MeasurementConstants.AVAIL_DOWN, 
                                       theMissing[i]);
                addData.add(new DataPoint(dm.getId(), mval));
            }
            
            // Check SRN to see if somehow the agent lost the schedule
            if (theMissing.length > 0) {
                // First see if it was reported recently
                if (cache.get(dm.getId(),
                    theMissing[theMissing.length - 1] + 1) != null)
                    continue;
                
                downPlatforms.add(aeid);
                
                ScheduleRevNum srn = srnCache.get(aeid);
                
                if (srn == null)
                    continue;
                
                if (debug) {
                    log.debug("Compare missing " + theMissing[0] +
                            " to last reported " + srn.getLastReported());                    
                }
                
                // That's odd, why is there no data, then?
                if (srn.getLastReported() > theMissing[0]) {
                    if (debug) {
                        log.debug("Reset report time for " + aeid);                        
                    }
                    
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
                Object dmv = availMap.remove(server.getEntityId());
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
            DerivedMeasurement dm = (DerivedMeasurement) it.next();
            if (!dm.getTemplate().getAlias().toUpperCase()
                    .equals(MeasurementConstants.CAT_AVAILABILITY))
                continue;

            cache.setAvailMetric(dm.getId());

            // End is at least more than 1/2 interval away
            long end = TimingVoodoo.closestTime(
                current - dm.getInterval(), dm.getInterval());

            // We have to get at least the measurement interval
            long maxInterval =
                Math.max(Math.max(interval, wait), dm.getInterval());

            // Begin is maximum of mbean interval or measurement create time
            long begin = Math.max(end - maxInterval,
                                  dm.getMtime() + dm.getInterval());
            begin = TimingVoodoo.roundDownTime(begin, dm.getInterval());

            if (debug) {
                log.debug("Check metric ID: " + dm.getId() +
                        " from " + begin + " to " + end);                
            }

            // If our time range is negative, then we just wait until next
            if (end < begin)
                continue;

            long[] theMissing;
            try {
                theMissing = getDataMan().getMissingDataTimestamps(
                    dm.getId(), dm.getInterval(), begin, end);
            } catch (DataNotAvailableException e) {
                log.error("Failed in AvailabilityCheckService", e);
                continue;
            }

            // Go through the data and add missing data points
            MetricValue mval;
            for (int i = theMissing.length - 1; i >= 0; i--) {
                if (debug) {
                    log.debug("Metric ID: " + dm.getId() +
                            " missing data at " + theMissing[i]);                    
                }

                // Insert the missing data point
                mval = new MetricValue(MeasurementConstants.AVAIL_DOWN, 
                                       theMissing[i]);
                addData.add(new DataPoint(dm.getId(), mval));
            }
        }
        watch.markTimeBegin("addData");
        
        getDataMan().addData(addData, false);
        watch.markTimeEnd("addData");
        wait = 0;
        log.debug(watch);
        
        
        if (debug) {
            log.debug("Availability Check Service finished executing: "+lDate);            
        }
    }

    /**
     * Get the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public long getInterval() {
        return interval;
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

