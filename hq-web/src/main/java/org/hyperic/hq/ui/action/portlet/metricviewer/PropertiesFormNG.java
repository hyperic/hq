package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.ui.action.portlet.DashboardBaseFormNG;

public class PropertiesFormNG extends DashboardBaseFormNG {
    protected final static String RESOURCES = ".ng.dashContent.metricviewer.resources";
    protected final static String NUM_TO_SHOW = ".dashContent.metricviewer.numToShow";
    protected final static String RES_TYPE = ".dashContent.metricviewer.resType";
    protected final static String METRIC = ".dashContent.metricviewer.metric";
    protected final static String DECSENDING = ".dashContent.metricviewer.descending";
    protected final static String TITLE = ".dashContent.metricViewer.title";

    private String _resType;
    private String _metric;
    private Integer _numberToShow;
    private String[] _ids;
    private String _descending;
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

    public String getResourceType() {
        return _resType;
    }

    public void setResourceType(String resType) {
        _resType = resType;
    }

    public String getAppdefType() {
        if (_resType != null && _resType.length() != 0) {
            AppdefEntityTypeID type = new AppdefEntityTypeID(_resType);
            return Integer.toString(type.getType());
        }
        return "";
    }

    public String getAppdefTypeID() {
        if (_resType != null && _resType.length() != 0) {
            return new AppdefEntityTypeID(_resType).getId().toString();
        }
        return "";
    }

    public String getMetric() {
        return _metric;
    }

    public void setMetric(String metric) {
        _metric = metric;
    }

    public String getDescending() {
        return _descending;
    }

    public void setDescending(String descending) {
        _descending = descending;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }
}
