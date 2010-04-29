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
