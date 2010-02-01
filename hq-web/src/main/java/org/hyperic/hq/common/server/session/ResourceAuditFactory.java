package org.hyperic.hq.common.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.util.i18n.MessageBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceAuditFactory {

    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    private static final ResourceAuditPurpose RESOURCE_CREATE = new ResourceAuditPurpose(0x2000, "resource create",
        "audit.resource.create");
    private static final ResourceAuditPurpose RESOURCE_DELETE = new ResourceAuditPurpose(0x2002, "resource delete",
        "audit.resource.delete");
    private static final ResourceAuditPurpose RESOURCE_MOVE = new ResourceAuditPurpose(0x2003, "resource move",
        "audit.resource.move");

    private static class ResourceAuditPurpose
        extends AuditPurpose {
        ResourceAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    private AuditManager auditManager;

    @Autowired
    public ResourceAuditFactory(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    public ResourceAudit createResource(Resource r, AuthzSubject creator, long start, long end) {
        String msg = MSGS.format("auditMsg.resource.create", r.getResourceType().getLocalizedName());
        ResourceAudit res = new ResourceAudit(r, creator, RESOURCE_CREATE, AuditImportance.MEDIUM, AuditNature.CREATE,
            msg, start, end);

        auditManager.saveAudit(res);
        return res;
    }

    public ResourceAudit deleteResource(Resource systemResource, AuthzSubject creator, long start, long end) {
        String msg = MSGS.format("auditMsg.resource.delete");
        ResourceAudit res = new ResourceAudit(systemResource, creator,
            RESOURCE_DELETE, AuditImportance.HIGH, AuditNature.DELETE, msg, start, end);

        auditManager.saveAudit(res);
        return res;
    }

    public ResourceAudit moveResource(Resource target, Resource destination, AuthzSubject mover, long start, long end) {
        String msg = MSGS.format("auditMsg.resource.move", destination.getName());
        ResourceAudit res = new ResourceAudit(target, mover, RESOURCE_MOVE, AuditImportance.HIGH, AuditNature.MOVE,
            msg, start, end);
        auditManager.saveAudit(res);
        return res;
    }
}
