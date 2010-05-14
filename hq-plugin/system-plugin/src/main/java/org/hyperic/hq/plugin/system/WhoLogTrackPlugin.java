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

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Who;

public class WhoLogTrackPlugin implements Runnable {

    private LogTrackPlugin plugin;
    private Sigar sigar;
    private long lastTime;

    public WhoLogTrackPlugin(LogTrackPlugin plugin) {
        this.plugin = plugin;
        this.sigar = new Sigar();
        this.lastTime = System.currentTimeMillis();
    }

    public void shutdown() {
        this.sigar.close();
    }
    
    public void run() {
        Who[] logins;
        try {
            logins = this.sigar.getWhoList();
        } catch (SigarException e) {
            return;
        }

        for (int i=0; i<logins.length; i++) {
            Who who = logins[i];

            long time = who.getTime() * 1000;
            if (time < this.lastTime) {
                //discard logins that happened before we started
                //or that we have already reported
                continue;
            }

            String msg =
                "login: " + who.getUser() + " " + who.getDevice();

            String host = who.getHost();
            if (host.length() != 0) {
                msg += " (" + host + ")";
            }

            this.plugin.reportEvent(time,
                                    LogTrackPlugin.LOGLEVEL_INFO,
                                    "system",
                                    msg);
        }

        this.lastTime = System.currentTimeMillis();
    }
}
