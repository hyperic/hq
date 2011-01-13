package org.hyperic.hq.events.server.session;

import java.util.Iterator;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.inventory.domain.Resource;

public class ResourceAlertDefinition extends AlertDefinition {
    private Resource resource;
    
    public Resource getResource() {
        return resource;
    }

    void setResource(Resource resource) {
        this.resource = resource;
    }
    
    //TODO really need either of these?
    public Integer getAppdefId() {
        return getResource().getId();
    }
    
    public int getAppdefType() {
        String rtName = getResource().getType().getName();
        AppdefEntityID resourceId = AppdefUtil.newAppdefEntityId(getResource());
        
        if (resourceId.isPlatform()) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (resourceId.isServer()) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (resourceId.isService()) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else if (resourceId.isApplication()) {
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        } else if (resourceId.isGroup()) {
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;
            //TODO Type based alerts need to be handled in a new way
//        } else if (rtName.equals(AuthzConstants.platformPrototypeTypeName)) {
//            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
//        } else if (rtName.equals(AuthzConstants.serverPrototypeTypeName)) {
//            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
//        } else if (rtName.equals(AuthzConstants.servicePrototypeTypeName)) {
//            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else {
            throw new IllegalArgumentException(rtName + " is not a valid Appdef Resource Type");
        }
    }
    
    public AppdefEntityID getAppdefEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }
    
    public AlertDefinitionValue getAlertDefinitionValue() {
       
         AlertDefinitionValue _value = new AlertDefinitionValue();

        _value.setId(getId());
        _value.setName(getName() == null ? "" : getName());
        _value.setCtime(getCtime());
        _value.setMtime(getMtime());
        _value.setDescription(getDescription());
        _value.setEnabled(isEnabled());
        _value.setActive(isActive());
        _value.setWillRecover(isWillRecover());
        _value.setNotifyFiltered(isNotifyFiltered());
        _value.setControlFiltered(isControlFiltered());
        _value.setPriority(getPriority());
        _value.setAppdefId(getAppdefId());
        _value.setAppdefType(getAppdefType());
        _value.setFrequencyType(getFrequencyType());
        _value.setCount(getCount());
        _value.setRange(getRange());
        _value.setDeleted(isDeleted());

        if (getEscalation() != null) {
            _value.setEscalationId(getEscalation().getId());
        } else {
            _value.setEscalationId(null);
        }

        _value.removeAllTriggers();
        for (Iterator i = getTriggers().iterator(); i.hasNext();) {
            RegisteredTrigger t = (RegisteredTrigger) i.next();
            _value.addTrigger(t.getRegisteredTriggerValue());
        }
        _value.cleanTrigger();

        _value.removeAllConditions();
        for (Iterator i = getConditions().iterator(); i.hasNext();) {
            AlertCondition c = (AlertCondition) i.next();

            _value.addCondition(c.getAlertConditionValue());
        }
        _value.cleanCondition();

        _value.removeAllActions();
        for (Iterator i = getActions().iterator(); i.hasNext();) {
            Action a = (Action) i.next();

            _value.addAction(a.getActionValue());
        }
        _value.cleanAction();

        return _value;
    }

}
