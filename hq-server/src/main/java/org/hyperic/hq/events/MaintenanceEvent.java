/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.events;

import java.util.Date;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.json.JSON;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

/**
 * Value object for scheduled maintenance events
 */
public class MaintenanceEvent
    extends ResourceLogEvent implements JSON {
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";

    // Constants for REST Service and Quartz Job
    private static final String OBJECT_NAME = "MaintenanceEvent";

    public static final String ENTITY_ID = "entityId";
    
    /**
     * @deprecated Use ENTITY_ID instead
     */
    public static final String GROUP_ID = "groupId";
    
    public static final String STATE = "state";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String MODIFIED_TIME = "modifiedTime";
    public static final String MODIFIED_BY = "modifiedBy";

    // State constants
    public static final String STATE_NEW = "new";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_COMPLETE = "complete";

    private String _state;
    private long _startTime;
    private long _endTime;
    private long _modifiedTime;
    private String _authzName;

    // Stats
    private Set<Integer> _measurements = new HashSet<Integer>();
    public long alertCount;
    public long errorCount;

    /**
     * @deprecated Use AppdefEntityID constructor instead
     */
    public MaintenanceEvent(Integer groupId) {
    	this(AppdefEntityID.newGroupID(groupId));
    }

    public MaintenanceEvent(AppdefEntityID entityId) {
        super(new TrackEvent(entityId,
                             System.currentTimeMillis(),
                             LogTrackPlugin.LOGLEVEL_INFO, "", ""));

        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        setSource(bundle.getString("maintenance.window"));
        setState(STATE_NEW);
        resetStats();
    }

    /**
     * @deprecated Use AppdefEntityID getter instead
     */
    public Integer getGroupId() {
    	if (getResource().isGroup()) {
    		return getResource().getId();
    	} else {
    		return null;
    	}
    }

    public AppdefEntityID getAppdefEntityID() {
        return getResource();
    }
    
    public String getState() {
        return _state;
    }

    public void setState(String state) {
        _state = state;
    }

    public long getStartTime() {
        return _startTime;
    }

    public void setStartTime(long startTime) {
        _startTime = startTime;
    }

    public long getEndTime() {
        return _endTime;
    }

    public void setEndTime(long endTime) {
        _endTime = endTime;
    }

    public long getModifiedTime() {
        return _modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        _modifiedTime = modifiedTime;
    }

    public String getModifiedBy() {
        return _authzName;
    }

    public void setModifiedBy(String authzName) {
        _authzName = authzName;
    }

    public boolean activate() {
        return STATE_RUNNING.equals(getState());
    }

    public Set<Integer> getMeasurements() {
        return _measurements;
    }

    public void resetStats() {
        alertCount = 0;
        errorCount = 0;
        _measurements.clear();
    }

    /**
     * Create a MaintenanceEvent object from a JobDetail
     * 
     */
    public static MaintenanceEvent build(JobDetail jobDetail) {
        JobDataMap jdMap = jobDetail.getJobDataMap();
        MaintenanceEvent event = null;
        
        // for backwards compatibility, need to check group id first
        if (jdMap.containsKey(GROUP_ID)) {
        	Integer groupId = jdMap.getIntegerFromString(GROUP_ID);
        	event = new MaintenanceEvent(groupId);
        } else {        
        	String entityId = jdMap.getString(ENTITY_ID);
        	event = new MaintenanceEvent(new AppdefEntityID(entityId));
        }
        
        event.setState(jdMap.getString(STATE));
        event.setStartTime(jdMap.getLongValue(START_TIME));
        event.setEndTime(jdMap.getLongValue(END_TIME));
        event.setModifiedTime(jdMap.getLongValue(MODIFIED_TIME));
        event.setModifiedBy(jdMap.getString(MODIFIED_BY));

        return event;
    }

    public void setEventMessage(String msg) {
        setMessage(msg);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        if ((getMessage() != null) && (getMessage().length() > 0)) {
            sb.append(getMessage());
            sb.append(" [Start Time: " + new Date(_startTime));
            sb.append(", End Time: " + new Date(_endTime));
            sb.append("]");
        } else {
            sb.append(OBJECT_NAME);
            sb.append("[" + ENTITY_ID + "=" + getAppdefEntityID());
            sb.append("," + STATE + "=" + _state);
            sb.append("," + START_TIME + "=" + new Date(_startTime));
            sb.append("," + END_TIME + "=" + new Date(_endTime));
            sb.append("," + MODIFIED_TIME + "=" + new Date(_modifiedTime));
            sb.append("," + MODIFIED_BY + "=" + _authzName);
            sb.append("]");
        }
        return sb.toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(ENTITY_ID, getAppdefEntityID())
                .put(STATE, getState())
                .put(START_TIME, getStartTime())
                .put(END_TIME, getEndTime());
        } catch (JSONException e) {
            throw new SystemException(e);
        }
        return json;
    }

    public String getJsonName() {
        return OBJECT_NAME;
    }
}
