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

package org.hyperic.hq.ui.action.portlet.autoDisc;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.ScanMethodState;

public class AIPlatformWithStatus
    extends AIPlatformValue {

    private ScanStateCore state = null;

    public AIPlatformWithStatus(AIPlatformValue aip, ScanStateCore state) {
        super(aip);
        this.state = state;
    }

    public boolean getIsAgentReachable() {
        return state != null;
    }

    public boolean getIsScanning() {
        return state != null && !state.getIsDone();
    }

    public String getStatus() {
        if (state == null)
            return "Agent is not responding";
        if (state.getGlobalException() != null) {
            return state.getGlobalException().getMessage();
        }
        ScanMethodState[] methstates = state.getScanMethodStates();
        String rval = "";
        String status;
        for (int i = 0; i < methstates.length; i++) {
            status = methstates[i].getStatus();
            if (status != null)
                rval += status.trim();
        }
        rval = rval.trim();
        if (rval.length() == 0) {
            rval = "Scan starting...";
        }
        return rval;
    }
}
