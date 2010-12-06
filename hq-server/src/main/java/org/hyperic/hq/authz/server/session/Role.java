/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.hq.common.server.session.Calendar;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.datastore.graph.annotation.NodeEntity;

@NodeEntity(partial=true)
public class Role  {
    private String     _description;
    private boolean    _system = false;
    private Resource   _resource;
    private Collection _resourceGroups = new ArrayList();
    private Collection _operations = new HashSet();
    private Collection _subjects = new ArrayList();
    private Collection _calendars = new ArrayList();
    private RoleValue  _roleValue = new RoleValue();
    private String _name;
    private String _sortName;
    private Integer _id;

    // for hibernate optimistic locks -- don't mess with this.
    // Named ugly-style since we already use VERSION in some of our tables.
    // really need to use Long instead of primitive value
    // because the database column can allow null version values.
    // The version column IS NULLABLE for migrated schemas. e.g. HQ upgrade
    // from 2.7.5.
    private Long    _version_;


    public Role() {
        super();
    }

    public void setId(Integer id) {
        _id = id;
    }

    public Integer getId() {
        return _id;
    }
    
    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        _name = name;
        setSortName(name);
    }

    public String getSortName() {
        return _sortName;
    }

    public void setSortName(String sortName) {
        _sortName = sortName != null ? sortName.toUpperCase() : null;
    }
    
    public String getDescription() {
        return _description;
    }
    
    void setDescription(String val) {
        _description = val;
    }

    public boolean isSystem() {
        return _system;
    }
    
    void setSystem(boolean fsystem) {
        _system = fsystem;
    }

    public Resource getResource() {
        return _resource;
    }
    
    void setResource(Resource resourceId) {
        _resource = resourceId;
    }

    public Collection getResourceGroups() {
        return _resourceGroups;
    }
    
    void removeResourceGroup(ResourceGroup group) {
        group.removeRole(this);
        getResourceGroups().remove(group);
    }
    
    void clearResourceGroups() {
        for (Iterator i=getResourceGroups().iterator(); i.hasNext(); ) {
            ResourceGroup grp = (ResourceGroup)i.next();
            grp.removeRole(this);
        }
        getResourceGroups().clear();
    }
    
    void setResourceGroups(Collection val) {
        _resourceGroups = val;
    }

    void addOperation(OperationType op) {
        getOperations().add(op);
    }
    
    public Collection<OperationType> getOperations() {
        return _operations;
    }
    
    void setOperations(Collection val) {
        _operations = val;
    }

    public Collection<AuthzSubject> getSubjects() {
        return _subjects;
    }
    
    void setSubjects(Collection val) {
        _subjects = val;
    }
    
    Collection getCalendarBag() {
        return _calendars;
    }
    
    void setCalendarBag(Collection c) {
        _calendars = c;
    }
    
    /**
     * Get a collection of {@link Calendar}s of the specified type for the
     * role.  
     */
    public Collection getCalendars(RoleCalendarType type) {
        List res = new ArrayList();
        
        for (Iterator i=getCalendars().iterator(); i.hasNext(); ) {
            RoleCalendar c = (RoleCalendar)i.next();
            
            if (c.getType().equals(type))
                res.add(c.getCalendar());
        }
        return res;
    }
    
    public Collection<RoleCalendar> getCalendars() {
        return Collections.unmodifiableCollection(_calendars);
    }
    
    void addCalendar(RoleCalendar c) {
        getCalendarBag().add(c);
    }
    
    void clearCalendars() {
        getCalendarBag().clear();
    }
    
    boolean removeCalendar(RoleCalendar c) {
        return getCalendarBag().remove(c);
    }

    void clearSubjects() {
        for (Iterator i=getSubjects().iterator(); i.hasNext(); ) {
            AuthzSubject s = (AuthzSubject)i.next();
            
            s.removeRole(this);
        }
        getSubjects().clear();
    }
    
    /**
     * @deprecated use (this) Role instead
     */
    public RoleValue getRoleValue() {
        _roleValue.setDescription(getDescription());
        _roleValue.setId(getId());
        _roleValue.setName(getName());
        _roleValue.setSortName(getSortName());
        _roleValue.setSystem(isSystem());
        
        _roleValue.removeAllOperationValues();
        if (getOperations() != null) {
            for (Iterator it = getOperations().iterator(); it.hasNext(); ) {
                OperationType op = (OperationType) it.next();
                _roleValue.addOperationValue(op);
            }
        }

        return _roleValue;
    }

    void setRoleValue(RoleValue val) {
        setId(val.getId());
        setName(val.getName());
        setDescription(val.getDescription());
        setSystem(val.getSystem());
    }

    public OwnedRoleValue getOwnedRoleValue() {
        OwnedRoleValue orv = new OwnedRoleValue(this);
        return orv;
    }
}

