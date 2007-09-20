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
import org.hyperic.util.i18n.MessageBundle;

public class ResourceAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    public static final ResourceAuditPurpose RESOURCE_CREATE = 
        new ResourceAuditPurpose(0x2000, "resource create", 
                                 "audit.resource.create");
    public static final ResourceAuditPurpose RESOURCE_UPDATE = 
        new ResourceAuditPurpose(0x2001, "resource update", 
                                 "audit.resource.update");
    public static final ResourceAuditPurpose RESOURCE_DELETE = 
        new ResourceAuditPurpose(0x2002, "resource delete", 
                                 "audit.resource.delete");

    public static class ResourceAuditPurpose extends AuditPurpose {
        ResourceAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    protected ResourceAudit() {}
    
    ResourceAudit(Resource r, AuthzSubject s, AuditPurpose p, 
                  AuditImportance i, AuditNature n, String msg, long start, 
                  long end) 
    { 
        super(s, r, p, n, i, msg);
        setStartTime(start);
        setEndTime(end);
    }

    public static ResourceAudit createResource(Resource r, AuthzSubject creator,
                                               long start, long end)
    {
        String msg = MSGS.format("auditMsg.resource.create", 
                                 r.getResourceType().getLocalizedName(),
                                 r.getName());
        ResourceAudit res = new ResourceAudit(r, creator, RESOURCE_CREATE,
                                              AuditImportance.MEDIUM, 
                                              AuditNature.CREATE,
                                              msg, start, end);  
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
    private static Resource getRootResource() {
        Integer ROOT_ID = new Integer(0);
        
        return ResourceManagerEJBImpl.getOne().findResourcePojoById(ROOT_ID);
    }
    
    public static ResourceAudit deleteResource(Resource r, AuthzSubject creator,
                                               long start, long end)
    {
        String msg = MSGS.format("auditMsg.resource.delete", 
                                 r.getResourceType().getLocalizedName(),
                                 r.getName());
        ResourceAudit res = new ResourceAudit(getRootResource(), creator, 
                                              RESOURCE_DELETE,
                                              AuditImportance.HIGH, 
                                              AuditNature.DELETE,
                                              msg, start, end);  
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
}
