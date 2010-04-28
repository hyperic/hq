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

package org.hyperic.hq.plugin.xen;

import org.hyperic.hq.product.PluginException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostCpu;
import com.xensource.xenapi.HostMetrics;

public class XenHostCollector extends XenCollector {

    public void collect() {
        try {
            Connection conn = connect();
            Host host = XenUtil.getHost(conn, getProperties());
            setAvailability(true);
            HostMetrics metrics = host.getMetrics(conn);
            HostMetrics.Record record = metrics.getRecord(conn);
            long free = record.memoryFree.longValue();
            long total = record.memoryTotal.longValue();
            long used = total - free;
            setValue("MemoryFree", free);
            setValue("MemoryTotal", total);
            setValue("MemoryUsed", used);

            double usage = 0;
            for (HostCpu cpu: host.getHostCPUs(conn)) {
                usage += cpu.getUtilisation(conn);
            }
            setValue("CPUUsage", usage);
        } catch (PluginException e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
    }
}
