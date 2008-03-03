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
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SessionMBeanBase;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.LastAvailUpObj;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.server.session.SRNCache;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.shared.AvailState;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.TimeUtil;
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
    private final String _logCtx = AvailabilityCheckService.class.getName();
    private Log _log = LogFactory.getLog(_logCtx);
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;

    private long _interval = 0;
    private long _startTime = 0;
    private long _wait = 5 * MeasurementConstants.MINUTE;

    private AvailabilityManagerLocal getAvailMan() {
        return AvailabilityManagerEJBImpl.getOne();
    }

    private DataManagerLocal getDataMan() {
        return DataManagerEJBImpl.getOne();
    }

    private MeasurementManagerLocal getMeasurementManager() {
        return MeasurementManagerEJBImpl.getOne();
    }

    /**
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        super.hit(lDate);
    }

    private List getDownPlatforms(Date lDate,
        AvailabilityManagerLocal availMan) {
        boolean debug = _log.isDebugEnabled();
        long lnow = lDate.getTime();
        int inow = (new Long(lnow/1000)).intValue();
        LastAvailUpObj avail = LastAvailUpObj.getInst();
        List platformResources = availMan.getPlatformResources();
        List rtn = new ArrayList(platformResources.size());
        synchronized (avail) {
            for (Iterator i=platformResources.iterator(); i.hasNext(); ) {
                Object[] array = (Object[])i.next();
                Measurement meas = (Measurement)array[0];
                Resource resource = (Resource)array[1];
                int interval = new Long(meas.getInterval()/1000).intValue();
                AvailState last = avail.get(meas.getId(),
                    new AvailState(meas.getId().intValue(), AVAIL_NULL, inow));
                if (debug) {
                    long t = new Long(last.getTimestamp()).longValue()*1000;
                    String msg = "Checking availability for " + last +
                        ", " + TimeUtil.toString(t) +
                        " vs. " + TimeUtil.toString(lnow) + " (checktime)";
                    _log.debug(msg);
                }
                if (last.getVal() != AVAIL_DOWN &&
                    (inow - last.getTimestamp()) >= interval*2) {
                    DataPoint point =  new DataPoint(meas.getId(),
                        new MetricValue(AVAIL_DOWN,
                        (new Long(last.getTimestamp()+interval*2))
                            .longValue()*1000));
                    ResourceDataPoint rdp =
                        new ResourceDataPoint(resource, point);
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
        List downPlatforms = getDownPlatforms(lDate, availMan);
        List backfillList = new ArrayList(downPlatforms.size()*50);
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
        this._interval = interval;
    }
    
    /**
     * Get the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public long getWait() {
        return this._wait;
    }

    /**
     * Set the wait for how long after service starts to backfill
     *
     * @jmx:managed-attribute
     */
    public void setWait(long wait) {
        this._wait = wait;
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

