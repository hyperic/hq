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

package org.hyperic.hq.measurement.server.session;

public class ScheduleRevNum implements java.io.Serializable {

    // Fields    
    private SrnId _id;
    private long _version;
    private int _srn;
    private long _minInterval = 0;
    private long _lastReported = 0;
    private boolean _pending;
    
    // Constructors
    public ScheduleRevNum() {
    }

    public ScheduleRevNum(SrnId id, int srn) {
        _id = id;
        _srn = srn;
    }

    public ScheduleRevNum(SrnId id, int srn, long minInterval,
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
    
    protected void setId(SrnId id) {
        _id = id;
    }

    public long get_version_() {
        return _version;
    }
    
    protected void set_version_(long version) {
        _version = version;
    }

    public int getSrn() {
        return _srn;
    }
    
    protected void setSrn(int srn) {
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
}
