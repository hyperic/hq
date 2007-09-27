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

package org.hyperic.hq.plugin.bind;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BindServerDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final String PTQL_QUERY =
        "State.Name.eq=named";

    private Log log =  LogFactory.getLog("BindServerDetector");

    private static String getServerProcess() {
        long[] pids = getPids(PTQL_QUERY);
        
        // Should only find a single named process.
        if (pids.length == 1) {
            String exe = getProcExe(pids[0]);
            
            if (exe != null) 
                return exe;
        }

        return null;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();

        String named = getServerProcess();

        if (named != null) {
            // Bind is running
            // Assume rndc is also installed in the same location.
            String rndc =
                new File(getParentDir(named, 1),"rndc").getAbsolutePath();

            // Get the version from 'named -version'
            String version;

            try {
                String[] argv = new String[] { named, "-version" };
                Process proc = Runtime.getRuntime().exec(argv);

                BufferedReader in = 
                    new BufferedReader(new InputStreamReader(proc.
                                                             getInputStream()));
                version = in.readLine();
                if (version == null) {
                    throw new PluginException("Unable to determine Bind " +
                                              "version");
                }
                if (version.indexOf("9.") == -1) {
                    this.log.info("Found unsupported version of Bind (" +
                                  version + ")");
                    return servers;
                }

                try {
                    in.close();
                } catch (IOException e) {}
            } catch (IOException e) {
                throw new PluginException("Unable to dermine Bind " +
                                          "version: " + e);
            }

            ServerResource server =  createServerResource(named);
            server.setName(getPlatformName() + " " + version);

            ConfigResponse productConfig = new ConfigResponse();

            productConfig.setValue(BindMeasurementPlugin.PROP_RNDC, rndc);
            productConfig.setValue(BindMeasurementPlugin.PROP_NAMED_STATS,
                                   "/var/named/named.stats");
            productConfig.setValue("process.query", PTQL_QUERY);
            server.setProductConfig(productConfig);
            server.setMeasurementConfig();

            servers.add(server);
        }

        return servers;
    }
}
