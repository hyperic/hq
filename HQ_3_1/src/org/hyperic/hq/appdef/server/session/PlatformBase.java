package org.hyperic.hq.appdef.server.session;

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

/**
 * Base for platform inventory
 */
public abstract class PlatformBase extends AppdefResource
{
    private String fqdn;
    private String certdn;
    private Integer cpuCount;

    public String getFqdn()
    {
        return this.fqdn;
    }

    public void setFqdn(String fqDN)
    {
        this.fqdn = fqDN;
        if (getName() == null) {
            setName(fqDN);
        }
    }

    public AppdefEntityID getEntityId()
    {
        return new AppdefEntityID(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
            getId().intValue());
    }

    public String getCertdn()
    {
        return this.certdn;
    }

    public void setCertdn(String certDN)
    {
        this.certdn = certDN;
    }

    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof PlatformBase) || !super.equals(obj)) {
            return false;
        }
        PlatformBase o = (PlatformBase)obj;
        return
            ((fqdn == o.getFqdn()) || (fqdn!=null && o.getFqdn()!=null &&
                                       fqdn.equals(o.getFqdn())))
            &&
            ((certdn==o.getCertdn()) || (certdn!=null && o.getCertdn()!=null &&
                                       certdn.equals(o.getCertdn())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (fqdn != null ? fqdn.hashCode() : 0);
        result = 37*result + (certdn != null ? certdn.hashCode() : 0);

        return result;
    }
}
