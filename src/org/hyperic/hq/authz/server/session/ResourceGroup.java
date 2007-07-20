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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupValue;

public class ResourceGroup extends AuthzNamedBean
{
    private Integer _cid;
    private String _description;
    private String _location;
    private boolean _system = false;
    private Integer _groupType;
    private Integer _groupEntType;
    private Integer _groupEntResType;
    private Integer _clusterId;
    private long _ctime;
    private long _mtime;
    private String _modifiedBy;
    private Resource _resource;
    private Collection _resourceSet = new HashSet();
    private Collection _roles = new HashSet();

    private ResourceGroupValue _resourceGroupValue = new ResourceGroupValue();

    protected ResourceGroup() {
        super();
    }

    ResourceGroup(ResourceGroupValue val) {
        setResourceGroupValue(val);
    }

    public Integer getCid() {
        return _cid;
    }

    protected void setCid(Integer val) {
        _cid = val;
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

    public Integer getGroupEntType() {
        return _groupEntType;
    }

    protected void setGroupEntType(Integer val) {
        _groupEntType = val;
    }

    public Integer getGroupEntResType() {
        return _groupEntResType;
    }

    protected void setGroupEntResType(Integer val) {
        _groupEntResType = val;
    }

    public Integer getClusterId() {
        return _clusterId;
    }

    protected void setClusterId(Integer val) {
        _clusterId = val;
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

    public void removeResource(Resource resource) {
        _resourceSet.remove(resource);
    }

    public void removeAllResources() {
        _resourceSet.clear();
    }

    public Collection getRoles() {
        return _roles;
    }

    protected void setRoles(Collection val) {
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

    protected void setResourceGroupValue(ResourceGroupValue val) {
        setClusterId(new Integer(val.getClusterId()));
        setCtime(val.getCTime());
        setDescription(val.getDescription());
        setGroupEntResType(new Integer(val.getGroupEntResType()));
        setGroupEntType(new Integer(val.getGroupEntType()));
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
