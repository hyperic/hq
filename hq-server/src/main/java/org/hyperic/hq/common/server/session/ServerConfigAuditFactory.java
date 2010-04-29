package org.hyperic.hq.common.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.util.i18n.MessageBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerConfigAuditFactory {
    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.common.Resources");

    private static final ServerConfigAuditPurpose CONFIG_UPDATE = new ServerConfigAuditPurpose(0x6000,
        "server config update", "audit.serverConfig.update");

    private static class ServerConfigAuditPurpose
        extends AuditPurpose {
        ServerConfigAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    private AuditManager auditManager;
    private ResourceManager resourceManager;

    @Autowired
    public ServerConfigAuditFactory(AuditManager auditManager, ResourceManager resourceManager) {
        this.auditManager = auditManager;
        this.resourceManager = resourceManager;
    }

    private ServerConfigAudit createAudit(AuthzSubject user, String propKey, String newVal, String old) {
        ServerConfigAudit res = new ServerConfigAudit(user, resourceManager
            .findResourceById(AuthzConstants.authzHQSystem), CONFIG_UPDATE, AuditImportance.HIGH, AuditNature.UPDATE,
            MSGS.format(propKey, newVal, old));

        res.setStartTime(System.currentTimeMillis());
        res.setEndTime(System.currentTimeMillis());
        res.setFieldName(propKey);
        res.setOldFieldValue(old);
        res.setNewFieldValue(newVal);
        auditManager.saveAudit(res);
        return res;
    }

    public ServerConfigAudit updateBaseURL(AuthzSubject user, String newVal, String old) {
        return createAudit(user, "auditMsg.serverConfig.baseUrl", newVal, old);
    }

    public ServerConfigAudit updateFromEmail(AuthzSubject user, String newVal, String old) {
        return createAudit(user, "auditMsg.serverConfig.fromEmail", newVal, old);
    }

    public ServerConfigAudit updateAnnounce(AuthzSubject user, UpdateStatusMode newVal, UpdateStatusMode old) {
        return createAudit(user, "auditMsg.serverConfig.announce", newVal.getValue(), old.getValue());
    }

    public ServerConfigAudit updateExternalHelp(AuthzSubject user, boolean newVal, boolean old) {
        return createAudit(user, "auditMsg.serverConfig.help", newVal + "", old + "");
    }

    public ServerConfigAudit updateDBMaint(AuthzSubject user, int newVal, int old) {
        return createAudit(user, "auditMsg.serverConfig.dbMaint", newVal + "", old + "");
    }

    public ServerConfigAudit updateDeleteDetailed(AuthzSubject user, int newVal, int old) {
        return createAudit(user, "auditMsg.serverConfig.deleteDetailed", newVal + "", old + "");
    }

    public ServerConfigAudit updateAlertPurgeInterval(AuthzSubject user, int newVal, int old) {
        return createAudit(user, "auditMsg.serverConfig.alertPurge", newVal + "", old + "");
    }

    public ServerConfigAudit updateEventPurgeInterval(AuthzSubject user, int newVal, int old) {
        return createAudit(user, "auditMsg.serverConfig.eventPurge", newVal + "", old + "");
    }

    public ServerConfigAudit updateAlertsEnabled(AuthzSubject user, boolean newVal, boolean old) {
        return createAudit(user, "auditMsg.serverConfig.alertsEnabled", newVal + "", old + "");
    }

    public ServerConfigAudit updateAlertNotificationsEnabled(AuthzSubject user, boolean newVal, boolean old) {
        return createAudit(user, "auditMsg.serverConfig.alertNotificationsEnabled", newVal + "", old + "");
    }

    public ServerConfigAudit updateHierarchicalAlertingEnabled(AuthzSubject user, boolean newVal, boolean old) {
        return createAudit(user, "auditMsg.serverConfig.hierarchicalAlertingEnabled", newVal + "", old + "");
    }
}
