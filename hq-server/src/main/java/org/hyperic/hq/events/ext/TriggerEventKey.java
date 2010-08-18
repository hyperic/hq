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

package org.hyperic.hq.events.ext;

/**
 * Key object used in {@link RegisteredTrigger}'s internal Map. Indicates the
 * combination of event type and instance a trigger is listening for
 * @author jhickey
 *
 */
public class TriggerEventKey {

    private final Class<?> eventClass;

    private final int instanceId;

    /**
     *
     * @param eventClass The event type
     * @param instanceId The instance the event occurred against, or RegisteredTriggers.KEY_ALL if interested in all instances
     */
    public TriggerEventKey(Class<?> eventClass, int instanceId) {
        this.eventClass = eventClass;
        this.instanceId = instanceId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof TriggerEventKey)) {
            return false;
        }
        TriggerEventKey other = (TriggerEventKey) obj;
        if (eventClass == null) {
            if (other.eventClass != null) {
                return false;
            }
        } else if (!eventClass.getName().equals(other.eventClass.getName())) {
            return false;
        }
        if (instanceId != other.instanceId) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventClass == null) ? 0 : eventClass.getName().hashCode());
        result = prime * result + instanceId;
        return result;
    }

}
