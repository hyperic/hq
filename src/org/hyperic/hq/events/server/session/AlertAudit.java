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

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.util.i18n.MessageBundle;

public class AlertAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    public static final AlertAuditPurpose ALERT_ENABLE = 
        new AlertAuditPurpose(0x5000, "alert enable", "audit.alert.enable");
    public static final AlertAuditPurpose ALERT_DISABLE = 
        new AlertAuditPurpose(0x5001, "alert disable", "audit.alert.disable");

    public static class AlertAuditPurpose extends AuditPurpose {
        AlertAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    protected AlertAudit() {}
    
    AlertAudit(AlertDefinition def, AuthzSubject s, AuditPurpose p, 
               AuditImportance i, AuditNature n, String msg, long time) 
    { 
        super(s, def.getResource(), p, n, i, msg);
        setStartTime(time);
        setEndTime(time);
    }

    public static AlertAudit enableAlert(AlertDefinition def,
                                         AuthzSubject modifier, long time)
    {
        final boolean enabled = def.isActive();
        String msg = enabled ?
                MSGS.format("auditMsg.alert.enable", def.getName()) :
                MSGS.format("auditMsg.alert.disable", def.getName());
        AlertAudit res = new AlertAudit(def, modifier,
                                        enabled ? ALERT_ENABLE : ALERT_DISABLE,
                                        enabled ? AuditImportance.MEDIUM :
                                                  AuditImportance.HIGH, 
                                        enabled ? AuditNature.ENABLE :
                                                  AuditNature.DISABLE,
                                        msg, time);  
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
}
