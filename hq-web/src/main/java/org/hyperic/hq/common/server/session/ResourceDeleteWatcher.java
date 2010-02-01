package org.hyperic.hq.common.server.session;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.common.shared.AuditManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("auditingResourceDeleteWatcher")
public class ResourceDeleteWatcher implements ResourceDeleteCallback {
    private AuditManager auditManager;

    @Autowired
    public ResourceDeleteWatcher(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    public void preResourceDelete(Resource r) {
        auditManager.handleResourceDelete(r);
    }

}
