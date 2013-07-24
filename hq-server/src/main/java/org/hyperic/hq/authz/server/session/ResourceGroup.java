/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.management.shared.GroupCriteria;

@SuppressWarnings("serial")
public class ResourceGroup extends PersistedObject implements ContainerManagedTimestampTrackable {
    private String _description;
    private String _location;
    private boolean _system = false;
    private Integer _groupType;
    private Integer _clusterId;
    private long _ctime;
    private long _mtime;
    private String _modifiedBy;
    private Resource _resource;
    private Resource _resourcePrototype;
    private Collection _roles = new HashSet();

    private ResourceGroupValue _resourceGroupValue = new ResourceGroupValue();
    private GroupCriteria groupCriteria;

    public static class ResourceGroupCreateInfo {
        private String   _name;
        private String   _description;
        private String   _location;
        private int      _groupType;
        private Resource _resourcePrototype;
        private int      _clusterId;
        private boolean  _system;
        private boolean  _privateGroup;
        
        public ResourceGroupCreateInfo(String name, String description,
                                       int groupType, Resource prototype,
                                       String location, int clusterId,
                                       boolean system, boolean privateGroup) {
            _name              = name;
            _description       = description;
            _resourcePrototype = prototype;
            _groupType         = groupType;
            _location          = location;
            _clusterId         = clusterId;
            _system            = system;
            _privateGroup      = privateGroup;
        }
        
        public String getName() { return _name; }
        public String getDescription() { return _description; }
        public String getLocation() { return _location; }
        public int getGroupType() { return _groupType; }
        public Resource getResourcePrototype() { return _resourcePrototype; }
        public int getClusterId() { return _clusterId; }
        public boolean isSystem() { return _system; }
        public boolean isPrivateGroup() { return _privateGroup; }
        public ResourceGroup getResourceGroup(AuthzSubject creator) {
            return new ResourceGroup(this, creator);
        }
    }
    
    public ResourceGroup() {
        super();
    }

    private ResourceGroup(ResourceGroupCreateInfo cInfo, AuthzSubject creator) {
        _clusterId         = new Integer(cInfo.getClusterId());
        _description       = cInfo.getDescription();
        _location          = cInfo.getLocation();
        _system            = cInfo.isSystem();
        _groupType         = new Integer(cInfo.getGroupType());
        _resourcePrototype = cInfo.getResourcePrototype();
        _ctime = _mtime    = System.currentTimeMillis();
        _modifiedBy        = creator.getName();
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

    /* (non-Javadoc)
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#getName()
     */
    public String getName() {
        if (_resource != null)
            return _resource.getName();
        return "";
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#setName(java.lang.String)
     */
    public void setName(String name) {
        if (_resource != null)
            _resource.setName(name);
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#getSortName()
     */
    public String getSortName() {
        if (_resource != null)
            return _resource.getSortName();
        return "";
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.authz.server.session.AuthzNamedBean#setSortName(java.lang.String)
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

    void setClusterId(Integer val) {
        _clusterId = val;
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
    
    public void setGroupCriteria(GroupCriteria groupCriteria) {
        this.groupCriteria = groupCriteria;
    }
    
    public GroupCriteria getGroupCriteria() {
        return groupCriteria;
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

    public Object getValueObject() {
        return getResourceGroupValue();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ResourceGroup) && super.equals(obj);
    }

    public AppdefGroupValue getAppdefResourceValue() {
        AppdefGroupValue val = new AppdefGroupValue();
        val.setClusterId(getClusterId());
        val.setCTime(new Long(getCtime()));
        val.setDescription(getDescription());
        val.setGroupEntResType(getGroupEntResType());
        val.setGroupEntType(getGroupEntType());
        val.setGroupType(getGroupType());
        val.setId(getId());
        val.setLocation(getLocation());
        val.setModifiedBy(getModifiedBy());
        val.setMTime(new Long(getMtime()));
        val.setName(getName());
        val.setTotalSize(Bootstrap.getBean(ResourceGroupManager.class).getNumMembers(this));
        final Resource proto = getResourcePrototype();
        if (proto != null) {
            int protoTypeId = proto.getResourceType().getId();
            int protoTypeInstanceId = proto.getInstanceId();
            if (AuthzConstants.authzPlatformProto.equals(protoTypeId)) {
                PlatformType platformType = Bootstrap.getBean(PlatformManager.class).findPlatformType(protoTypeInstanceId);
                val.setAppdefResourceTypeValue(platformType.getAppdefResourceTypeValue());
            } else if (AuthzConstants.authzServerProto.equals(protoTypeId)) {
                ServerType serverType = Bootstrap.getBean(ServerManager.class).findServerType(protoTypeInstanceId);
                val.setAppdefResourceTypeValue(serverType.getAppdefResourceTypeValue());
            } else if (AuthzConstants.authzServiceProto.equals(protoTypeId)) {
                ServiceType serviceType = Bootstrap.getBean(ServiceManager.class).findServiceType(protoTypeInstanceId);
                val.setAppdefResourceTypeValue(serviceType.getAppdefResourceTypeValue());
            }
        } else {
            AppdefResourceTypeValue tvo = new GroupTypeValue();
            tvo.setId(getGroupType());
            tvo.setName(AppdefEntityConstants.getAppdefGroupTypeName(getGroupType()));
            val.setAppdefResourceTypeValue(tvo);
        }
        return val;
    }
}
