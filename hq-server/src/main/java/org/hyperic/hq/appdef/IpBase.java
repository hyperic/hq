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

package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.IpValue;

public abstract class IpBase extends AppdefBean
{
    protected String _address;
    private String _netmask;
    private String _macAddress;
    private IpValue _ipValue = new IpValue();

    public String getAddress() {
        return _address;
    }

    public void setAddress(String address) {
        _address = address;
    }

    public String getNetmask() {
        return _netmask;
    }

    public void setNetmask(String netmask) {
        _netmask = netmask;
    }

    public String getMacAddress() {
        return _macAddress;
    }

    public void setMacAddress(String MACAddress) {
        _macAddress = MACAddress;
    }

    /**
     * convenience method for copying simple values
     * from the legacy Value Object
     *
     * @deprecated
     */
    public void setIpValue(IpValue valueHolder) {
        setAddress(valueHolder.getAddress());
        setNetmask(valueHolder.getNetmask());
        setMacAddress(valueHolder.getMACAddress());
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) Ip Object instead
     */
    public IpValue getIpValue()
    {
        _ipValue.setAddress(getAddress());
        _ipValue.setNetmask(getNetmask());
        _ipValue.setMACAddress(getMacAddress());
        _ipValue.setId(getId());
        _ipValue.setMTime(getMTime());
        _ipValue.setCTime(getCTime());
        return _ipValue;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof IpBase) || !super.equals(obj)) {
            return false;
        }
        IpBase o = (IpBase) obj;
        return
               ((_address == o.getAddress()) ||
                (_address != null && o.getAddress() != null &&
                 _address.equals(o.getAddress())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (_address != null ? _address.hashCode() : 0);

        return result;
    }
}
