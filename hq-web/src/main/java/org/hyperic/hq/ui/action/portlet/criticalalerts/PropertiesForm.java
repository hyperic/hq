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

package org.hyperic.hq.ui.action.portlet.criticalalerts;

import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm  {
    public final static String ALERT_NUMBER =
        ".dashContent.criticalalerts.numberOfAlerts";
    public final static String PAST = ".dashContent.criticalalerts.past";
    public final static String PRIORITY =
        ".dashContent.criticalalerts.priority";
    public final static String SELECTED_OR_ALL =
        ".dashContent.criticalalerts.selectedOrAll";
    public final static String TITLE =
        ".dashContent.criticalAlerts.title";

    private Integer _numberOfAlerts;
    private String _priority;
    private long _past;
    private String _selectedOrAll;
    private String _key;
    private String[] _ids;
    private String _title;
    
    public PropertiesForm() {
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
