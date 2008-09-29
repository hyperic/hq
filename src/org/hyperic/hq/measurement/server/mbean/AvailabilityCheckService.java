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

package org.hyperic.hq.measurement.server.mbean;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SessionMBeanBase;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.AvailabilityCache;
import org.hyperic.hq.measurement.server.session.MeasDataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.TimeUtil;

/**
 * This job is responsible for filling in missing availabilty metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AvailabilityCheck"
 */
public class AvailabilityCheckService
    extends SessionMBeanBase
    implements AvailabilityCheckServiceMBean
{
    private Log _log = LogFactory.getLog(AvailabilityCheckService.class);
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;

    private long _interval = 0;
    private long _startTime = 0;
    private long _wait = 5 * MeasurementConstants.MINUTE;

    /**
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        super.hit(lDate);
    }
    
    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
	    return TimingVoodoo.roundDownTime(
	        (current-meas.getInterval()), meas.getInterval());
    }

    private long getBeginWindow(long end, Measurement meas) {
        final long interval = 0;
        final long wait = 5 * MeasurementConstants.MINUTE;
        long measInterval = meas.getInterval();

        // We have to get at least the measurement interval
        long maxInterval =
            Math.max(Math.max(interval, wait), measInterval);
    
        // Begin is maximum of mbean interval or measurement create time
        long begin = Math.max(end - maxInterval, meas.getMtime() + measInterval);
        return TimingVoodoo.roundDownTime(begin, measInterval);
    }

    private List getDownPlatforms(Date lDate) {
        final boolean debug = _log.isDebugEnabled();
        AvailabilityCache cache = AvailabilityCache.getInstance();
        AvailabilityManagerLocal availMan = AvailabilityManagerEJBImpl.getOne();
        List platformResources = availMan.getPlatformResources();
        final long now = lDate.getTime();
        List rtn = new ArrayList(platformResources.size());
        synchronized (cache) {
            for (Iterator i = platformResources.iterator(); i.hasNext();) {
                Measurement meas = (Measurement)i.next();
                long interval = meas.getInterval();
                long end = getEndWindow(now, meas);
                long begin = getBeginWindow(end, meas);
                DataPoint defaultPt =
                    new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                DataPoint last = cache.get(meas.getId(), defaultPt);
                long lastTimestamp = last.getTimestamp();
                if (debug) {
                    String msg = "Checking availability for " + last +
                        ", " + TimeUtil.toString(lastTimestamp) +
                        " vs. " + TimeUtil.toString(now) + " (checktime)";
                    _log.debug(msg);
                }
                if (begin > end) {
                    // this represents the scenario where the measurement mtime
                    // was modified recently and therefore we need to wait
                    // another interval
                    continue;
                }
                if (last.getValue() == AVAIL_DOWN ||
                    (now - lastTimestamp) >= interval*2)
                {
                    long t = last.getValue() != AVAIL_DOWN ?
                             lastTimestamp + interval :
                             TimingVoodoo.roundDownTime(now - interval,
                                                        interval);
                    DataPoint point = new DataPoint(meas.getId(),
                                                    new MetricValue(AVAIL_DOWN,
                                                                    t));
                    rtn.add(new ResourceDataPoint(meas.getResource(), point));
                }
            }
        }
        return rtn;
    }
    
    private class ResourceDataPoint extends DataPoint {
        private Resource _resource;
        public ResourceDataPoint(Resource resource, DataPoint point) {
            super(point.getMetricId().intValue(), point.getValue(),
                point.getTimestamp());
            _resource = resource;
        }
        public Resource getResource() {
            return _resource;
        }
    }

    protected void hitInSession(Date lDate) {
        boolean debug = _log.isDebugEnabled();
        if (debug) {
            _log.debug("Availability Check Service started executing: "+lDate);            
        }
        long current = lDate.getTime();
        // Don't start backfilling immediately
        if (!canStart(current)) {
            _log.debug("not starting availability check");
            return;
        }
        AvailabilityManagerLocal availMan = AvailabilityManagerEJBImpl.getOne();
        AvailabilityCache cache = AvailabilityCache.getInstance();
        synchronized (cache) {
            List downPlatforms = getDownPlatforms(lDate);
            List backfillList = new ArrayList();
            for (Iterator i=downPlatforms.iterator(); i.hasNext(); ) {
                ResourceDataPoint rdp = (ResourceDataPoint)i.next();
                Resource platform = rdp.getResource();
                if (debug) {
                    _log.debug("platform measurement id " + rdp.getMetricId() +
                               " is being marked down");
                }
                backfillList.add(rdp);
                List associatedResources =
                    availMan.getAvailMeasurementChildren(platform);
                if (debug) {
                    _log.debug("platform id " + platform.getId() + " has " +
                        associatedResources.size() + " associated resources");
                }
                for (Iterator j=associatedResources.iterator(); j.hasNext(); ) {
                    Measurement meas = (Measurement)j.next();
                    if (!meas.isEnabled()) {
                        continue;
                    }
                    long end = getEndWindow(current, meas);
                    DataPoint defaultPt =
                        new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                    DataPoint lastPt = cache.get(meas.getId(), defaultPt);
                    long backfillTime =
                        lastPt.getTimestamp() + meas.getInterval();
                    if (backfillTime > current) {
                        continue;
                    }
                    if (debug) {
                        _log.debug("measurement id " + meas.getId() + " is " +
                                   "being marked down, time=" + backfillTime);
                    }
                    MetricValue val = new MetricValue(AVAIL_DOWN, backfillTime);
                    MeasDataPoint point = new MeasDataPoint(
                        meas.getId(), val, meas.getTemplate().isAvailability());
                    backfillList.add(point);
                }
            }
            final int batchSize = 500;
            int i=0;
            for (i=0; (i+batchSize)<backfillList.size(); i+=batchSize) {
                availMan.addData(backfillList.subList(i, i+batchSize));
            }
            if (backfillList.size() > i) {
                availMan.addData(backfillList.subList(i, backfillList.size()));
            }
        }
    }

    private boolean canStart(long now) {
        if (_startTime == 0) {
            _startTime = now;
            return false;
        } else if ((_startTime + _wait) > now) {
            return false;
        }
        return true;
    }

    /**
     * Get the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public long getInterval() {
        return _interval;
    }

    /**
     * Set the interval for how often this mbean is called
     *
     * @jmx:managed-attribute
     */
    public void setInterval(long interval) {
        _interval = interval;
    }
    
    /**
     * Get the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public long getWait() {
        return _wait;
    }

    /**
     * Set the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public void setWait(long wait) {
        _wait = wait;
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
        _log.info("Stopping " + this.getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}
}

