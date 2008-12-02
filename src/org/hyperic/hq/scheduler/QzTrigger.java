/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.scheduler;

import java.util.Collection;

public class QzTrigger  implements java.io.Serializable {

    // Fields
    private QzTriggerId _id;
    private String _jobName;
    private String _jobGroup;
    private boolean _isVolatile;
    private String _description;
    private long _nextFireTime;
    private long _prevFireTime;
    private String _triggerState;
    private String _triggerType;
    private long _startTime;
    private long _endTime;
    private String _calendarName;
    private Integer _misFireInstr;
    private byte[] _jobData;
    private QzJobDetail _jobDetail;
    private Collection _triggerListeners;
    private int     _priority;

    // Constructors
    public QzTrigger() {
    }

    // Property accessors
    public QzTriggerId getId() {
        return _id;
    }
    
    public void setId(QzTriggerId id) {
        _id = id;
    }

    public String getJobName() {
        return _jobName;
    }
    
    public void setJobName(String jobName) {
        _jobName = jobName;
    }

    public String getJobGroup() {
        return _jobGroup;
    }
    
    public void setJobGroup(String jobGroup) {
        _jobGroup = jobGroup;
    }

    public boolean isIsVolatile() {
        return _isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        _isVolatile = isVolatile;
    }

    public String getDescription() {
        return _description;
    }
    
    public void setDescription(String description) {
        _description = description;
    }

    public long getNextFireTime() {
        return _nextFireTime;
    }
    
    public void setNextFireTime(long nextFireTime) {
        _nextFireTime = nextFireTime;
    }

    public long getPrevFireTime() {
        return _prevFireTime;
    }
    
    public void setPrevFireTime(long prevFireTime) {
        _prevFireTime = prevFireTime;
    }

    public String getTriggerState() {
        return _triggerState;
    }
    
    public void setTriggerState(String triggerState) {
        _triggerState = triggerState;
    }

    public String getTriggerType() {
        return _triggerType;
    }
    
    public void setTriggerType(String triggerType) {
        _triggerType = triggerType;
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

    public String getCalendarName() {
        return _calendarName;
    }
    
    public void setCalendarName(String calendarName) {
        _calendarName = calendarName;
    }

    public Integer getMisFireInstr() {
        return _misFireInstr;
    }
    
    public void setMisFireInstr(Integer misFireInstr) {
        _misFireInstr = misFireInstr;
    }

    public byte[] getJobData() {
        return _jobData;
    }
    
    public void setJobData(byte[] jobData) {
        _jobData = jobData;
    }

    public QzJobDetail getJobDetail() {
        return _jobDetail;
    }
    
    public void setJobDetail(QzJobDetail jobDetail) {
        _jobDetail = jobDetail;
    }

    public Collection getTriggerListeners() {
        return _triggerListeners;
    }
    
    public void setTriggerListeners(Collection triggerListeners) {
        _triggerListeners = triggerListeners;
    }

    public int getPriority() {
        return _priority;
    }

    public void setPriority(int priority) {
        _priority = priority;
    }
}