package org.hyperic.hq.plugin.domain;

import org.springframework.roo.addon.dod.RooDataOnDemand;
import org.hyperic.hq.plugin.domain.ResourceType;

@RooDataOnDemand(entity = ResourceType.class)
public class ResourceTypeDataOnDemand {

	public ResourceType getNewTransientResourceType(int index) {
        org.hyperic.hq.plugin.domain.ResourceType obj = new org.hyperic.hq.plugin.domain.ResourceType();
        //TODO Roo doesn't automatically generate this even though using NotNull validation.  I'm guessing this is b/c it's marked as Transient?
        obj.setName("name_" + index);
        return obj;
    }
}
