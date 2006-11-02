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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.hyperic.hq.authz.shared.ResourceTypeValue;

public class ResourceType extends AuthzNamedBean
{
    private Integer cid;
    private Resource resource;
    private boolean system = false;
    private Collection operations = new ArrayList();
    private Collection resources = new ArrayList();

    private ResourceTypeValue resourceTypeValue = new ResourceTypeValue();

    // Constructors

    /** default constructor */
    public ResourceType() {
        super();
    }

    /** minimal constructor */
    public ResourceType(ResourceTypeValue val) {
        setResourceTypeValue(val);
    }

    /** full constructor */
    public ResourceType(String name, Integer cid, Resource resource,
                        boolean fsystem, Collection operations,
                        Collection resources) {
        super(name);
        this.cid = cid;
        this.resource = resource;
        this.system = fsystem;
        this.operations = operations;
        this.resources = resources;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer val) {
        cid = val;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource val) {
        resource = val;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean val) {
        system = val;
    }

    public Collection getOperations() {
        return operations;
    }

    public void setOperations(Collection val) {
        operations = val;
    }

    public Collection getResources() {
        return resources;
    }

    public void setResources(Collection val) {
        resources = val;
    }

    /**
     * @deprecated use (this) ResourceType instead
     */
    public ResourceTypeValue getResourceTypeValue() {
        resourceTypeValue.setId(getId());
        resourceTypeValue.setName(getName());
        resourceTypeValue.setSystem(isSystem());
        
        // Clear out the operation values first
        resourceTypeValue.removeAllOperationValues();
        if (getOperations() != null) {
            for (Iterator it = getOperations().iterator(); it.hasNext(); ) {
                Operation op = (Operation) it.next();
                resourceTypeValue.addOperationValue(op.getOperationValue());
            }
        }
        return resourceTypeValue;
    }

    public void setResourceTypeValue(ResourceTypeValue val) {
        setId(val.getId());
        setName(val.getName());
        setSystem(val.getSystem());
    }

    public Object getValueObject() {
        return getResourceTypeValue();
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof ResourceType) && super.equals(obj);
    }
}
