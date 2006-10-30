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

package org.hyperic.hq.measurement;

import org.hyperic.hq.measurement.shared.ScheduleRevNumValue;

public class ScheduleRevNum implements java.io.Serializable {

    // Fields    
    private SrnId _id;
    private long _version_;
    private Integer _srn;
    private long _minInterval;
    private long _lastReported;
    private boolean _pending;
    
    // Constructors
    public ScheduleRevNum() {
    }

    public ScheduleRevNum(SrnId id, Integer srn) {
        _id = id;
        _srn = srn;
    }

    public ScheduleRevNum(SrnId id, Integer srn, long minInterval,
                          long lastReported, boolean pending) {
        _id = id;
        _srn = srn;
        _minInterval = minInterval;
        _lastReported = lastReported;
        _pending = pending;
    }
   
    // Property accessors
    public SrnId getId() {
        return _id;
    }
    
    public void setId(SrnId id) {
        _id = id;
    }

    public long get_version_() {
        return _version_;
    }
    
    public void set_version_(long _version_) {
        _version_ = _version_;
    }

    public Integer getSrn() {
        return _srn;
    }
    
    public void setSrn(Integer srn) {
        _srn = srn;
    }

    public long getMinInterval() {
        return _minInterval;
    }
    
    public void setMinInterval(long minInterval) {
        _minInterval = minInterval;
    }

    public long getLastReported() {
        return _lastReported;
    }
    
    public void setLastReported(long lastReported) {
        _lastReported = lastReported;
    }

    public boolean isPending() {
        return _pending;
    }
    
    public void setPending(boolean pending) {
        _pending = pending;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof ScheduleRevNum)) {
            return false;
        }
        ScheduleRevNum o = (ScheduleRevNum)obj;
        return _id == o.getId() ||
               (_id != null && o.getId() != null && _id.equals(o.getId()));
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (_id != null ? _id.hashCode() : 0);

        return result;
    }

    /**
     * Legacy EJB DTO pattern
     * @deprecated Use (this) ScheduleRevNum object instead
     */
    public ScheduleRevNumValue getScheduleRevNumValue() {
        ScheduleRevNumValue val = new ScheduleRevNumValue();

        val.setAppdefType(getId().getAppdefType());
        val.setInstanceId(getId().getInstanceId());
        val.setSRN(getSrn().intValue());
        val.setMinInterval(getMinInterval());
        val.setLastReported(getLastReported());
        val.setPending(isPending());

        return val;
    }
}


