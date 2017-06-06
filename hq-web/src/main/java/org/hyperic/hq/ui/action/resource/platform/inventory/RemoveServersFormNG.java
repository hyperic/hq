package org.hyperic.hq.ui.action.resource.platform.inventory;

import org.hyperic.hq.ui.action.resource.RemoveResourceFormNG;

public class RemoveServersFormNG extends RemoveResourceFormNG {

	
    public RemoveServersFormNG() {
        super();
    }

    public Integer getFs() {
        return getF();
    }

    public void setFs(Integer f) {
        setF(f);
    }
}
