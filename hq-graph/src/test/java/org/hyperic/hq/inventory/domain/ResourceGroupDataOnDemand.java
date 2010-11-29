package org.hyperic.hq.inventory.domain;

import org.springframework.roo.addon.dod.RooDataOnDemand;
import org.hyperic.hq.inventory.domain.ResourceGroup;

@RooDataOnDemand(entity = ResourceGroup.class)
public class ResourceGroupDataOnDemand {

	public ResourceGroup getNewTransientResourceGroup(int index) {
        org.hyperic.hq.inventory.domain.ResourceGroup obj = new org.hyperic.hq.inventory.domain.ResourceGroup();
        //TODO Roo doesn't automatically generate this even though using NotNull validation.  I'm guessing this is b/c it's marked as Transient?
        obj.setName("name_" + index);
        return obj;
    }
}
