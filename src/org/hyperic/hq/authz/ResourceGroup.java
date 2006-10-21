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

package org.hyperic.hq.authz;

import java.io.Serializable;
import java.util.Collection;

import org.hyperic.hq.authz.shared.ResourceGroupValue;

public class ResourceGroup extends AuthzNamedEntity implements Serializable {

    private Integer cid;
    private String description;
    private String location;
    private boolean system;
    private Integer groupType;
    private Integer groupEntType;
    private Integer groupEntResType;
    private Integer clusterId;
    private long ctime;
    private long mtime;
    private String modifiedBy;
    private Resource resource;
    private Collection resources;
    private Collection roles;

    private ResourceGroupValue resourceGroupValue = new ResourceGroupValue();

    // Constructors

    /** default constructor */
    public ResourceGroup() {
        super();
    }

    /** minimal constructor */
    public ResourceGroup(ResourceGroupValue val) {
        setResourceGroupValue(val);
    }

    /** full constructor */
    public ResourceGroup(String name, Integer cid,
                         String description, String location, boolean fsystem,
                         Integer groupType, Integer groupEntType,
                         Integer groupEntResType, Integer clusterId,
                         long ctime, long mtime, String modifiedBy,
                         Resource resourceId, Collection resources,
                         Collection roles) {
        super(name);
        this.cid = cid;
        this.description = description;
        this.location = location;
        this.system = fsystem;
        this.groupType = groupType;
        this.groupEntType = groupEntType;
        this.groupEntResType = groupEntResType;
        this.clusterId = clusterId;
        this.ctime = ctime;
        this.mtime = mtime;
        this.modifiedBy = modifiedBy;
        this.resource = resourceId;
        this.resources = resources;
        this.roles = roles;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer val) {
        cid = val;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String val) {
        description = val;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String val) {
        location = val;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean val) {
        system = val;
    }

    public Integer getGroupType() {
        return groupType;
    }

    public void setGroupType(Integer val) {
        groupType = val;
    }

    public Integer getGroupEntType() {
        return groupEntType;
    }

    public void setGroupEntType(Integer val) {
        groupEntType = val;
    }

    public Integer getGroupEntResType() {
        return groupEntResType;
    }

    public void setGroupEntResType(Integer val) {
        groupEntResType = val;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer val) {
        clusterId = val;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long val) {
        ctime = val;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long val) {
        mtime = val;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String val) {
        modifiedBy = val;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource val) {
        resource = val;
    }

    public Collection getResources() {
        return resources;
    }

    public void setResources(Collection val) {
        resources = val;
    }

    public Collection getRoles() {
        return roles;
    }

    public void setRoles(Collection val) {
        roles = val;
    }

    public ResourceGroupValue getResourceGroupValue() {
        return resourceGroupValue;
    }

    public void setResourceGroupValue(ResourceGroupValue val) {
        resourceGroupValue = val;
    }

    public Object getValueObject() {
        return getResourceGroupValue();
    }


    public boolean equals(Object obj)
    {
        return (obj instanceof ResourceGroup) && super.equals(obj);
    }
}
