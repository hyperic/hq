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

package org.hyperic.hq.plugin.netservices;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.RPC;

public class RPCCollector extends NetServicesCollector {

    static final String RPC_VERSION = "2";
    static final String PROGRAM_NFS = "nfs";
    private static final Map PROGRAMS = new HashMap();

    static {
        PROGRAMS.put(PROGRAM_NFS, new Long(100003));
    }

    private long program, version;

    protected void init() throws PluginException {
        if (GenericPlugin.isWin32()) {
            String msg = "This service is not supported on Windows";
            throw new PluginException(msg);
        }
        super.init();

        String program = getProperty("program");
        Long pnum = (Long)PROGRAMS.get(program);
        if (pnum != null) {
            this.program = pnum.longValue();
        }
        else {
            this.program = RPC.getProgram(program);
        }

        String version = getProperty("version");
        try {
            this.version = Long.parseLong(version);
        } catch (NumberFormatException e) {
            //XXX workaround for cmd-line where "version"
            //is overwritten w/ the hq version
            this.version = 2;
        }

        setSource("portmapper@" + getHostname());
    }

    public void collect() {

        startTime();

        String address;
        
        try {
            address =
                InetAddress.getByName(getHostname()).getHostAddress();
        } catch (UnknownHostException e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
            return;
        }

        int rc =
            RPC.ping(address,
                     NetFlags.CONN_UDP,
                     this.program,
                     this.version);
        endTime();

        if (rc == 0) {
            setAvailability(true);
        }
        else {
            setAvailability(false);
            setErrorMessage(RPC.strerror(rc));
        }
    }
}
