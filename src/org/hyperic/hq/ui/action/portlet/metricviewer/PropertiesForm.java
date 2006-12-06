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

    private String _resType;
    private String _metric;
    private Integer _numberToShow;
    private String[] _ids;
    private String _token;
    
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

    public String getToken() {
        return _token;
    }

    public void setToken(String token) {
        _token = token;
    }
}
