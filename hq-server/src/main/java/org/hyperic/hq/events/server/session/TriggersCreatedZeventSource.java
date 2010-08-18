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

package org.hyperic.hq.events.server.session;

import org.hyperic.hq.zevents.ZeventSourceId;
/**
 * Source ID object for a TriggersCreatedZevent. This represents the
 * Alert Definition whose triggers were created.
 * @author jhickey
 *
 */
public class TriggersCreatedZeventSource implements ZeventSourceId {

    private static final long serialVersionUID = 3480600667010718596L;

    private final Integer id;

    /**
     *
     * @param id The id of the AlertDefinition that the event is for.
     */
    public TriggersCreatedZeventSource(Integer id)
    {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof TriggersCreatedZeventSource)) {
            return false;
        }

        return ((TriggersCreatedZeventSource) o).getId().equals(getId());
    }

    /**
     * @return The id of the AlertDefinition that the event is for.
     */
    public Integer getId() {
        return id;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.intValue();
        return result;
    }

    public String toString() {
        return "Triggers Created for Alert Def ID[" + id + "]";
    }
}
