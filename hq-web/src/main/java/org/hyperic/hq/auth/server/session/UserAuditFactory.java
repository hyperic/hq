package org.hyperic.hq.auth.server.session;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectField;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditNature;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.util.i18n.MessageBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserAuditFactory {

    private static final MessageBundle MSGS = MessageBundle.getBundle("org.hyperic.hq.auth.Resources");

    private static final UserAuditPurpose USER_LOGIN = new UserAuditPurpose(0x4000, "user login", "audit.user.login");
    private static final UserAuditPurpose USER_LOGOUT = new UserAuditPurpose(0x4001, "user logout", "audit.user.logout");
    private static final UserAuditPurpose USER_CREATE = new UserAuditPurpose(0x4002, "user logout", "audit.user.create");
    private static final UserAuditPurpose USER_UPDATE = new UserAuditPurpose(0x4003, "user logout", "audit.user.update");

    private static class UserAuditPurpose
        extends AuditPurpose {
        UserAuditPurpose(int code, String desc, String localeProp) {
            super(code, desc, localeProp, MSGS.getResourceBundle());
        }
    }

    private AuditManager auditManager;

    @Autowired
    public UserAuditFactory(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    public UserAudit loginAudit(AuthzSubject user) {
        String msg = MSGS.format("auditMsg.user.login", user.getFullName());
        UserAudit res = new UserAudit(user.getResource(), user, USER_LOGIN, AuditImportance.LOW, AuditNature.START, msg);

        auditManager.saveAudit(res);
        return res;
    }

    public UserAudit logoutAudit(AuthzSubject user) {
        String msg = MSGS.format("auditMsg.user.logout", user.getFullName());
        UserAudit res = new UserAudit(user.getResource(), user, USER_LOGOUT, AuditImportance.LOW, AuditNature.STOP, msg);

        auditManager.saveAudit(res);
        return res;
    }

    public UserAudit createAudit(AuthzSubject creator, AuthzSubject newUser) {
        String msg = MSGS.format("auditMsg.user.create", newUser.getFullName() + "(" + newUser.getName() + ")");
        UserAudit res = new UserAudit(newUser.getResource(), creator, USER_CREATE, AuditImportance.HIGH,
            AuditNature.CREATE, msg);

        auditManager.saveAudit(res);
        return res;
    }

    public UserAudit updateAudit(AuthzSubject updator, AuthzSubject target, AuthzSubjectField field, String oldVal,
                                 String newVal) {
        String msg = MSGS.format("auditMsg.user.update", target.getFullName(), field.getValue(), newVal);
        UserAudit res = new UserAudit(target.getResource(), updator, USER_UPDATE, AuditImportance.LOW,
            AuditNature.UPDATE, msg);

        res.setFieldName(field.getValue());
        res.setOldFieldValue(oldVal);
        res.setNewFieldValue(newVal);
        auditManager.saveAudit(res);
        return res;
    }
}
