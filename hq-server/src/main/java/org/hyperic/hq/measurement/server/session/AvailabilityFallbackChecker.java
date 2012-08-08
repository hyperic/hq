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
import org.springframework.transaction.annotation.Transactional;



/**
 * Run(List<DataPoint<platformId,curTimeStamp,availabilityStatus>)
For each given Platform:
-	Check for VC info. If exists:
o	If associated Platform is up, set DataPoint's status to UP_PENDING_CHECK.
o	If associated Platform is down, set DataPoint's status to DOWN_PENDING_CHECK. 
o	If associated Platform is marked as _PENDING_RECHECK, copy its status.
o	Update DataPoint's timestamp.
-	If VC info does not exist, try Ping-ing:
o	If ping succeeds, set DataPoint's status to UP_PENDING_RECHECK.
o	If Platform is down, set DataPoint's status to DOWN_PENDING_RECHECK.
o	Update DataPoint's timestamp.
-	If availability status had changed from the input given, modify platform's descendants' status, and add their DataPoints to the given List:
o	If new status is DOWN_PENDING, add a DataPoint for each descendant: 
<id, timestamp,DOWN_PENDING_RECHECK>.
o	If new status is UP_PENDING, add a DataPoint for each descendant: <id,timestamp,UNKNOWN>.

 * @author amalia
 *
 */
public class AvailabilityFallbackChecker {
	
    private final Log log = LogFactory.getLog(AvailabilityFallbackChecker.class);
    private final Object lock = new Object();

	private AvailabilityManager availabilityManager;
	private AvailabilityCache availabilityCache;
	private ResourceManager resourceManager;
	//AvailabilityDataDAO availabilityDao;
	private long curTimeStamp = 0;

	
    private void logDebug(String message) {
    	if (availabilityManager.isDevDebug())
    		log.info("aaa==========:" + message);
    	else
    		log.debug(message);
    }
    
    

	public AvailabilityFallbackChecker(AvailabilityManager availabilityManager, AvailabilityCache availabilityCache, ResourceManager resourceManager) {
		this.availabilityManager = availabilityManager;
		this.availabilityCache = availabilityCache;
		this.resourceManager = resourceManager;
	}
	

	public void checkAvailability(Collection<ResourceDataPoint> availabilityDataPoints, long curTimeStamp) {
		this.curTimeStamp = curTimeStamp;
		checkAvailability(availabilityDataPoints);
		this.curTimeStamp = 0;		
	}

	
	public void checkAvailability(Collection<ResourceDataPoint> availabilityDataPoints) {
		log.info("checkAvailability: start");
		Collection<ResourceDataPoint> resPlatforms = new ArrayList<ResourceDataPoint>();
		for (ResourceDataPoint availabilityDataPoint : availabilityDataPoints) {
			ResourceDataPoint platformAvailPoint = checkPlatformAvailability(availabilityDataPoint);
			resPlatforms.add(platformAvailPoint);
			logDebug("checkAvailability: " + platformAvailPoint.getValue());
			//addPlatformDescendants(platformAvailPoint, res);
		}
		log.info("checkAvailability: found " + resPlatforms.size() + " Platforms.");
		Collection<DataPoint> res = addStatusOfPlatformsDescendants(resPlatforms);
		log.info("checkAvailability: found " + res.size() + " platforms & descendants.");
		//res.addAll(availabilityDataPoints);
		storeUpdates(res);
		logDebug("checkAvailability: end");
	}

    public Object getLock() {
        return lock;
    }


    
    
	// check if agent, and if so - mark it as down.
	private boolean isHQAgent(Measurement meas) {
		try {
			// Need to re-attach the Resource, since session may be closed.
			//Session session = SessionFactoryUtils.getSession(availabilityDao.getFactory(), true);
			
			Resource measResource = meas.getResource();
			measResource = resourceManager.getResourceById(measResource.getId());
			Resource prototype = measResource.getPrototype();
			if (prototype == null)
				return false;
			
			String prototypeName = prototype.getName();
			if (prototypeName.equals(AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME)) {
				log.info("isHQHagent:  Found: " + measResource.getId());
				return true;
			}
		} catch (Exception e) {
			logDebug(e.toString());
			return false;
		} 
		return false;
		
	}


	private void storeUpdates(Collection<DataPoint> availabilityDataPoints) {
		List<DataPoint> availDataPoints = new ArrayList<DataPoint>(availabilityDataPoints);
		this.availabilityManager.addData(availDataPoints, true, true);
	}

	private ResourceDataPoint checkPlatformAvailability(ResourceDataPoint availabilityDataPoint) {
		logDebug("checkPlatformAvailability");
		ResourceDataPoint res = getPlatformStatusFromVC(availabilityDataPoint);
		if (res != null)
			return res;
		res = getPlatformStatusByPing(availabilityDataPoint);
		if (res != null)
			return res;
		return availabilityDataPoint;
	}


	private ResourceDataPoint getPlatformStatusByPing(ResourceDataPoint availabilityDataPoint) {
		logDebug("getPlatformStatusByPing" );
		// TODO Auto-generated method stub
		return null;
	}


	private ResourceDataPoint getPlatformStatusFromVC(ResourceDataPoint availabilityDataPoint) {
		logDebug("getPlatformStatusFromVC" );
		Integer platformId = availabilityDataPoint.getResource().getId();
		List<Integer> resourceIds = new ArrayList<Integer>();
		resourceIds.add(platformId);
		final Map<Integer, List<Measurement>> virtualParent = availabilityManager.getAvailMeasurementParent(
				resourceIds, AuthzConstants.ResourceEdgeVirtualRelation);
		if (isEmptyMap(virtualParent)) {
			return null;
		}
		// else - there should be a single measurement of the related VM Instance:
		List<Measurement> resourceEdgeVirtualRelations = virtualParent.get(platformId);
		if ((resourceEdgeVirtualRelations == null) | (resourceEdgeVirtualRelations.isEmpty()) )
		{
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
        final DataPoint lastParentDataPoint = availabilityCache.get(vmParentMeasurement.getId(), defaultParentDataPoint);
        if (lastParentDataPoint == null)
        	return null;
        double parentStatus = lastParentDataPoint.getValue();
        if ((parentStatus == MeasurementConstants.AVAIL_UP) || (parentStatus == MeasurementConstants.AVAIL_DOWN)) {
        	DataPoint newDataPoint = new DataPoint(availabilityDataPoint.getMeasurementId(), lastParentDataPoint.getMetricValue());
        	ResourceDataPoint resPoint = new ResourceDataPoint(availabilityDataPoint.getResource(), newDataPoint);
    		log.info("getPlatformStatusFromVC: found parent measurement: " + lastParentDataPoint.getMeasurementId() + "; adding point: " + resPoint.toString());
        	return resPoint;
        }

		return null;
	}
	
	private boolean isEmptyMap(Map<Integer, List<Measurement>> rHierarchy) {
		if (rHierarchy == null)
			return true;
		if (rHierarchy.size() ==0)
			return true;
		if (rHierarchy.isEmpty())
			return true;
		return false;
	}


	@Transactional
	private Collection<DataPoint> addStatusOfPlatformsDescendants(Collection<ResourceDataPoint> checkedPlatforms) {
		logDebug("addStatusOfPlatformsDescendants: start" );
		final Collection<DataPoint> res = new ArrayList<DataPoint>();
		final List<Integer> resourceIds = new ArrayList<Integer>();
		for (ResourceDataPoint rDataPoint : checkedPlatforms) {
			resourceIds.add(rDataPoint.getResource().getId());
		}
		final Map<Integer, List<Measurement>> rHierarchy = availabilityManager.getAvailMeasurementChildren(
				resourceIds, AuthzConstants.ResourceEdgeContainmentRelation);
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
				if (isHQAgent(meas))
					curStatus = MeasurementConstants.AVAIL_DOWN;
				
				final long curTimeStamp = getCurTimestamp();
				final long backfillTime = getBackfillTime(curTimeStamp, meas);
				if (backfillTime > curTimeStamp) {
					// TODO this means that the resource was updated during the last interval. Shouldn't platform be marked as UP?
					continue;
				}
				final MetricValue val = new MetricValue(curStatus, backfillTime);
				final MeasDataPoint point = new MeasDataPoint(meas.getId(), val, true);
				res.add(point);
			}
		}
		logDebug("addStatusOfPlatformsDescendants: end, res size: " + res.size() );
		return res;
	}
	
	private long getBackfillTime(long current, Measurement meas) {
		final long end = getEndWindow(current, meas);
		final DataPoint defaultPt = new DataPoint(meas.getId()
				.intValue(), MeasurementConstants.AVAIL_NULL, end);
		final DataPoint lastPt = availabilityCache.get(meas.getId(),
				defaultPt);
		final long backfillTime = lastPt.getTimestamp() + meas.getInterval();
		return backfillTime;
	}
	
    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
        return TimingVoodoo.roundDownTime((current - meas.getInterval()), meas.getInterval());
    }


	
	
    private long getCurTimestamp() {
    	if (this.curTimeStamp != 0)
    		return this.curTimeStamp;
        return System.currentTimeMillis();
    }


}
