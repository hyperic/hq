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
package org.hyperic.hq.common.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.util.i18n.MessageBundle;

public class ServerConfigAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    public static final ServerConfigAuditPurpose CONFIG_UPDATE = 
        new ServerConfigAuditPurpose(0x6000, "server config update", 
                                     "audit.serverConfig.update");

    public static class ServerConfigAuditPurpose extends AuditPurpose {
        ServerConfigAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }
    
    protected ServerConfigAudit() {}

    ServerConfigAudit(AuthzSubject s, Resource r, AuditPurpose p, 
                      AuditImportance i, AuditNature n, String msg)  
    { 
        super(s, r, p, n, i, msg);
    }

    private static Resource getSystemResource() {
        return ResourceManagerEJBImpl.getOne()
                    .findResourcePojoById(AuthzConstants.authzHQSystem);
    }
    
    private static ServerConfigAudit createAudit(AuthzSubject user,
                                                 String propKey,
                                                 String newVal, String old) 
    {
        ServerConfigAudit res = 
            new ServerConfigAudit(user, getSystemResource(), CONFIG_UPDATE,
                                  AuditImportance.HIGH, AuditNature.UPDATE,
                                  MSGS.format(propKey, newVal, old));

        res.setStartTime(System.currentTimeMillis());
        res.setEndTime(System.currentTimeMillis());
        res.setFieldName(propKey);
        res.setOldFieldValue(old);
        res.setNewFieldValue(newVal);
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }

    public static ServerConfigAudit updateBaseURL(AuthzSubject user, 
                                                  String newVal, String old) 
    {
        return createAudit(user, "auditMsg.serverConfig.baseUrl", newVal, old);
    }
    
    public static ServerConfigAudit updateFromEmail(AuthzSubject user, 
                                                    String newVal, String old) 
    {
        return createAudit(user, "auditMsg.serverConfig.fromEmail", newVal, 
                           old);
    }
    
    public static ServerConfigAudit updateAnnounce(AuthzSubject user, 
                                                   UpdateStatusMode newVal,
                                                   UpdateStatusMode old)
    {
        return createAudit(user, "auditMsg.serverConfig.announce", 
                           newVal.getValue(), old.getValue()); 
    }
    
    public static ServerConfigAudit 
        updateExternalHelp(AuthzSubject user, boolean newVal, boolean old)
    {
        return createAudit(user, "auditMsg.serverConfig.help", newVal + "", 
                           old + ""); 
    }
    
    public static ServerConfigAudit updateDBMaint(AuthzSubject user, int newVal,
                                                  int old)
    {
        return createAudit(user, "auditMsg.serverConfig.dbMaint", newVal + "", 
                           old + ""); 
    }
    
    public static ServerConfigAudit updateDeleteDetailed(AuthzSubject user, 
                                                         int newVal, int old)
    {
        return createAudit(user, "auditMsg.serverConfig.deleteDetailed", 
                           newVal + "", old + "");  
    }
    
    public static ServerConfigAudit updateNightlyReindex(AuthzSubject user,
                                                   boolean newVal,
                                                   boolean old)
    {
        return createAudit(user, "auditMsg.serverConfig.nightlyReindex", 
                           newVal + "", old + "");  
    }

    public static ServerConfigAudit updateAlertPurgeInterval(AuthzSubject user, 
                                                             int newVal, int old)
    {
        return createAudit(user, "auditMsg.serverConfig.alertPurge", 
                           newVal + "", old + "");  
    }
    
    public static ServerConfigAudit updateEventPurgeInterval(AuthzSubject user, 
                                                             int newVal, int old)
    {
        return createAudit(user, "auditMsg.serverConfig.eventPurge", 
                           newVal + "", old + "");  
    }
}
