/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2012], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.management.shared;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;

@SuppressWarnings("serial")
public class PolicyMember extends PersistedObject {
    
    private long created;
    private Resource resource;
    private ResourceGroup resourceGroup;

    public long getCreated() {
        return created;
    }
    public void setCreated(long created) {
        this.created = created;
    }
    public Resource getResource() {
        return resource;
    }
    public void setResource(Resource resource) {
        this.resource = resource;
    }
    public ResourceGroup getResourceGroup() {
        return resourceGroup;
    }
    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public int hashCode() {
        return resource.getId().hashCode() + resourceGroup.getId().hashCode();
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PolicyMember) {
            PolicyMember p = (PolicyMember) o;
            if (resource.getId().equals(p.resource.getId()) && resourceGroup.getId().equals(p.resourceGroup.getId())) {
                return true;
            }
        }
        return false;
    }

}
