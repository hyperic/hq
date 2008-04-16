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

package org.hyperic.hq.events.server.session;

import junit.framework.TestCase;

/**
 * Tests the AlertDefinition class.
 */
public class AlertDefinition_test extends TestCase {

    public AlertDefinition_test(String name) {
        super(name);
    }
    
    /**
     * Test activating/deactivating an alert definition.
     */
    public void testActivatingAlertDefinition() {
        AlertDefinition def = new AlertDefinition();
        
        assertFalse(def.isEnabled());
        
        assertFalse(def.isActive());
        
        def.setActiveStatus(true);
        
        assertTrue(def.isEnabled());

        assertTrue(def.isActive());
        
        def.setActiveStatus(false);
        
        assertFalse(def.isEnabled());
        
        assertFalse(def.isActive());
    }
    
    /**
     * Test activating an alert definition then enable/disable the alert 
     * definition. The operation will always succeed.
     */
    public void testEnablingActivatedAlertDefinition() {
        AlertDefinition def = new AlertDefinition();
        
        def.setActiveStatus(true);
        
        assertTrue(def.isEnabled());
        
        assertTrue(def.isActive());
        
        assertTrue(def.setEnabledStatus(true));
        
        assertTrue(def.isEnabled());
        
        assertTrue(def.isActive());

        assertTrue(def.setEnabledStatus(false));
        
        assertFalse(def.isEnabled());
        
        assertTrue(def.isActive());
    }
    
    /**
     * Test deactivating an alert definition then attempting to enable/disable 
     * the alert definition. The operation will never succeed.
     */
    public void testEnablingDeactivatedAlertDefinition() {
        AlertDefinition def = new AlertDefinition();
        
        def.setActiveStatus(false);
        
        assertFalse(def.isEnabled());
        
        assertFalse(def.isActive());
        
        assertFalse(def.setEnabledStatus(true));
        
        assertFalse(def.isEnabled());

        assertFalse(def.isActive());
        
        assertFalse(def.setEnabledStatus(false));
        
        assertFalse(def.isEnabled());
        
        assertFalse(def.isActive());
    }
    

}
