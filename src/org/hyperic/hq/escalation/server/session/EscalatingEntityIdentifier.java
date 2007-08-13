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

/**
 * The minimal state that uniquely identifies an entity performing 
 * escalations.
 */
final class EscalatingEntityIdentifier {
    
    private final Integer _id;
    private final EscalationAlertType _alertType;
    
    /**
     * Creates an instance from the entity performing escalations.
     *
     * @param def The entity performing escalations that may be uniquely 
     *            identified by this instance.
     */
    public EscalatingEntityIdentifier(PerformsEscalations def) {
        if (def == null) {
            throw new NullPointerException("def is null.");
        }
        
        _id = def.getId();
        _alertType = def.getAlertType();
    }
    
    /**
     * Creates an instance representing the escalating entity associated 
     * with this escalation state.
     *
     * @param state The escalation state.
     */
    public EscalatingEntityIdentifier(EscalationState state) {
        if (state == null) {
            throw new NullPointerException("state is null.");
        }
        
        _id = new Integer(state.getAlertDefinitionId());
        _alertType = state.getAlertType();
    }
    
    /**
     * The escalating entity Id.
     * 
     * @return The Id.
     */
    public Integer getId() {
        return _id;
    }
    
    /**
     * The escalating entity alert type.
     * 
     * @return The alert type.
     */
    public EscalationAlertType getAlertType() {
        return _alertType;
    }
    
    public String toString() {
        return this.getClass().getName()+
               " Alert Type: desc="+getAlertType().getDescription()+
               ", code="+getAlertType().getCode()+
               "; Escalating Entity: id="+getId();
    }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj instanceof EscalatingEntityIdentifier) {
            EscalatingEntityIdentifier compareTo = (EscalatingEntityIdentifier)obj;
            return getAlertType().equals(compareTo.getAlertType()) 
                              && getId().equals(compareTo.getId());
        }
        
        return false;
    }
    
    public int hashCode() {
        int result = 17;
        result += 37*result + getId().intValue();
        result += 37*result + getAlertType().hashCode();
        return result;
    }
    
}
