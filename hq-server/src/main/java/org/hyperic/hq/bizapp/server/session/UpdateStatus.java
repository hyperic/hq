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

package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hibernate.PersistedObject;

public class UpdateStatus 
    extends PersistedObject
{
    private String  _report;
    private int     _updateModeEnum;
    private boolean _ignored;

    protected UpdateStatus() {
    }
    
    UpdateStatus(String report, UpdateStatusMode mode) {
        _report         = report;
        _updateModeEnum = mode.getCode();
        _ignored        = false;
    }
    
    public String getReport() {
        return _report;
    }
    
    protected void setReport(String report) {
        _report = report;
    }
    
    protected int getUpdateModeEnum() {
        return _updateModeEnum;
    }
    
    protected void setUpdateModeEnum(int mode) {
        _updateModeEnum = mode;
    }
    
    public UpdateStatusMode getMode() {
        return UpdateStatusMode.findByCode(_updateModeEnum);
    }
    
    void setMode(UpdateStatusMode mode) {
        _updateModeEnum = mode.getCode();
    }
    
    public boolean isIgnored() {
        return _ignored;
    }
    
    protected void setIgnored(boolean ignored) {
        _ignored = ignored;
    }
}
