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

package org.hyperic.hq.bizapp.server.session;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.Sigar;

class SysStats {
    private static final Log _log = LogFactory.getLog(SysStats.class);

    static Properties getCpuMemStats() {
        Sigar sigar = null;
        Properties props = new Properties();
        
        try {
            sigar = new Sigar();
            CpuInfo[] cpus = sigar.getCpuInfoList();
            
            props.setProperty("platform.numCPUs", "" + cpus.length);
            
            if (cpus.length == 0) {
                props.setProperty("platform.speed", "-1");
            } else {
                props.setProperty("platform.speed", "" + cpus[0].getMhz());
            }
            
            props.setProperty("platform.mem", "" + sigar.getMem().getTotal());
            props.setProperty("hq.mem", "" + Runtime.getRuntime().maxMemory());
            return props;
        } catch(Exception e) {
            _log.warn("Error getting sys-stats");
            return props;
        } finally {
            if (sigar != null) 
                sigar.close();
        }
    }
}
