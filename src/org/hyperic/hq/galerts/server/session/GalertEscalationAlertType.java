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
package org.hyperic.hq.galerts.server.session;

import java.util.Iterator;
import java.util.ResourceBundle;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationStateChange;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;

public final class GalertEscalationAlertType 
    extends EscalationAlertType
{
    private static final String BUNDLE = "org.hyperic.hq.galerts.Resources";
    
    public static final GalertEscalationAlertType GALERT = 
        new GalertEscalationAlertType(0xbadbabe, "Group Alert",
                                      "escalation.type.galert");

    private static Object INIT_LOCK = new Object();
    private static GalertManagerLocal _alertMan;
    
    private GalertManagerLocal getGalertMan() {
        synchronized (INIT_LOCK) {
            if (_alertMan == null) {
                _alertMan = GalertManagerEJBImpl.getOne();
            }
        }
        return _alertMan;
    }
    
    public Escalatable findEscalatable(Integer id) {
        return getGalertMan().findEscalatableAlert(id);
    }

    public PerformsEscalations findDefinition(Integer defId) {
        return getGalertMan().findById(defId);
    }

    protected void setEscalation(Integer defId, Escalation escalation) {
        GalertManagerLocal gMan = getGalertMan();
        GalertDef def = gMan.findById(defId);
        gMan.update(def, escalation);
    }

    protected void changeAlertState(Escalatable esc, AuthzSubject who, 
                                    EscalationStateChange newState) 
    {
        GalertLog alert = (GalertLog) esc.getAlertInfo();
        if (newState.isFixed()) 
            getGalertMan().fixAlert(alert);
    }

    protected void logActionDetails(Escalatable esc, Action action, 
                                    String detail, AuthzSubject subject) 
    {
        GalertLog alert = (GalertLog) esc.getAlertInfo();
        getGalertMan().createActionLog(alert, detail, action, subject);
    }

    private GalertEscalationAlertType(int code, String desc, String localeProp){ 
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE));
    }

    protected String getLastFixedNote(PerformsEscalations def) {
        GalertLog alert = getGalertMan().findLastFixedByDef((GalertDef) def);
        if (alert != null) {
            long lastlog = 0;
            String fixedNote = null;
            for (Iterator it = alert.getActionLog().iterator(); it.hasNext(); )
            {
                GalertActionLog log = (GalertActionLog) it.next();
                if (log.getAction() == null && log.getTimeStamp() > lastlog) {
                    fixedNote = log.getDetail();
                }
            }
            return fixedNote;
        }
        return null;
    }
}
