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

package org.hyperic.hq.plugin.vmware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMwareException;
import org.hyperic.sigar.vmware.VMwareServer;

public class VMwareProductInfo {
    private static Map cache = new HashMap();
    public String name;
    public String version;
    public String release;
    public int build;
    public int major;
    public int minor;
    public int rev;

    public String toString() {
        return "VMware " + this.name + " " +
               this.major + ".x";
    }

    public static VMwareProductInfo getInfo(Properties props)
        throws VMwareException {

        VMwareProductInfo info = (VMwareProductInfo)cache.get(props);
        if (info == null) {
            synchronized (VMwareConnectParams.LOCK) {
                info = getProductInfo(props);
            }
            cache.put(props, info);
        }

        return info;
    }

    private static VMwareProductInfo getProductInfo(Properties props)
        throws VMwareException {

        VMwareProductInfo info = null;
        VMwareConnectParams params =
            new VMwareConnectParams(props);
        VMwareServer server = new VMwareServer();
        server.connect(params);

        List names = server.getRegisteredVmNames();

        if (names.size() != 0) {
            String name = (String)names.get(0);
            VM vm = new VM();
            vm.connect(params, name);

            info = new VMwareProductInfo();

            info.name =
                VM.PRODUCTS[vm.getProductInfo(VM.PRODINFO_PRODUCT)];

            info.major = 
                vm.getProductInfo(VM.PRODINFO_VERSION_MAJOR);

            info.minor = 
                vm.getProductInfo(VM.PRODINFO_VERSION_MINOR);

            info.rev = 
                vm.getProductInfo(VM.PRODINFO_VERSION_REVISION);

            info.build =
                vm.getProductInfo(VM.PRODINFO_BUILD);

            info.version = info.major + "." + info.minor;

            info.release = info.version + "." + info.rev;

            vm.disconnect();
            vm.dispose();
        }

        server.disconnect();
        server.dispose();
        params.dispose();

        return info;
    }
}
