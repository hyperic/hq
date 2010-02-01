package org.hyperic.hq.common.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.SubjectRemoveCallback;
import org.hyperic.hq.common.shared.AuditManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class SubjectDeleteWatcher implements SubjectRemoveCallback {
    private AuditManager auditManager;
    
    
    @Autowired
    public SubjectDeleteWatcher(AuditManager auditManager) {
        this.auditManager = auditManager;
    }



    public void subjectRemoved(AuthzSubject toDelete) {
        auditManager.handleSubjectDelete(toDelete);
    }

}
