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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

public class ResourceType extends AuthzNamedBean {
    private static final Map TYPE_TO_PROP = new HashMap();
    
    static {
        TYPE_TO_PROP.put(AuthzConstants.authzPlatform, "resource.platform");
        TYPE_TO_PROP.put(AuthzConstants.authzServer, "resource.server");
        TYPE_TO_PROP.put(AuthzConstants.authzService, "resource.service");
        TYPE_TO_PROP.put(AuthzConstants.authzApplication, "resource.application");
        TYPE_TO_PROP.put(AuthzConstants.authzEscalation, "resource.escalation");
        TYPE_TO_PROP.put(AuthzConstants.authzGroup, "resource.group");
        TYPE_TO_PROP.put(AuthzConstants.authzSubject, "resource.subject");
        TYPE_TO_PROP.put(AuthzConstants.authzRole, "resource.role");
    }
    
    private Resource   _resource;
    private boolean    _system = false;
    private Collection _operations = new ArrayList();

    private ResourceTypeValue _resourceTypeValue = new ResourceTypeValue();

    protected ResourceType() {
        super();
    }

    ResourceType(String name, Resource resource, boolean fsystem) {
        super(name);
        _resource = resource;
        _system   = fsystem;
    }

    public Resource getResource() {
        return _resource;
    }

    protected void setResource(Resource val) {
        _resource = val;
    }

    public boolean isSystem() {
        return _system;
    }

    protected void setSystem(boolean val) {
        _system = val;
    }

    protected Collection getOperationsBag() {
        return _operations;
    }

    protected void setOperationsBag(Collection val) {
        _operations = val;
    }
    
    Operation createOperation(String name) {
        return new Operation(this, name);
    }

    public Collection getOperations() {
        return Collections.unmodifiableCollection(_operations);
    }

    /**
     * Convert an authz ResourceType to appdef type.  Currently only platform
     * server and service types are supported.
     * @return One of AppdefEntityConstants.APPDEF_TYPE*
     */
    public int getAppdefType() {
        if (getId().equals(AuthzConstants.authzPlatform)) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (getId().equals(AuthzConstants.authzServer)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (getId().equals(AuthzConstants.authzService)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else {
            throw new IllegalArgumentException("ResourceType " + getId() +
                                               " not supported");
        }
    }

    /**
     * @deprecated use (this) ResourceType instead
     */
    public ResourceTypeValue getResourceTypeValue() {
        _resourceTypeValue.setId(getId());
        _resourceTypeValue.setName(getName());
        _resourceTypeValue.setSystem(isSystem());
        
        // Clear out the operation values first
        _resourceTypeValue.removeAllOperationValues();
        if (getOperations() != null) {
            for (Iterator it = getOperations().iterator(); it.hasNext(); ) {
                Operation op = (Operation) it.next();
                _resourceTypeValue.addOperationValue(op);
            }
        }
        return _resourceTypeValue;
    }

    public Object getValueObject() {
        return getResourceTypeValue();
    }
    
    public String getLocalizedName() {
        ResourceBundle b = 
            ResourceBundle.getBundle("org.hyperic.hq.authz.Resources");
        String prop = (String)TYPE_TO_PROP.get(getId());
        
        if (prop == null) {
            return getName();
        }
        
        String res = b.getString(prop);
        if (res == null) {
            return getName();
        }
        
        return res;
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + ((_resource != null && _resource.getId() != null)
                                ? _resource.getId().intValue() : 0);
        return result;
    }
    
    public boolean equals(Object obj) {
        ResourceType o;
        
        if (obj == this)
            return true;
        
        if (obj == null || obj instanceof ResourceType == false)
            return false;
        
        o = (ResourceType)obj;
        return o.isSystem() == isSystem() &&
               (o.getResource() == getResource() ||
                o.getResource() != null && getResource() != null &&
                o.getResource().getId().equals(getResource().getId()));
    }
}
