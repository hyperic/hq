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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.util.config.ConfigResponse;

public abstract class DashboardConfig
    extends PersistedObject
{
    private Crispo _config;
    private String _name;

    protected DashboardConfig() {
    }
    
    protected DashboardConfig(String name, Crispo config) {
        _name   = name;
        _config = config;
    }
    
    public ConfigResponse getConfig() {
        return _config.toResponse();
    }
    
    protected Crispo getCrispo() {
        return _config;
    }
    
    protected void setCrispo(Crispo config) {
        _config = config;
    }
    
    public String getName() {
        return _name;
    }
    
    protected void setName(String n) {
        _name = n;
    }
    
    public abstract boolean isEditable(AuthzSubject by);
    
    public int hashCode() {
        int hash = 17;

        hash = hash * 37 + getName().hashCode();
        hash = hash * 37 + (getCrispo() != null ? getCrispo().hashCode() : 0);
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof DashboardConfig == false)
            return false;
        
        DashboardConfig oe = (DashboardConfig)o;

        if (!getName().equals(oe.getName()))
            return false;
        
        if (getCrispo().getId() != oe.getCrispo().getId())
            return false;

        return true;
    }
}
