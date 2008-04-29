package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

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

public abstract class ServerBase extends AppdefResource
{
    protected String autoinventoryIdentifier;
    protected String installPath;
    protected boolean servicesAutomanaged;

    public String getAutoinventoryIdentifier()
    {
        return this.autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier)
    {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public String getInstallPath()
    {
        return this.installPath;
    }

    public void setInstallPath(String installPath)
    {
        this.installPath = installPath;
    }

    public boolean isServicesAutomanaged()
    {
        return this.servicesAutomanaged;
    }

    public void setServicesAutomanaged(boolean servicesAutomanaged)
    {
        this.servicesAutomanaged = servicesAutomanaged;
    }

    public AppdefEntityID getEntityId()
    {
        return AppdefEntityID.newServerID(getId());
    }

    /**
     * @deprecated use isServicesAutomanaged()
     * @return
     */
    public boolean getServicesAutomanaged()
    {
        return isServicesAutomanaged();
    }
}
