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

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.PortConfigOption;
import org.hyperic.util.config.StringConfigOption;

public class Tomcat41MeasurementPlugin
    extends ServletMeasurementPlugin
{
    // Local properties
    private static final String PROP_TOMCAT_CONNECTOR      = "connector";
    private static final String PROP_TOMCAT_CONNECTOR_PORT = "connectorPort";
    private static final String PROP_TOMCAT_CONNECTOR_NAME = "connectorName";
    private static final String PROP_TOMCAT_CONNECTOR_PATH = "connectorPath";

    private static final String PROP_TOMCAT_SERVICE        = "service";

    // Valid values for PROP_TOMCAT_CONNECTOR
    static final String[] CONNECTORS = { "jk", "http", "https" };

    public Tomcat41MeasurementPlugin() {
        super(ServletProductPlugin.NAME);
    }
        
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        String name = info.getName();
        ConfigSchema schema = new ConfigSchema();

        StringConfigOption servlet =
            new StringConfigOption(JMXRemote.PROP_SERVLET,
                                   "Name of servlet",
                                   config.getValue(JMXRemote.PROP_SERVLET)),
            
            host = new StringConfigOption(JMXRemote.PROP_HOST,
                                          "Name of virtual host",
                                          "localhost"),

            service = new StringConfigOption(PROP_TOMCAT_SERVICE,
                                             "Name of tomcat service",
                                             "Tomcat-Standalone"),
                
            context = new StringConfigOption(JMXRemote.PROP_CONTEXT,
                                             "Name of the webapp context",
                                             "/examples"),

            nameAttribute = new StringConfigOption(PROP_TOMCAT_CONNECTOR_NAME,
                                                   "Tomcat connector name"),

            path = new StringConfigOption(PROP_TOMCAT_CONNECTOR_PATH,
                                          "Connector path to check for " +
                                          "availability",
                                          "/");
        
        EnumerationConfigOption connector =
            new EnumerationConfigOption(PROP_TOMCAT_CONNECTOR,
                                        "Tomcat connector scheme",
                                        CONNECTORS[0],
                                        CONNECTORS);
        
        PortConfigOption port =
            new PortConfigOption(PROP_TOMCAT_CONNECTOR_PORT,
                                 "Connector port", 8009);
        
        if(info.getType() == TypeInfo.TYPE_SERVICE) {        
            if (name.endsWith(ServletProductPlugin.WEBAPP_NAME)) {
                schema.addOption(host);
                schema.addOption(context);
                schema.addOption(service);
            } else if( name.endsWith(ServletProductPlugin.CONNECTOR_NAME)) {
                schema.addOption(connector);
                schema.addOption(port);
                schema.addOption(path);
                // Tomcat 5.0 and 5.5 connectors use the mbean name attribute
                if (info.isVersion(ServletProductPlugin.TOMCAT_VERSION_50) ||
                    info.isVersion(ServletProductPlugin.TOMCAT_VERSION_55)) {
                    schema.addOption(nameAttribute);
                }
            }
        }
        
        return schema;
    }
}
