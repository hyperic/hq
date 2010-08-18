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

package org.hyperic.hq.control.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
/**
 * DTO to hold the frequency of invocations of a specific control action
 * @author jhickey
 *
 */
public class ControlFrequency {
    private AppdefEntityID id;
    private String action;
    private long count;

    public ControlFrequency(AppdefEntityID id, String action, long count) {
        this.id = id;
        this.action = action;
        this.count = count;
    }

    /**
     * 
     * @return The entity against which the control action was executed
     */
    public AppdefEntityID getId() {
        return id;
    }

    /**
     * 
     * @return The name of the action
     */
    public String getAction() {
        return action;
    }

    /**
     * 
     * @return The number of times the action was invoked
     */
    public long getCount() {
        return count;
    }

}
