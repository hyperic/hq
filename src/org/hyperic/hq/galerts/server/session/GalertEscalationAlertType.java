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

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.MEscalation;
import org.hyperic.hq.escalation.server.session.MEscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;

public final class GalertEscalationAlertType 
    extends MEscalationAlertType
{
    public static final GalertEscalationAlertType GALERT = 
        new GalertEscalationAlertType(0xbadbabe, "Group Alert");

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
        return getGalertMan().findAlertLog(id); 
    }

    public PerformsEscalations findDefinition(Integer defId) {
        return getGalertMan().findById(defId);
    }

    public void setEscalation(Integer defId, MEscalation escalation) {
        getGalertMan().findById(defId).setEscalation(escalation);
    }

    protected void fixAlert(Integer alertId, AuthzSubject fixer) {
        GalertManagerLocal gMan = getGalertMan();
        GalertLog alert = gMan.findAlertLog(alertId);
        String msg = "Alert fixed by " + fixer.getFullName();
        
        gMan.createActionLog(alert, msg, null, fixer);
    }

    protected void logActionDetails(Integer alertId, Action action, 
                                    String detail) 
    {
        GalertLog alert = getGalertMan().findAlertLog(alertId);
        
        getGalertMan().createActionLog(alert, detail, action, null);
    }

    private GalertEscalationAlertType(int code, String desc) {
        super(code, desc);
    }
}
