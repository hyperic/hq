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

import org.hyperic.hq.product.ConfigFileTrackPlugin;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.Log4JLogTrackPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;
import org.hyperic.hq.product.TypeBuilder;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

import java.util.ArrayList;

/**
 * Plugin to handle all servlet containers
 */
public class ServletProductPlugin
    extends ProductPlugin {

    public static final String NAME = "servlet";

    // Server description for all Servlet Containers
    public static final String SERVER_DESC = "Servlet Container";

    // Servlet Container services.  Not all servers provide each
    // service.
    public static final String WEBAPP_NAME    = "Webapp";
    public static final String CONNECTOR_NAME = "Connector";

    // ServletExec
    static final String SE_NAME        = "servletexec";
    static final String SE_SERVER_NAME = "ServletExec";
    static final String SE_VERSION_42  = "4.2";

    static final String[] SE_DEPLOYED_SERVICES_42 = {
        WEBAPP_NAME,
    };

    // JRun
    static final String JRUN_NAME = "jrun";
    static final String JRUN_SERVER_NAME = "JRun";
    static final String JRUN_VERSION_4   = "4.x";

    static final String[] JRUN_DEPLOYED_SERVICES = {
        WEBAPP_NAME,
    };

    // Jakarta Tomcat
    static final String TOMCAT_NAME        = "tomcat";
    static final String TOMCAT_SERVER_NAME = "Tomcat";
    static final String TOMCAT_VERSION_40  = "4.0";
    static final String TOMCAT_VERSION_41  = "4.1";
    static final String TOMCAT_VERSION_50  = "5.0";
    static final String TOMCAT_VERSION_55  = "5.5";
    static final String PROP_TOMCAT_BASE   = "tomcatBase";

    static final String[] TOMCAT_DEPLOYED_SERVICES_40 = {
        WEBAPP_NAME,
    };

    static final String[] TOMCAT_INTERNAL_SERVICES_41 = {
        CONNECTOR_NAME,
    };
    
    static final String[] TOMCAT_DEPLOYED_SERVICES_41 = {
        WEBAPP_NAME,
    };

    static final String[] TOMCAT_INTERNAL_SERVICES_50 = {
        CONNECTOR_NAME,
    };
    
    static final String[] TOMCAT_DEPLOYED_SERVICES_50 = {
        WEBAPP_NAME,
    };

    static final String[] TOMCAT_INTERNAL_SERVICES_55 = {
        CONNECTOR_NAME,
    };
    
    static final String[] TOMCAT_DEPLOYED_SERVICES_55 = {
        WEBAPP_NAME,
    };

    // Caucho Resin
    static final String RESIN_SERVER_NAME = "Resin";
    static final String RESIN_VERSION_2 = "2.x";

    static final String[] RESIN_DEPLOYED_SERVICES = {
        WEBAPP_NAME,
    };

    public ServletProductPlugin() {
        setName(NAME);
    }

    public static boolean isJBossEmbeddedVersion(TypeInfo info, String dir) {
        String version = info.getVersion();
        
        if (version.equals(TOMCAT_VERSION_40)) {
            return false;
        }
        if (version.equals(TOMCAT_VERSION_41) &&
            dir.indexOf("jbossweb-tomcat41.sar") != -1) {
            return true;
        } 
        if (version.equals(TOMCAT_VERSION_50) &&
            dir.indexOf("jbossweb-tomcat50.sar") != -1) {
            return true;
        }
        if (version.equals(TOMCAT_VERSION_55) &&
            dir.indexOf("jbossweb-tomcat55.sar") != -1) {
            return true;
        }

        return false;
    }

    public GenericPlugin getPlugin(String type, TypeInfo info)
    {
        if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            // Server Control Plugins
            if (info.getType() == TypeInfo.TYPE_SERVER) {
                // ServletExec 4.2 (Both Win32 and Unix share this?)
                if (info.getName().startsWith(SE_SERVER_NAME))
                    return new ServletExecServerControlPlugin();
                // JRun 4.x
                if (info.getName().equals(JRUN_SERVER_NAME + " " +
                                          JRUN_VERSION_4)) {
                    return new JRunServerControlPlugin();
                }
                // Resin 2.x
                if (info.getName().startsWith(RESIN_SERVER_NAME)) {
                    return new ResinServerControlPlugin();
                }
                // Tomcat
                if (info.getName().startsWith(TOMCAT_SERVER_NAME)) {
                    if (info.isUnixPlatform()) {
                        return new TomcatServerControlPlugin();
                    } else if (info.isWin32Platform()) {
                        if (info.isVersion(TOMCAT_VERSION_40)) {
                            return new Tomcat40Win32ControlPlugin();
                        } else if (info.isVersion(TOMCAT_VERSION_41)) {
                            return new Tomcat41Win32ControlPlugin();
                        } else if (info.isVersion(TOMCAT_VERSION_50) ||
                                   info.isVersion(TOMCAT_VERSION_55)) {
                            // Only needed for different service name, could
                            // be consolidated.
                            return new Tomcat50Win32ControlPlugin();
                        }
                    }
                }
            }
            // Service Control Plugins
            if (info.getType() == TypeInfo.TYPE_SERVICE) {
                // JRun 4.x
                if (info.getName().startsWith(JRUN_SERVER_NAME + " " +
                                              JRUN_VERSION_4)) {
                    if (info.getName().endsWith(WEBAPP_NAME))
                        return new JRunServiceControlPlugin();
                }
                // Tomcat 4.0
                if (info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                              TOMCAT_VERSION_40)) {
                    if (info.getName().endsWith(WEBAPP_NAME))
                        return new Tomcat40ServiceControlPlugin();
                }
                // Tomcat 4.1, 5.0, 5.5
                if (info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                              TOMCAT_VERSION_41) ||
                    info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                              TOMCAT_VERSION_50) ||
                    info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                              TOMCAT_VERSION_55))
                {
                    if (info.getName().endsWith(WEBAPP_NAME)) {
                        return new Tomcat41ServiceControlPlugin();
                    }
                }
            }
        }
        // Measurement Plugins
        else if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            // Tomcat 4.1, 5.0, 5.5
            if (info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_41) ||
                info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_50) ||
                info.getName().startsWith(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_55))
                {
                    return new Tomcat41MeasurementPlugin();
                }

            // Tomcat 4.0, ServletExec 4.2, JRun 4.x
            return new ServletMeasurementPlugin(NAME);
        }
        // Response Time Plugins.  Only support Servers and Webapps
        else if (type.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            if (info.isService(WEBAPP_NAME) ||
                info.getType() == TypeInfo.TYPE_SERVER) {
                return new ServletRtPlugin();
            }
        }
        // Autoinventory Plugins.  XXX: TODO, combine all of these
        else if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
            if (info.getType() == TypeInfo.TYPE_SERVER) {
                // JRun 4.x
                if (info.getName().equals(JRUN_SERVER_NAME + " " +
                                          JRUN_VERSION_4)) {
                    return new JRunServerDetector();
                }
                // Tomcat 4.0
                if (info.getName().equals(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_40)) {
                    return new Tomcat40ServerDetector();
                }
                // Tomcat 4.1
                if (info.getName().equals(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_41)) {
                    return new Tomcat41ServerDetector();
                }
                // Tomcat 5.0
                if (info.getName().equals(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_50)) {
                    return new Tomcat50ServerDetector();
                }
                // Tomcat 5.5
                if (info.getName().equals(TOMCAT_SERVER_NAME + " " +
                                          TOMCAT_VERSION_55)) {
                    return new Tomcat55ServerDetector();
                }
                // Resin 2.x
                if (info.getName().startsWith(RESIN_SERVER_NAME)) {
                    return new ResinServerDetector();
                }
            }
        }
        else if (type.equals(ProductPlugin.TYPE_LOG_TRACK)) {
            if (info.getType() == TypeInfo.TYPE_SERVER) {
                return new Log4JLogTrackPlugin();
            }
        }
        else if (type.equals(ProductPlugin.TYPE_CONFIG_TRACK)) {
            if (info.getType() == TypeInfo.TYPE_SERVER) {
                return new ConfigFileTrackPlugin();
            }
        }

        return null;
    }

    public TypeInfo[] getTypes() {

        TypeBuilder types;
        ServerTypeInfo server;
        
        // Generic Servlet Container type
        types = new TypeBuilder(null, SERVER_DESC,
                                TypeBuilder.UNIX_PLATFORM_NAMES);

        // ServletExec 4.2
        server = types.addServer(SE_SERVER_NAME, SE_VERSION_42);
        types.addServices(server, SE_DEPLOYED_SERVICES_42);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // JRun 4.x
        server = types.addServer(JRUN_SERVER_NAME,
                                 JRUN_VERSION_4);
        types.addServices(server, JRUN_DEPLOYED_SERVICES);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // Tomcat 4.0
        server = types.addServer(TOMCAT_SERVER_NAME,
                                 TOMCAT_VERSION_40);
        types.addServices(server, TOMCAT_DEPLOYED_SERVICES_40);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // Tomcat 4.1
        server = types.addServer(TOMCAT_SERVER_NAME,
                                 TOMCAT_VERSION_41);
        types.addServices(server, TOMCAT_DEPLOYED_SERVICES_41,
                          TOMCAT_INTERNAL_SERVICES_41);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // Tomcat 5.0
        server = types.addServer(TOMCAT_SERVER_NAME,
                                 TOMCAT_VERSION_50);
        types.addServices(server, TOMCAT_DEPLOYED_SERVICES_50,
                          TOMCAT_INTERNAL_SERVICES_50);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // Tomcat 5.5
        server = types.addServer(TOMCAT_SERVER_NAME,
                                 TOMCAT_VERSION_55);
        types.addServices(server, TOMCAT_DEPLOYED_SERVICES_55,
                          TOMCAT_INTERNAL_SERVICES_55);

        // Clone unix server and services for win32
        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);

        // Resin 2.x
        server = types.addServer(RESIN_SERVER_NAME,
                                 RESIN_VERSION_2);
        types.addServices(server, RESIN_DEPLOYED_SERVICES);

        types.addServerAndServices(server,
                                   TypeBuilder.WIN32_PLATFORM_NAMES);
        return types.getTypes();
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        ArrayList configOptions = new ArrayList();

        if (info.getType() != TypeInfo.TYPE_SERVER) {
            return new ConfigSchema();
        }

        StringConfigOption jmxUrlConfig=
                new StringConfigOption(JMXRemote.PROP_JMX_URL,
                                       "URL for JMX communication. " +
                                       "Protocol, host, port " +
                                       "are required.",
                                       "http://localhost:8080");
        jmxUrlConfig.setMinLength(1);
        configOptions.add(jmxUrlConfig);

        StringConfigOption userConfig =
            new StringConfigOption(JMXRemote.PROP_JMX_USER,
                                   "User for JMX communication. Must " +
                                   "match the user configured on the " +
                                   "server side",
                                   "");
        userConfig.setOptional(true);
        configOptions.add(userConfig);
        
        StringConfigOption passConfig=
            new StringConfigOption(JMXRemote.PROP_JMX_PASS,
                                   "Password for JMX communication. Must " + 
                                   "match the password configured on the " +
                                   "server side",
                                   null);
        passConfig.setSecret(true);
        passConfig.setOptional(true);
        configOptions.add(passConfig);
        
        ConfigOption options[]=new ConfigOption[ configOptions.size()];
        for( int i=0; i< configOptions.size() ; i++ ) {
            options[i]=(ConfigOption)configOptions.get(i);
        }
        
        return new ConfigSchema(options);
    }
}
