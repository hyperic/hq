package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class PropertiesFormNG extends  BaseValidatorFormNG {

    static String RANGE = ".dashContent.recentlyApproved.range";

    private Integer _range;

    public PropertiesFormNG() {
        super();
    }

    public Integer getRange() {
        return _range;
    }

    public void setRange(Integer range) {
        _range = range;
    }
    
}
