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

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.IpValue;

/**
 *
 */
public class Ip extends IpBase
{
    private Platform platform;

    /**
     * default constructor
     */
    public Ip()
    {
        super();
    }

    public Ip(IpValue ipv) {
        setAddress(ipv.getAddress());
        setNetmask(ipv.getNetmask());
        setMACAddress(ipv.getMACAddress());
    }

    public Platform getPlatform()
    {
        return this.platform;
    }

    public void setPlatform(Platform platform)
    {
        this.platform = platform;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Ip) || !super.equals(obj)) {
            return false;
        }
        Ip o = (Ip) obj;
        return ((platform == o.getPlatform()) ||
                (platform != null && o.getPlatform() != null &&
                 platform.equals(o.getPlatform())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (platform != null ? platform.hashCode() : 0);

        return result;
    }
}
