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

/*
 * AbstractEvent.java
 *
 * Created on September 11, 2002, 2:45 PM
 */

package org.hyperic.hq.events;

import java.io.Serializable;

/**
 * Subsystems will extend the abstract Event class to be able to
 * return a specific payload value.
 */
public abstract class AbstractEvent implements Serializable {
    
    private static final long serialVersionUID = 1300452915258577781L;
    
    private Long    _id;
    private Integer _instanceId;
    private long    _timestamp = System.currentTimeMillis();
    
    public Long getId() {
        return _id;
    }
    
    public void setId(Long id) {
        _id = id;
    }
    
    public Integer getInstanceId() {
        return _instanceId;
    }
    
    public void setInstanceId(Integer instanceId) {
        _instanceId = instanceId;
    }
    
    public long getTimestamp() {
        return _timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public boolean isLoggingSupported() {
        return this instanceof LoggableInterface;
    }
    
    public boolean isAlertDefinitionLastFiredUpdateEvent() {
        return this instanceof AlertDefinitionLastFiredUpdateEvent;
    }
}
