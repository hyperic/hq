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

import org.hyperic.hq.zevents.Zevent;

/**
 * Represents that triggers have been created in the database (due to create or
 * update of an alert definition) and signals to listeners that the in-memory
 * representations of these triggers can be created. Note this event does not
 * have a payload.
 * @author jhickey
 *
 */
public class TriggersCreatedZevent
    extends Zevent
{

    public TriggersCreatedZevent(Integer alertDefId) {
        super(new TriggersCreatedZeventSource(alertDefId), null);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof TriggersCreatedZevent)) {
            return false;
        }
        TriggersCreatedZevent other = (TriggersCreatedZevent) obj;
        return getSourceId().equals(other.getSourceId());
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getSourceId().hashCode();
        return result;
    }

}
