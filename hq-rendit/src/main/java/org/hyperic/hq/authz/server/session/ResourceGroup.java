package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.authz.shared.ResourceGroupValue;

public class ResourceGroup {
    private String _description;
    private String _location;
    private boolean _system = false;
    private boolean _orCriteria = true;
    private Integer _groupType;
    private Integer _clusterId;
    private long _ctime;
    private long _mtime;
    private String _modifiedBy;
    private Resource _resource;
    private Resource _resourcePrototype;
    private Collection _roles = new HashSet();
    private List _criteria = new ArrayList();
    private Integer id;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public ResourceGroup() {
        super();
    }

    void markDirty() {
        _mtime = System.currentTimeMillis();
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#getName()
     */
    public String getName() {
        if (_resource != null)
            return _resource.getName();
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.hyperic.hq.authz.server.session.AuthzNamedBean#setName(java.lang.
     * String)
     */
    void setName(String name) {
        if (_resource != null)
            _resource.setName(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#getSortName()
     */
    public String getSortName() {
        if (_resource != null)
            return _resource.getSortName();
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.hyperic.hq.authz.server.session.AuthzNamedBean#setSortName(java.lang
     * .String)
     */
    void setSortName(String sortName) {
        if (_resource != null)
            _resource.setSortName(sortName);
    }

    public String getDescription() {
        return _description;
    }

    void setDescription(String val) {
        _description = val;
    }

    public String getLocation() {
        return _location;
    }

    void setLocation(String val) {
        _location = val;
    }

    public boolean isSystem() {
        return _system;
    }

    void setSystem(boolean val) {
        _system = val;
    }

    public Integer getGroupType() {
        return _groupType;
    }

    void setGroupType(Integer val) {
        _groupType = val;
    }

    public boolean isMixed() {
        return getResourcePrototype() == null;
    }

    /**
     * @deprecated Use getResourcePrototype() instead.
     */
    public Integer getGroupEntResType() {
        if (_resourcePrototype == null) {
            return new Integer(-1);
        }
        return _resourcePrototype.getInstanceId();
    }

    public Integer getClusterId() {
        return _clusterId;
    }

    void setClusterId(Integer val) {
        _clusterId = val;
    }

    public boolean isOrCriteria() {
        return _orCriteria;
    }

    void setOrCriteria(boolean val) {
        _orCriteria = val;
    }

    public long getCtime() {
        return _ctime;
    }

    void setCtime(Long val) {
        _ctime = val != null ? val.longValue() : 0;
    }

    public long getMtime() {
        return _mtime;
    }

    void setMtime(Long val) {
        _mtime = val != null ? val.longValue() : 0;
    }

    public String getModifiedBy() {
        return _modifiedBy;
    }

    void setModifiedBy(String val) {
        _modifiedBy = val;
    }

    void setResourcePrototype(Resource r) {
        _resourcePrototype = r;
    }

    /**
     * Checks if this group is compatable with the passed resource.
     * 
     * @param resource A resource prototype. Note that this is NOT an instance
     *        of the prototype.
     * 
     * @return false if this is not a compatable group, or if the passed
     *         resource is not an instace of this.getResourcePrototype()
     */
    public boolean isCompatableWith(Resource resource) {
        return _resourcePrototype != null && _resourcePrototype.equals(resource);
    }

    /**
     * If the group is compatable, this method returns the prototype for all
     * resources contained within.
     */
    public Resource getResourcePrototype() {
        return _resourcePrototype;
    }

    void setResource(Resource r) {
        _resource = r;
    }

    public Resource getResource() {
        return _resource;
    }

    public Collection<Role> getRoles() {
        return _roles;
    }

    void setRoles(Collection val) {
        _roles = val;
    }

    // hibernate getter method
    // ResourceGroupManager should call getCritterList instead
    protected List getCriteriaList() {
        return _criteria;
    }

    // hibernate setter method
    // ResourceGroupManager should call setCritterList instead
    void setCriteriaList(List val) {
        _criteria = val;
    }

//    /**
//     * Getter method used to retrieve the criteria list for a ResourceGroup.
//     * @return CritterList The criteria list associated with this ResourceGroup
//     *         instance.
//     * @throws GroupException
//     */
//    public CritterList getCritterList() throws GroupException {
//        List critters = new ArrayList();
//        // iterate through all the persisted criteria, and convert to critters
//        // to put into a critter list
//        //TODO can't get rid of critters yet?
////        for (Iterator it = getCriteriaList().iterator(); it.hasNext();) {
////            PersistedCritter dump = (PersistedCritter) it.next();
////            CritterType type = CritterRegistry.getRegistry()
////                .getCritterTypeForClass(dump.getKlazz());
////            critters.add(type.compose(dump));
////        }
//        return new CritterList(critters, _orCriteria);
//    }
//
//    // used by the ResourceManager to set the criteria list for a resource group
//    // note that the ResourceManager should invoke this method rather than
//    // the setCriteriaList setter used by hibernate
//    void setCritterList(CritterList criteria) throws GroupException {
//        List dumps = new ArrayList();
//        // iterate through all the critters in the critter list
//        // and convert them to persisted critters
//        // finally update the resource group with the new set of critters
//        int index = 0;
//        //TODO can't get rid of critters  yet?
////        for (Iterator it = criteria.getCritters().iterator(); it.hasNext(); index++) {
////            Critter critter = (Critter) it.next();
////            CritterType critType = critter.getCritterType();
////            PersistedCritter dump = new PersistedCritter(this, critType, index);
////            critType.decompose(critter, dump);
////            dumps.add(dump);
////        }
//        this.setOrCriteria(criteria.isAny());
//        // overwrite the contents of the criteria persisted by hibernate
//        getCriteriaList().clear();
//        getCriteriaList().addAll(dumps);
//    }

    public void addRole(Role role) {
        //TODO support any persistent set through API?
        //role.getResourceGroups().add(this);
        _roles.add(role);
    }

    public void removeRole(Role role) {
        _roles.remove(role);
    }

    public void removeAllRoles() {
        _roles.clear();
    }

    /**
     * @TODO: This method needs to be removed in favor of more discrete
     *        operations. Not all the properties here can be changed once a
     *        group is created.
     */
    void setResourceGroupValue(ResourceGroupValue val) {
        setClusterId(new Integer(val.getClusterId()));
        setCtime(val.getCTime());
        setDescription(val.getDescription());
        setGroupType(new Integer(val.getGroupType()));
        setId(val.getId());
        setLocation(val.getLocation());
        setModifiedBy(val.getModifiedBy());
        setMtime(val.getMTime());
        setName(val.getName());
        setSystem(val.getSystem());
    }


    public boolean equals(Object obj) {
        return (obj instanceof ResourceGroup) && super.equals(obj);
    }
}
