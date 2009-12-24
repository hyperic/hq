/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.portlet.metricviewer;

import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;

public class PropertiesForm
    extends DashboardBaseForm {

    protected final static String RESOURCES = ".dashContent.metricviewer.resources";
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
