package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {

    public final static String NUM_TO_SHOW =
        ".dashContent.metricviewer.numToShow";

    private Integer _numberToShow;
    private String[] _ids;

    public PropertiesForm() {
        super();
    }

    public String[] getIds() {
        return _ids;
    }

    public void setIds(String[] ids) {
        _ids = ids;
    }

    public Integer getNumberToShow() {
        return _numberToShow;
    }

    public void setNumberToShow(Integer numberToShow) {
        _numberToShow = numberToShow;
    }
}
