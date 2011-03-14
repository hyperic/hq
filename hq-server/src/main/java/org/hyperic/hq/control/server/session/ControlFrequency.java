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

/**
 * DTO to hold the frequency of invocations of a specific control action
 * @author jhickey
 * 
 */
public class ControlFrequency {
    private int id;
    private String action;
    private long count;

    public ControlFrequency(int id, String action, long count) {
        this.id = id;
        this.action = action;
        this.count = count;
    }

    /**
     * 
     * @return The entity against which the control action was executed
     */
    public int getId() {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + (int) (count ^ (count >>> 32));
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ControlFrequency other = (ControlFrequency) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        if (count != other.count)
            return false;
        if (id != other.id)
            return false;
        return true;
    }

}
