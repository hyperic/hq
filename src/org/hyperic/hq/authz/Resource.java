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

import java.util.Collection;
import java.util.ArrayList;

import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Resource extends AuthzNamedBean
{
    public static final Log log = LogFactory.getLog(Resource.class);

    // Fields
    private ResourceType resourceType;
    private Integer instanceId;
    private Integer cid;
    private AuthzSubject owner;
    private boolean system = false;
    private Collection resourceGroups = new ArrayList();
    private Collection virtuals = new ArrayList();

    private ResourceValue resourceValue = new ResourceValue();

    // Constructors

    /**
     * default constructor
     */
    public Resource()
    {
        super();
    }

    /**
     * minimal constructor
     */
    public Resource(ResourceValue val)
    {
        setResourceValue(val);
    }

    /**
     * full constructor
     */
    public Resource(ResourceType resourceTypeId, Integer instanceId,
                    Integer cid, AuthzSubject subjectId, String name,
                    boolean fsystem, Collection resourceGroups)
    {
        super(name);
        this.resourceType = resourceTypeId;
        this.instanceId = instanceId;
        this.cid = cid;
        this.owner = subjectId;
        this.system = fsystem;
        this.resourceGroups = resourceGroups;
    }

    public ResourceType getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceTypeId)
    {
        resourceType = resourceTypeId;
    }

    public Integer getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(Integer val)
    {
        instanceId = val;
    }

    public Integer getCid()
    {
        return cid;
    }

    public void setCid(Integer val)
    {
        cid = val;
    }

    public AuthzSubject getOwner()
    {
        return owner;
    }

    public void setOwner(AuthzSubject val)
    {
        owner = val;
    }

    public boolean isSystem()
    {
        return system;
    }

    public void setSystem(boolean fsystem)
    {
        system = fsystem;
    }

    public Collection getResourceGroups()
    {
        return resourceGroups;
    }

    public Collection getVirtuals() {
        return virtuals;
    }

    public void setVirtuals(Collection virtuals) {
        this.virtuals = virtuals;
    }

    public void setResourceGroups(Collection val)
    {
        resourceGroups = val;
    }

    /**
     * @deprecated use (this) Resource instead
     */
    public ResourceValue getResourceValue()
    {
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

    public void setResourceValue(ResourceValue val)
    {
        setId(val.getId());
        setInstanceId(val.getInstanceId());
        setName(val.getName());
        setResourceType(new ResourceType(val.getResourceTypeValue()));
        setSystem(val.getSystem());
    }

    public Object getValueObject()
    {
        return getResourceValue();
    }

    public ResourceValue getResourceValueObject()
    {
        ResourceValue vo = new ResourceValue();
        vo.setSortName(getSortName());
        vo.setInstanceId(getInstanceId());
        vo.setSystem(isSystem());
        vo.setName((getName() == null) ? "" : getName());
        vo.setId(getId());
        if ( getOwner() != null )
            vo.setAuthzSubjectValue( getOwner().getAuthzSubjectValue() );
        else
            vo.setAuthzSubjectValue( null );
        return vo;
    }

    public boolean isOwner(Integer possibleOwner)
    {
        boolean is = false;

        if (possibleOwner == null) {
            log.error("possible Owner is NULL. This is probably not what you want.");
            /* XXX throw exception instead */
        } else {
            /* overlord owns every thing */
            if (is = possibleOwner.equals(AuthzConstants.overlordId)
                    == false) {
                if (log.isDebugEnabled() && possibleOwner != null) {
                    log.debug("User is " + possibleOwner +
                              " owner is " + getOwner().getId());
                }
                is = (possibleOwner.equals(getOwner().getId()));
            }
        }
        return is;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Resource) || !super.equals(obj)) {
            return false;
        }
        Resource o = (Resource) obj;
        return
            ((resourceType == o.getResourceType()) ||
             (resourceType != null && o.getResourceType() != null &&
              resourceType.equals(o.getResourceType())))
            &&
            ((instanceId == o.getInstanceId()) ||
             (instanceId != null && o.getInstanceId() != null &&
              instanceId.equals(o.getInstanceId())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result =
            37 * result + (resourceType != null ? resourceType.hashCode() : 0);
        result = 37 * result + (instanceId != null ? instanceId.hashCode() : 0);

        return result;
    }
}
