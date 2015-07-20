package org.hyperic.hq.ui.action.portlet.availsummary;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.DashboardBaseFormNG;
import org.hyperic.hq.ui.action.portlet.DashboardFormNG;

public class PropertiesFormNG extends DashboardBaseFormNG {

    public final static String RESOURCES = Constants.USERPREF_KEY_AVAILABITY_RESOURCES_NG;
    public final static String NUM_TO_SHOW = ".dashContent.availsummary.numToShow";
    public final static String TITLE = ".dashContent.availSummary.title";

    private Integer _numberToShow;
    private String[] _ids;
    private String _title;

    public PropertiesFormNG() {
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

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }
}
