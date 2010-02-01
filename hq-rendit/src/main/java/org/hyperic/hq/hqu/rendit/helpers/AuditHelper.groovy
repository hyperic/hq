package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.common.server.session.AuditPurpose
import org.hyperic.hq.common.server.session.AuditImportance

class AuditHelper extends BaseHelper {
    private auditMan = Bootstrap.getBean(AuditManager.class)
    
    AuditHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all Audits in the system with the given criteria.
     */
    def findAudits(long startTime, long endTime, AuditImportance minImportance,
                   AuditPurpose purpose, AuthzSubject target, String klazz,
                   PageInfo pInfo) 
    {
        auditMan.find(user, pInfo, startTime, endTime, minImportance,
                      purpose, target, klazz)
    }
}
