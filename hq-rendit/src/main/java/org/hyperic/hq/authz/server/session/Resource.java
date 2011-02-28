package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;


/**
 * This was once a Hibernate-backed POJO in the webapp, it's now just a DTO for
 * plugin API compatibility
 * @author jhickey
 * 
 */
public class Resource extends AuthzNamedBean implements Comparable {
    private ResourceType _resourceType;
    private Resource _prototype;
    private Integer _instanceId;
    private AuthzSubject _owner;
    private long _mtime = System.currentTimeMillis();
    private boolean _system = false;
    private Collection _virtuals = new ArrayList();
    private Collection _fromEdges = new ArrayList();
    private Collection _toEdges = new ArrayList();
    private Collection _groupBag = new ArrayList();
   
   
    public Resource() {
        
    }
    
    public Resource(ResourceType type, Resource prototype, String name, AuthzSubject owner,
                    Integer instanceId, boolean system) {
        super(name);
        _resourceType = type;
        _prototype = prototype;
        _instanceId = instanceId;
        _owner = owner;
        _system = system;
    }

    protected Collection getGroupBag() {
        return _groupBag;
    }

    protected void setGroupBag(Collection b) {
        _groupBag = b;
    }

    public boolean isInAsyncDeleteState() {
        return _resourceType == null;
    }

    public ResourceType getResourceType() {
        return _resourceType;
    }

    protected void setResourceType(ResourceType resourceTypeId) {
        _resourceType = resourceTypeId;
    }

    public Resource getPrototype() {
        return _prototype;
    }

    protected void setPrototype(Resource p) {
        _prototype = p;
    }

    public Integer getInstanceId() {
        return _instanceId;
    }

    protected void setInstanceId(Integer val) {
        _instanceId = val;
    }

    public AuthzSubject getOwner() {
        return _owner;
    }

    protected void setOwner(AuthzSubject val) {
        _owner = val;
    }

    public boolean isSystem() {
        return _system;
    }

    /**
     * Returns true of this is the root resource
     */
    public boolean isRoot() {
        return false;
    }

    public void markDirty() {
        _mtime = System.currentTimeMillis();
    }

    public void setMtime(long mtime) {
        _mtime = mtime;
    }

    public long getMtime() {
        return _mtime;
    }

    protected void setSystem(boolean fsystem) {
        _system = fsystem;
    }

    public Collection getVirtuals() {
        return _virtuals;
    }

    protected void setVirtuals(Collection virtuals) {
        _virtuals = virtuals;
    }

    protected void setFromEdges(Collection e) {
        _fromEdges = e;
    }

    protected Collection getFromEdges() {
        return _fromEdges;
    }

    protected void setToEdges(Collection e) {
        _toEdges = e;
    }

    protected Collection getToEdges() {
        return _toEdges;
    }

    public boolean isOwner(Integer possibleOwner) {
        boolean is = false;
        // Overlord owns everything.
        if (is = possibleOwner.equals(AuthzConstants.overlordId) == false) {
            is = (possibleOwner.equals(getOwner().getId()));
        }
        return is;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Resource) || !super.equals(obj)) {
            return false;
        }
        Resource o = (Resource) obj;
        return ((_resourceType == o.getResourceType()) || (_resourceType != null &&
                                                           o.getResourceType() != null && _resourceType
            .equals(o.getResourceType()))) &&
               ((_instanceId == o.getInstanceId()) || (_instanceId != null &&
                                                       o.getInstanceId() != null && _instanceId
                   .equals(o.getInstanceId())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (_resourceType != null ? _resourceType.hashCode() : 0);
        result = 37 * result + (_instanceId != null ? _instanceId.hashCode() : 0);

        return result;
    }

    public int compareTo(Object arg0) {
        if (!(arg0 instanceof Resource) || getSortName() == null ||
            ((Resource) arg0).getSortName() == null)
            return -1;

        return getSortName().compareTo(((Resource) arg0).getSortName());
    }

    public String toString() {
        return new StringBuilder().append(getResourceType().getId()).append(":").append(getId())
            .toString();
    }

}
