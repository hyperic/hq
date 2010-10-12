/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
