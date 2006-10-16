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
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RtPlugin;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.servlet.client.JMXRemote;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ServletDiscoveryPlugin 
    implements RuntimeDiscoverer {

    private static final String WEBAPP_OBJ = "hyperic-hq:type=Context,";
    private static final String WEBAPP_OBJ_COMPAT = "covalent-cam:type=Context,";

    protected static final String DEFAULT_INTERVAL = "60";
    
    private static Log log = LogFactory.getLog("ServletDiscoveryPlugin");

    public ServletDiscoveryPlugin () {}

    public abstract 
    RuntimeResourceReport discoverResources(int serverId, 
                                            AIPlatformValue aiplatform,
                                            ConfigResponse config)
    throws PluginException;

    protected String getHost(ConfigResponse config)
        throws PluginException {

        JMXRemote jmxRemote = new JMXRemote();
        String jmxUrl = config.getValue(JMXRemote.PROP_JMX_URL);
        jmxRemote.setJmxUrl(jmxUrl);

        try {
            jmxRemote.init();
        } catch (Exception e) {
            throw new PluginException("Unable to get MBean info: " + 
                                      e.getMessage());
        }

        return jmxRemote.getHost();
    }
    
    protected Manifest getMBeanInfo(ConfigResponse config)
        throws PluginException 
    {
        // Initialize the JMXRemote object
        JMXRemote jmxRemote = new JMXRemote();
        String jmxUrl = config.getValue(JMXRemote.PROP_JMX_URL);
        String user = config.getValue(JMXRemote.PROP_JMX_USER);
        String password = config.getValue(JMXRemote.PROP_JMX_PASS);
        String installPath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        String host;

        jmxRemote.setJmxUrl(jmxUrl);
        // XXX: need to add authentication supprt
        // jmxRemote.setUser(user);
        // jmxRemote.setPass(password);
 
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

        return mBeanInfo;
    }

    protected AIServerExtValue generateServer(int serverId)
    {
        AIServerExtValue server = new AIServerExtValue();
        server.setId(new Integer(serverId));
        server.setPlaceholder(true);
        return server;
    }

    protected ArrayList discoverWebapps(Manifest mBeanInfo,
                                        String serviceTypeName, String host)
    {
        ArrayList webapps = new ArrayList();
        Set objectNames = mBeanInfo.getEntries().keySet();
        for (Iterator i = objectNames.iterator(); i.hasNext();) {
            
            String objectName = (String)i.next();
            if (objectName.startsWith(WEBAPP_OBJ) ||
                objectName.startsWith(WEBAPP_OBJ_COMPAT)) {
                // Found one
                Attributes atts = mBeanInfo.getAttributes(objectName);
                String name = atts.getValue("ContextName");
                String base = atts.getValue("DocBase");
                String logDir = atts.getValue("ResponseTimeLogDir");

                AIServiceValue service = new AIServiceValue();
                service.setServiceTypeName(serviceTypeName);

                // %serverName% will be substituted on the server side
                service.setName("%serverName%" + " " +
                                name + " " + serviceTypeName);

                ConfigResponse productConfig = new ConfigResponse();
                ConfigResponse metricConfig = new ConfigResponse();
                ConfigResponse controlConfig = new ConfigResponse();
                ConfigResponse rtConfig = null;

                try {
                    metricConfig.setValue(JMXRemote.PROP_HOST, host);
                    metricConfig.setValue(JMXRemote.PROP_CONTEXT, name);
                    // XXX: this should be an attribute
                    metricConfig.setValue("service", "Tomcat-Standalone");
                    metricConfig.setValue("interval", DEFAULT_INTERVAL);
                    
                    // This sux.
                    if ((serviceTypeName.indexOf("Tomcat 4.1") != -1) ||
                        (serviceTypeName.indexOf("Tomcat 5") != -1)) {
                        // Always use forward slashes
                        String serviceName = name.replace('\\', '/');
                        controlConfig.setValue("serviceName", "//" + 
                                               host + serviceName);
                    } else if (serviceTypeName.indexOf("JRun 4.x") != -1) {
                        // Jrun 
                        controlConfig.setValue("serviceName", name.
                                               substring(1, name.length()));
                    } else {
                        // Tomcat 4.0
                        controlConfig.setValue("serviceName", name);
                    }
                    controlConfig.setValue("docBase", "file:" + base);

                    String rtLogDir = atts.getValue("responseTimeLogDir");
                    if (rtLogDir == null) {
                        this.log.debug("ResponseTimeLogDir property not " +
                                       "found. Skipping response time " +
                                       "auto configuration.");
                    } else {
                        // Response Time AutoConfiguration 
                        // unfortunately, the name is prefixed with a \ or / 
                        // depending on the host OS
                        String rtFileName = name.substring(
                                                (name.indexOf(File.separator) + 1));
                        rtConfig = RtPlugin.getConfig(rtFileName, rtLogDir);
                        service.setResponseTimeConfig(rtConfig.encode());
                    }

                    service.setProductConfig(productConfig.encode());
                    service.setControlConfig(controlConfig.encode());
                    service.setMeasurementConfig(metricConfig.encode());
                } catch (EncodingException e) {
                    throw new SystemException("Error generating config.", 
                                                 e);
                } catch (InvalidOptionValueException e) {
                    throw new SystemException("Error generating config.", 
                                                 e);
                } catch (InvalidOptionException e) {
                    throw new SystemException("Error generating config.", 
                                                 e);
                }

                webapps.add(service);
            }
        }

        return webapps;
    }
}
