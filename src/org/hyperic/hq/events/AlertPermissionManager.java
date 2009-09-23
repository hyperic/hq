package org.hyperic.hq.events;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;

public interface AlertPermissionManager {

    void canManageAlerts(AuthzSubject who, AppdefEntityID id) throws PermissionException;

    void canManageAlerts(AuthzSubject who, AlertDefinitionInterface adi) throws PermissionException;

    AppdefEntityID getAppdefEntityID(AlertDefinitionInterface adi);

    void canModifyEscalation(Integer subjectId) throws PermissionException;

    void canRemoveEscalation(Integer subjectId) throws PermissionException;

}
