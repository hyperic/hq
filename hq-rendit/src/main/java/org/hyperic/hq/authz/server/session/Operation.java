package org.hyperic.hq.authz.server.session;

public class Operation extends AuthzNamedBean {
    private ResourceType _resourceType;

    protected Operation() {
    }

    Operation(ResourceType type, String name) {
        super(name);
        _resourceType = type;
    }

    public ResourceType getResourceType() {
        return _resourceType;
    }

    protected void setResourceType(ResourceType resourceType) {
        _resourceType = resourceType;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj instanceof Operation == false)
            return false;
        
        Operation o = (Operation) obj;
        return getName().equals(o.getName());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getName().hashCode();
        return result;
    }
}

