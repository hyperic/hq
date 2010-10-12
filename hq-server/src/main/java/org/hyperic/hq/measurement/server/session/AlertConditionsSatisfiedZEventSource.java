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

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.zevents.ZeventSourceId;

/**
 * Source ID object for an AlertConditionsSatisifiedZEvent. This represents the
 * Alert Definition whose conditions were satisfied.
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedZEventSource implements ZeventSourceId {

    private static final long serialVersionUID = 8416378224728973400L;

    private final int id;

    /**
     *
     * @param id The id of the AlertDefinition that the event is for.
     */
    public AlertConditionsSatisfiedZEventSource(int id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof AlertConditionsSatisfiedZEventSource)) {
            return false;
        }

        return ((AlertConditionsSatisfiedZEventSource) o).getId() == getId();
    }

    /**
     * @return The id of the AlertDefinition that the event is for.
     */
    public int getId() {
        return id;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    public String toString() {
        return "Alert Def ID[" + id + "]";
    }

}
