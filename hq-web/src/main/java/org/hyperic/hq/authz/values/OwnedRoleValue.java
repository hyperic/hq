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

package org.hyperic.hq.authz.values;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.RoleValue;

public class OwnedRoleValue extends RoleValue {

    private int memberCount;
    private AuthzSubject owner;

    public OwnedRoleValue()
    {
        super();
    }

    public OwnedRoleValue(Role parent) {
        super(parent.getRoleValue());
        this.setOwner(parent.getResource().getOwner());
    }

    public int getMemberCount() {
        return this.memberCount;
    }

    public void setMemberCount(int count) {
        this.memberCount = count;
    }

    public AuthzSubject getOwner() {
        return this.owner;
    }

    public void setOwner(AuthzSubject owner) {
        this.owner = owner;
    }

    public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("id=" + getId() + " ");
        str.append("name=" + getName() + " ");
        str.append("description=" + getDescription() + " ");
        str.append("memberCount=" + getMemberCount() + " ");
        str.append("owner=" + getOwner());
        str.append("}");
        return(str.toString());
    }
}
