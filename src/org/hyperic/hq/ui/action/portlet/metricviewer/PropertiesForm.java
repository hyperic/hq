package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;

public class PropertiesForm extends DashboardBaseForm {

    protected final static String RESOURCES =
        ".dashContent.metricviewer.resources";
    protected final static String NUM_TO_SHOW =
        ".dashContent.metricviewer.numToShow";
    protected final static String RES_TYPE =
        ".dashContent.metricviewer.resType";
    protected final static String METRIC =
        ".dashContent.metricviewer.metric";

    private AppdefEntityTypeID _resType;
    private String _metric;
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

    public String getResourceType() {
        return _resType.getAppdefKey();
    }

    public void setResourceType(String resType) {
        _resType = new AppdefEntityTypeID(resType);
    }

    public int getAppdefType() {
        return _resType.getType();
    }

    public int getAppdefTypeID() {
        return _resType.getID();
    }

    public String getMetric() {
        return _metric;
    }

    public void setMetric(String metric) {
        _metric = metric;
    }
}
