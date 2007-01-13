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

import org.hyperic.hq.authz.shared.OperationValue;

public class Operation extends AuthzNamedBean {
    private ResourceType _resourceType;
    private Integer      _cid;
    private Collection   _roles = new ArrayList();

    private OperationValue _operationValue = new OperationValue();

    protected Operation() {
    }

    public Operation(OperationValue val) {
        setOperationValue(val);
    }

/*
    public Operation(String name, ResourceType resourceType, Integer cid,
                     Collection roles)
    {
        super(name);
        _resourceType = resourceType;
        _cid          = cid;
        _roles        = roles;
    }
    */

    public ResourceType getResourceType() {
        return _resourceType;
    }

    protected void setResourceType(ResourceType resourceTypeId) {
        _resourceType = resourceTypeId;
    }

    public Integer getCid() {
        return _cid;
    }

    protected void setCid(Integer val) {
        _cid = val;
    }

    public Collection getRoles() {
        return Collections.unmodifiableCollection(_roles);
    }

    protected Collection getRolesBag() {
        return _roles;
    }

    protected void setRolesBag(Collection val) {
        _roles = val;
    }

    void addRole(Role role) {
        getRolesBag().add(role);
    }

    void removeRole(Role role) {
        getRolesBag().remove(role);
    }

    void removeAllRoles() {
        getRolesBag().clear();
    }

    /**
     * @deprecated use (this) Operation instead
     */
    public OperationValue getOperationValue() {
        _operationValue.setId(getId());
        _operationValue.setName(getName());
        return _operationValue;
    }

    protected void setOperationValue(OperationValue val) {
        setId(val.getId());
        setName(val.getName());
    }

    public Object getValueObject() {
        return getOperationValue();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Operation) || !super.equals(obj)) {
            return false;
        }
        Operation o = (Operation) obj;
        return ((_resourceType == o.getResourceType()) ||
                (_resourceType != null && o.getResourceType() != null &&
                 _resourceType.equals(o.getResourceType())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result =
            37 * result + (_resourceType != null ? _resourceType.hashCode() : 0);

        return result;
    }
}
