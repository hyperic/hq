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

package org.hyperic.hq.plugin.websphere;

import java.io.File;
import java.util.List;

import java.util.Properties;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.websphere.ejs.EjsWebsphereRuntimeDiscoverer;

/**
 * WebSphere AE 4.0 Admin server detector.
 */
public class WebsphereDetectorAE4
    extends WebsphereDetector {

    private static final String PTQL_QUERY =
        "State.Name.eq=java,Args.*.eq=com.ibm.ejs.sm.server.AdminServer";

    private static final String ADMIN_PORT =
        "com.ibm.ejs.sm.adminServer.bootstrapPort";

    private static final String ADMIN_HOST =
        "com.ibm.ejs.sm.adminServer.bootstrapHost";

    private Properties adminProps;
    private Properties setupProps;
    private EjsWebsphereRuntimeDiscoverer discoverer = null;

    public List discoverServers(ConfigResponse config) 
        throws PluginException {

        if (this.discoverer == null) {
            this.discoverer = new EjsWebsphereRuntimeDiscoverer();
        }

        return this.discoverer.discoverServers(config);
    }

    protected String getProcessQuery() {
        return PTQL_QUERY;
    }

    private Properties getAdminProps(File path) {
        File adminCfg = new File(path, "bin/admin.config");
        return loadProps(adminCfg);
    }

    private Properties getSetupProps(File path) {
        File adminCfg = new File(path, "bin/setupCmdLine.sh");
        return loadProps(adminCfg);
    }

    protected String getAdminHost() {
        return this.adminProps.getProperty(ADMIN_HOST,
                                           "localhost");
    }

    protected String getAdminPort() {
        return this.adminProps.getProperty(ADMIN_PORT,
                                           "900");
    }

    protected String getNodeName() {
        String node = this.setupProps.getProperty("COMPUTERNAME");
        if (node != null) {
            return getNodeNameFromFQDN(node);
        }
        return node;
    }

    protected String getStartupScript() {
        return "bin/startupServer.sh";
    }

    protected void initDetector(File root) {
        this.adminProps = getAdminProps(root);
        this.setupProps = getSetupProps(root);
    }

    public static void main(String[] args) {
        List servers = getServerProcessList(PTQL_QUERY);
        for (int i=0; i<servers.size(); i++) {
            System.out.println(servers.get(i));
        }
    }
}
