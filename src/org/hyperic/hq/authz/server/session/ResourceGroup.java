/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.CritterRegistry;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;

public class ResourceGroup extends AuthzNamedBean
    implements ContainerManagedTimestampTrackable
{
    private String _description;
    private String _location;
    private boolean _system = false;
    private int  _orCriteriaFlag = 1;
    private Integer _groupType;
    private Integer _clusterId;
    private long _ctime;
    private long _mtime;
    private String _modifiedBy;
    private Resource _resource;
    private Resource _resourcePrototype;
    private Collection _resourceSet = new HashSet();
    private Collection _roles = new HashSet();
    private List _criteria = new ArrayList();

    private ResourceGroupValue _resourceGroupValue = new ResourceGroupValue();

    public static class ResourceGroupCreateInfo {
        private String   _name;
        private String   _description;
        private String   _location;
        private int      _groupType;
        private Resource _resourcePrototype;
        private int      _clusterId;
        private boolean  _system;
        
        public ResourceGroupCreateInfo(String name, String description,
                                       int groupType,
                                       Resource resourcePrototype,
                                       String location,
                                       int clusterId, boolean system)  
        {
            _name              = name;
            _description       = description;
            _resourcePrototype = resourcePrototype;
            _groupType         = groupType;
            _location          = location;
            _clusterId         = clusterId;
            _system            = system;
        }
        
        public String getName() { return _name; }
        public String getDescription() { return _description; }
        public String getLocation() { return _location; }
        public int getGroupType() { return _groupType; }
        public Resource getResourcePrototype() { return _resourcePrototype; }
        public int getClusterId() { return _clusterId; }
        public boolean getSystem() { return _system; }
    }
    
    public ResourceGroup() {
        super();
    }

    ResourceGroup(ResourceGroupCreateInfo cInfo, AuthzSubject creator) {
        setName(cInfo.getName());
        _clusterId         = new Integer(cInfo.getClusterId());
        _description       = cInfo.getDescription();
        _location          = cInfo.getLocation();
        _system            = cInfo.getSystem();
        _groupType         = new Integer(cInfo.getGroupType());
        _resourcePrototype = cInfo.getResourcePrototype();
        _ctime = _mtime    = System.currentTimeMillis();
        _modifiedBy        = creator.getName();
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

    public String getDescription() {
        return _description;
    }

    protected void setDescription(String val) {
        _description = val;
    }

    public String getLocation() {
        return _location;
    }

    protected void setLocation(String val) {
        _location = val;
    }

    public boolean isSystem() {
        return _system;
    }

    protected void setSystem(boolean val) {
        _system = val;
    }

    public Integer getGroupType() {
        return _groupType;
    }

    protected void setGroupType(Integer val) {
        _groupType = val;
    }

    public boolean isMixed() {
        return getResourcePrototype() == null;
    }
    
    /**
     * @deprecated Use getResourcePrototype() instead.
     * XXX: ADHOC groups lose the Group or Application types with the change
     * to use a Resource prototype for compatible groups.
     */
    public Integer getGroupEntType() {
        if (_resourcePrototype == null) {
            return new Integer(-1);
        }

        Integer type = _resourcePrototype.getResourceType().getId();
        if (type.equals(AuthzConstants.authzPlatformProto)) {
            return new Integer(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        } else if (type.equals(AuthzConstants.authzServerProto)) {
            return new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVER);
        } else if (type.equals(AuthzConstants.authzServiceProto)) {
            return new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        } else {
            return new Integer(-1); // Backwards compat.
        }
    }

    /**
     * @deprecated Use getResourcePrototype() instead.
     */
    public Integer getGroupEntResType() {
        if (_resourcePrototype  == null) {
            return new Integer(-1);
        }
        return _resourcePrototype.getInstanceId();
    }

    public Integer getClusterId() {
        return _clusterId;
    }

    protected void setClusterId(Integer val) {
        _clusterId = val;
    }
    
    public int getOrCriteria() {
        return _orCriteriaFlag;
    }

    protected void setOrCriteria(int val) {
        _orCriteriaFlag = val;
    }
    
    public long getCtime() {
        return _ctime;
    }

    protected void setCtime(Long val) {
        _ctime = val != null ? val.longValue() : 0;
    }

    public long getMtime() {
        return _mtime;
    }

    protected void setMtime(Long val) {
        _mtime = val != null ? val.longValue() : 0;
    }

    public String getModifiedBy() {
        return _modifiedBy;
    }

    protected void setModifiedBy(String val) {
        _modifiedBy = val;
    }

    protected Collection getResourceSet() {
        return _resourceSet;
    }

    protected void setResourcePrototype(Resource r) {
        _resourcePrototype = r;
    }

    /**
     * Checks if this group is compatable with the passed resource. 
     *
     * @param resource A resource prototype.  Note that this is NOT an
     *                 instance of the prototype.
     * 
     * @return false if this is not a compatable group, or if the passed
     *               resource is not an instace of this.getResourcePrototype()
     */
    public boolean isCompatableWith(Resource resource) {
        return _resourcePrototype != null && 
            _resourcePrototype.equals(resource);
    }
    
    /**
     * If the group is compatable, this method returns the prototype for all
     * resources contained within.
     */
    public Resource getResourcePrototype() {
        return _resourcePrototype;
    }

    protected void setResource(Resource r) {
        _resource = r;
    }
    
    public Resource getResource() {
        return _resource;
    }
    
    public Collection getResources()
    {
        TreeSet resources = new TreeSet(new AuthzNamedBean.Comparator());
        // Filter our the resource that is this group
        for (Iterator it = getResourceSet().iterator(); it.hasNext(); ) {
            Resource res = (Resource) it.next();
            if (!res.getInstanceId().equals(getId()) ||
                !res.getResourceType().getId().equals(AuthzConstants.authzGroup)
               ) {
                resources.add(res);
            }
        }
        return resources;
    }

    protected void setResourceSet(Collection val) {
        _resourceSet = val;
    }

    public void addResource(Resource resource) {
        resource.getResourceGroups().add(this);
        _resourceSet.add(resource);
    }
    
    public void addResources(Collection r) {
        for (Iterator i = r.iterator(); i.hasNext(); ) {
            addResource((Resource)i.next());
        }
    }

    public void removeResource(Resource resource) {
        _resourceSet.remove(resource);
    }

    public void removeAllResources() {
        /**
         * Why is this not symmetrical?  addResource also adds the group to
         * the resource's list.  we should remove ourselves here as well
         * XXX
         */
        _resourceSet.clear();
    }

    public Collection getRoles() {
        return _roles;
    }

    protected void setRoles(Collection val) {
        _roles = val;
    }

    // hibernate getter method
    // ResourceGroupManager should call getCritterList instead
    protected List getCriteriaList() {
        return _criteria;
    }

    // hibernate setter method
    // ResourceGroupManager should call setCritterList instead 
    protected void setCriteriaList(List val) {
        _criteria = val;
    }
    
    /**
     * Getter method used to retrieve the criteria list for a ResourceGroup.
     * @return CritterList The criteria list associated with this ResourceGroup instance.
     * @throws GroupException
     */
    public CritterList getCritterList() throws GroupException {
        List critters = new ArrayList();
        // iterate through all the persisted criteria, and convert to critters
        // to put into a critter list
        for (Iterator it = getCriteriaList().iterator(); it.hasNext();) {
            PersistedCritter dump = (PersistedCritter)it.next();
            CritterType type = CritterRegistry.getRegistry().getCritterTypeForClass(dump.getKlazz());
            critters.add(type.compose(dump));
        }
        return new CritterList(critters, _orCriteriaFlag > 0);
    }

     // used by the ResourceManager to set the criteria list for a resource group
    // note that the ResourceManager should invoke this method rather than
    // the setCriteriaList  setter used by hibernate
    protected void setCritterList(CritterList criteria) throws GroupException {
        List dumps = new ArrayList();
        // iterate through all the critters in the critter list
        // and convert them to persisted critters
        // finally update the resource group with the new set of critters
        int index = 0;
        for (Iterator it = criteria.getCritters().iterator(); it.hasNext(); index++) {
            Critter critter = (Critter)it.next();
            CritterType critType = critter.getCritterType();
            PersistedCritter dump = new PersistedCritter(this, critType, index);
            critType.decompose(critter, dump);
            dumps.add(dump);
        }
        this.setOrCriteria(criteria.isAny() ? 1 : 0);
        setCriteriaList(dumps);
    }
    
    public void addRole(Role role) {
        role.getResourceGroups().add(this);
        _roles.add(role);
    }

    public void removeRole(Role role) {
        _roles.remove(role);
    }

    public void removeAllRoles() {
        _roles.clear();
    }

    /**
     * Returns true if this group contains a resource of the specified ID
     * @deprecated Should use call into the manager instead.
     */
    public boolean existsAppdefEntity(AppdefEntityID ent) {
        for (Iterator i=getResources().iterator(); i.hasNext(); ) {
            Resource r = (Resource)i.next();
            AppdefEntityID rId = new AppdefEntityID(r);
            
            if (rId.equals(ent))
                return true;
        }
        return false;
    }

    /**
     * @deprecated use (this) ResourceGroup instead
     */
    public ResourceGroupValue getResourceGroupValue() {
        _resourceGroupValue.setClusterId(getClusterId().intValue());
        _resourceGroupValue.setCTime(new Long(getCtime()));
        _resourceGroupValue.setDescription(getDescription());
        _resourceGroupValue.setGroupEntResType(getGroupEntResType().intValue());
        _resourceGroupValue.setGroupEntType(getGroupEntType().intValue());
        _resourceGroupValue.setGroupType(getGroupType().intValue());
        _resourceGroupValue.setId(getId());
        _resourceGroupValue.setLocation(getLocation());
        _resourceGroupValue.setModifiedBy(getModifiedBy());
        _resourceGroupValue.setMTime(new Long(getMtime()));
        _resourceGroupValue.setName(getName());
        _resourceGroupValue.setSortName(getSortName());
        _resourceGroupValue.setSystem(isSystem());
        return _resourceGroupValue;
    }

    /**
     * @TODO: This method needs to be removed in favor of more discrete
     * operations.  Not all the properties here can be changed once a group is
     * created.
     */
    protected void setResourceGroupValue(ResourceGroupValue val) {
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

    public Object getValueObject() {
        return getResourceGroupValue();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ResourceGroup) && super.equals(obj);
    }
}
