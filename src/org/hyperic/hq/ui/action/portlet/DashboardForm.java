package org.hyperic.hq.ui.action.portlet;

import java.util.List;

import org.hyperic.hq.ui.action.BaseValidatorForm;

public class DashboardForm extends BaseValidatorForm {

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

	public List getDashboards() {
		return _dashboards;
	}

	public void setDashboards(List dashboards) {
		this._dashboards = dashboards;
	}

	public String getSelectedDashboardId() {
		return _selectedDashboardId;
	}

	public void setSelectedDashboardId(String selectedDashboardId) {
		this._selectedDashboardId = selectedDashboardId;
	}

	public String getDefaultDashboard() {
		return _defaultDashboard;
	}

	public void setDefaultDashboard(String dashboard) {
		_defaultDashboard = dashboard;
	}
}
