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

package org.hyperic.hq.plugin.ntp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class NTPServerDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static Log log = LogFactory.getLog("NTPServerDetector");

    private static final String[] NTPD_PATHS = {
        "/usr/sbin/ntpd",
        "/usr/bin/ntpd",
        "/sbin/ntpd",
        "/bin/ntpd"
    };

    private static final String[] NTPD_PID_PATHS = {
        "/var/run/ntpd.pid"
    };

    private static File getPidFile() {
        
        for (int i=0; i<NTPD_PID_PATHS.length; i++) {
            File ntpdPid = new File(NTPD_PID_PATHS[i]);
            if (ntpdPid.exists()) {
                return ntpdPid;
            }
        }
        
        return null;
    }

    private static String getServerProcess(String query) {
        long[] pids = getPids(query);
        
        // Should only find a single ntpd process.
        if (pids.length == 1) {
            String exe = getProcExe(pids[0], "ntpd");
            
            if (exe != null) {
                if (!exe.startsWith("/")) {
                    // We know it's running, but cannot determine from
                    // the process table the location.  Try a few known
                    // locations
                    for (int i=0; i<NTPD_PATHS.length; i++) {
                        File ntpd = new File(NTPD_PATHS[i]);
                        if (ntpd.exists()) {
                            return NTPD_PATHS[i];
                        }
                    }

                    log.error("Unable to determine ntpd location " +
                              "from process table");
                    return null;
                }

                return exe;
            }
        }

        return null;
    }

    private String findNtpdc(String path) {
        File ntpd = new File(path);
        //(ntpd | xntpd) + "c"
        String program = ntpd.getName() + "c";
        File ntpdc = new File(ntpd.getParent(), program);
        if (!ntpdc.exists()) {
            ntpdc = new File("/usr/bin", program);
            if (!ntpd.exists()) {
                return program; //to be resolved by ExecutableProcess
            }
        }
        return ntpdc.getPath();
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();
        String ntpd;
        String query;

        // Check pid file
        File ntpdPid = getPidFile();

        if (ntpdPid != null) {
            query = "Pid.PidFile.eq=" + ntpdPid.getAbsolutePath();
        } else {
            query = "State.Name.eq=ntpd";
        }

        ntpd = getServerProcess(query);

        if (ntpd != null) {
            // ntpd is running
            String ntpdc = findNtpdc(ntpd);

            ServerResource server = createServerResource(ntpd);

            ConfigResponse productConfig = new ConfigResponse();

            productConfig.setValue(NTPDCollector.PROP_NTPDC, ntpdc);
            productConfig.setValue("process.query", query);
            productConfig.setValue(NTPDCollector.PROP_TIMEOUT, "1");
            server.setProductConfig(productConfig);

            server.setMeasurementConfig();

            servers.add(server);
        }

        return servers;
    }
}
