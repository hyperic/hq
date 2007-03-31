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

package org.hyperic.hq.plugin.mule;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;

public class MuleServerDetector extends MxServerDetector {
    private static final String URL_EXPR =
        "//property[@name=\"connectorServerUrl\"]/@value";

    private static final String ID_EXPR =
        "/mule-configuration/@id";

    private static final String PROP_WRAPPER_PID =
        "-Dwrapper.pid=";

    private static final String PROP_WRAPPER_CWD =
        "wrapper.working.dir=";

    private static final String PROP_DOMAIN = "domain";
    private static final String PROP_CONFIG = "mule.config";

    private static final String DOMAIN_PREFIX = "Mule";

    private Map _instances = new HashMap();

    //attempt to find the xml config file for this server
    //1.3 has a wrapper parent process with the working.dir
    //we use to resolve relative -config files
    private void configureProcess(MxProcess process) {
        String[] args = process.getArgs();
        String wrapperPid = null;
        String config = null;
        final String propHome = getProcHomeProperty();
        final String defPropHome = "-D" + propHome + "=";
        String home = null;

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith(PROP_WRAPPER_PID)) {
                wrapperPid = arg.substring(PROP_WRAPPER_PID.length());
            }
            else if (arg.equals("-config")) {
                config = args[i+1];
            }
            else if (arg.startsWith(defPropHome)) {
                home = arg.substring(defPropHome.length());
            }
        }

        if (config == null) {
            return;
        }

        File configFile = new File(config);
        if (!configFile.isAbsolute()) {
            if (wrapperPid != null) {
                args =
                    getProcArgs(Long.parseLong(wrapperPid));
                for (int i=0; i<args.length; i++) {
                    String arg = args[i];
                    if (!arg.startsWith(PROP_WRAPPER_CWD)) {
                        continue;
                    }
                    arg = arg.substring(PROP_WRAPPER_CWD.length());
                    configFile = new File(arg, config);
                    break;
                }
            }
        }

        if (configFile.exists()) {
            try {
                Document doc = getDocument(configFile);
                String url = getXPathValue(doc, URL_EXPR);
                process.setURL(url);
                getLog().debug(configFile + " jmx.url=" + url);
                String id = getXPathValue(doc, ID_EXPR);
                HashMap instance = new HashMap();
                instance.put(PROP_DOMAIN, DOMAIN_PREFIX + "." + id);
                instance.put(PROP_CONFIG,
                             getCanonicalPath(configFile.getPath()));
                if (home != null) {
                    instance.put(propHome, home);
                }
                _instances.put(url, instance);
            } catch (IOException e) {
                getLog().error("Error parsing: " + configFile, e);
            }
        }
        else {
            getLog().debug(configFile + " does not exist");
        }
    }

    protected List getServerProcessList() {
        //super class will find the process list
        //we go through each to configure jmx.url
        //from the -config file
        List procs = super.getServerProcessList();

        for (int i=0; i<procs.size(); i++) {
            configureProcess((MxProcess)procs.get(i));
        }

        return procs;
    }

    private boolean isMuleDomain(ConfigResponse config) {
        final String serverInfo =
            "Mule:type=org.mule.ManagementContext,name=MuleServerInfo";

        try {
            ObjectName name = new ObjectName(serverInfo);
            MxUtil.getMBeanServer(config.toProperties()).
                getAttribute(name, "ServerId");
            return true;
        } catch (Exception e) {}        

        return false;
    }

    protected void setProductConfig(ServerResource server,
                                    ConfigResponse config) {

        String url = config.getValue(MxUtil.PROP_JMX_URL);
        Map instance = (Map)_instances.get(url);

        if (instance != null) {
            config.merge(new ConfigResponse(instance), true);
        }

        super.setProductConfig(server, config);

        //1.3   uses Mule:
        //1.3.1 uses Mule.$serverId:
        if (isMuleDomain(config)) {
            config.setValue(PROP_DOMAIN, DOMAIN_PREFIX);
            server.setProductConfig(config);
        }
    }
}
