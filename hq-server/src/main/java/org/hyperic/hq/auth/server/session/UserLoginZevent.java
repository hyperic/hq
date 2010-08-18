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

package org.hyperic.hq.auth.server.session;

import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;

public class UserLoginZevent extends Zevent {

    static {
        ZeventManager.getInstance().registerEventClass(UserLoginZevent.class);
    }

    public static class UserLoginZeventSource implements ZeventSourceId {

        private int _id;

        public UserLoginZeventSource(int id) {
            _id = id;
        }

        public int getId() {
            return _id;
        }

        public int hashCode() {
            int result = 17;
            result = 37*result+_id;
            result = 37*result+this.getClass().toString().hashCode();
            return result;
        }

        public boolean equals(Object o) {
            if (o== this)
                return true;

            if (o == null || !(o instanceof UserLoginZeventSource))
                return false;

            return ((UserLoginZeventSource)o).getId() == getId();
        }
    }

    public static class UserLoginZeventPayload implements ZeventPayload {

        Integer _subjectId;

        public UserLoginZeventPayload(Integer id) {
            _subjectId = id;
        }

        public Integer getSubjectId() {
            return _subjectId;
        }
    }
    
    public UserLoginZevent(Integer id) {
        super(new UserLoginZeventSource(id.intValue()),
              new UserLoginZeventPayload(id));
    }
}
