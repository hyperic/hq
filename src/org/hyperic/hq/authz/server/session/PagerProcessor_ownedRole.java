package org.hyperic.hq.authz.server.session;

import org.hyperic.util.pager.PagerProcessor;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.dao.DAOFactory;

public class PagerProcessor_ownedRole implements PagerProcessor {
 
    public PagerProcessor_ownedRole () {}

    public Object processElement(Object o) {
        if (o == null) return null;
        try {
            if ( o instanceof Role) {
                Role role = (Role)o;

                int numSubjects = DAOFactory.getDAOFactory().getRoleDAO()
                    .size(role.getSubjects());

                OwnedRoleValue value =
                    new OwnedRoleValue(role.getRoleValue(),
                                       role.getResource().getOwner()
                                           .getAuthzSubjectValue());
                value.setMemberCount(numSubjects);

                return value;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to OwnedRoleValue: "
                + e);
        }
        return o;
    }
}
