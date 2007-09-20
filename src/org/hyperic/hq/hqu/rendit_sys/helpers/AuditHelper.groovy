package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl
import org.hyperic.hq.common.server.session.AuditPurpose
import org.hyperic.hq.common.server.session.AuditImportance

class AuditHelper extends BaseHelper {
    private auditMan = AuditManagerEJBImpl.one
    
    AuditHelper(AuthzSubject user) {
        super(user)
    }

    def findAudits(long startTime, long endTime, AuditImportance minImportance,
                   AuditPurpose purpose, AuthzSubject target, String klazz,
                   PageInfo pInfo) 
    {
        auditMan.find(user, pInfo, startTime, endTime, minImportance,
                      purpose, target, klazz)
    }
}
