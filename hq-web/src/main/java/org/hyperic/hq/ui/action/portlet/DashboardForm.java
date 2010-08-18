/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.action.portlet;

import java.util.List;

import org.hyperic.hq.ui.action.BaseValidatorForm;

public class DashboardForm
    extends BaseValidatorForm {

    private static final long serialVersionUID = 2079527624648446846L;

    private List _dashboards;
    private String _selectedDashboardId;
    private String _defaultDashboard;
    private boolean _popDialog;

    public boolean getPopDialog() {
        return _popDialog;
    }

    public void setPopDialog(boolean dialog) {
        _popDialog = dialog;
    }

    public boolean isDashboardSelectable() {
        return _dashboards.size() > 1;
    }

    public List getDashboards() {
        return _dashboards;
    }

    public void setDashboards(List dashboards) {
        _dashboards = dashboards;
    }

    public String getSelectedDashboardId() {
        return _selectedDashboardId;
    }

    public void setSelectedDashboardId(String selectedDashboardId) {
        _selectedDashboardId = selectedDashboardId;
    }

    public String getDefaultDashboard() {
        return _defaultDashboard;
    }

    public void setDefaultDashboard(String dashboard) {
        _defaultDashboard = dashboard;
    }
}
