package org.hyperic.hq.events.server.session;

import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.inventory.domain.ResourceType;

public class ResourceTypeAlertDefinition
    extends AlertDefinition {

    private ResourceType resourceType;

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public AlertDefinitionValue getAlertDefinitionValue() {
        // TODO Auto-generated method stub
        return null;
    }

}
