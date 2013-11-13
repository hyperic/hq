/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2013], VMware, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.Resource;

@SuppressWarnings("serial")
public class PolicyStatus extends PersistedObject {
    
    // Based on the length of the EAM_POLICY_RESOURCE_STATUS.CONFIG_STATUS_BUF in DB 
    private static final int MAX_STATUS_BUF_WIDTH = 512;
    
    private long created;
    private long modified;
    private ManagementPolicy policy;
    private Resource policyGroupMember;
    private int configStatus;
    private String configStatusBuf;

    public long getCreated() {
        return created;
    }
    public void setCreated(long created) {
        this.created = created;
    }
    public long getModified() {
        return modified;
    }
    public void setModified(long modified) {
        this.modified = modified;
    }
    public int getConfigStatus() {
        return configStatus;
    }
    public void setConfigStatus(int configStatus) {
        this.configStatus = configStatus;
    }
    public String getConfigStatusBuf() {
        return configStatusBuf;
    }
    public void setConfigStatusBuf(String configStatusBuf) {
        this.configStatusBuf = StringUtils.abbreviate(configStatusBuf, MAX_STATUS_BUF_WIDTH) ;
    }
    public Resource getPolicyGroupMember() {
        return policyGroupMember;
    }
    public void setPolicyGroupMember(Resource policyGroupMember) {
        this.policyGroupMember = policyGroupMember;
    }
    public ManagementPolicy getPolicy() {
        return policy;
    }
    public void setPolicy(ManagementPolicy policy) {
        this.policy = policy;
    }

    public int hashCode() {
        return getPolicy().getId().hashCode() + getPolicyGroupMember().getId().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PolicyStatus) {
            PolicyStatus p = (PolicyStatus) o;
            if (getPolicy().getId().equals(p.getPolicy().getId()) &&
                    getPolicyGroupMember().getId().equals(p.getPolicyGroupMember().getId())) {
                return true;
            }
        }
        return false;
    }

}
