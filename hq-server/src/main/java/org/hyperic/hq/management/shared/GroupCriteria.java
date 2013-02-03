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
    private Resource resourcePrototype;

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
    public Resource getResourcePrototype() {
        return resourcePrototype;
    }
    void setResourcePrototype(Resource resourcePrototype) {
        this.resourcePrototype = resourcePrototype;
    }

    public Pattern getResourceNameRegexPattern() {
        if (resourceNameRegex != null && resourceNameRegexPattern == null) {
            resourceNameRegexPattern = Pattern.compile(resourceNameRegex, Pattern.CASE_INSENSITIVE);
        }
        return resourceNameRegexPattern;
    }

    public int hashCode() {
        int rtn = 17;
        rtn += (name != null) ? name.hashCode() : 0;
        rtn += (resourceNameRegex != null) ? resourceNameRegex.hashCode() : 0;
        rtn += (resourcePrototype != null && resourcePrototype.getName() != null) ? resourcePrototype.getName().hashCode() : 0;
        return rtn;
    }
    
    public String toString() {
        final StringBuilder rtn = new StringBuilder(128);
        rtn.append(name != null ? name : "");
        rtn.append("resourceNameRegex=");
        rtn.append(resourceNameRegex != null ? resourceNameRegex : "null");
        rtn.append(",resourcePrototype=");
        rtn.append(resourcePrototype != null ? resourcePrototype : "null");
        return rtn.toString();
    }

}
