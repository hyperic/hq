package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;

public class ResourceFilteringCondition extends FilteringCondition<Resource> {

    @Override
    public boolean check(Resource entity) {
        return true;
    }

}
