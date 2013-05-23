package org.hyperic.hq.notifications.filtering;

import java.util.Set;

public class ResourceFilteringCondition extends FilteringCondition<Integer> {

    private Set<Integer> resourceIds;

    public ResourceFilteringCondition(Set<Integer> resourceIds) {
        super();
        this.resourceIds = resourceIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceFilteringCondition that = (ResourceFilteringCondition) o;

        if (resourceIds != null ? !resourceIds.equals(that.resourceIds) : that.resourceIds != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return resourceIds != null ? resourceIds.hashCode() : 0;
    }

    @Override
    public boolean check(Integer resourceId) {
        return resourceIds == null || resourceIds.isEmpty() || resourceIds.contains(resourceId);
    }

    @Override
    public String toString() {
        return "resourceIds= " + resourceIds;
    }
}
