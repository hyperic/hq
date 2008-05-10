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

package org.hyperic.hq.product.jmx;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.SigarException;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

/**
 * Detector for Sun 1.5+ JVMs with remote JMX enabled 
 */
public class SunMxServerDetector extends MxServerDetector {

    //XXX can also find this stuff reading hotspot perf data
    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        setPlatformConfig(platformConfig);

        //XXX disabled by default for now
        String enabled =
            getManager().getProperty("jmx.sun.discover", "false");
        if (enabled.equals("false")) {
            return null;
        }

        List servers = new ArrayList();
        long[] pids = getPids(PROC_JAVA);

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];
            String[] args = getProcArgs(pid);
            ConfigResponse config = new ConfigResponse();

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (configureMxURL(config, arg)) {
                    String name = null;
                    try {
                        name =
                            ProcUtil.getJavaMainClass(getSigar(), pid);
                    } catch (SigarException e) {
                    }

                    if ((name == null) ||
                        (name = name.trim()).length() == 0)
                    {
                        String port = parseMxPort(arg);                        
                        if (port == null) {
                            name = " (local)";
                        }
                        else {
                            name = "@ " + port;
                        }
                    }
                    else {
                        //jmx-plugin.xml may map the classname to product name
                        String productName =
                            getPluginProperty("MAIN-CLASS." + name);
                        if (productName != null) {
                            name = productName;
                        }
                    }

                    String identifier = args[j];
                    String installpath = getProcExe(pid);
                    ServerResource server = newServerResource(installpath);
                    if (!server.getIdentifier().equals(installpath)) {
                        //only if INVENTORY_ID was not set
                        server.setIdentifier(identifier);    
                    }

                    //setName() before discoverServerConfig() to allow user override
                    server.setName(server.getName() + " " + name);
                    discoverServerConfig(server, pid);

                    getLog().debug(server.getName() + " identifier=" + identifier);
                    setProductConfig(server, config);
                    server.setMeasurementConfig();

                    servers.add(server);
                    break;
                }
            }
        }

        return servers;
    }
}
