/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * AlertMetricsControlForm.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import org.hyperic.hq.ui.util.MonitorUtils;

/**
 * Represents the common set of controls on various pages that display alert
 * metrics.
 * 
 * 
 */
public class AlertMetricsControlForm
    extends MetricsControlForm {

    private Boolean isAlertDefaults = Boolean.FALSE;

    public AlertMetricsControlForm() {
        super();
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" isAlertDefaults=").append(this.getAlertDefaults());

        return s.toString();
    }

    protected void setDefaults() {
        setAlertDefaults(Boolean.FALSE);
    }

    public Boolean getAlertDefaults() {
        return this.isAlertDefaults;
    }

    public void setAlertDefaults(Boolean isAlertDefaults) {
        super.setDefaults();
        this.isAlertDefaults = isAlertDefaults;

        if (isAlertDefaults.booleanValue()) {
            setA(ACTION_LASTN);
            setRn(MonitorUtils.DEFAULT_VALUE_RANGE_LASTN);
            setRu(MonitorUtils.DEFAULT_VALUE_RANGE_UNIT);
        }
    }
}