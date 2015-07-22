package org.hyperic.hq.ui.action.portlet.criticalalerts;

import org.hyperic.hq.ui.action.portlet.DashboardBaseFormNG;

public class PropertiesFormNG extends DashboardBaseFormNG {
    public final static String ALERT_NUMBER = ".dashContent.criticalalerts.numberOfAlerts";
    public final static String PAST = ".dashContent.criticalalerts.past";
    public final static String PRIORITY = ".dashContent.criticalalerts.priority";
    public final static String SELECTED_OR_ALL = ".dashContent.criticalalerts.selectedOrAll";
    public final static String TITLE = ".dashContent.criticalAlerts.title";

    private Integer _numberOfAlerts;
    private String _priority;
    private long _past;
    private String _selectedOrAll;
    private String _key;
    private String[] _ids;
    private String _title;

    public PropertiesFormNG() {
        super();
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }

    public Integer getNumberOfAlerts() {
        return _numberOfAlerts;
    }

    public void setNumberOfAlerts(Integer numberOfAlerts) {
        _numberOfAlerts = numberOfAlerts;
    }

    public String getPriority() {
        return _priority;
    }

    public void setPriority(String priority) {
        _priority = priority;
    }

    public long getPast() {
        return _past;
    }

    public void setPast(long past) {
        _past = past;
    }

    public String getSelectedOrAll() {
        return _selectedOrAll;
    }

    public void setSelectedOrAll(String selectedOrAll) {
        _selectedOrAll = selectedOrAll;
    }

    public String getKey() {
        return _key;
    }

    public void setKey(String key) {
        _key = key;
    }

    public String[] getIds() {
        return _ids;
    }

    public void setIds(String[] ids) {
        _ids = ids;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        this._title = title;
    }

}
