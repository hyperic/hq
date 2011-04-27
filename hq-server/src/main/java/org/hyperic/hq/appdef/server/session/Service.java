package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServiceValue;

public class Service
    extends AppdefResource {

    private AppdefResource parent;

    private ServiceType serviceType;

    private String autoinventoryIdentifier;


    public String getAutoinventoryIdentifier() {
        return autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier) {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public AppdefResource getParent() {
        return parent;
    }

    public void setParent(AppdefResource parent) {
        this.parent = parent;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public AppdefEntityID getEntityId() { // TODO remove this method
        return AppdefEntityID.newServiceID(getId());
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
        return serviceType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getServiceValue();
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) Service object instead
     */
    public ServiceValue getServiceValue() {
        ServiceValue _serviceValue = new ServiceValue();
        _serviceValue.setSortName(getSortName());
        _serviceValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        _serviceValue.setModifiedBy(getModifiedBy());
        _serviceValue.setOwner(getOwnerName());
        _serviceValue.setLocation(getLocation());
        _serviceValue.setName(getName());
        _serviceValue.setDescription(getDescription());
        _serviceValue.setId(getId());
        _serviceValue.setResourceId(getResource() != null ? getResource().getId() : null);
        _serviceValue.setMTime(getMTime());
        _serviceValue.setCTime(getCTime());
        if (getParent() != null) {
            _serviceValue.setParent(getParent().getAppdefResourceValue());
        } else
            _serviceValue.setParent(null);
        if (getServiceType() != null) {
            _serviceValue.setServiceType(getServiceType().getServiceTypeValue());
        } else
            _serviceValue.setServiceType(null);
        return _serviceValue;
    }

    /**
     * legacy DTO pattern. Compare this entity bean to a value object
     * @deprecated should use (this) Service object and hibernate dirty() check
     * @return true if the service value matches this entity
     */
    public boolean matchesValueObject(ServiceValue obj) {
        boolean matches;
        matches = super.matchesValueObject(obj) &&
                  (getName() != null ? getName().equals(obj.getName()) : (obj.getName() == null)) &&
                  (getDescription() != null ? getDescription().equals(obj.getDescription()) : (obj
                      .getDescription() == null)) &&
                  (getLocation() != null ? getLocation().equals(obj.getLocation()) : (obj
                      .getLocation() == null)) &&
                  (getAutoinventoryIdentifier() != null ? getAutoinventoryIdentifier().equals(
                                                           obj.getAutoinventoryIdentifier())
                                                       : (obj.getAutoinventoryIdentifier() == null));
        return matches;
    }

}
