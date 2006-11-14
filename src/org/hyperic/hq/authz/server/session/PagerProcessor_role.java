package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.util.pager.PagerProcessor;

public class PagerProcessor_role implements PagerProcessor {
 
    public PagerProcessor_role () {}

    public Object processElement(Object o) {
        if (o == null) return null;
        try {
            if ( o instanceof Role ) {
                return ((Role) o).getRoleValue();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to RoleValue: "
                + e);
        }
        return o;
    }
}
