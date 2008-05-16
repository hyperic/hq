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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

/**
 * Abstract class for appdef resource events.
 */
public abstract class ResourceZevent extends Zevent {

    public ResourceZevent(AuthzSubjectValue subject,
                          AppdefEntityID id) {
        super(new ResourceZeventSource(id),
              new ResourceZeventPayload(subject, id));
    }


    public ResourceZevent(ResourceZeventSource source,
                          ResourceZeventPayload payload) {
        super(source, payload);
    }

    public AppdefEntityID getAppdefEntityID() {
        return ((ResourceZeventPayload)getPayload()).
            getAppdefEntityID();
    }

    public AuthzSubjectValue getAuthzSubjectValue() {
        return ((ResourceZeventPayload)getPayload()).
            getAuthzSubjectValue();
    }

    protected static class ResourceZeventSource
        implements ZeventSourceId
    {
        private AppdefEntityID _id;

        public ResourceZeventSource(AppdefEntityID id) {
            _id = id;
        }

        public int hashCode() {
            return _id.hashCode();
        }

        public boolean equals(Object other) {
            return _id.equals(other);
        }
    }

    protected static class ResourceZeventPayload
        implements ZeventPayload
    {
        private AppdefEntityID _id;
        private AuthzSubjectValue _subject;

        public ResourceZeventPayload(AuthzSubjectValue subject,
                                     AppdefEntityID id) {
            _subject = subject;
            _id = id;
        }

        public AuthzSubjectValue getAuthzSubjectValue() {
            return _subject;
        }

        public AppdefEntityID getAppdefEntityID() {
            return _id;
        }
    }
}
