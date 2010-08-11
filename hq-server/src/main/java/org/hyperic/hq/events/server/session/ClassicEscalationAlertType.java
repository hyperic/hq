/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;

public final class ClassicEscalationAlertType 
    extends EscalationAlertType
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final ClassicEscalationAlertType CLASSIC = 
        new ClassicEscalationAlertType(0xdeadbeef, "Classic", 
                                       "escalation.type.classic");

    
    private AlertManager getAlertMan() {
        return Bootstrap.getBean(AlertManager.class);
    }
    
    private AlertDefinitionManager getDefMan() {
       return Bootstrap.getBean(AlertDefinitionManager.class);
    }

    public Escalatable findEscalatable(Integer alertId) {
        AlertManager aMan = getAlertMan();
        Alert a = aMan.findAlertById(alertId);
        String shortReason, longReason;
        
        shortReason = aMan.getShortReason(a);
        longReason  = aMan.getLongReason(a);
        
        return ClassicEscalatableCreator.createEscalatable(a, shortReason,
                                                           longReason);
    }

    public PerformsEscalations findDefinition(Integer defId) {
        return getDefMan().getByIdNoCheck(defId);
    }
    
    protected void setEscalation(Integer defId, Escalation escalation) {
        EscalationManager escMan = Bootstrap.getBean(EscalationManager.class);
        AlertDefinition def = getDefMan().getByIdNoCheck(defId);
        // End any escalation we were previously doing.
        escMan.endEscalation(def);
            
        def.setEscalation(escalation);
        long mtime = System.currentTimeMillis();
        def.setMtime(mtime);

        Collection children = def.getChildren();
        for (Iterator it = children.iterator(); it.hasNext(); ) {
            def = (AlertDefinition) it.next();
            // End any escalation we were previously doing.
            escMan.endEscalation(def);
                
            def.setEscalation(escalation);
            def.setMtime(mtime);
        }
    }

    protected void changeAlertState(Escalatable esc, AuthzSubject who,
                                    EscalationStateChange newState) 
    {
        Alert alert = (Alert) esc.getAlertInfo();
        if (newState.isFixed()) {
            getAlertMan().setAlertFixed(alert);
        }
        else if (newState.isAcknowledged() || newState.isEscalated()) {
            alert.invalidate();
        }
    }
    
    protected void logActionDetails(Escalatable esc, Action action, 
                                    String detail, AuthzSubject subject) 
    {
        Alert alert = (Alert) esc.getAlertInfo();
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

    protected Collection getPerformersOfEscalation(Escalation escalation) {
        return Bootstrap.getBean(AlertDefinitionManager.class).getUsing(escalation);
    }
}
