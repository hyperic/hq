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

package org.hyperic.hq.plugin.weblogic;

import java.util.Map;
import java.util.HashMap;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.Metric;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * This class contains constants and Metrics used by the various
 * plugins for measurement, control, etc.
 */
public class WeblogicMetric {

    public static final String PROP_ADMIN_URL      = "admin.url";
    public static final String PROP_ADMIN_USERNAME = "admin.username";
    public static final String PROP_ADMIN_PASSWORD = "admin.password";

    public static final String PROP_SERVER_URL     = "server.url";

    public static final String PROP_NODEMGR_ADDR   = "nodemgr.address";
    public static final String PROP_NODEMGR_PORT   = "nodemgr.port";

    //config props used in ConfigSchema and measurement templates
    public static final String PROP_DOMAIN     = "domain"; //e.g. petstore
    public static final String PROP_SERVER     = "server"; //e.g. petstoreServer
    public static final String PROP_REALM      = "realm"; //e.g. myrealm
    public static final String PROP_JVM        = "jvm.runtime";

    public static final String PROP_APP        = "application"; //e.g. petstore
    public static final String PROP_WEBAPP     = "webapp"; //e.g. supplier
    public static final String PROP_WEBAPP_DIR = "webapp.dir"; //e.g. supplier
    public static final String PROP_EJB        = "ejb"; //e.g. cartEjb
    public static final String PROP_EJB_COMPONENT = "ejb.component";
    public static final String PROP_EXQ        = "exq"; //e.g. default
    public static final String PROP_JDBC_CONN  = "jdbc.conn"; //e.g. petstorePool
    public static final String PROP_JMS_SRV    = "jms.server";
    public static final String PROP_JMS_DEST   = "jms.destination";
    public static final String PROP_JTA_RES    = "jta.resource";

    private static String LOCATION =
        "%domain%:Location=%server%,";
    
    private static String RUNTIME_LOCATION =
        LOCATION + "ServerRuntime=%server%,";

    //used for measurement and control
    static final String SERVER_RUNTIME =
        LOCATION +
        "Name=%server%," +
        "Type=ServerRuntime";

    private static final String SERVER_SECURITY_RUNTIME = 
        LOCATION + 
        "Name=%server%," +
        "Type=ServerSecurityRuntime";

    private static final String JVM_RUNTIME =
        RUNTIME_LOCATION +
        "Name=%server%," +
        "Type=";

    static final String SERVER_RUNTIME_STATE = "StateVal";

    private static final String JDBC_CONNECTION_POOL_RUNTIME =
        RUNTIME_LOCATION +
        "Name=%jdbc.conn%," +
        "Type=JDBCConnectionPoolRuntime";

    static final String JDBC_CONNECTION_POOL_RUNTIME_STATE =
        "PoolState";

    //used for measurement and control
    static final String APPLICATION =
        LOCATION +
        "Name=%application%," +
        "Type=ApplicationConfig";

    static final String APPLICATION_STATE = "Deployed";

    private static final String APPLICATION_RUNTIME_ATTR = 
        "ApplicationRuntime=%server%_%application%";
    
    private static final String APPLICATION_RUNTIME_ATTR_9 = 
        "ApplicationRuntime=%application%";

    private static final String WEBAPP_COMPONENT_RUNTIME =
        RUNTIME_LOCATION +
        "Type=WebAppComponentRuntime";

    static final String WEBAPP_COMPONENT_RUNTIME_STATUS = "Status";

    static final String WEBAPP_COMPONENT =
        "%domain%:Application=%application%," +
        "Name=%webapp%," +
        "Type=WebAppComponent";

    private static final String EJB_COMPONENT_RUNTIME =
        RUNTIME_LOCATION +
        "Type=EJBComponentRuntime";

    static final String EJB_COMPONENT_RUNTIME_STATUS = "Status";

    static final String EJB_COMPONENT = 
        "%domain%:Application=%application%,Name=%ejb%," +
        "Type=EJBComponent";

    private static final String JMS_DEST_RUNTIME = 
        RUNTIME_LOCATION +
        "JMSServerRuntime=%jms.server%," +
        "Name=%jms.destination%," +
        "Type=JMSDestinationRuntime";

    private static final String CONNECTOR_SERVICE_RUNTIME =
        RUNTIME_LOCATION +
        "Type=ConnectorServiceRuntime," +
        "Name=ConnectorService";

    private static final String TX_RESOURCE_RUNTIME =
        RUNTIME_LOCATION +
        "Type=TransactionResourceRuntime";

    private static final String[] CONFIG_PROPS = {
        PROP_ADMIN_URL,
        PROP_ADMIN_USERNAME,
        PROP_ADMIN_PASSWORD,
    };

    private static final String CONFIG_TMPL =
        Metric.configTemplate(CONFIG_PROPS);

    static String getObjectTemplate(String version, String type) {
        return (String)getMetricProps(version).get(type);
    }

    static String getObjectTemplate(GenericPlugin plugin, String type) {
        return getObjectTemplate(plugin.getTypeInfo().getVersion(), type);
    }
    
    //used for replacement in hq-plugin.xml
    static Map getMetricProps(String typeVersion) {
        int version = WeblogicConfig.majorVersion(typeVersion);
        HashMap props = new HashMap();
        String ejbAttrs;

        //some subtle diffs between versions
        if (version >= 9) {
            props.put("ServerSecurityRuntime",
                      SERVER_SECURITY_RUNTIME +
                      ",ServerRuntime=%server%");

            props.put("ConnectorServiceRuntime",
                      CONNECTOR_SERVICE_RUNTIME);
            
            props.put("JVMRuntime",
                      JVM_RUNTIME + "%jvm.runtime%");

            props.put("JDBCConnectionPoolRuntime",
                      JDBC_CONNECTION_POOL_RUNTIME + "," +
                      APPLICATION_RUNTIME_ATTR_9);

            props.put("TxResourceRuntime",
                      TX_RESOURCE_RUNTIME +
                      ",Name=%jta.resource%" +
                      ",JTARuntime=JTARuntime");

            props.put("WebAppComponentRuntime",
                      WEBAPP_COMPONENT_RUNTIME + "," +
                      APPLICATION_RUNTIME_ATTR_9 + "," +
                      "Name=%webapp%");

            props.put("EJBComponentRuntime",
                      EJB_COMPONENT_RUNTIME + "," +
                      APPLICATION_RUNTIME_ATTR_9 + "," +
                      "Name=%ejb%");
            
            ejbAttrs =
                APPLICATION_RUNTIME_ATTR_9 + "," +
                "Name=%ejb%";
        }
        else {
            props.put("ServerSecurityRuntime",
                      SERVER_SECURITY_RUNTIME);

            props.put("ConnectorServiceRuntime",
                      CONNECTOR_SERVICE_RUNTIME + "Runtime");

            props.put("JVMRuntime",
                      JVM_RUNTIME + "JVMRuntime");

            props.put("JDBCConnectionPoolRuntime",
                      JDBC_CONNECTION_POOL_RUNTIME);

            props.put("TxResourceRuntime",
                      TX_RESOURCE_RUNTIME +
                      ",Name=JTAResourceRuntime_%jta.resource%");

            String webappRuntime =
                WEBAPP_COMPONENT_RUNTIME + "," +
                "Name=%server%_%server%_%application%_%webapp%";
            
            if (version == 6) {
                String ejbName =
                    "Name=%application%_%ejb%";
                
                props.put("EJBComponentRuntime",
                          EJB_COMPONENT_RUNTIME + "," +
                          ejbName);
                
                ejbAttrs = ejbName;
            }
            else {
                String ejbName =
                    "Name=%server%_%application%_%ejb%";

                props.put("EJBComponentRuntime",
                          EJB_COMPONENT_RUNTIME + "," +
                          APPLICATION_RUNTIME_ATTR + "," +
                          ejbName);

                webappRuntime +=
                    "," + APPLICATION_RUNTIME_ATTR;
                
                ejbAttrs =
                    APPLICATION_RUNTIME_ATTR + "," +
                    ejbName;
            }

            props.put("WebAppComponentRuntime",
                      webappRuntime);
        }

        props.put("ServerRuntime",
                  SERVER_RUNTIME);

        props.put("Application",
                  APPLICATION);

        props.put("JMSDestinationRuntime",
                  JMS_DEST_RUNTIME);

        String[] ejbTypes = {
            "EntityEJB",
            "MessageDrivenEJB",
            "StatelessEJB",
            "StatefulEJB",
            "EJBTransaction",
            "EJBPool",
            "EJBCache",
            "EJBLocking",
        };

        for (int i=0; i<ejbTypes.length; i++) {
            String name = ejbTypes[i] + "Runtime";
            String oname =
                RUNTIME_LOCATION + 
                "Type=" + name + "," +
                "EJBComponentRuntime=%ejb.component%" + "," +
                ejbAttrs;             
            if (name.startsWith("EJB") && (version >= 9)) {
                oname += ",${BeanType}Runtime=%ejb%";
            }
            props.put(name, oname);
        }

        props.put("Location", LOCATION);

        props.put("RuntimeLocation", RUNTIME_LOCATION);

        return props;
    }

    static String template(String objectName, String attr) {
        return objectName + ":" + attr + ":" + CONFIG_TMPL;
    }

    static String translateNode(String template, ConfigResponse config) {
        final String pat = "%" + PROP_ADMIN_URL + "%";
        String node = config.getValue(PROP_SERVER_URL);
        if (node != null) {
            template = StringUtil.replace(template, pat, node);
        }
        return template;
    }
}
