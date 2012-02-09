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
package org.hyperic.hq.plugin.openldap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.util.config.ConfigResponse;

public class OpenLDAPServerDetector
        extends ServerDetector
        implements AutoServerDetector {

    private static final String SERVER_NAME = "OpenLDAP";
    // generic process name, generic server daemon
    private static final String PROCESS_NAME = "slapd";
    // this PTQL query matches the PROCESS_NAME and returns the parent process id
    private static final String PTQL_QUERY =
            "State.Name.re=" + PROCESS_NAME + ",State.Name.Pne=$1,Args.0.re=.*" + PROCESS_NAME + "$";
    private final Log log = getLog();

    public List getServerResources(ConfigResponse platformConfig)
            throws PluginException {
        List servers = new ArrayList();
        List paths = getServerProcessList();
        for (int i = 0; i < paths.size(); i++) {
            String dir = (String) paths.get(i);
            List found = getServerList(dir);
            if (!found.isEmpty()) {
                servers.addAll(found);
            }
        }
        return servers;
    }

    private List getServerProcessList() {
        List servers = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        log.debug("[getServerProcessList] found '" + pids.length + "' pids");
        for (int i = 0; i < pids.length; i++) {
            String exe = getProcExe(pids[i]);
            if (exe == null) {
                log.debug("[getServerProcessList] no info for pid '" + pids.length + "'");
                continue;
            }
            File binary = new File(exe);
            if (!binary.isAbsolute()) {
                log.debug("[getServerProcessList] Process='" + binary + "' pid='" + pids.length + "' is not a valid process");
                continue;
            }
            servers.add(binary.getAbsolutePath());
        }
        return servers;
    }

    public List getServerList(String path) throws PluginException {
        List servers = new ArrayList();
        String installdir = getParentDir(path, 2);
        String version = "";

        // Only check the binaries if they match the path we expect
        if (path.contains(PROCESS_NAME)) {
            File versionFile = new File(installdir + "/lib/libldap-2.3.so.0");
            boolean ok = versionFile.exists();
            if (ok) {
                version = "2.3.x";
            }
            log.debug("process=" + path + " version=" + version + " (" + versionFile + " " + (ok ? "found" : "NOT FOUND") + ")");

            ServerResource server = createServerResource(installdir);
            // Set custom properties
            ConfigResponse cprop = new ConfigResponse();
            cprop.setValue("version", version);
            server.setCustomProperties(cprop);
            //server.setProductConfig();
            setProductConfig(server, new ConfigResponse());
            // sets a default Measurement Config property with no values
            server.setMeasurementConfig();
            server.setName(getPlatformName() + " " + SERVER_NAME + " " + version);
            servers.add(server);
        } else {
            log.debug("process=" + path + " is not a OpenLdap)");
        }
        return servers;
    }
}
