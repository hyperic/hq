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

package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for
 * dealing with image-based form buttons.
 */
public class DashboardBaseForm
    extends BaseValidatorForm {

    /** Holds value of property portletName. */
    private String _portletName;
    private String _token;

    // -------------------------------------constructors

    public DashboardBaseForm() {
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

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _portletName = null;
        _token = null;
    }

}
