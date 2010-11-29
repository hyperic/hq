package org.hyperic.hq.inventory.domain;

import org.springframework.roo.addon.dod.RooDataOnDemand;
import org.hyperic.hq.inventory.domain.Resource;

@RooDataOnDemand(entity = Resource.class)
public class ResourceDataOnDemand {

	public Resource getNewTransientResource(int index) {
        org.hyperic.hq.inventory.domain.Resource obj = new org.hyperic.hq.inventory.domain.Resource();
        //TODO Roo doesn't automatically generate this even though using NotNull validation.  I'm guessing this is b/c it's marked as Transient?
        obj.setName("name_" + index);
        return obj;
    }
}
