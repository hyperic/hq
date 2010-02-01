package org.hyperic.hq.events.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.util.i18n.MessageBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AlertAuditFactory {

    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    private static final AlertAuditPurpose ALERT_ENABLE = new AlertAuditPurpose(0x5000, "alert enable",
        "audit.alert.enable");
    private static final AlertAuditPurpose ALERT_DISABLE = new AlertAuditPurpose(0x5001, "alert disable",
        "audit.alert.disable");
    private static final AlertAuditPurpose ALERT_DELETE = new AlertAuditPurpose(0x5002, "alert delete",
        "audit.alert.delete");

    private static class AlertAuditPurpose
        extends AuditPurpose {
        AlertAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    private AuditManager auditManager;

    @Autowired
    public AlertAuditFactory(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    public AlertAudit enableAlert(AlertDefinition def, AuthzSubject modifier) {
        final boolean enabled = def.isActive();
        String msg = enabled ? MSGS.format("auditMsg.alert.enable", def.getName()) : MSGS.format(
            "auditMsg.alert.disable", def.getName());
        AlertAudit res = new AlertAudit(def, modifier, enabled ? ALERT_ENABLE : ALERT_DISABLE,
            enabled ? AuditImportance.MEDIUM : AuditImportance.HIGH,
            enabled ? AuditNature.ENABLE : AuditNature.DISABLE, msg, System.currentTimeMillis());

        auditManager.saveAudit(res);
        return res;
    }

    public AlertAudit deleteAlert(AlertDefinition def, AuthzSubject modifier) {
        String msg = MSGS.format("auditMsg.alert.delete", def.getName());
        AlertAudit res = new AlertAudit(def, modifier, ALERT_DELETE, AuditImportance.HIGH, AuditNature.DELETE, msg,
            System.currentTimeMillis());

        auditManager.saveAudit(res);
        return res;
    }
}
