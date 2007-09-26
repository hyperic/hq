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
package org.hyperic.hq.product.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.util.i18n.MessageBundle;

public class PluginAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.product.Resources");

    public static final PluginAuditPurpose PLUGIN_DEPLOYED = 
        new PluginAuditPurpose(0x9000, "plugin deployed", "audit.plugin.deploy"); 
    public static final PluginAuditPurpose PLUGIN_UPDATED = 
        new PluginAuditPurpose(0x9001, "plugin updated", "audit.plugin.update"); 
        
    public static class PluginAuditPurpose extends AuditPurpose {
        PluginAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    protected PluginAudit() {}
    
    PluginAudit(Resource r, AuthzSubject s, AuditPurpose p, 
                AuditImportance i, AuditNature n, String msg, long startTime,
                long endTime)
    { 
        super(s, r, p, n, i, msg);
        setStartTime(startTime);
        setEndTime(endTime);
    }

    private static Resource getRootResource() {
        Integer ROOT_ID = new Integer(0);
        
        return ResourceManagerEJBImpl.getOne().findResourcePojoById(ROOT_ID);
    }
    
    public static PluginAudit deployAudit(String pluginName, long start, 
                                          long end) 
    {
        AuthzSubject overlord = 
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        String msg = MSGS.format("auditMsg.plugin.deploy", pluginName);
        PluginAudit res = new PluginAudit(getRootResource(), overlord, 
                                          PLUGIN_DEPLOYED, AuditImportance.HIGH,
                                          AuditNature.CREATE, msg, start, end); 
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
    public static PluginAudit updateAudit(String pluginName, long start, 
                                          long end) 
    {
        AuthzSubject overlord = 
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        String msg = MSGS.format("auditMsg.plugin.update", pluginName);
        PluginAudit res = new PluginAudit(getRootResource(), overlord, 
                                          PLUGIN_UPDATED, AuditImportance.HIGH,
                                          AuditNature.UPDATE, msg, start, end); 
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
}
