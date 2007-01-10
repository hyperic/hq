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

    public AppdefEntityID getAppdefEntityID() {
        return ((ResourceZeventPayload)getPayload()).
            getAppdefEntityID();
    }

    public AuthzSubjectValue getAuthzSubjectValue() {
        return ((ResourceZeventPayload)getPayload()).
            getAuthzSubjectValue();
    }

    public static class ResourceZeventSource
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

    public static class ResourceZeventPayload
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
