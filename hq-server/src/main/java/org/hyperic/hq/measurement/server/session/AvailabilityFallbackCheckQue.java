package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class AvailabilityFallbackCheckQue {

    private final Log log = LogFactory.getLog(AvailabilityFallbackCheckQue.class);
    private ConcurrentLinkedQueue<ResourceDataPoint> platformsRecheckQue;
    private Map<Integer, ResourceDataPoint> currentPlatformsInQue;
    private Set<Integer> platformsRecheckInProgress;
    private Set<Integer> platformsPendingQueRemoval;
    private Map<Integer,Integer> measurementIdToPlatformId;
    private Map<Integer,Long> platformIdToLastUpdateTimestamp; 

    
    private void logDebug(String message) {
    	//log.info("aaa==========:" + message);
    }
    
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    public AvailabilityFallbackCheckQue() {
		this.platformsRecheckQue = new ConcurrentLinkedQueue<ResourceDataPoint>();
		this.currentPlatformsInQue = new HashMap<Integer, ResourceDataPoint>();
		this.platformsRecheckInProgress = new HashSet<Integer>();
		this.platformsPendingQueRemoval = new HashSet<Integer>();
		this.measurementIdToPlatformId = new HashMap<Integer,Integer>();
		this.platformIdToLastUpdateTimestamp = new HashMap<Integer,Long>();
    }
    
    public synchronized int getSize(){
    	return this.platformsRecheckQue.size();
    }
    
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    
    /**
     * result may be null, in case the last update was done by the agent.
     * @param platformId
     * @return
     */
    public Long getLastUpdateTimeByServer(Integer platformId) {
    	return this.platformIdToLastUpdateTimestamp.get(platformId);
    }
    
    /**
     * 
     * @param platformId
     * @param dataPoint
     * @return
     */
    public synchronized boolean addToQue(Integer platformId, ResourceDataPoint dataPoint) {
    	logDebug("addToQue: start, curQueSize: " + getSize());
    	if (this.currentPlatformsInQue.containsKey(platformId)) 
    		return false;
    	//RecheckPlatformDataPoint platformDataPoint = createRecheckDataPoint(platformId, dataPoint);
    	this.platformsRecheckQue.add(dataPoint);
    	this.currentPlatformsInQue.put(platformId, dataPoint);
    	this.measurementIdToPlatformId.put(dataPoint.getMeasurementId(), platformId);
    	logDebug("addToQue: added");
    	return true;
    }
    
    /**
     * 
     * @param platformsToAdd
     * @return
     */
    public synchronized int addToQue(Map<Integer, ResourceDataPoint> platformsToAdd) {
    	logDebug("addToQue: list");
    	int res = 0;
    	for (Integer platformId : platformsToAdd.keySet()) {
    		boolean added = addToQue(platformId, platformsToAdd.get(platformId));
    		res += added ? 1 : 0;
		}
    	logDebug("addToQue: added "+res);
    	return res;
    }
    
    /**
     * 
     * @return
     */
	public synchronized ResourceDataPoint poll() {
    	logDebug("poll: start, que size: " + getSize());
		ResourceDataPoint res = this.platformsRecheckQue.poll();
		if (res != null) {
			Integer platformId = res.getResource().getId();
			this.platformsRecheckInProgress.add(platformId);
		}
    	logDebug("poll: end, que size: " + getSize());
		return res;
	}


	/**
	 * 
	 * @param dataPoints
	 * @param isUpdateFromServer
	 * @return
	 */
	public synchronized Collection<DataPoint> beforeDataUpdate(Collection<DataPoint> dataPoints, boolean isUpdateFromServer) {
		
    	logDebug("beforeDataUpdate: start, dataPoints size: " + dataPoints.size());
		Long curTimeStamp = new Long(getCurTimestamp());
		Collection<DataPoint> res = new ArrayList<DataPoint>();
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
					this.currentPlatformsInQue.remove(platformId);
					this.measurementIdToPlatformId.remove(measurementId);
					this.platformIdToLastUpdateTimestamp.remove(platformId);
				}
				else {
					res.add(dataPoint);
					ResourceDataPoint queDataPoint = this.currentPlatformsInQue.get(platformId);
					ResourceDataPoint pointToAddToQue = new ResourceDataPoint(queDataPoint.getResource(), dataPoint);
					addToQue(platformId, pointToAddToQue);
					this.platformIdToLastUpdateTimestamp.put(platformId, curTimeStamp);
				}	
			}
			
			else {
				// update from agent
				res.add(dataPoint);
				removeFromQue(platformId);
			}
			
		}

    	logDebug("beforeDataUpdate: end, res size: " + res.size());
		return res;
	}
	
    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    private long getCurTimestamp() {
        return System.currentTimeMillis();
    }

    private synchronized boolean removeFromQue(Integer platformId) {
    	ResourceDataPoint platformDataPoint = this.currentPlatformsInQue.get(platformId);
    	if (platformDataPoint == null) {
    		// this platform may be in recheck process
    		if (this.platformsRecheckInProgress.contains(platformId)) {
    			this.platformsPendingQueRemoval.add(platformId);
    			return true;
    		}
    		return false;
    	}
    	//else 
		this.platformsRecheckQue.remove(platformDataPoint);
		this.currentPlatformsInQue.remove(platformId);   
		this.measurementIdToPlatformId.remove(platformId);
		this.platformIdToLastUpdateTimestamp.remove(platformId);
    	return true;
    }

    
	


    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
	
//	public RecheckPlatformDataPoint createRecheckDataPoint(Integer platformId, DataPoint other) {
//		return (new RecheckPlatformDataPoint(platformId, other));
//	};
//	
//
//    
//	public class RecheckPlatformDataPoint extends DataPoint implements Comparable<RecheckPlatformDataPoint> {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		private Integer platformId;
//		
//		public RecheckPlatformDataPoint(Integer platformId, DataPoint other) {
//			super(other);
//			this.platformId = platformId;
//		}
//		
//		public Integer getPlatformId() {
//			return platformId;
//		}
//
//		/**
//		 * Note: if 2 DataPoints differ by timestamp only, it will still return 0!
//		 */
//		public int compareTo(RecheckPlatformDataPoint other) {
//			if (other == null)
//				return 1;
//			
//			return (platformId.compareTo(other.getPlatformId()));
//		}
//	}
		

}
