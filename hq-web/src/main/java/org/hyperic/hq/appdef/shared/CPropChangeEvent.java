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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;

/**
 * Event sent when a resource's custom property value changes
 */
public class CPropChangeEvent extends AbstractEvent
    implements java.io.Serializable, ResourceEventInterface {

    private static final long serialVersionUID = -856543980977047083L;
    
    private AppdefEntityID resource;
    private String key;
    private String oldValue;
    private String newValue;

    /**
     * @param resource
     * @param key
     * @param oldValue
     * @param newValue
     */
    public CPropChangeEvent(AppdefEntityID resource, String key,
                            String oldValue, String newValue) {
        super();
        // Use the resource ID as instance ID
        this.setInstanceId(resource.getId());
        
        this.resource = resource;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public AppdefEntityID getResource() {
        return resource;
    }
    
    public void setResource(AppdefEntityID resource) {
        this.resource = resource;
    }
    
    public String toString() {
        return "Changed from " + oldValue + " to " + newValue;
    }
}
