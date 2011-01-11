package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ResourceType extends AuthzNamedBean {
    
    private Resource   _resource;
    private boolean    _system = false;
    private Collection _operations = new ArrayList();
    private int appdefType;

    public ResourceType() {
        super();
    }

    ResourceType(String name, Resource resource, boolean fsystem) {
        super(name);
        _resource = resource;
        _system   = fsystem;
    }

    public Resource getResource() {
        return _resource;
    }

    protected void setResource(Resource val) {
        _resource = val;
    }

    public boolean isSystem() {
        return _system;
    }

    protected void setSystem(boolean val) {
        _system = val;
    }

    protected Collection getOperationsBag() {
        return _operations;
    }

    protected void setOperationsBag(Collection val) {
        _operations = val;
    }

    Operation createOperation(String name) {
        return new Operation(this, name);
    }

    public Collection<Operation> getOperations() {
        return Collections.unmodifiableCollection(_operations);
    }

    /**
     * Convert an authz ResourceType to appdef type.  Currently only platform
     * server and service types are supported.
     * @return One of AppdefEntityConstants.APPDEF_TYPE*
     */
    public int getAppdefType() {
      return this.appdefType;
    }
    
    public void setAppdefType(int appdefType) {
        this.appdefType = appdefType;
    }

    public String getLocalizedName() {
        //TODO this had some resource bundle stuff before
        return getName();
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + ((_resource != null && _resource.getId() != null)
                                ? _resource.getId().intValue() : 0);
        return result;
    }

    public boolean equals(Object obj) {
        ResourceType o;

        if (obj == this)
            return true;

        if (obj == null || obj instanceof ResourceType == false)
            return false;

        o = (ResourceType)obj;
        return o.isSystem() == isSystem() &&
               (o.getResource() == getResource() ||
                o.getResource() != null && getResource() != null &&
                o.getResource().getId().equals(getResource().getId()));
    }

}
