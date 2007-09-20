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
package org.hyperic.hq.auth.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectField;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.util.i18n.MessageBundle;

public class UserAudit extends Audit {
    private static final MessageBundle MSGS = 
        MessageBundle.getBundle("org.hyperic.hq.auth.Resources");

    public static final UserAuditPurpose USER_LOGIN = 
        new UserAuditPurpose(0x4000, "user login", "audit.user.login"); 
    public static final UserAuditPurpose USER_LOGOUT = 
        new UserAuditPurpose(0x4001, "user logout", "audit.user.logout"); 
    public static final UserAuditPurpose USER_CREATE = 
        new UserAuditPurpose(0x4002, "user logout", "audit.user.create"); 
    public static final UserAuditPurpose USER_UPDATE = 
        new UserAuditPurpose(0x4003, "user logout", "audit.user.update"); 
        

    public static class UserAuditPurpose extends AuditPurpose {
        UserAuditPurpose(int code, String desc, String localeProp) { 
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    protected UserAudit() {}
    
    UserAudit(Resource r, AuthzSubject s, AuditPurpose p, 
              AuditImportance i, AuditNature n, String msg)
    { 
        super(s, r, p, n, i, msg);
        long now = System.currentTimeMillis();
        setStartTime(now);
        setEndTime(now);
    }

    private static Resource getRootResource() {
        Integer ROOT_ID = new Integer(0);
        
        return ResourceManagerEJBImpl.getOne().findResourcePojoById(ROOT_ID);
    }
    
    public static UserAudit loginAudit(AuthzSubject user) {
        String msg = MSGS.format("auditMsg.user.login", user.getFullName());
        UserAudit res = new UserAudit(user.getResource(), user, USER_LOGIN,
                                      AuditImportance.LOW, 
                                      AuditNature.START, msg); 
                                      
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
    public static UserAudit logoutAudit(AuthzSubject user) {
        String msg = MSGS.format("auditMsg.user.logout", user.getFullName());
        UserAudit res = new UserAudit(user.getResource(), user, USER_LOGOUT,
                                      AuditImportance.LOW, 
                                      AuditNature.STOP, msg); 
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
    public static UserAudit createAudit(AuthzSubject creator,
                                        AuthzSubject newUser) 
    {
        String msg = MSGS.format("auditMsg.user.create", 
                                 newUser.getFullName() + "(" + 
                                 newUser.getName() + ")");
        UserAudit res = new UserAudit(newUser.getResource(), creator, 
                                      USER_CREATE, AuditImportance.HIGH, 
                                      AuditNature.CREATE, msg); 
        
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
    public static UserAudit updateAudit(AuthzSubject updator,
                                        AuthzSubject target,
                                        AuthzSubjectField field,
                                        String oldVal, String newVal) 
    {
        String msg = MSGS.format("auditMsg.user.update", 
                                 target.getFullName(), field.getValue(), 
                                 newVal);
        UserAudit res = new UserAudit(target.getResource(), updator, 
                                      USER_UPDATE, AuditImportance.LOW, 
                                      AuditNature.UPDATE, msg); 
        
        res.setFieldName(field.getValue());
        res.setOldFieldValue(oldVal);
        res.setNewFieldValue(newVal);
        AuditManagerEJBImpl.getOne().saveAudit(res);
        return res;
    }
    
}
