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

package org.hyperic.hq.plugin.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

public class VMwareGuestInfo {

    private static final String PTQL_QUERY =
        "State.Name.eq=" +
        (OperatingSystemReflection.IS_WIN32() ?
         "VMwareService" : "vmware-guestd");

    private static String findGuestd(Sigar sigar) {
        long[] pids;
        try {
            pids = ProcessFinder.find(sigar, PTQL_QUERY);
        } catch (SigarException e) {
            return null;
        }
        
        for (int i=0; i<pids.length; i++) {
            try {
                return sigar.getProcExe(pids[i]).getName();
            } catch (SigarException e) {
                //fallthru
            }

            try {
                String[] args = sigar.getProcArgs(pids[i]);
                if (args.length > 0) {
                    return args[0];
                }
            } catch (SigarException e) {}
        }

        return null;
    }

    //get vars passed in by VMware host
    static Map getGuestInfo(Sigar sigar) {
        Map info = new HashMap();

        String guestd = findGuestd(sigar);

        if (guestd == null) {
            return null;
        }

        String[] props = {
            ProductPlugin.PROP_PLATFORM_NAME,
            ProductPlugin.PROP_PLATFORM_FQDN,
            ProductPlugin.PROP_PLATFORM_IP,
            ProductPlugin.PROP_PLATFORM_ID,
        };

        String[] argv = {
            guestd, "--cmd", null
        };

        for (int i=0; i<props.length; i++) {
            String key = props[i];
            argv[2] = "info-get guestinfo.hq." + key;
            Process proc;
            BufferedReader in = null;
            String line;

            try {
                proc = Runtime.getRuntime().exec(argv);
                in = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));
                while ((line = in.readLine()) != null) {
                    //Seen on windows:
                    //Warning: GuestApp: no value for option 'log'
                    if (line.startsWith("Warning:") ||
                        line.startsWith("No value found"))
                    {
                        continue;
                    }
                    info.put(key, line);
                }
            } catch (IOException e) {
                break;
            } finally {
                if (in != null) {
                    try { in.close(); } catch (IOException e) {}
                }
            }
        }

        //Sanity checks.
        String id =
            (String)info.get(ProductPlugin.PROP_PLATFORM_ID);
        if (id == null) {
            return null;
        }

        try {
            Long.parseLong(id);
        } catch (NumberFormatException e) {
            //Windows output has Warning string if not set.
            return null;
        }

        return info;
    }

    public static void main(String[] args) {
        Sigar sigar = new Sigar();
        System.out.println(getGuestInfo(sigar));
        sigar.close();
    }
}
