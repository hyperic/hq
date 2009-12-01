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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;

/**
 * The escalation state ties an escalation chain to an alert definition. 
 */
public class EscalationState 
    extends PersistedObject
{
    // Pointer to the next action to execute in the escalation chain
    private int _nextAction;

    // Time in ms since the epoch that the next action should be executed
    private long _nextActionTime;

    // Escalation this state is tied to
    private Escalation _escalation;
    
    private EscalationAlertType _alertType;
    
    // The alert def id -- slightly superfluous, since the alert points at it
    private int _alertDefId;
    private int _alertId;

    // This flag indicates that the remaining time to the next action was due
    // to the escalation being paused.  Once the escalation continues, the
    // flag will be reset.
    private boolean _paused;
    
    // If the escalation has been acknowledged, this is who did it.
    private AuthzSubject _acknowledgedBy;
    
    protected EscalationState(){
    }

    protected EscalationState(Escalatable alert) {
        PerformsEscalations def = alert.getDefinition();
        
        _escalation     = def.getEscalation();
        _nextAction     = 0;
        _nextActionTime = System.currentTimeMillis();
        _alertDefId     = def.getId().intValue();
        _alertType      = def.getAlertType();
        _alertId        = alert.getId().intValue();
        _paused         = false;
    }

    public int getNextAction() {
        return _nextAction;
    }

    protected void setNextAction(int nextAction) {
        _nextAction = nextAction;
    }

    public long getNextActionTime() {
        return _nextActionTime;
    }

    protected void setNextActionTime(long nextActionTime) {
        _nextActionTime = nextActionTime;
    }

    public Escalation getEscalation() {
        return _escalation;
    }

    protected void setEscalation(Escalation escalation) {
        _escalation = escalation;
    }

    public int getAlertDefinitionId() {
        return _alertDefId;
    }

    protected void setAlertDefinitionId(int alertDefinitionId) {
        _alertDefId = alertDefinitionId;
    }

    public int getAlertId() {
        return _alertId;
    }

    protected void setAlertId(int alertId) {
        _alertId = alertId;
    }

    public EscalationAlertType getAlertType() {
        return _alertType;
    }

    protected int getAlertTypeEnum() {
        return _alertType.getCode();
    }

    protected void setAlertTypeEnum(int typeCode) {
        _alertType = EscalationAlertType.findByCode(typeCode);
    }
    
    public boolean isPaused() {
        return _paused;
    }
    
    protected void setPaused(boolean paused) {
        _paused = paused;
    }
    
    public AuthzSubject getAcknowledgedBy() {
        return _acknowledgedBy;
    }
    
    protected void setAcknowledgedBy(AuthzSubject subject) {
        _acknowledgedBy = subject;
    }
}
