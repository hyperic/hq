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

import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;

/**
 * A subclass of <code>ResourceForm</code> representing the <em>RemoveAlert</em>
 * form.
 */
public class RemoveFormNG
    extends ResourceFormNG {

    /** Holds value of alerts. */
    private Integer[] _alerts;
    private String[] _ealerts;
    private Integer _ad;

    private String _buttonAction;
    private String _output;
    private String _ackNote;
    private String _fixedNote;
    private boolean _fixAll;
    private long _pauseTime;

    public RemoveFormNG() {
    }

    public String toString() {
        if (_alerts == null)
            return "empty";
        else
            return _alerts.toString();
    }

    /**
     * Getter for alerts
     * @return alerts in an array
     * 
     */
    public Integer[] getAlerts() {
        return _alerts;
    }

    /**
     * Setter for alerts
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

    public String getOutput() {
        return _output;
    }

    public void setOutput(String output) {
        _output = output;
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

    public boolean isFixAll() {
        return _fixAll;
    }

    public void setFixAll(boolean fixAll) {
        _fixAll = fixAll;
    }

    public long getPauseTime() {
        return _pauseTime;
    }

    public void setPauseTime(long pauseTime) {
        _pauseTime = pauseTime;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _alerts = null;
        _ealerts = null;
        _ad = null;
        _buttonAction = null;
        _output = null;
        _ackNote = null;
        _fixedNote = null;
        _fixAll = false;
        _pauseTime = 0;
    }
}
