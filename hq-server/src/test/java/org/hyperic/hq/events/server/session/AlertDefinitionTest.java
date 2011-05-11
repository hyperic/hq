/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hyperic.hq.measurement.MeasurementConstants;

/**
 * Tests the AlertDefinition class.
 */
public class AlertDefinitionTest extends TestCase {

    private static String EQUALS_COMPARATOR = "=";
    private static String NOT_EQUALS_COMPARATOR = "!=";
    private static String GREATER_THAN_COMPARATOR = ">";
    private static String LESS_THAN_COMPARATOR = "<";

    public AlertDefinitionTest(String name) {
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
    
    public void testIsAvailabilityUpMultiCondition() {
        testIsAvailabilityMultiCondition(true);
    }
    
    public void testIsAvailabilityDownMultiCondition() {
        testIsAvailabilityMultiCondition(false);
    }
    
    private void testIsAvailabilityMultiCondition(boolean up) {
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond1 = new AlertCondition();
        AlertCondition cond2 = new AlertCondition();
        
        def.addCondition(cond1);
        def.addCondition(cond2);
        
        assertFalse("Multi-conditional alert definition not supported to be configured for availability",
                    def.isAvailability(up));
    }
    
    public void testIsAvailabilityUpValidConditionName() {
    	testIsAvailabilityValidConditionName(MeasurementConstants.CAT_AVAILABILITY);
    	testIsAvailabilityValidConditionName("Server Availability");
    }
    
    private void testIsAvailabilityValidConditionName(String conditionName) {
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond = new AlertCondition();
        
        cond.setName(conditionName);
        cond.setComparator(EQUALS_COMPARATOR);
        cond.setThreshold(MeasurementConstants.AVAIL_UP);
        def.addCondition(cond);
        
        assertTrue("The condition name is '" + conditionName
        				+ "'. It must contain " + MeasurementConstants.CAT_AVAILABILITY,
                    def.isAvailability(true));        
    }
    
    public void testIsAvailabilityUpInvalidConditionName() {
        testIsAvailabilityInvalidConditionName(true);
    }
    
    public void testIsAvailabilityDownInvalidConditionName() {
        testIsAvailabilityInvalidConditionName(false);
    }
    
    private void testIsAvailabilityInvalidConditionName(boolean up) {
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond = new AlertCondition();
        
        cond.setName(MeasurementConstants.CAT_THROUGHPUT);
        def.addCondition(cond);
        
        assertFalse("The condition name must contain " + MeasurementConstants.CAT_AVAILABILITY,
                    def.isAvailability(up));
    }
    
    public void testIsAvailabilityUpEqualsComparator() {
        testIsAvailabilityEqualsComparator(true);
    }
    
    public void testIsAvailabilityUpEqualsComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(true, EQUALS_COMPARATOR);
    }
    
    public void testIsAvailabilityDownEqualsComparator() {
        testIsAvailabilityEqualsComparator(false);        
    }
    
    public void testIsAvailabilityDownEqualsComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(false, EQUALS_COMPARATOR);
    }
    
    private void testIsAvailabilityEqualsComparator(boolean up) {        
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond = new AlertCondition();
        def.addCondition(cond);
        cond.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond.setComparator(EQUALS_COMPARATOR);
        if (up) {
            cond.setThreshold(MeasurementConstants.AVAIL_UP);
        } else {
            cond.setThreshold(MeasurementConstants.AVAIL_DOWN);
        }
                
        assertTrue("The alert definition is not properly configured for availability",
                   def.isAvailability(up));
    }
    
    public void testIsAvailabilityUpNotEqualsComparator() {        
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond = new AlertCondition();
        def.addCondition(cond);
        cond.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond.setComparator(NOT_EQUALS_COMPARATOR);
        cond.setThreshold(MeasurementConstants.AVAIL_DOWN);
        
        assertTrue("The alert definition is not properly configured for up availability",
                    def.isAvailability(true));
    }
    
    public void testIsAvailabilityUpNotEqualsComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(true, NOT_EQUALS_COMPARATOR);
    }
    
    public void testIsAvailabilityDownNotEqualsComparator() {
        AlertDefinition def = new AlertDefinition();
        AlertCondition cond = new AlertCondition();
        def.addCondition(cond);
        cond.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond.setComparator(NOT_EQUALS_COMPARATOR);
        cond.setThreshold(MeasurementConstants.AVAIL_UP);
        
        assertTrue("The alert definition is not properly configured for down availability",
                    def.isAvailability(false));        
    }
    
    public void testIsAvailabilityDownNotEqualsComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(false, NOT_EQUALS_COMPARATOR);
    }
    
    public void testIsAvailabilityUpGreaterThanComparator() {
        List<AlertDefinition> list = new ArrayList<AlertDefinition>();
        
        // add valid alert definitions to test here
        
        AlertDefinition def1 = new AlertDefinition();
        AlertCondition cond1 = new AlertCondition();
        def1.addCondition(cond1);
        cond1.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond1.setComparator(GREATER_THAN_COMPARATOR);
        cond1.setThreshold(MeasurementConstants.AVAIL_DOWN);
        list.add(def1);

        AlertDefinition def2 = new AlertDefinition();
        AlertCondition cond2 = new AlertCondition();
        def2.addCondition(cond2);
        cond2.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond2.setComparator(GREATER_THAN_COMPARATOR);
        cond2.setThreshold(0.999);
        list.add(def2);

        AlertDefinition def3 = new AlertDefinition();
        AlertCondition cond3 = new AlertCondition();
        def3.addCondition(cond3);
        cond3.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond3.setComparator(GREATER_THAN_COMPARATOR);
        cond3.setThreshold(0.5);
        list.add(def3);
        
        for (AlertDefinition def :list) { 
            assertTrue("The alert definition is not properly configured for up availability",
                        def.isAvailability(true));
        }
    }
    
    public void testIsAvailabilityUpGreaterThanComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(true, GREATER_THAN_COMPARATOR);
    }
    
    public void testIsAvailabilityDownLessThanComparator() {
        List<AlertDefinition> list = new ArrayList<AlertDefinition>();
        
        // add valid alert definitions to test here
        
        AlertDefinition def1 = new AlertDefinition();
        AlertCondition cond1 = new AlertCondition();
        def1.addCondition(cond1);
        cond1.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond1.setComparator(LESS_THAN_COMPARATOR);
        cond1.setThreshold(MeasurementConstants.AVAIL_UP);
        list.add(def1);
        
        AlertDefinition def2 = new AlertDefinition();
        AlertCondition cond2 = new AlertCondition();
        def2.addCondition(cond2);
        cond2.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond2.setComparator(LESS_THAN_COMPARATOR);
        cond2.setThreshold(0.001);
        list.add(def2);

        AlertDefinition def3 = new AlertDefinition();
        AlertCondition cond3 = new AlertCondition();
        def3.addCondition(cond3);
        cond3.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond3.setComparator(LESS_THAN_COMPARATOR);
        cond3.setThreshold(0.5);
        list.add(def3);
        
        for (AlertDefinition def: list) {
            assertTrue("The alert definition is not properly configured for down availability",
                        def.isAvailability(false));
        }
    }
    
    public void testIsAvailabilityDownLessThanComparatorInvalidThreshold() {
        testIsAvailabilityInvalidThreshold(false, LESS_THAN_COMPARATOR);
    }

    private void testIsAvailabilityInvalidThreshold(boolean up, String comparator) {
        List<AlertDefinition> list = new ArrayList<AlertDefinition>();
        
        // add invalid alert definitions to test here

        AlertDefinition def1 = new AlertDefinition();
        AlertCondition cond1 = new AlertCondition();
        def1.addCondition(cond1);
        cond1.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond1.setComparator(comparator);
        cond1.setThreshold(MeasurementConstants.AVAIL_UNKNOWN);
        list.add(def1);
        
        AlertDefinition def2 = new AlertDefinition();
        AlertCondition cond2 = new AlertCondition();
        def2.addCondition(cond2);
        cond2.setName(MeasurementConstants.CAT_AVAILABILITY);
        cond2.setComparator(comparator);
        cond2.setThreshold(MeasurementConstants.AVAIL_PAUSED);
        list.add(def2);
        
        for (AlertDefinition def : list ) {
            assertFalse("The alert definition is properly configured for availability",
                        def.isAvailability(up));
        }        
    }
}
