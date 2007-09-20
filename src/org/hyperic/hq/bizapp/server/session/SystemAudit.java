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
package org.hyperic.hq.bizapp.server.session;

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

public class SystemAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.bizapp.Resources");

    public static final SystemAuditPurpose HQ_STARTED = 
        new SystemAuditPurpose(0x3000, "hq started", 
                                 "audit.hq.started");

    public static class SystemAuditPurpose extends AuditPurpose {
        SystemAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    protected SystemAudit() {}
    
    SystemAudit(Resource r, AuthzSubject s, AuditPurpose p, 
                AuditImportance i, AuditNature n, String msg)
    { 
        super(s, r, p, n, i, msg);
        setStartTime(System.currentTimeMillis());
        setEndTime(System.currentTimeMillis());
    }

    private static Resource getRootResource() {
        Integer ROOT_ID = new Integer(0);
        
        return ResourceManagerEJBImpl.getOne().findResourcePojoById(ROOT_ID);
    }
    
    public static AuthzSubject getOverlord() {
        return AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
    }
    
    public static SystemAudit createUpAudit(long startupTime) {
        String msg = MSGS.format("auditMsg.hq.started"); 
        SystemAudit res = new SystemAudit(getRootResource(), getOverlord(), 
                                          HQ_STARTED, AuditImportance.MEDIUM, 
                                          AuditNature.START, msg); 

        res.setEndTime(System.currentTimeMillis());
        res.setStartTime(res.getEndTime() - startupTime);
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
}
