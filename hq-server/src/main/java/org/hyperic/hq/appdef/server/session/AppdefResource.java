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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.authz.server.session.Resource;

/**
 * abstract base class for all appdef resources
 */
public abstract class AppdefResource extends AppdefNamedBean
{
    private Resource _resource;
    
    /**
     * default constructor
     */
    public AppdefResource()
    {
        super();
    }

    public Resource getResource() {
        return _resource;
    }

    protected void setResource(Resource resource) {
        _resource = resource;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.appdef.server.session.AppdefNamedBean#getName()
     */
    public String getName() {
        if (_resource != null)
            return _resource.getName();
        return super.getName();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.appdef.server.session.AppdefNamedBean#setName(java.lang.String)
     */
    public void setName(String name) {
        if (_resource != null)
            _resource.setName(name);
        else
            super.setName(name);
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.appdef.server.session.AppdefNamedBean#getSortName()
     */
    public String getSortName() {
        if (_resource != null)
            return _resource.getSortName();
        return super.getSortName();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.appdef.server.session.AppdefNamedBean#setSortName(java.lang.String)
     */
    public void setSortName(String sortName) {
        if (_resource != null)
            _resource.setSortName(sortName);
        else
            super.setSortName(sortName);
    }
}
