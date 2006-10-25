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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.hyperic.hq.authz.shared.RoleValue;

public class Role extends AuthzNamedEntity
{

    // Fields    

     private Integer cid;
     private String description;
     private boolean system;
     private Resource resource;
     private Collection resourceGroups = new ArrayList();
     private Collection operations = new ArrayList();
     private Collection subjects = new ArrayList();

     private RoleValue roleValue = new RoleValue();
     
     // Constructors

    /** default constructor */
    public Role() {
        super();
    }

	/** minimal constructor */
    public Role(RoleValue val) {
        super();
        setRoleValue(val);
    }
    
    /** full constructor */
    public Role(String name, Integer cid, String sortName, String description,
                boolean fsystem, Resource resourceId, Collection resourceGroups,
                Collection operations, Collection subjects) {
        super(name);
        this.cid = cid;
        this.description = description;
        this.system = fsystem;
        this.resource = resourceId;
        this.resourceGroups = resourceGroups;
        this.operations = operations;
        this.subjects = subjects;
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
    public boolean isSystem() {
        return system;
    }
    
    public void setSystem(boolean fsystem) {
        system = fsystem;
    }
    public Resource getResource() {
        return resource;
    }
    
    public void setResource(Resource resourceId) {
        resource = resourceId;
    }
    public Collection getResourceGroups() {
        return resourceGroups;
    }
    
    public void setResourceGroups(Collection val) {
        resourceGroups = val;
    }
    public Collection getOperations() {
        return operations;
    }
    
    public void setOperations(Collection val) {
        operations = val;
    }
    public Collection getSubjects() {
        return subjects;
    }
    
    public void setSubjects(Collection val) {
        subjects = val;
    }


    public RoleValue getRoleValue() {
        roleValue.setDescription(getDescription());
        roleValue.setId(getId());
        roleValue.setName(getName());
        roleValue.setSortName(getSortName());
        roleValue.setSystem(isSystem());
        
        roleValue.removeAllOperationValues();
        if (getOperations() != null) {
            for (Iterator it = getOperations().iterator(); it.hasNext(); ) {
                Operation op = (Operation) it.next();
                roleValue.addOperationValue(op.getOperationValue());
            }
        }

        return roleValue;
    }

    public void setRoleValue(RoleValue val) {
        setId(val.getId());
        setName(val.getName());
        setDescription(val.getDescription());
        setSortName(val.getSortName());
        setSystem(val.getSystem());
    }

    public Object getValueObject() {
        return getRoleValue();
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof Role) && super.equals(obj);
    }
}


