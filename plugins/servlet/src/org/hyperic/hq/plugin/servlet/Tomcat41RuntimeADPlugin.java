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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeResourceReport;

import org.hyperic.hq.common.SystemException;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat41RuntimeADPlugin 
    extends ServletDiscoveryPlugin
{                                                  
    static final String CONNECTOR_OBJ = "Catalina:type=Connector,";

    static final String WEBAPP_SERVICE_TYPENAME =
        ServletProductPlugin.TOMCAT_SERVER_NAME + " " +
        ServletProductPlugin.TOMCAT_VERSION_41 + " " +
        ServletProductPlugin.WEBAPP_NAME;

    static final String CONNECTOR_SERVICE_TYPENAME =
        ServletProductPlugin.TOMCAT_SERVER_NAME + " " +
        ServletProductPlugin.TOMCAT_VERSION_41 + " " +
        ServletProductPlugin.CONNECTOR_NAME;

    static Log log = LogFactory.getLog("Tomcat41RuntimeADPlugin");

    public Tomcat41RuntimeADPlugin () {}

    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config) 
        throws PluginException
    {
        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);

        Manifest mBeanInfo = getMBeanInfo(config);
        String host = getHost(config);

        // Generate a server with the same ID
        AIServerExtValue server = generateServer(serverId);

        // Do connector discovery
        ArrayList connectors = discoverConnectors(mBeanInfo, 
                                                  CONNECTOR_SERVICE_TYPENAME);
        // Do webapp discovery
        ArrayList webapps = discoverWebapps(mBeanInfo,
                                            WEBAPP_SERVICE_TYPENAME,
                                            host);

        ArrayList services = new ArrayList();
        services.addAll(connectors);
        services.addAll(webapps);

        server.setAIServiceValues((AIServiceValue[])services.
                                  toArray(new AIServiceValue[0]));
        aiplatform.addAIServerValue(server);
        rrr.addAIPlatform(aiplatform);

        return rrr;
    }

    protected ArrayList discoverConnectors(Manifest mBeanInfo,
                                           String serviceTypeName)
    {
        ArrayList connectors = new ArrayList();
        Set objectNames = mBeanInfo.getEntries().keySet();
        
        for (Iterator i = objectNames.iterator(); i.hasNext();) {
            
            String objectName = (String)i.next();
            if (objectName.startsWith(CONNECTOR_OBJ)) {
                // Found one
                Attributes atts = mBeanInfo.getAttributes(objectName);

                // XXX: We dont support monitoring of the JK connectors.
                //
                String handler = atts.getValue("protocolHandlerClassName");
                if (handler != null &&
                    handler.equals("org.apache.jk.server.JkCoyoteHandler")) {
                    continue;
                }

                String port = atts.getValue("port");
                String scheme = atts.getValue("scheme");

                AIServiceValue service = new AIServiceValue();
                service.setServiceTypeName(serviceTypeName);
                // %serverName% will be substituted on the server side
                service.setName("%serverName%" + " " + port + " " + 
                                serviceTypeName);

                ConfigResponse productConfig = new ConfigResponse();
                ConfigResponse metricConfig = new ConfigResponse();

                try {
                    metricConfig.setValue("connector", scheme);
                    metricConfig.setValue("connectorPort", port);
                    metricConfig.setValue("interval", DEFAULT_INTERVAL);

                    service.setProductConfig(productConfig.encode());
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

                connectors.add(service);
            }
        }

        return connectors;
    }
}
