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
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.LastAvailUpObj;
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

    private List getDownPlatforms(Date lDate) {
        boolean debug = _log.isDebugEnabled();
        long now = lDate.getTime();
        LastAvailUpObj avail = LastAvailUpObj.getInst();
        AvailabilityManagerLocal availMan = AvailabilityManagerEJBImpl.getOne();
        List platformResources = availMan.getPlatformResources();
        List rtn = new ArrayList(platformResources.size());
        synchronized (avail) {
            for (Iterator i = platformResources.iterator(); i.hasNext();) {
                Measurement meas = (Measurement)i.next();
                long interval = meas.getInterval();
                DataPoint last = avail.get(meas.getId(),
                    new DataPoint(meas.getId().intValue(), AVAIL_NULL, now));
                if (debug) {
                    long t = last.getTimestamp();
                    String msg = "Checking availability for " + last +
                        ", " + TimeUtil.toString(t) +
                        " vs. " + TimeUtil.toString(now) + " (checktime)";
                    _log.debug(msg);
                }
                if (last.getValue() != AVAIL_DOWN &&
                    (now - last.getTimestamp()) >= interval*2) {
                    DataPoint point =  new DataPoint(meas.getId(),
                        new MetricValue(AVAIL_DOWN,
                                        last.getTimestamp()+ interval * 2));
                    ResourceDataPoint rdp =
                        new ResourceDataPoint(meas.getResource(), point);
                    rtn.add(rdp);
                }
            }
        }
        return rtn;
    }
    
    private class ResourceDataPoint {
        private Resource _resource;
        private DataPoint _point;
        public ResourceDataPoint(Resource resource, DataPoint point) {
            _resource = resource;
            _point = point;
        }
        public DataPoint getDataPoint() {
            return _point;
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
            return;
        }
        AvailabilityManagerLocal availMan = AvailabilityManagerEJBImpl.getOne();
        List downPlatforms = getDownPlatforms(lDate);
        List backfillList = new ArrayList();
        for (Iterator i=downPlatforms.iterator(); i.hasNext(); ) {
            ResourceDataPoint rdp = (ResourceDataPoint)i.next();
            Resource platform = rdp.getResource();
            DataPoint point = rdp.getDataPoint();
            backfillList.add(point);
            List associatedResources =
                availMan.getAvailMeasurementChildren(platform);
            if (debug) {
                _log.debug("platform id " + platform.getId() + " has " +
                    associatedResources.size() + " associated resources");
            }
            for (Iterator j=associatedResources.iterator(); j.hasNext(); ) {
                Measurement meas = (Measurement)j.next();
                if (debug) {
                    _log.debug("measurement id " + meas.getId() + " is " +
                               "being marked down");
                }
                point = new DataPoint(meas.getId(),
                    new MetricValue(AVAIL_DOWN, current));
                backfillList.add(point);
            }
        }
        availMan.addData(backfillList);
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

