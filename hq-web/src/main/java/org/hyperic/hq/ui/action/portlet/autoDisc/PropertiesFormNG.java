package org.hyperic.hq.ui.action.portlet.autoDisc;

import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class PropertiesFormNG extends BaseValidatorFormNG {

	
    private Integer range;

    public PropertiesFormNG() {
    }

    public Integer getRange() {
        return this.range;
    }

    public void setRange(Integer range) {
        this.range = range;
    }
}
