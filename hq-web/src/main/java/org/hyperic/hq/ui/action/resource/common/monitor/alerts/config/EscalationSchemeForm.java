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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.beans.EscalationActionBean;

public class EscalationSchemeForm extends ResourceForm {
    private Integer _escId;
    private String  _escName;
    private List    _actions;
    private boolean _allowPause;
    private long    _pauseRange;
    private int     _notification;
    private int     _ad;
    private int     _gad;

    public Integer getEscId() {
        return _escId;
    }

    public void setEscId(Integer escId) {
        _escId = escId;
    }

    public String getMode() {
        return "viewEscalation";
    }

    public void setMode(String mode) {
    }

    public String getEscName() {
        return _escName;
    }

    public void setEscName(String escName) {
        _escName = escName;
    }

    public boolean isAllowPause() {
        return _allowPause;
    }

    public void setAllowPause(boolean allowPause) {
        _allowPause = allowPause;
    }

    public int getNotification() {
        return _notification;
    }

    public void setNotification(int notification) {
        _notification = notification;
    }

    public long getPauseRange() {
        return _pauseRange;
    }

    public void setPauseRange(long pauseRange) {
        _pauseRange = pauseRange;
    }

    public int getAd() {
        return _ad;
    }

    public void setAd(int ad) {
        _ad = ad;
    }

    public int getGad() {
        return _gad;
    }

    public void setGad(int gad) {
        _gad = gad;
    }

    public EscalationActionBean[] getActions() {
        EscalationActionBean[] actions =
            new EscalationActionBean[_actions.size()];
        return (EscalationActionBean[]) _actions.toArray(actions);
    }

    public EscalationActionBean getAction(int index) {
        if (index >= _actions.size()) {
            setNumActions(index + 1);
        }
        return (EscalationActionBean) _actions.get(index);
    }

    public void setNumActions(int numActions) {
        while (_actions.size() < numActions) {
            _actions.add(new EscalationActionBean());
        }

        // Remove extra conditions if necessary
        if (_actions.size() > numActions) {
            for (int i = _actions.size(); i > numActions; i--)
                _actions.remove(i - 1);
        }
    }

    public void resetActions() {
        _actions = new ArrayList();
        setNumActions(1);
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _escId = null;
        _escName = null;
        _ad = 0;
        _gad = 0;
        resetActions();
    }

    public String toString() {
        StringBuffer strOut = new StringBuffer(super.toString());
        strOut.append("\nescId=" + _escId)
              .append("\nescName=" + _escName);
        return strOut.toString();
    }
}
