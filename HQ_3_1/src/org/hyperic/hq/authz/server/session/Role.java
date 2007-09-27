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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.hq.common.server.session.Calendar;

public class Role extends AuthzNamedBean {
    private Integer    _cid;
    private String     _description;
    private boolean    _system = false;
    private Resource   _resource;
    private Collection _resourceGroups = new ArrayList();
    private Collection _operations = new HashSet();
    private Collection _subjects = new ArrayList();
    private Collection _calendars = new ArrayList();
    private RoleValue  _roleValue = new RoleValue();

    public Role() {
        super();
    }

    public Role(RoleValue val) {
        super();
        setRoleValue(val);
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

    public boolean isSystem() {
        return _system;
    }
    
    protected void setSystem(boolean fsystem) {
        _system = fsystem;
    }

    public Resource getResource() {
        return _resource;
    }
    
    protected void setResource(Resource resourceId) {
        _resource = resourceId;
    }

    public Collection getResourceGroups() {
        return _resourceGroups;
    }
    
    void clearResourceGroups() {
        for (Iterator i=getResourceGroups().iterator(); i.hasNext(); ) {
            ResourceGroup grp = (ResourceGroup)i.next();
            
            grp.removeRole(this);
        }
        getResourceGroups().clear();
    }
    
    protected void setResourceGroups(Collection val) {
        _resourceGroups = val;
    }

    void addOperation(Operation op) {
        getOperations().add(op);
    }
    
    public Collection getOperations() {
        return _operations;
    }
    
    protected void setOperations(Collection val) {
        _operations = val;
    }

    public Collection getSubjects() {
        return _subjects;
    }
    
    protected void setSubjects(Collection val) {
        _subjects = val;
    }
    
    protected Collection getCalendarBag() {
        return _calendars;
    }
    
    protected void setCalendarBag(Collection c) {
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
    
    public Collection getCalendars() {
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
                Operation op = (Operation) it.next();
                _roleValue.addOperationValue(op.getOperationValue());
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

    public Object getValueObject() {
        return getRoleValue();
    }

    public boolean equals(Object obj) {
        return (obj instanceof Role) && super.equals(obj);
    }

    public OwnedRoleValue getOwnedRoleValue() {
        OwnedRoleValue orv = new OwnedRoleValue(this.getRoleValue(),
                                                this.getResource().getOwner()
                                                    .getAuthzSubjectValue());
        return orv;
    }
}

