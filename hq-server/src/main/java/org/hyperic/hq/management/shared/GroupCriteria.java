/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
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

package org.hyperic.hq.management.shared;

import java.util.Set;
import java.util.regex.Pattern;

import org.hyperic.hq.authz.server.session.AuthzNamedBean;
import org.hyperic.hq.authz.server.session.Resource;

@SuppressWarnings("serial")
public class GroupCriteria extends AuthzNamedBean {
    
    private long ctime;
    private String name;
    private String creator;
    private String resourceNameRegex;
    private Pattern resourceNameRegexPattern;
    private String description;
    private Set<Resource> resourcePrototypes;

    public long getCtime() {
        return ctime;
    }
    public void setCtime(long ctime) {
        this.ctime = ctime;
    }
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }
    public String getResourceNameRegex() {
        return resourceNameRegex;
    }
    public void setResourceNameRegex(String resourceNameRegex) {
        this.resourceNameRegex = resourceNameRegex;
        this.resourceNameRegexPattern = null;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Set<Resource> getResourcePrototypes() {
        return resourcePrototypes;
    }
    public void setResourcePrototypes(Set<Resource> resourcePrototypes) {
        this.resourcePrototypes = resourcePrototypes;
    }

    public Pattern getResourceNameRegexPattern() {
        if ((resourceNameRegex != null) && (resourceNameRegexPattern == null)) {
            resourceNameRegexPattern = Pattern.compile(resourceNameRegex);
        }
        return resourceNameRegexPattern;
    }

    @Override
    public int hashCode() {
        int rtn = 17;
        rtn += (name != null) ? name.hashCode() : 0;
        rtn += (resourceNameRegex != null) ? resourceNameRegex.hashCode() : 0;

        if (resourcePrototypes != null) {
            for (Resource prototype : resourcePrototypes) {
                rtn += (prototype.getName() != null) ? prototype.getName().hashCode() : 0;
            }
        }
        
        
        
        return rtn;
    }
    
    @Override
    public String toString() {
        final StringBuilder rtn = new StringBuilder(128);
        rtn.append(name != null ? name : "");
        rtn.append("resourceNameRegex=");
        rtn.append(resourceNameRegex != null ? resourceNameRegex : "null");
        rtn.append(",resourcePrototype=");
 
        if (resourcePrototypes == null) {
            rtn.append("null");
        } else  {
            for (Resource prototype : resourcePrototypes) {
                rtn.append(prototype);
                rtn.append(",");
            }
        }
         return rtn.toString();
    }

}
