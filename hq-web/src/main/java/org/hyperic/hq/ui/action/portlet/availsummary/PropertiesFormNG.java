/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.availsummary;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.portlet.DashboardBaseFormNG;
import org.hyperic.hq.ui.action.portlet.DashboardFormNG;

public class PropertiesFormNG extends DashboardBaseFormNG {

    public final static String RESOURCES = Constants.USERPREF_KEY_AVAILABITY_RESOURCES_NG;
    public final static String NUM_TO_SHOW = ".ng.dashContent.availSummary.numToShow";
    public final static String TITLE = ".ng.dashContent.availSummary.title";

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
