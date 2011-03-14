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

package org.hyperic.hq.ui.server.session;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.config.domain.Crispo;

@Entity
@DiscriminatorValue("ROLE")
public class RoleDashboardConfig
    extends DashboardConfig {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", unique = true)
    private Role role;

    protected RoleDashboardConfig() {
    }

    public RoleDashboardConfig(Role r, String name, Crispo config) {
        super(name, config);
        role = r;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof RoleDashboardConfig == false)
            return false;

        RoleDashboardConfig oe = (RoleDashboardConfig) o;

        if (!super.equals(oe))
            return false;

        return role.equals(oe.getRole());
    }

    public Role getRole() {
        return role;
    }

    public int hashCode() {
        int hash = super.hashCode();

        hash = hash * 37 + role.hashCode();
        return hash;
    }

    public boolean isEditable(AuthzSubject by) {
        //PermissionManager pMan = PermissionManagerFactory.getInstance();

        // try {
        // TODO perm check
        // pMan.check(by.getId(), _role.getResource().getType(),
        // _role.getId(), AuthzConstants.roleOpModifyRole);
        return true;
        // } catch (PermissionException e) {
        // return false;
        // }
    }

    protected void setRole(Role r) {
        role = r;
    }
}
