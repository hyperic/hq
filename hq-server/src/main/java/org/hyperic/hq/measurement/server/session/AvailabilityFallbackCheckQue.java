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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * An object that manages a que of Platform IDs for platforms for which fallback availability check needs to be done.
 * Every X seconds, a FallbackChecker polls N platform IDs, checks for their availability, and re-adds them to the Que
 * 
 * <BR><B><U>Que flow:</U></B>
 * <BR><B>addToQue</B> - Platform ID is added to the platformsRecheckQue, if it's not yet there.
 * <BR><B>poll</B> - PlatformID moves from platformsRecheckQue to platformsRecheckInProgress member.
 * <BR><B>beforeDataUpdate</B> - before availability data is about to be stored in the DB/Cache, the que is updated:
 * <BR>    - if this is an update from the server: 
 * <BR>        If PlatformID exists in platformsPendingQueRemoval - it is removed from all the members/
 * <BR>    Otherwise: PlatformID is moved back from platformsRecheckInProgress into platformsRecheckQue 
 *             to be rechecked after the next interval.
 *     <BR>- if this is an update from the agent: 
 *     <BR>    if currently PlatformID is not processed (it is not in platformsRecheckInProgress) - it is removed from all Que members.
 *     <BR>     Otherwise: PlatformID is added to platformsPendingQueRemoval, and will be cleaned once the recheck process ends.
 * 
 * @author amalia
 *
 */
public class AvailabilityFallbackCheckQue {

    
    // TODO: following code review:
    // minimize checks here since these are synchronized updates of availability data.
    // HowTo? Remove the need for platformsRecheckInProgress: Add LastTimeUpdate to the ResourceDataPoint. This is the lastTimeStamp that we know
    // of when starting to calculate availability for a Platform. before availMgr updates data,  
    // where it checks for timestamps mismatches, it can check if the the LastTimeUpdate is before the actual last timestamp in the cache, and if so -
    // remove the platform from the que, and not update it.
    
    private final Log log = LogFactory.getLog(AvailabilityFallbackCheckQue.class);
    
    // a que holding the platforms pending recheck
    private final ConcurrentLinkedQueue<ResourceDataPoint> platformsRecheckQue;
    
    // Map:PlatformID->latest availability ResourceDataPoint>, for quick-access of platformsRecheckQue items. 
    private final Map<Integer, ResourceDataPoint> currentPlatformsInQue;

    // Set of Platform IDs, for platforms whose status is currently checked by AvailabilityFallbackChecker.
    private final Set<Integer> platformsRecheckInProgress;
    
    // Set of Platform IDs, for platforms currently rechecked, which agent information was sent.
    private final Set<Integer> platformsPendingQueRemoval;
    
    // Map: MeasurementID->PlatformID for quick-access
    private final Map<Integer,Integer> measurementIdToPlatformId;

    
    
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    /**
     * Default CTor
     */
    public AvailabilityFallbackCheckQue() {
        this.platformsRecheckQue = new ConcurrentLinkedQueue<ResourceDataPoint>();
        this.currentPlatformsInQue = new HashMap<Integer, ResourceDataPoint>();
        this.platformsRecheckInProgress = new HashSet<Integer>();
        this.platformsPendingQueRemoval = new HashSet<Integer>();
        this.measurementIdToPlatformId = new HashMap<Integer,Integer>();
    }
    
    public void clearQue() {
           log.info("Clearing availability check queues.");
        this.platformsRecheckQue.clear();
        this.currentPlatformsInQue.clear();
        this.platformsRecheckInProgress.clear();
        this.platformsPendingQueRemoval.clear();
        this.measurementIdToPlatformId.clear();
    }
    
    /**
     * @return the number of Platforms pending availability status check
     */
    public synchronized int getSize(){
        return this.platformsRecheckQue.size();
    }
    
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    
    
    /**
     * Add a single platform to the rechecks que.
     * @param platformId - the platform to be checked.
     * @param dataPoint - the latest status associated with the platform
     * @return true, if added. false, if the platform is already in the que and will not be re-added.
     */
    public synchronized boolean addToQue(Integer platformId, ResourceDataPoint dataPoint) {
        if (log.isDebugEnabled()) {
           log.debug("addToQue: start resourcePlatformId=" + platformId+ ", curQueSize: " + getSize());
        }
        if (this.currentPlatformsInQue.containsKey(platformId)) {
            // fix for jira issue HQ-4325 - keep the existing platform down data
            // point timestamp updated to the last availability check time
            ResourceDataPoint existingDataPoint = this.currentPlatformsInQue.get(platformId);
            existingDataPoint.getMetricValue().setTimestamp(dataPoint.getTimestamp());
            // in case there was an agent update in the middle, we do not want to remove it from the recheck que.
            //this.platformsPendingQueRemoval.remove(platformId);
            return false;
        }
        this.platformsRecheckQue.add(dataPoint);
        this.currentPlatformsInQue.put(platformId, dataPoint);
        this.measurementIdToPlatformId.put(dataPoint.getMeasurementId(), platformId);
        return true;
    }
    
    /**
     * Adds a collection of platforms to the rechecks que. Platforms that already exist in the que will not be re-added.
     * @param platformsToAdd - map:PlatformID->latest status associated with the platform
     * @return the number of platforms actually added.
     */
    public synchronized int addToQue(Map<Integer, ResourceDataPoint> platformsToAdd) {
        int res = 0;
        for (Integer platformId : platformsToAdd.keySet()) {
            boolean added = addToQue(platformId, platformsToAdd.get(platformId));
            res += added ? 1 : 0;
        }
        if (log.isDebugEnabled()) {
           log.debug("addToQue: added "+res);
        }
        return res;
    }
    
    /**
     * Poll a platform to check availability for,
     * @return the next ResourceDataPoint in the que. The ResourceDataPoint is the latest status of the checked platform. Return null if the que is empty.
     */
    public synchronized ResourceDataPoint poll() {
        if (log.isDebugEnabled()) {
           log.debug("poll: start, que size: " + getSize());
        }
        ResourceDataPoint res = this.platformsRecheckQue.poll();
        if (res != null) {
            Integer platformId = res.getResource().getId();
            this.platformsRecheckInProgress.add(platformId);
        }
        return res;
    }


    /**
     * A method to be called before storing availability status updates.
     * <BR> This method updates the que and filters the given dataPoints list, so that agent updates will always override server calculations.
     * <BR> In case the update is from the agent:
     * <BR>     - the returned list is identical to the input list.
     * <BR>     - all platforms that are about to be updated are removed from the que (or added to the PendingRemoval list, if check is in progress).
     * <BR> In case the update is from the server:
     * <BR>     - if the platform are in the PendingRemoval list - they are removed completely from the Que, and not inserted into the result.
     * <BR>     - Otherwise - they are added into the result, and platforms are re-inserted into the recheckQue.
     * <BR> <B> Notes: </B>
     * <BR> - Case 1: agent updated platform status which recheck was done. In this case, the platform's descendants will not be added into the dataPoints
     * input since the checker will not add descendants with updated availability status. In case it will: the descendant status will remain as "UNKNOWN" until 
     * the next agent status update (half-an-interval or so).
     * <BR>    - Case 2: Over-checking: A platform is checked and then re-inserted to the que. 
     * <BR> In case of threads working on the same que, we may get over-checking, instead of checking every 2 minutes or so.
     * <BR> If we start working with several threads we need to use 2 queues intermittently - all threads poll from Que1 until the que is empty, 
     * every checked platform is inserted into Que2. In the next checks-round - poll from Que2 and insert into Que1.
     * @param dataPoints - a collection of availability datapoints about to be added.
     * @param isUpdateFromServer - true, if the server fallback checks asked for the updated, false if the agent updated with the status.
     * @return a filtered collection of datapoints, the ones that should be updated in the DB/cache
     */
    public synchronized Collection<DataPoint> beforeDataUpdate(Collection<DataPoint> dataPoints,
                                                               boolean isUpdateFromServer) {
        Collection<DataPoint> res = new ArrayList<DataPoint>();
        Collection<DataPoint> removed = new ArrayList<DataPoint>();
        final boolean debug = log.isDebugEnabled();
        for (DataPoint dataPoint : dataPoints) {
            Integer measurementId = dataPoint.getMeasurementId();
            Integer resourceId = this.measurementIdToPlatformId.get(measurementId);
            if (resourceId == null) {
                // we do not know this resource as a platformId. one of:
                // 1. this is not a platform - update it.
                // 2. this is a platform that is not in the recheck que - update it.
                res.add(dataPoint);
                continue;
            }
            Integer platformId = resourceId;
            if (isUpdateFromServer) {
                this.platformsRecheckInProgress.remove(platformId);
                if (this.platformsPendingQueRemoval.contains(platformId)) {
                    // this should not be updated from the server.
                    this.platformsPendingQueRemoval.remove(platformId);
                    ResourceDataPoint platformDataPoint = this.currentPlatformsInQue.get(platformId);
                    this.platformsRecheckQue.remove(platformDataPoint);
                    this.measurementIdToPlatformId.remove(measurementId);
                    this.currentPlatformsInQue.remove(platformId);
                    if (debug) {
                        removed.add(dataPoint);
                    }
                } else {
                    res.add(dataPoint);
                    ResourceDataPoint queDataPoint = this.currentPlatformsInQue.get(platformId);
                    ResourceDataPoint pointToAddToQue = new ResourceDataPoint(queDataPoint.getResource(), dataPoint);
                    this.currentPlatformsInQue.remove(platformId);
                    addToQue(platformId, pointToAddToQue);
                }    
            } else {
                // update from agent
                res.add(dataPoint);
                removeFromQueBeforeUpdateFromAgent(platformId);
            }
        }
        if (isUpdateFromServer) {
            if (debug) {
                log.debug("beforeDataUpdate from Server: updating: " + res.size() + " out of " + dataPoints.size() +
                      ", removed=" + removed);
            }
        }
        return res;
    }
    
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    // return platform Id
    private synchronized Integer removeFromCurrentPlatformsInQue(ResourceDataPoint dp) {
        Integer platformId = null;
         for (Entry<Integer, ResourceDataPoint> entry : currentPlatformsInQue.entrySet()) {
             if (dp.equals(entry.getValue())) {
                 platformId = entry.getKey();
                 break;
             }
         }

         if (platformId != null) {
             currentPlatformsInQue.remove(platformId);
         }
         return platformId;
    }
    
    // return measurement Id
    private synchronized Integer removeFromMeasIdToPlatId(Integer platformId) {
        Integer measId = null;
         for (Entry<Integer, Integer> entry : measurementIdToPlatformId.entrySet()) {
             if (platformId.equals(entry.getValue())) {
                 measId = entry.getKey();
                 break;
             }
         }

         if (measId != null) {
             measurementIdToPlatformId.remove(measId);
         }
         return measId;
    }
    
    
    public synchronized int cleanQueFromNonExistant() {
        int res = 0;
        Collection<ResourceDataPoint> pointsToDel = new ArrayList<ResourceDataPoint>();
        for (ResourceDataPoint dp : this.platformsRecheckQue) {
            if ((dp.getResource() == null) || (dp.getResource().isInAsyncDeleteState()) ) {
                res++;
                pointsToDel.add(dp);
                Integer platformId = removeFromCurrentPlatformsInQue(dp);
                if (platformId != null) {
                    removeFromMeasIdToPlatId(platformId);
                }
            }            
        }
        platformsRecheckQue.removeAll(pointsToDel);
        if ((res != 0) && log.isDebugEnabled()) {
            log.debug("cleanQueFromNonExistant: removing " + res + " platforms.");
        }
        return res;
    }

    public synchronized int removeFromQue(Collection<Integer> platformResourceIds) {
        int res = 0;
        final boolean debug = log.isDebugEnabled();
        for (Integer platformId : platformResourceIds) {
            if (this.platformsRecheckInProgress.contains(platformId)) {
                if (debug) {
                    log.debug("0 adding to pending queue platformId=" + platformId+ ", curQueSize: " + getSize());
                }
                this.platformsPendingQueRemoval.add(platformId);
                continue;
            }
            ResourceDataPoint platformDataPoint = this.currentPlatformsInQue.get(platformId);
            if (platformDataPoint == null) {
                // this point is not in the que.
                continue;
            }
            //else 
            this.platformsRecheckQue.remove(platformDataPoint);
            this.currentPlatformsInQue.remove(platformId); 
            Integer measId = platformDataPoint.getMeasurementId();
            this.measurementIdToPlatformId.remove(measId);
            res++;
        }
        return res;
    }
    
    
    private synchronized boolean removeFromQueBeforeUpdateFromAgent(Integer platformId) {
        if (this.platformsRecheckInProgress.contains(platformId)) {
            if (log.isDebugEnabled()) {
                log.debug("adding to pending queue removal platformId=" + platformId+ ", curQueSize: " + getSize());
            }
            this.platformsPendingQueRemoval.add(platformId);
            return true;
        }
        

        ResourceDataPoint platformDataPoint = this.currentPlatformsInQue.get(platformId);
        if (platformDataPoint == null) {
            // this point is not in the que.
            return false;
        }
        //else 
        this.platformsRecheckQue.remove(platformDataPoint);
        this.currentPlatformsInQue.remove(platformId); 
        Integer measId = platformDataPoint.getMeasurementId();
        this.measurementIdToPlatformId.remove(measId);
        
        return true;
    }

    


}
