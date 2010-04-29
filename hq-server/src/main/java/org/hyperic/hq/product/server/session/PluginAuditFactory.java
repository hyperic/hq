package org.hyperic.hq.product.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
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
public class PluginAuditFactory {

    private ResourceManager resourceManager;
    private AuditManager auditManager;
    private AuthzSubjectManager authzSubjectManager;

    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.product.Resources");

    private static final PluginAuditPurpose PLUGIN_DEPLOYED = new PluginAuditPurpose(0x9000, "plugin deployed",
        "audit.plugin.deploy");
    private static final PluginAuditPurpose PLUGIN_UPDATED = new PluginAuditPurpose(0x9001, "plugin updated",
        "audit.plugin.update");

    private static class PluginAuditPurpose
        extends AuditPurpose {
        PluginAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    @Autowired
    public PluginAuditFactory(ResourceManager resourceManager, AuditManager auditManager, AuthzSubjectManager authzSubjectManager) {
        this.resourceManager = resourceManager;
        this.auditManager = auditManager;
        this.authzSubjectManager = authzSubjectManager;
    }

    public PluginAudit deployAudit(String pluginName, long start, long end) {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        String msg = MSGS.format("auditMsg.plugin.deploy", pluginName);
        PluginAudit res = new PluginAudit(resourceManager.findResourceById(AuthzConstants.authzHQSystem), overlord,
            PLUGIN_DEPLOYED, AuditImportance.HIGH, AuditNature.CREATE, msg, start, end);

        auditManager.saveAudit(res);
        return res;
    }

    public PluginAudit updateAudit(String pluginName, long start, long end) {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        String msg = MSGS.format("auditMsg.plugin.update", pluginName);
        PluginAudit res = new PluginAudit(resourceManager.findResourceById(AuthzConstants.authzHQSystem), overlord,
            PLUGIN_UPDATED, AuditImportance.HIGH, AuditNature.UPDATE, msg, start, end);

        auditManager.saveAudit(res);
        return res;
    }
}
