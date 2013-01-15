/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2013], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.product.MetricValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Availability status checker for platforms which availability status data was not received for over 2 intervals.
 * <BR><B>Details:</B>
 * <BR> The checker receives a collection of DataPoints (latest availability status) for platforms that need rechecking.
 * <BR> It checks if there exists VC associations for the platform (if this platform status is given by a VM agent while
 * there also exists a VCented agent monitoring it). 
 * If so - update the status according to the status given by the VCenter agent.
 * <BR> If VC information exists and the Platform is UP - all its servers/services status is set as UNKNOWN.
 * <BR> If VC information does not exist, or is DOWN - all its servers/services status is set as DOWN.
 * <BR> Agent status is marked as DOWN in any case.
 * @author amalia
 *
 */
@Component
public class AvailabilityFallbackChecker {
    
    //TODO: (Code review comments)
    // Handle the case of 2 very different intervals. If the VM interval is 1 min, and the VC interval is 1 hr? Perhaps should limit the availability status validity to 5(?) times the VM interval.
    
    private final Log log = LogFactory.getLog(AvailabilityFallbackChecker.class);

    private AvailabilityManager availabilityManager;
    private AvailabilityCache availabilityCache;
    private ResourceManager resourceManager;
    
    // For testing purposes, in case we need to perform checks with a constant timestamp.
    // if curTimeStamp is 0, we check for the actual current time. 
    private long curTimeStamp = 0;
    
    private final int MAX_UPDATES_PER_BATCH = 1000;

    // --------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------
    @Autowired
    public AvailabilityFallbackChecker(AvailabilityManager availabilityManager, AvailabilityCache availabilityCache,
                                       ResourceManager resourceManager) {
        this.availabilityManager = availabilityManager;
        this.availabilityCache = availabilityCache;
        this.resourceManager = resourceManager;
    }

    // --------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------
    
    /**
     * Check platforms' availability with constant time stamp all through the check. Update DB/Cache accordingly.
     * This means that if we use Ping checks (that may take several seconds), the timestamp will remain the same.
     * Platforms' servers/services statuses may be updated as well.
     * @param availabilityDataPoints - latest availability status for Platforms
     * @param curTimeStamp - timestamp to use through the checks
     */
    public void checkAvailability(Collection<ResourceDataPoint> availabilityDataPoints, long curTimeStamp) {
        this.curTimeStamp = curTimeStamp;
        checkAvailabilityInChunks(availabilityDataPoints);
        this.curTimeStamp = 0;
    }

    /**
     * Check platforms' availability. Update DB/Cache accordingly.
     * Platforms' servers/services statuses may be updated as well.
     * @param availabilityDataPoints
     */
    public void checkAvailability(Collection<ResourceDataPoint> availabilityDataPoints) {
        if ((availabilityDataPoints == null) || (availabilityDataPoints.isEmpty()) ) {
            return;            
        }
        final boolean debug = log.isDebugEnabled();
        if (debug) log.debug("checkAvailability: start");
        final Collection<ResourceDataPoint> resPlatforms = new ArrayList<ResourceDataPoint>();
        final long currTime = getCurTimestamp();
        for (ResourceDataPoint availabilityDataPoint : availabilityDataPoints) {
            //log.info("checkAvailability-Platform: " + availabilityDataPoint.getResource().getId() + " value: " + availabilityDataPoint.getValue());
            ResourceDataPoint platformAvailPoint = checkPlatformAvailability(availabilityDataPoint, currTime);
            //log.info("checkAvailability-Platform: " + platformAvailPoint.getResource().getId() + " value: " + platformAvailPoint.getValue());
            resPlatforms.add(platformAvailPoint);
        }
        if (debug) log.debug("checkAvailability: checking " + resPlatforms.size() + " platforms.");
        List<DataPoint> res = addStatusOfPlatformsDescendants(resPlatforms);
        if (debug) log.debug("checkAvailability: updating " + res.size() + " platforms & descendants.");
        if (!res.isEmpty()) {
            synchronized (availabilityCache) {
                availabilityManager.addData(res, true, true);
            }
        }
    }
    
    /**
     * This method is written in order to solve issue: HHQ-5619:
     * Backfiller causes StackOverflow when trying to update 73000 resources' availability.
     * The reason for the Stackoverflow is a Hibernate 3.2 bug in which queries above 9000-10000 IDs fail with NodeTraverse StackOverflow.
     * TODO: This method should be removed once Hibernate is upgraded.
     * 
     *  This method is the same as CheckAvailability, only the update is done in chunks of upto MAX_UPDATES_PER_BATCH.
     *  The updates are divided into clusters, a cluster per Platform and its Servers and Services.
     *  We merge several Platform Clusters into a single Chunk, so its size does not exceed MAX_UPDATES_PER_BATCH.
     *  Each such chunk is updated separately.
     *  
     * Check platforms' availability. Update DB/Cache accordingly.
     * Platforms' servers/services statuses may be updated as well.
     * @param availabilityDataPoints
     */
    public void checkAvailabilityInChunks(Collection<ResourceDataPoint> availabilityDataPoints) {
        if ((availabilityDataPoints == null) || (availabilityDataPoints.isEmpty())) {
            return;
        }
        final boolean debug = log.isDebugEnabled();
        if (debug) log.debug("checkAvailability: start");
        Collection<ResourceDataPoint> resPlatforms = new ArrayList<ResourceDataPoint>();
        final long currTime = getCurTimestamp();
        for (ResourceDataPoint adp : availabilityDataPoints) {
            ResourceDataPoint platformAvail = checkPlatformAvailability(adp, currTime);
            if (debug) log.debug("checkAvailability before resourceId=" + adp + ", after resourceId=" + platformAvail);
            resPlatforms.add(platformAvail);
        }
        if (debug) log.debug("marking " + resPlatforms.size() + " platforms down, resourceIds=" + resPlatforms);
        List<DataPoint> datapoints = getAllHierarchyMeasurementData(resPlatforms);
        // HHQ-5726 - only one thread can access availabilityManager.addData() at once
        synchronized (availabilityCache) {
            for (int i=0; i<datapoints.size(); i+=MAX_UPDATES_PER_BATCH) {
                int max = Math.min(i + MAX_UPDATES_PER_BATCH, datapoints.size());
                List<DataPoint> sublist = datapoints.subList(i, max);
                availabilityManager.addData(sublist, true, true);
            }
        }
    }
    
    /**
     * check if the given Measurement belongs to an HQ Agent, and if so - mark it as down.
     * @param meas - Measurement of a checked server/service.
     * @return true if this is an HQAgent, false otherwise.
     */
    private boolean isHQAgent(Measurement meas) {
        Resource measResource = meas.getResource();
        // log.debug("isHQHagent? " + measResource.getName());
        // TODO remove the following line, and recheck
        measResource = resourceManager.getResourceById(measResource.getId());
        Resource prototype = measResource.getPrototype();
        if (prototype == null) {
            return false;
        }
        String prototypeName = prototype.getName();
        if (prototypeName.equals(AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME)) {
            if (log.isDebugEnabled()) {
                log.debug("isHQHagent:  Found: " + measResource.getId());
            }
            return true;
        }
        return false;
    }

    /**
     * Check availability for a single platform
     * @param availabilityDataPoint - latest availability status
     * @return new availability status to update
     */
    private ResourceDataPoint checkPlatformAvailability(ResourceDataPoint availabilityDataPoint, long currTimestamp) {
        ResourceDataPoint res = getPlatformStatusFromVC(availabilityDataPoint);
        if (res != null) {
            return res;            
        }
        // If we want to add a check using ping:
        //res = getPlatformStatusByPing(availabilityDataPoint);
        //if (res != null)
        //    return res;
        res = availabilityDataPoint;
        if (availabilityDataPoint.getMetricValue().getValue() != MeasurementConstants.AVAIL_DOWN) {
            DataPoint resDP = new DataPoint(availabilityDataPoint.getMeasurementId(), MeasurementConstants.AVAIL_DOWN, currTimestamp);
            res = new ResourceDataPoint(availabilityDataPoint.getResource(), resDP);            
        }
        return res;
    }

    /**
     * Check for availability status from VC information, if exists.
     * VC information exists if this platform is monitored by a VM agent, and there also exists a VCenter agent that monitors this platform.
     * @param availabilityDataPoint - latest availability status
     * @return new availability status to update, if exists. Null otherwise.
     */
    private ResourceDataPoint getPlatformStatusFromVC(ResourceDataPoint availabilityDataPoint) {
        Integer platformId = availabilityDataPoint.getResource().getId();
        if (log.isDebugEnabled()) {
            log.debug("getPlatformStatusFromVC: platformId" + platformId);            
        }
        List<Integer> resourceIds = new ArrayList<Integer>();
        resourceIds.add(platformId);
        final Map<Integer, List<Measurement>> virtualParent = availabilityManager.getAvailMeasurementDirectParent(
                resourceIds, AuthzConstants.ResourceEdgeVirtualRelation);
        if (isEmptyMap(virtualParent)) {
            return null;
        }
        // else - there should be a single measurement of the related VM Instance:
        List<Measurement> resourceEdgeVirtualRelations = virtualParent.get(platformId);
        if ((resourceEdgeVirtualRelations == null) | (resourceEdgeVirtualRelations.isEmpty()) ) {
            //log.info("getPlatfromStatusFromVC: Platform " + platformId + " got no virtual parents. Ignoring.");
            return null;
        }
        if (resourceEdgeVirtualRelations.size() != 1) {
            log.warn("getPlatfromStatusFromVC: Platform " + platformId + " got " + resourceEdgeVirtualRelations.size() + " virtual parents. Ignoring.");
            return null;
        }
        // we now have the VM Instance Measurement ID. We will copy its latest availability status
        Measurement vmParentMeasurement = resourceEdgeVirtualRelations.get(0);
        long endTimeStamp = getEndWindow(getCurTimestamp(), vmParentMeasurement);
        final DataPoint defaultParentDataPoint = new DataPoint(vmParentMeasurement.getId().intValue(), MeasurementConstants.AVAIL_NULL, endTimeStamp);
        DataPoint lastParentDataPoint = null;
        synchronized (availabilityCache) {
            lastParentDataPoint = availabilityCache.get(vmParentMeasurement.getId(), defaultParentDataPoint);
        }
        if (lastParentDataPoint == null) {
            return null;            
        }
        
        double parentStatus = lastParentDataPoint.getValue();
        if ((parentStatus == MeasurementConstants.AVAIL_UP) || (parentStatus == MeasurementConstants.AVAIL_DOWN)) {
            DataPoint newDataPoint = new DataPoint(availabilityDataPoint.getMeasurementId(), lastParentDataPoint.getMetricValue());
            ResourceDataPoint resPoint = new ResourceDataPoint(availabilityDataPoint.getResource(), newDataPoint);
            if (log.isDebugEnabled()) {
                log.debug("getPlatformStatusFromVC: found parent measurement: " + lastParentDataPoint.getMeasurementId() + 
                          "; adding point: " + resPoint.toString());
            }
            return resPoint;
        }

        return null;
    }
    
    private boolean isEmptyMap(Map<Integer, List<Measurement>> rHierarchy) {
        if (rHierarchy == null) {
            return true;            
        }
        if (rHierarchy.size() ==0) {
            return true;            
        }
        if (rHierarchy.isEmpty()) {
            return true;            
        }
        return false;
    }

    /**
     * Given a list of platforms' data points, return a collection of datapoints of platforms' servers an services,
     * with their appropriate status.
     * @param checkedPlatforms - new calculated availability status of platforms.
     * @return collection of statuses of the platforms' servers an services.
     */
    private List<DataPoint> getAllHierarchyMeasurementData(Collection<ResourceDataPoint> checkedPlatforms) {
        final boolean debug = log.isDebugEnabled();
        if (debug) log.debug("addStatusOfPlatformsDescendants: start");
        final List<DataPoint> res = new ArrayList<DataPoint>();
        final Map<Integer, List<Measurement>> rHierarchy = getMeasurementHierarchy(checkedPlatforms);
        final long currTimestamp = getCurTimestamp();
        for (ResourceDataPoint rdp : checkedPlatforms) {
            final Resource platform = rdp.getResource();
            res.add(rdp);
            final List<Measurement> associatedMeasurements = rHierarchy.get(platform.getId());
            if (associatedMeasurements == null) {
                continue;
            }
            double assocStatus = MeasurementConstants.AVAIL_DOWN;
            if (rdp.getMetricValue().getValue() == MeasurementConstants.AVAIL_UP) {
                assocStatus = MeasurementConstants.AVAIL_UNKNOWN;
            }
            for (Measurement meas : associatedMeasurements) {
                if (!meas.isEnabled()) {
                    continue;
                }
                double curStatus = assocStatus;
                if (isHQAgent(meas)) {
                    curStatus = MeasurementConstants.AVAIL_DOWN;
                }
                final long end = getEndWindow(currTimestamp, meas);
                final DataPoint defaultPt = new DataPoint(meas.getId().intValue(), MeasurementConstants.AVAIL_NULL, end);
                DataPoint lastPt = availabilityCache.get(meas.getId(), defaultPt);
                final long backfillTime = lastPt.getTimestamp() + meas.getInterval();
                if (backfillTime > currTimestamp) {
                    // the resource was updated during the last interval. we do
                    // not want to update it.
                    // TODO: Shouldn't platform be marked as UP?
                    continue;
                }
                final MetricValue val = new MetricValue(curStatus, backfillTime);
                final MeasDataPoint point = new MeasDataPoint(meas.getId(), val, true);
                res.add(point);
            }
        }
        if (debug) log.debug("addStatusOfPlatformsDescendants: end, res size: " + res.size());
        return res;
    }
    
    private Map<Integer, List<Measurement>> getMeasurementHierarchy(Collection<ResourceDataPoint> checkedPlatforms) {
        final List<Integer> resourceIds = new ArrayList<Integer>();
        for (ResourceDataPoint rDataPoint : checkedPlatforms) {
            resourceIds.add(rDataPoint.getResource().getId());
        }
        return availabilityManager.getAvailMeasurementChildren(resourceIds, AuthzConstants.ResourceEdgeContainmentRelation);
    }

    /**
     * Given a list of platforms' data points, return a collection of datapoints of platforms' servers an services,
     * with their appropriate status.
     * @param checkedPlatforms - new calculated availability status of platforms.
     * @return collection of statuses of the platforms' servers an services.
     */
    private List<DataPoint> addStatusOfPlatformsDescendants(Collection<ResourceDataPoint> checkedPlatforms) {
        log.debug("addStatusOfPlatformsDescendants: start" );
        final List<DataPoint> res = new ArrayList<DataPoint>();
        final List<Integer> resourceIds = new ArrayList<Integer>();
        for (ResourceDataPoint rDataPoint : checkedPlatforms) {
            resourceIds.add(rDataPoint.getResource().getId());
        }
        final Map<Integer, List<Measurement>> rHierarchy = availabilityManager.getAvailMeasurementChildren(
                resourceIds, AuthzConstants.ResourceEdgeContainmentRelation);
        final long curTimeStamp = getCurTimestamp();
        for (ResourceDataPoint rdp : checkedPlatforms) {
            final Resource platform = rdp.getResource();
            res.add(rdp);
            final List<Measurement> associatedResources = rHierarchy.get(platform.getId());
            if (associatedResources == null) {
                continue;
            }
            double assocStatus = MeasurementConstants.AVAIL_DOWN;
            if (rdp.getMetricValue().getValue() == MeasurementConstants.AVAIL_UP) {
                assocStatus = MeasurementConstants.AVAIL_UNKNOWN;
            }
            for (Measurement meas : associatedResources) {
                if (!meas.isEnabled()) {
                    continue;
                }
                double curStatus = assocStatus;
                if (isHQAgent(meas)) {
                    curStatus = MeasurementConstants.AVAIL_DOWN;                    
                }
                final long backfillTime = getBackfillTime(curTimeStamp, meas);
                if (backfillTime > curTimeStamp) {
                    // the resource was updated during the last interval. we do not want to update it.
                    // TODO: Shouldn't platform be marked as UP?
                    continue;
                }
                final MetricValue val = new MetricValue(curStatus, backfillTime);
                final MeasDataPoint point = new MeasDataPoint(meas.getId(), val, true);
                res.add(point);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("addStatusOfPlatformsDescendants: end, res size: " + res.size() );            
        }
        return res;
    }
    
    /**
     * get the time of the first interval that was not updated.
     * @param current - current time stamp
     * @param meas - measurement to check for
     * @return the time of the first interval that was not updated.
     */
    private long getBackfillTime(long current, Measurement meas) {
        final long end = getEndWindow(current, meas);
        final DataPoint defaultPt = new DataPoint(meas.getId().intValue(), MeasurementConstants.AVAIL_NULL, end);
        DataPoint lastPt = availabilityCache.get(meas.getId(), defaultPt);
        final long backfillTime = lastPt.getTimestamp() + meas.getInterval();
        return backfillTime;
    }
    
    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
        return TimingVoodoo.roundDownTime((current - meas.getInterval()), meas.getInterval());
    }


    
    /**
     * if curTimeStamp is 0, return the real current time.
     * Otherwise - return curTimeStamp set by the calling method.
     * @return time
     */
    private long getCurTimestamp() {
        if (curTimeStamp != 0) {
            return curTimeStamp;
        }
        return System.currentTimeMillis();
    }


}
