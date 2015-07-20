package org.hyperic.hq.ui.action.portlet.resourcehealth;

import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class PropertiesFormNG extends BaseValidatorFormNG {

	
    private String[] ids;
    private String order;

    public PropertiesFormNG() {
        super();
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }

    public String[] getIds() {
        return this.ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

	
}
