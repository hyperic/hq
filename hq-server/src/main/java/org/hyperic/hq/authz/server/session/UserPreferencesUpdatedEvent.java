/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.util.config.ConfigResponse;

/**
 * Indicates that user preferences have been added or updated
 * @author jhickey
 * 
 */
public class UserPreferencesUpdatedEvent
    extends Zevent {

    /**
     * 
     * @param operatorId The user performing the setPrefs operation
     * @param subjectId The user whose prefs are to be set
     * @param prefs The prefs
     */
    public UserPreferencesUpdatedEvent(Integer operatorId, Integer subjectId, ConfigResponse prefs) {
        super(new UserZeventSource(subjectId), new UserZeventPayload(operatorId, prefs));
    }

    protected static class UserZeventSource implements ZeventSourceId {
        private static final long serialVersionUID = 6174948452047802996L;
        
        private Integer subjectId;

        public UserZeventSource(Integer subjectId) {
            this.subjectId = subjectId;
        }

        public int hashCode() {
            return subjectId.hashCode();
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (other instanceof UserZeventSource) {
                UserZeventSource src = (UserZeventSource) other;
                return this.subjectId.equals(src.subjectId);
            }

            return false;
        }

        public Integer getSubjectId() {
            return subjectId;
        }
    }

    protected static class UserZeventPayload implements ZeventPayload {
        private Integer operatorId;
        private ConfigResponse prefs;

        public UserZeventPayload(Integer operatorId, ConfigResponse prefs) {
            this.operatorId = operatorId;
            this.prefs = prefs;
        }

        public Integer getOperatorId() {
            return operatorId;
        }

        public ConfigResponse getPrefs() {
            return prefs;
        }

    }
}
