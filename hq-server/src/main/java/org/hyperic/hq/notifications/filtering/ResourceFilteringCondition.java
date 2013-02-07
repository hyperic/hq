package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;

public class ResourceFilteringCondition<E extends Resource> extends FilteringCondition<E> {
    protected String name;
    public ResourceFilteringCondition(String name) {
        super();
        this.name = name;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof ResourceFilteringCondition) {return false;}
        ResourceFilteringCondition other = (ResourceFilteringCondition) obj;
        if(name == null) {
            if(other.name != null) return false;
        }else if(!name.equals(other.name)) return false;
        return true;
    }
    @Override
    public boolean check(E rsc) {
        String name = rsc.getName();
        return name.equals(this.name);
    }
}
