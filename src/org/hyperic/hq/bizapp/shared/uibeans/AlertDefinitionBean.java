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

package org.hyperic.hq.bizapp.shared.uibeans;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/*******************************************************************************
 * Bean to hold alert values from AlertValue and AlertDefinitionValue
 ******************************************************************************/

public final class AlertDefinitionBean {
    private long _ctime;
    private Integer _alertDefId;
    private String _name;
    private String _description;
    private boolean _enabled;
    private Integer _type;
    private Integer _parentId;

    public AlertDefinitionBean(Integer alertDefId, long ctime, String name,
                               String description, boolean enabled,
                               Integer parentId) {
        _alertDefId = alertDefId;
        _ctime = ctime;
        _name = name;
        _description = description;
        _enabled = enabled;
        _parentId = parentId;
    }

    public Integer getAlertDefId() {
        return _alertDefId;
    }

    public String getName() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }

    public boolean getEnabled() {
        return _enabled;
    }

    public long getCtime() {
        return _ctime;
    }

    public Integer getType() {
        return _type;
    }

    public Integer getParentId() {
        return _parentId;
    }

    public void setAppdefEntityID(AppdefEntityID aeid) {
        _type = new Integer(aeid.getType());
    }
}
