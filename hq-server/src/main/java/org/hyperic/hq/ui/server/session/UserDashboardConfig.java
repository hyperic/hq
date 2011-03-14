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
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.ui.server.session.DashboardConfig;

@Entity
@DiscriminatorValue("USER")
public class UserDashboardConfig
    extends DashboardConfig {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", unique = true)
    private AuthzSubject user;

    protected UserDashboardConfig() {
    }

    public UserDashboardConfig(AuthzSubject user, String name, Crispo config) {
        super(name, config);
        this.user = user;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof UserDashboardConfig == false)
            return false;

        UserDashboardConfig oe = (UserDashboardConfig) o;

        if (!super.equals(oe))
            return false;

        return user.equals(oe.getUser());
    }

    public AuthzSubject getUser() {
        return user;
    }

    public int hashCode() {
        int hash = super.hashCode();

        hash = hash * 37 + user.hashCode();
        return hash;
    }

    public boolean isEditable(AuthzSubject by) {
        return getUser().equals(by);
    }

    protected void setUser(AuthzSubject user) {
        this.user = user;
    }
}
