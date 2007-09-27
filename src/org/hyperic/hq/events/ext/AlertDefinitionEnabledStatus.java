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

package org.hyperic.hq.events.ext;

/**
 * An object for signaling between threads the enabled status for a 
 * particular alert definition.
 */
final class AlertDefinitionEnabledStatus {

    private final Object lock = new Object();

    private final boolean defaultEnabledStatus;
    
    private boolean enabledStatus = false;
        
    /**
     * Creates an instance, specifying the default value for the enabled status.
     * 
     * @param defaultValue <code>true</code> if the status is enabled by default;
     *                     <code>false</code> if the status is disabled by default.
     */
    public AlertDefinitionEnabledStatus(boolean defaultValue) {
        synchronized (lock) {
            defaultEnabledStatus = defaultValue;
            enabledStatus = defaultValue;
        }
    }
    
    /**
     * Check if the alert definition is enabled.
     * 
     * @return <code>true</code> if enabled; <code>false</code> if disabled.
     */
    public boolean isAlertDefinitionEnabled() {
        synchronized (lock) {
            return enabledStatus;            
        }
    }
    
    /**
     * Flip the alert definition enabled status. If the status is enabled by 
     * default, then the status will be flipped to disabled. Any further attempts 
     * to flip the status will be a no-op. The status must be reset before it 
     * may be flipped again.
     */
    public void flipEnabledStatus() {
        synchronized (lock) {
            enabledStatus = !defaultEnabledStatus;
        }
    }
    
    /**
     * Reset the enabled status to the default value.
     */
    public void resetEnabledStatus() {
        synchronized (lock) {
            enabledStatus = defaultEnabledStatus;            
        }
    }
    
}
