package org.hyperic.hq.events;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;

public interface AlertPermissionManager {
    
    void canFixAcknowledgeAlerts(AuthzSubject user, AppdefEntityID entityId) throws PermissionException;
    
    void canFixAcknowledgeAlerts(AuthzSubject who, AlertDefinitionInterface adi) throws PermissionException;
    
    void canCreateResourceTypeAlertDefinitionTemplate(AuthzSubject user) throws PermissionException;
    
    void canViewResourceTypeAlertDefinitionTemplate(AuthzSubject user) throws PermissionException;
    
    void canCreateAlertDefinition(AuthzSubject user, AppdefEntityID entityId) throws PermissionException;
    
    void canViewAlertDefinition(AuthzSubject user, AppdefEntityID entityId) throws PermissionException;
    
    void canModifyAlertDefinition(AuthzSubject user, AppdefEntityID entityId) throws PermissionException;
    
    void canDeleteAlertDefinition(AuthzSubject user, AppdefEntityID entityId) throws PermissionException;
    
    AppdefEntityID getAppdefEntityID(AlertDefinitionInterface adi);

    void canModifyEscalation(Integer subjectId) throws PermissionException;

    void canRemoveEscalation(Integer subjectId) throws PermissionException;

}
