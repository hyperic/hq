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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.PluginException;

import org.hyperic.hq.plugin.websphere.jmx.WebsphereRuntimeDiscoverer5;

/**
 * WebSphere 5.0 Admin server detector.
 */
public class WebsphereDetector5
    extends WebsphereDetector {

    private static final String PTQL_QUERY =
        "State.Name.eq=java,Args.*.eq=com.ibm.ws.runtime.WsServer";

    private static final String SOAP_PORT_EXPR =
        "//specialEndpoints[@endPointName=\"SOAP_CONNECTOR_ADDRESS\"]//@port";

    private WebsphereRuntimeDiscoverer5 discoverer = null;
    private String node = null;
    private String port = null;
    private String installpath;

    protected List discoverServers(ConfigResponse config)
        throws PluginException {

        if (this.discoverer == null) {
            String version = getTypeInfo().getVersion();
            this.discoverer = new WebsphereRuntimeDiscoverer5(version);
        }

        //for use w/ -jar hq-product.jar or agent.properties
        Properties props = getManager().getProperties();
        String[] credProps = {
            WebsphereProductPlugin.PROP_USERNAME,
            WebsphereProductPlugin.PROP_PASSWORD,
            WebsphereProductPlugin.PROP_SERVER_NODE
        };
        for (int i=0; i<credProps.length; i++) {
            String name = credProps[i];
            String value =
                props.getProperty(name, config.getValue(name));
            if (value == null) {
                //prevent NPE since user/pass is not required
                value = "";
            }
            config.setValue(name, value);
        }

        return this.discoverer.discoverServers(config);
    }

    protected String getProcessQuery() {
        return PTQL_QUERY;
    }

    protected String getAdminHost() {
        return getManager().getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                                        "localhost");
    }

    private File findServerIndex() {
        //any serverindex.xml will do.
        File[] cells =
            new File(this.installpath + "/config/cells").listFiles();

        if (cells == null) {
            return null;
        }

        for (int i=0; i<cells.length; i++) {
            File[] nodes =
                new File(cells[i], "nodes").listFiles();

            if (nodes == null) {
                continue;
            }

            for (int j=0; j<nodes.length; j++) {
                File index = new File(nodes[j], "serverindex.xml");
                if (index.exists() && index.canRead()) {
                    return index;
                }
            }
        }

        return null;
    }

    protected String getAdminPort() {
        if (this.port != null) {
            return this.port;
        }
        final String prop =
            WebsphereProductPlugin.PROP_ADMIN_PORT;

        File index = findServerIndex();

        if (index != null) {
            this.port =
                getXPathValue(index, SOAP_PORT_EXPR);
            getLog().debug("Configuring " + prop + "=" + this.port +
                           " from: " + index);
        }

        if (this.port == null) {
            this.port =
                getManager().getProperty(prop, "8880");
        }

        return this.port;
    }

    protected String getNodeName() {
        return this.node;
    }

    protected String getStartupScript() {
        if (isWin32()) {
            return "bin\\startNode.bat";
        }
        else {
            return "bin/startNode.sh";
        }
    }

    protected boolean isServiceControl() {
        return false;
    }

    protected void initDetector(File root) {
        this.installpath = root.getAbsolutePath();

        //sadly, the setupCmdLine script is the
        //best way to determine the node name
        final String NODE_PROP = "WAS_NODE=";

        File cmdline =
            new File(root, "bin/setupCmdLine" +
                     getScriptExtension());
        Reader reader = null;

        try {
            reader = new FileReader(cmdline);
            BufferedReader buffer =
                new BufferedReader(reader);
            String line;

            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                int ix = line.indexOf(NODE_PROP);
                if (ix == -1) {
                    continue;
                }
                this.node =
                    line.substring(ix + NODE_PROP.length());
                break;
            }
        } catch (IOException e) {
            
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
    }

    public static String getRunningInstallPath() {
        return getRunningInstallPath(PTQL_QUERY);
    }

    static List getServerProcessList() {
        return getServerProcessList(PTQL_QUERY);
    }

    public static void main(String[] args) {
        List servers = getServerProcessList(PTQL_QUERY);
        for (int i=0; i<servers.size(); i++) {
            System.out.println(servers.get(i));
        }
    }
}
