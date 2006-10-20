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
import java.util.Iterator;

import org.hyperic.hq.authz.shared.ResourceTypeValue;

public class ResourceType extends AuthzNamedEntity implements Serializable {

    private Integer cid;
    private Integer resourceId;
    private boolean fsystem;
    private Collection operations;
    private Collection resources;

    private ResourceTypeValue resourceTypeValue = new ResourceTypeValue();

    // Constructors

    /** default constructor */
    public ResourceType() {
    }

    /** minimal constructor */
    public ResourceType(ResourceTypeValue val) {
        setResourceTypeValue(val);
    }

    /** full constructor */
    public ResourceType(String name, Integer cid, Integer resourceId,
                        boolean fsystem, Collection operations,
                        Collection resources) {
        super(name);
        this.cid = cid;
        this.resourceId = resourceId;
        this.fsystem = fsystem;
        this.operations = operations;
        this.resources = resources;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer val) {
        cid = val;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer val) {
        resourceId = val;
    }

    public boolean isFsystem() {
        return fsystem;
    }

    public void setFsystem(boolean val) {
        fsystem = val;
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

    public ResourceTypeValue getResourceTypeValue() {
        resourceTypeValue.setId(getId());
        resourceTypeValue.setName(getName());
        resourceTypeValue.setSystem(isFsystem());
        
        // Clear out the operation values first
        resourceTypeValue.cleanOperationValue();
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
        setFsystem(val.getSystem());
    }

    public boolean equals(Object other) {
        if ((this == other))
            return true;
        if ((other == null))
            return false;
        if (!(other instanceof ResourceType))
            return false;
        ResourceType castOther = (ResourceType) other;

        return ((getName() == castOther.getName()) ||
                (getName() != null && castOther.getName() != null
                        && getName()
                .equals(castOther.getName())));
    }

    public int hashCode() {
      int result = super.hashCode();
      result = 37*result + (isFsystem() ? 0 : 1);

      result = 37*result + ((getOperations() != null) ?
              getOperations().hashCode() : 0);
      return result;
    }

}
