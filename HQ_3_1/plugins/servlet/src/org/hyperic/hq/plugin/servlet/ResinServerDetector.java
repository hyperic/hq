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

package org.hyperic.hq.plugin.servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResinServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    // Process query for 2.x
    // XXX: Check for Apache fronted Resin servers
    static final String PTQL_2 =
        "State.Name.eq=java,Args.*.eq=com.caucho.server.http.HttpServer";

    private Log log =  LogFactory.getLog("ResinServerDetector");

    private static List getServerProcessList(String version)
        throws PluginException
    {
        ArrayList servers = new ArrayList();
        long[] pids = getPids(PTQL_2);

        for (int i = 0; i < pids.length; i++) {

            String[] argv = getProcArgs(pids[i]);
            for (int j = 0; j < argv.length; j++) {
                
                if (argv[j].startsWith("-Dresin.home")) {
                    int idx = argv[j].indexOf("=");
                    servers.add(argv[j].substring(idx + 1));
                }
            }
        }

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();
        List paths = getServerProcessList(getTypeInfo().getVersion());

        for (int i = 0; i < paths.size(); i++) {

            String dir = (String)paths.get(i);

            // Validate
            File conf = new File(dir, "conf");
            File resinConf = new File(conf.getAbsolutePath(), "resin.conf");

            if (resinConf.exists()) {

                ResinConfig config = ResinConfig.getConfig(resinConf);
                
                if (config != null) {
                    
                    List resinServers = config.getServers();

                    for (int j = 0; j < resinServers.size(); j++) {
                        ResinServer server = (ResinServer)resinServers.get(j);

                        ServerResource resource = createServerResource(dir);
                        
                        resource.setName(getPlatformName() + " " +
                                         getTypeInfo().getName() + " " +
                                         server.host + ":" + server.port);
                        resource.setIdentifier(dir + " " + server.host + server.port);

                        ConfigResponse productConfig = new ConfigResponse();
                        productConfig.setValue(JMXRemote.PROP_JMX_URL,
                                               "http://" + server.host + ":" +
                                               server.port + "/");
                        resource.setProductConfig(productConfig);
                        
                        ConfigResponse controlConfig = new ConfigResponse();
                        if (server.id != null) {
                            //For multiple http listeners
                            controlConfig.setValue(ResinServerControlPlugin.
                                                   PROP_ID, server.id);

                        }
                        //Validate control script
                        File script = new File(dir, "bin/httpd.sh");
                        if (script.exists()) {
                            controlConfig.setValue(ResinServerControlPlugin.
                                                   PROP_PROGRAM,
                                                   script.getAbsolutePath());
                        }
                        controlConfig.setValue(ResinServerControlPlugin.
                                               PROP_TIMEOUT, "30");

                        resource.setControlConfig(controlConfig);

                        resource.setMeasurementConfig();
                        
                        servers.add(resource);
                    }
                }
            }
        }   

        return servers;
    }

    protected List discoverServices(ConfigResponse config) 
        throws PluginException
    {
        JMXRemote jmxRemote = new JMXRemote();

        String jmxUrl = config.getValue(JMXRemote.PROP_JMX_URL);
        String user = config.getValue(JMXRemote.PROP_JMX_USER);
        String password = config.getValue(JMXRemote.PROP_JMX_PASS);
        String installPath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        String host;

        jmxRemote.setJmxUrl(jmxUrl);
 
        Manifest mBeanInfo;
        try {
            jmxRemote.init();
            host = jmxRemote.getHost();
            mBeanInfo = jmxRemote.getRemoteInfo();
            jmxRemote.shutdown();
        } catch (Exception e) {
            throw new PluginException("Unable to get MBean info: " +
                                      e.getMessage());
        }

        ArrayList services = new ArrayList();
        Set objectNames = mBeanInfo.getEntries().keySet();
        
        for (Iterator i = objectNames.iterator(); i.hasNext();) {
            String objectName = (String)i.next();

            if (objectName.startsWith("hyperic-hq:") &&
                (objectName.indexOf("type=Context") != -1)) {
                // Found one
                Attributes atts = mBeanInfo.getAttributes(objectName);
                String name = atts.getValue("ContextName");

                ServiceResource service = new ServiceResource();
                service.setType(this, ServletProductPlugin.WEBAPP_NAME);
                service.setServiceName(name);

                ConfigResponse productConfig = new ConfigResponse();
                service.setProductConfig(productConfig);

                ConfigResponse metricConfig = new ConfigResponse();
                metricConfig.setValue(JMXRemote.PROP_HOST, host);
                metricConfig.setValue(JMXRemote.PROP_CONTEXT, name);
                service.setMeasurementConfig(metricConfig);
            
                services.add(service);
            }
        }

        return services;
    }
}


