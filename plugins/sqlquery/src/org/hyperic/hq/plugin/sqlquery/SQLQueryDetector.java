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

package org.hyperic.hq.plugin.sqlquery;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

public class SQLQueryDetector extends DaemonDetector {

    static final String PROP_QUERY = "jdbcQuery";

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        Connection conn = null;
        Properties props = config.toProperties();
        Map plugins = getServiceInventoryPlugins();
        List services = new ArrayList();

        if (plugins == null) {
            getLog().debug("No service autoinventory plugins defined");
            return null;
        }

        try {
            conn = SQLQueryMeasurementPlugin.getConnection(props);
            for (Iterator it=plugins.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                String type = (String)entry.getKey();
                //String name = (String)entry.getValue();
                services.addAll(discoverServices(config, conn, type));
            }
        } catch (SQLException e) {
            throw new PluginException("Connection error: " +
                                      e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    getLog().error("Closing connection: " + e);
                }
            }
        }

        return services;
    }

    protected List discoverServices(ConfigResponse serverConfig,
                                    Connection conn,
                                    String type)
        throws PluginException {

        List services = new ArrayList();

        String query = getTypeProperty(type, PROP_QUERY);

        if (query == null) {
            throw new PluginException(PROP_QUERY + " not defined");
        }

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            throw new PluginException("Creating statement: " + e);            
        }

        try {
            rs = stmt.executeQuery(query);

            ResultSetMetaData data = rs.getMetaData();

            List names = new ArrayList();
            names.add(null); //unused index 0

            int num = data.getColumnCount();
            for (int i=1; i<=num; i++) {
                names.add(data.getColumnName(i));
            }
        
            String serviceTypeName = getTypeNameProperty(type);

            while (rs.next()) {
                ServiceResource service = new ServiceResource();
                
                StringBuffer name = new StringBuffer();
                ConfigResponse productConfig = new ConfigResponse();
                
                for (int i=1; i<=num; i++) {
                    String value = rs.getString(i);
                    if (value == null) {
                        continue;
                    }
                    name.append(value).append(' ');
                    
                    productConfig.setValue((String)names.get(i),
                                           value);
                }

                name.append(serviceTypeName);

                String serviceName =
                    formatAutoInventoryName(type,
                                            serverConfig,
                                            productConfig,
                                            null);                    

                if (serviceName == null) {
                    service.setServiceName(name.toString());
                }
                else {
                    service.setName(serviceName);
                }

                service.setType(type);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig();

                services.add(service);
            }
        } catch (SQLException e) {
            throw new PluginException("Error in query " + query + 
                                      ": " + e.getMessage());
        } finally {
            if (rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {
                    getLog().error("Closing result set: " + e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    getLog().error("Closing statement: " + e);
                }
            }
        }

        return services;
    }
}
