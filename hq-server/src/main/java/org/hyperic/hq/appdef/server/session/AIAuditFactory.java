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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.util.i18n.MessageBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AIAuditFactory {

    private ResourceManager resourceManager;
    private AuditManager auditManager;
    private AuthzSubjectManager authzSubjectManager;
    
    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.appdef.Resources");

    private static final AIAuditPurpose IMPORT_RUNTIME = new AIAuditPurpose(0x1000, "runtime import",
        "audit.import.runtime");
    private static final AIAuditPurpose IMPORT_APPROVE = new AIAuditPurpose(0x1001, "approve import",
        "audit.import.approve");

    private static class AIAuditPurpose
        extends AuditPurpose {
        AIAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    @Autowired
    public AIAuditFactory(ResourceManager resourceManager, AuditManager auditManager, AuthzSubjectManager authzSubjectManager) {
        this.resourceManager = resourceManager;
        this.auditManager = auditManager;
        this.authzSubjectManager = authzSubjectManager;
    }

    public AIAudit newImportAudit(AuthzSubject user) {
        AIAudit res = new AIAudit(user, resourceManager.findResourceById(AuthzConstants.authzHQSystem), IMPORT_APPROVE,
            AuditImportance.HIGH, AuditNature.CREATE, MSGS.format("auditMsg.import.approve"));

        auditManager.saveAudit(res);
        return res;
    }

    public AIAudit newRuntimeImportAudit(Agent reporter) {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        AIAudit res = new AIAudit(overlord, resourceManager.findResourceById(AuthzConstants.authzHQSystem),
            IMPORT_RUNTIME, AuditImportance.MEDIUM, AuditNature.CREATE, MSGS.format("auditMsg.import.runtime", reporter
                .getAddress()));

        auditManager.saveAudit(res);
        return res;
    }
}
