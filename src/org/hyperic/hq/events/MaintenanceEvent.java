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

package org.hyperic.hq.events;

import java.util.ResourceBundle;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;

/**
 * Value object for scheduled maintenance events
 */
public class MaintenanceEvent extends ResourceLogEvent
    implements LoggableInterface 
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";

    private long _startTime;
    private long _endTime;
    private long _modifiedTime;
    
    public MaintenanceEvent(Integer groupId) {
        super(new TrackEvent(AppdefEntityID.newGroupID(groupId),
                             System.currentTimeMillis(),
                             LogTrackPlugin.LOGLEVEL_INFO, "", ""));

        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        setSource(bundle.getString("maintenance.window"));
    }
    
    public Integer getGroupId() {
    	return getResource().getId();
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
    
    public void setMaintenanceWindowMessage(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        setMessage(bundle.getString(key));
    }
    
    public String toString() {
    	return new StringBuffer("MaintenanceEvent")
    					.append("[groupId=" + getGroupId())
    					.append(",startTime=" + _startTime)
    					.append(",endTime=" + _endTime)
    					.append(",modifiedTime=" + _modifiedTime)
    					.append("]")
    					.toString();
    }
}
