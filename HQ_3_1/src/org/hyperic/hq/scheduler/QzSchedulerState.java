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

public class QzSchedulerState  implements java.io.Serializable {

    // Fields
    private String _instanceName;
    private long _lastCheckinTime;
    private long _checkinInterval;
    private String _recoverer;

    // Constructors
    public QzSchedulerState() {
    }

    // Property accessors
    public String getInstanceName() {
        return _instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        _instanceName = instanceName;
    }

    public long getLastCheckinTime() {
        return _lastCheckinTime;
    }
    
    public void setLastCheckinTime(long lastCheckinTime) {
        _lastCheckinTime = lastCheckinTime;
    }

    public long getCheckinInterval() {
        return _checkinInterval;
    }
    
    public void setCheckinInterval(long checkinInterval) {
        _checkinInterval = checkinInterval;
    }

    public String getRecoverer() {
        return _recoverer;
    }
    
    public void setRecoverer(String recoverer) {
        _recoverer = recoverer;
    }
}
