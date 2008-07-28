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

public class MaintenanceEvent {
    private int _groupId;
    private long _startTime;
    private long _endTime;
    private long _modifiedTime;
    
    public MaintenanceEvent() {
    }
    
    public int getGroupId() {
    	return _groupId;
    }
    
    public void setGroupId(int groupId) {
    	_groupId = groupId;
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
    
    public String toString() {
    	return new StringBuffer("MaintenanceEvent")
    					.append("[groupId=" + _groupId)
    					.append(",startTime=" + _startTime)
    					.append(",endTime=" + _endTime)
    					.append(",modifiedTime=" + _modifiedTime)
    					.append("]")
    					.toString();
    }
}
