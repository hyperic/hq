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

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.ejb.FinderException;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerLocal;

public final class ClassicEscalationAlertType 
    extends EscalationAlertType
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final ClassicEscalationAlertType CLASSIC = 
        new ClassicEscalationAlertType(0xdeadbeef, "Classic", 
                                       "escalation.type.classic");

    private static Object INIT_LOCK = new Object();
    private static AlertManagerLocal           _alertMan;
    private static AlertDefinitionManagerLocal _defMan;
    
    private void setup() {
        synchronized (INIT_LOCK) {
            if (_alertMan == null) {
                _alertMan = AlertManagerEJBImpl.getOne();
                _defMan = AlertDefinitionManagerEJBImpl.getOne();
            }
        }
    }
    
    private AlertManagerLocal getAlertMan() {
        setup();
        return _alertMan;
    }
    
    private AlertDefinitionManagerLocal getDefMan() {
        setup();
        return _defMan;
    }

    public Escalatable findEscalatable(Integer alertId) {
        AlertManagerLocal aMan = getAlertMan();
        Alert a = aMan.findAlertById(alertId);
        String shortReason, longReason;
        
        shortReason = aMan.getShortReason(a);
        longReason  = aMan.getLongReason(a);
        
        return ClassicEscalatableCreator.createEscalatable(a, shortReason,
                                                           longReason);
    }

    public PerformsEscalations findDefinition(Integer defId) {
        try {
            return getDefMan().getByIdNoCheck(defId, false);
        } catch(FinderException e) {
            return null;
        }
    }
    
    protected void setEscalation(Integer defId, Escalation escalation) {
        try {
            AlertDefinitionManagerLocal defMan = getDefMan();
            AlertDefinition def = defMan.getByIdNoCheck(defId, false);
            def.setEscalation(escalation);
            
            Collection children = def.getChildren();
            for (Iterator it = children.iterator(); it.hasNext(); ) {
                def = (AlertDefinition) it.next();
                def.setEscalation(escalation);
            }

        } catch(FinderException e) {
            throw new SystemException(e);
        }
    }

    protected void changeAlertState(Integer alertId, AuthzSubject who,
                                    EscalationStateChange newState) 
    {
        Alert alert = getAlertMan().findAlertById(alertId);

        if (newState.isFixed()) 
            getAlertMan().setAlertFixed(alert);
    }
    
    protected void logActionDetails(Integer alertId, Action action, 
                                    String detail, AuthzSubject subject) 
    {
        Alert alert = getAlertMan().findAlertById(alertId);
        
        getAlertMan().logActionDetail(alert, action, detail, subject);
    }

    private ClassicEscalationAlertType(int code, String desc, String localeProp) 
    {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE));
    }

    protected String getLastFixedNote(PerformsEscalations def) {
        Alert alert =
            getAlertMan().findLastFixedByDefinition((AlertDefinition) def);
        if (alert != null) {
            long lastlog = 0;
            String fixedNote = null;
            for (Iterator it = alert.getActionLog().iterator(); it.hasNext(); )
            {
                AlertActionLog log = (AlertActionLog) it.next();
                if (log.getAction() == null && log.getTimeStamp() > lastlog) {
                    fixedNote = log.getDetail();
                }
            }
            return fixedNote;
        }
        return null;
    }
}
