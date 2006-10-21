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

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;

public class Resource extends AuthzNamedEntity implements Serializable {

    // Fields
    private ResourceType resourceType;
    private Integer instanceId;
    private Integer cid;
    private AuthzSubject owner;
    private String sortName;
    private boolean system;
    private Collection resourceGroups;
    private Collection group;
    private Collection roles;

    private ResourceValue resourceValue = new ResourceValue();

    // Constructors

    /** default constructor */
    public Resource() {
        super();
    }
    
    /** minimal constructor */
    public Resource(ResourceValue val) {
        setResourceValue(val);
    }

    /** full constructor */
    public Resource(ResourceType resourceTypeId, Integer instanceId,
            Integer cid, AuthzSubject subjectId, String name, String sortName,
            boolean fsystem, Collection resourceGroups, Collection group,
            Collection roles) {
        super(name);
        this.resourceType = resourceTypeId;
        this.instanceId = instanceId;
        this.cid = cid;
        this.owner = subjectId;
        this.sortName = sortName;
        this.system = fsystem;
        this.resourceGroups = resourceGroups;
        this.group = group;
        this.roles = roles;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceTypeId) {
        resourceType = resourceTypeId;
    }

    public Integer getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Integer val) {
        instanceId = val;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer val) {
        cid = val;
    }

    public AuthzSubject getOwner() {
        return owner;
    }

    public void setOwner(AuthzSubject val) {
        owner = val;
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String val) {
        sortName = val;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean fsystem) {
        system = fsystem;
    }

    public Collection getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(Collection val) {
        resourceGroups = val;
    }

    public Collection getGroup() {
        return group;
    }

    public void setGroup(Collection val) {
        group = val;
    }

    public Collection getRoles() {
        return roles;
    }

    public void setRoles(Collection val) {
        roles = val;
    }

    public ResourceValue getResourceValue() {
        resourceValue.setId(getId());
        resourceValue.setAuthzSubjectValue(getOwner().getAuthzSubjectValue());
        resourceValue.setInstanceId(getInstanceId());
        resourceValue.setName(getName());
        resourceValue.setSortName(getSortName());
        resourceValue.setSystem(isSystem());
        
        // Resource type of a resource should never change
        if (resourceValue.getResourceTypeValue() == null)
            resourceValue
                .setResourceTypeValue(getResourceType().getResourceTypeValue());

        return resourceValue;
    }

    public void setResourceValue(ResourceValue val) {
        setId(val.getId());
        setInstanceId(val.getInstanceId());
        setName(val.getName());
        setSortName(val.getSortName());
        setSystem(val.getSystem());
    }

    public Object getValueObject() {
        return getResourceValue();
    }

    public boolean equals(Object other) {
        if ((this == other))
            return true;
        if ((other == null))
            return false;
        if (!(other instanceof Resource))
            return false;
        Resource castOther = (Resource) other;

        return ((getResourceType() == castOther.getResourceType()) || (this
                .getResourceType() != null
                && castOther.getResourceType() != null && this
                .getResourceType().equals(castOther.getResourceType())))
                && ((getInstanceId() == castOther.getInstanceId()) || (this
                        .getInstanceId() != null
                        && castOther.getInstanceId() != null && this
                        .getInstanceId().equals(castOther.getInstanceId())));
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 37 * result
                + ((getSortName() != null) ? getSortName().hashCode() : 0);

        result = 37 * result
                + ((getInstanceId() != null) ? getInstanceId().hashCode() : 0);

        result = 37 * result + (system ? 0 : 1);

        result = 37 * result
                + ((getResourceType() != null) ? getResourceType()
                        .hashCode() : 0);
        result = 37 * result
                + ((getOwner() != null) ? getOwner()
                        .hashCode() : 0);
        return result;
    }

}
