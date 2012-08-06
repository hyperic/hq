package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.product.MetricValue;



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
	private long curTimeStamp = 0;

	
    private void logDebug(String message) {
    	if (availabilityManager.isDevDebug())
    		log.info("aaa==========:" + message);
    }
    
    

	public AvailabilityFallbackChecker(AvailabilityManager availabilityManager, AvailabilityCache availabilityCache) {
		this.availabilityManager = availabilityManager;
		this.availabilityCache = availabilityCache;
	}
	

	public void testCheckAvailability(Collection<ResourceDataPoint> availabilityDataPoints, long curTimeStamp) {
		this.curTimeStamp = curTimeStamp;
		checkAvailability(availabilityDataPoints);
		this.curTimeStamp = 0;		
	}

	
	public void checkAvailability(Collection<ResourceDataPoint> availabilityDataPoints) {
		logDebug("checkAvailability: start");
		Collection<ResourceDataPoint> resPlatforms = new ArrayList<ResourceDataPoint>();
		for (ResourceDataPoint availabilityDataPoint : availabilityDataPoints) {
			ResourceDataPoint platformAvailPoint = checkPlatformAvailability(availabilityDataPoint);
			resPlatforms.add(platformAvailPoint);
			//addPlatformDescendants(platformAvailPoint, res);
		}
		logDebug("checkAvailability: found " + resPlatforms.size() + " Platforms.");
		Collection<DataPoint> res = addStatusOfPlatformsDescendants(resPlatforms);
		logDebug("checkAvailability: found " + res.size() + " platforms & descendants.");
		//res.addAll(availabilityDataPoints);
		storeUpdates(res);
		logDebug("checkAvailability: end");
	}

    public Object getLock() {
        return lock;
    }


	/**
	 * add the DataPoints of the 
	 * @param availabilityDataPoint
	 * @param platformsDescendants

	private void addPlatformDescendants(ResourceDataPoint availabilityDataPoint,
			Collection<DataPoint> platformsDescendants) {

		long timestamp = availabilityDataPoint.getTimestamp();
		
		double descendantGenStatus = MeasurementConstants.AVAIL_DOWN;
		double platformStatus = availabilityDataPoint.getMetricValue().getValue();
		if (platformStatus == MeasurementConstants.AVAIL_UP) {
			descendantGenStatus = MeasurementConstants.AVAIL_UNKNOWN;;
		}
			
		Collection<Integer> descendantsMeasurementIDs = getPlatformDescendants(availabilityDataPoint);
		if (descendantsMeasurementIDs == null) {
			return;
		}
		
		boolean foundHQAgent = false;
		for (Integer descendantMeasurementID : descendantsMeasurementIDs) {
			double descendantStatus = descendantGenStatus;
			if ((!foundHQAgent) && (isHQAgent(descendantMeasurementID)) ) {
				foundHQAgent = true;
				descendantStatus = MeasurementConstants.AVAIL_DOWN;
			}
			DataPoint descendantDataPoint = new DataPoint(descendantMeasurementID, new MetricValue(descendantStatus, timestamp));
			platformsDescendants.add(descendantDataPoint);
		}
		
	}
	 */
    
    
	private boolean isHQAgent(Measurement meas) {
		// TODO check if agent, and if so - mark it as down.
//		Resource measResource = meas.getResource();
//		logDebug("isHQHagent: " + measResource.toString());
//		Resource prototype = measResource.getPrototype();
//		String prototypeName = prototype.getName();
//		if (prototypeName.equals(AppdefEntityConstants.HQ_AGENT_PROTOTYPE_NAME))
			return true;

//		return false;
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
		// TODO Auto-generated method stub
/*		Integer platformId = availabilityDataPoint.getResource().getId();
		List<Integer> resourceIds = new ArrayList<Integer>();
		resourceIds.add(platformId);
		final Map<Integer, List<Measurement>> rHierarchy = availabilityManager.getAvailMeasurementChildren(
				resourceIds, AuthzConstants.ResourceEdgeVirtualRelation);
		if (isEmptyMap(rHierarchy)) {
			return null;
		}
*/		return null;
	}
	
	private boolean isEmptyMap(Map<Integer, List<Measurement>> rHierarchy) {
		// TODO Auto-generated method stub
		if (rHierarchy == null)
			return true;
		if (rHierarchy.size() ==0)
			return true;
		if (rHierarchy.isEmpty())
			return true;
		for (List<Measurement> list : rHierarchy.values()) {
			
		}
		return false;
	}



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
			if (rdp.getValue() != MeasurementConstants.AVAIL_DOWN) {
				// platform may be paused, so skip pausing its children
				continue;
			}
			final List<Measurement> associatedResources = rHierarchy.get(platform.getId());
			if (associatedResources == null) {
				continue;
			}
			
			//TODO Uncomment
//			double assocStatus = MeasurementConstants.AVAIL_UNKNOWN;
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
