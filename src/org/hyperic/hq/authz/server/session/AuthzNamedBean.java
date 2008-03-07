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

import org.hyperic.hibernate.PersistedObject;

public abstract class AuthzNamedBean extends PersistedObject
{
    private String _name;
    private String _sortName;

    protected AuthzNamedBean() {
    }

    protected AuthzNamedBean(String name) {
        _name = name;
        setSortName(name);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        _name = name;
        setSortName(name);
    }

    public String getSortName() {
        return _sortName;
    }

    protected void setSortName(String sortName) {
        _sortName = sortName != null ? sortName.toUpperCase() : null;
    }

    /**
     * @deprecated use (this) Pojo instead
     */
    public abstract Object getValueObject();

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        
        if (obj == null || obj instanceof AuthzNamedBean == false) {
            return false;
        }
        
        AuthzNamedBean o = (AuthzNamedBean) obj;
        return ((_name == o.getName()) ||
                (_name != null && o.getName() != null && 
                 _name.equals(o.getName())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37 * result + (_name != null ? _name.hashCode() : 0);

        return result;
    }
    
    public class Comparator implements java.util.Comparator {

        public int compare(Object arg0, Object arg1) {
            if (!(arg0 instanceof AuthzNamedBean) ||
                    !(arg1 instanceof AuthzNamedBean))
                return 0;

            
            return ((AuthzNamedBean) arg0).getName().compareTo(
                   ((AuthzNamedBean) arg1).getName());
        }
    }
}
