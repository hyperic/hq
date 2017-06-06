package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class DashboardBaseFormNG extends BaseValidatorFormNG {

    /** Holds value of property portletName. */
    protected String _portletName;
    protected String _token;

    // -------------------------------------constructors

    public DashboardBaseFormNG() {
        super();
    }

    // -------------------------------------public methods
    /**
     * Getter for property portletName.
     * @return Value of property displayOnDash.
     * 
     */
    public String getPortletName() {
        return _portletName;
    }

    /**
     * Setter for property displayOnDash.
     * @param removePortlet New value of property displayOnDash.
     * 
     */
    public void setPortletName(String portletName) {
        _portletName = portletName;
    }

    public void setToken(String token) {
        _token = token;
    }

    public String getToken() {
        return _token;
    }

    public void reset() {
        super.reset();
        _portletName = null;
        _token = null;
    }

}
