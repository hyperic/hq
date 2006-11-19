package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.IpValue;

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

public abstract class IpBase extends AppdefBean
{
    protected String address;
    private String netmask;
    private String MACAddress;
    private IpValue ipValue = new IpValue();

    public String getAddress()
    {
        return this.address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask(String netmask)
    {
        this.netmask = netmask;
    }

    public String getMACAddress()
    {
        return this.MACAddress;
    }

    public void setMACAddress(String MACAddress)
    {
        this.MACAddress = MACAddress;
    }

    /**
     * convenience method for copying simple values
     * from the legacy EJB Value Object
     *
     * @deprecated
     * @param valueHolder
     */
    public void setIpValue(IpValue valueHolder)
    {
        setAddress( valueHolder.getAddress() );
        setNetmask( valueHolder.getNetmask() );
        setMACAddress( valueHolder.getMACAddress() );
    }

    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) Ip Object instead
     * @return
     */
    public IpValue getIpValue()
    {
        ipValue.setAddress(getAddress());
        ipValue.setNetmask(getNetmask());
        ipValue.setMACAddress(getMACAddress());
        ipValue.setId(getId());
        ipValue.setMTime(getMTime());
        ipValue.setCTime(getCTime());
        return ipValue;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof IpBase) || !super.equals(obj)) {
            return false;
        }
        IpBase o = (IpBase) obj;
        return
               ((address == o.getAddress()) ||
                (address != null && o.getAddress() != null &&
                 address.equals(o.getAddress())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (address != null ? address.hashCode() : 0);

        return result;
    }
}
