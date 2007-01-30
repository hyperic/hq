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

import org.hyperic.hq.authz.shared.OperationValue;

public class Operation extends AuthzNamedBean {
    private ResourceType _resourceType;
    private Integer      _cid;

    private OperationValue _operationValue = new OperationValue();

    protected Operation() {
    }

    Operation(ResourceType type, String name) {
        super(name);
        _resourceType = type;
    }

    public ResourceType getResourceType() {
        return _resourceType;
    }

    protected void setResourceType(ResourceType resourceType) {
        _resourceType = resourceType;
    }

    public Integer getCid() {
        return _cid;
    }

    protected void setCid(Integer val) {
        _cid = val;
    }

    /**
     * @deprecated use (this) Operation instead
     */
    public OperationValue getOperationValue() {
        _operationValue.setId(getId());
        _operationValue.setName(getName());
        return _operationValue;
    }

    public Object getValueObject() {
        return getOperationValue();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj instanceof Operation == false)
            return false;
        
        Operation o = (Operation) obj;
        return getName().equals(o.getName());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getName().hashCode();
        return result;
    }
}
