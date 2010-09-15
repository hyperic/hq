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

package org.hyperic.hq.plugin.system;

import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarException;

public class NetInterfaceData {

    private NetInterfaceConfig _config;
    private NetInterfaceStat _stat;

    private NetInterfaceData() {}

    public static NetInterfaceData gather(SigarProxy sigar, String name)
        throws SigarException {
    
        NetInterfaceData data = new NetInterfaceData();
        data._config = sigar.getNetInterfaceConfig(name);
        
        try {
            data._stat = sigar.getNetInterfaceStat(name);
        } catch (SigarException e) {
            // _stat is null
        }
        
        return data;
    }

    public NetInterfaceConfig getConfig() {
        return _config;
    }

    public NetInterfaceStat getStat() {
        return _stat;
    }
}
