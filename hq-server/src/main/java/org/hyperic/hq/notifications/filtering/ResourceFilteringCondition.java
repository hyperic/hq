package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;

public class ResourceFilteringCondition<E extends Resource> extends FilteringCondition<E> {
    protected String name;
    public ResourceFilteringCondition(String name) {
        super();
        this.name = name;
    }
    @Override
    public boolean check(E rsc) {
        return rsc.getName().equals(this.name);
    }

}
