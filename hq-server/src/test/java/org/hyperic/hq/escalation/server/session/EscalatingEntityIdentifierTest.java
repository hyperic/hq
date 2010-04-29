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

package org.hyperic.hq.escalation.server.session;

import junit.framework.TestCase;

import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;

/**
 * Tests the EscalatingEntityIdentifier class.
 */
public class EscalatingEntityIdentifierTest extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public EscalatingEntityIdentifierTest(String name) {
        super(name);
    }
    
    public void testNullConstructorParams() {
        try {
            new EscalatingEntityIdentifier((PerformsEscalations)null);
        } catch (NullPointerException e) {
            // expected outcome
        }
        
        try {
            new EscalatingEntityIdentifier((EscalationState)null);
        } catch (NullPointerException e) {
            // expected outcome
        }
    }
    
    public void testCreatInstanceFromPerformsEscalation() {
        Integer id1 = new Integer(1);
        
        EscalatingEntityIdentifier gaEs1 = 
            new EscalatingEntityIdentifier(
                    new TestPerformsEscalations(id1, GalertEscalationAlertType.GALERT));

        assertEquals(id1, gaEs1.getId());
        assertEquals(GalertEscalationAlertType.GALERT, gaEs1.getAlertType());
    }
    
    public void testCreatInstanceFromEscalationState() {
        Integer id1 = new Integer(1);
        
        EscalationState state = new EscalationState();
        state.setAlertDefinitionId(id1.intValue());
        state.setAlertTypeEnum(GalertEscalationAlertType.GALERT.getCode());
        
        EscalatingEntityIdentifier gaEs1 = 
            new EscalatingEntityIdentifier(state);

        assertEquals(id1, gaEs1.getId());
        assertEquals(GalertEscalationAlertType.GALERT, gaEs1.getAlertType());
    }
    
    public void testEquals() {
        Integer id1 = new Integer(1);
        Integer id2 = new Integer(2);
        
        EscalatingEntityIdentifier classicEs1a = 
            new EscalatingEntityIdentifier(
                    new TestPerformsEscalations(id1, ClassicEscalationAlertType.CLASSIC));

        EscalatingEntityIdentifier classicEs1b = 
            new EscalatingEntityIdentifier(
                    new TestPerformsEscalations(id1, ClassicEscalationAlertType.CLASSIC));
        
        EscalatingEntityIdentifier classicEs2 = 
            new EscalatingEntityIdentifier (
                    new TestPerformsEscalations(id2, ClassicEscalationAlertType.CLASSIC));

        EscalatingEntityIdentifier gaEs1a = 
            new EscalatingEntityIdentifier(
                    new TestPerformsEscalations(id1, GalertEscalationAlertType.GALERT));
        
        EscalatingEntityIdentifier gaEs2 = 
            new EscalatingEntityIdentifier(
                    new TestPerformsEscalations(id2, GalertEscalationAlertType.GALERT));
        
        // check equals
        assertEquals(classicEs1a, classicEs1a);
        assertEquals(classicEs1a, classicEs1b);
        assertFalse(classicEs1a.equals(classicEs2));
        assertFalse(classicEs1a.equals(gaEs1a));
        assertFalse(classicEs1a.equals(gaEs2));
        assertFalse(classicEs2.equals(gaEs2));
        
        // check hash codes
        assertEquals(classicEs1a.hashCode(), classicEs1b.hashCode());
    }
    
    private class TestPerformsEscalations implements PerformsEscalations {

        private final Integer _id;
        private final EscalationAlertType _alertType;
        
        public TestPerformsEscalations(Integer id, EscalationAlertType alertType) {
            _id = id;
            _alertType = alertType;
        }
        
        public EscalationAlertType getAlertType() {
            return _alertType;
        }

        public AlertDefinitionInterface getDefinitionInfo() {
            return null;
        }

        public Escalation getEscalation() {
            return null;
        }

        public Integer getId() {
            return _id;
        }

        public String getName() {
            return null;
        }

        public boolean isDeleted() {
            return false;
        }
        
    }

}
