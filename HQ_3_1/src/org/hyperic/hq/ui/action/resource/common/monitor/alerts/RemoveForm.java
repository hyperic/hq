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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceForm;

/**
 * A subclass of <code>ResourceForm</code> representing the
 * <em>RemoveAlert</em> form.
 */
public class RemoveForm extends ResourceForm  {

    /** Holds value of  alerts. */
    private Integer[] _alerts;
    private String[] _ealerts;
    private Integer _ad;
    
    private String _buttonAction;
    private String _ackNote;
    private String _fixedNote;

    public RemoveForm() {
    }

    public String toString() {
        if (_alerts == null)
            return "empty";
        else
            return _alerts.toString();    
    }
    
    /** Getter for alerts
     * @return alerts in an array 
     *
     */
    public Integer[] getAlerts() {
        return _alerts;
    }
    
    /** Setter for alerts
     * @param alerts As an Integer array  
     *
     */
    public void setAlerts(Integer[] alerts) {
        _alerts = alerts;
    }

    public String[] getEalerts() {
        return _ealerts;
    }

    public void setEalerts(String[] ealerts) {
        this._ealerts = ealerts;
    }

    public Integer getAd() {
        return _ad;
    }
    
    public void setAd(Integer ad) {
        _ad = ad;
    }

    public String getButtonAction() {
        return _buttonAction;
    }

    public void setButtonAction(String action) {
        _buttonAction = action;
    }
    
    public String getAckNote() {
        return _ackNote;
    }

    public void setAckNote(String ackNote) {
        this._ackNote = ackNote;
    }

    public String getFixedNote() {
        return _fixedNote;
    }

    public void setFixedNote(String fixedNote) {
        this._fixedNote = fixedNote;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _alerts = null;
        _ealerts = null;
        _ad = null;
        _buttonAction = null;
        _ackNote = null;
        _fixedNote = null;
    }
}
