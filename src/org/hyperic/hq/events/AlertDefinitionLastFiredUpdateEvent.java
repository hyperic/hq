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

package org.hyperic.hq.events;

import java.io.Serializable;

import org.hyperic.hq.events.server.session.AlertDefinition;

/**
 * An event indicating that an alert definition last fired time has 
 * been updated to a new value.
 */
public class AlertDefinitionLastFiredUpdateEvent extends AbstractEvent
        implements Serializable, Comparable {

    private static final long serialVersionUID = -8148649541156405020L;

    /**
     * Creates an instance.
     *
     * @param alertDef The alert definition.
     * @param lastFiredTime The new last fired time.
     */
    public AlertDefinitionLastFiredUpdateEvent(AlertDefinition alertDef, 
                                               long lastFiredTime) {
        this(alertDef.getId(), lastFiredTime);
    }
    
    /**
     * Creates an instance.
     *
     * @param alertDefId The alert definition id.
     * @param lastFiredTime The new last fired time.
     */
    public AlertDefinitionLastFiredUpdateEvent(Integer alertDefId, 
                                               long lastFiredTime) {
        this.setInstanceId(alertDefId);
        this.setTimestamp(lastFiredTime);
    }
    
    /**
     * Retrieve the alert definition id.
     * 
     * @return The alert definition id.
     */
    public Integer getAlertDefinitionId() {
        return getInstanceId();
    }
    
    /**
     * Retrieve the last fired time.
     * 
     * @return The last fired time in milliseconds.
     */
    public long getLastFiredTime() {
        return getTimestamp();
    }
    
    public int compareTo(Object o) {
        AlertDefinitionLastFiredUpdateEvent event 
                    = (AlertDefinitionLastFiredUpdateEvent)o;
        
        if (this.getLastFiredTime() != event.getLastFiredTime()) {
            return (int)(this.getLastFiredTime() - 
                    event.getLastFiredTime());
        } else {
            return this.getAlertDefinitionId().intValue() - 
                   event.getAlertDefinitionId().intValue();
        }
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (o instanceof AlertDefinitionLastFiredUpdateEvent) {
            AlertDefinitionLastFiredUpdateEvent event 
                        = (AlertDefinitionLastFiredUpdateEvent)o;
            
            return this.getLastFiredTime() == event.getLastFiredTime() && 
                   this.getAlertDefinitionId().equals(event.getAlertDefinitionId());
        }
        
        return false;
    }
    
    public int hashCode() {
        int result = 37;
        result = 17*result + (int)this.getLastFiredTime();
        result = 17*result + this.getAlertDefinitionId().hashCode();
        return result;
    }
    
    public String toString() {
        return getAlertDefinitionId()+":"+getLastFiredTime();
    }
    
}
