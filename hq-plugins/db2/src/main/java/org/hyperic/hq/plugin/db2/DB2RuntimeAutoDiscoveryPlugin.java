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

package org.hyperic.hq.plugin.db2;

import java.util.ArrayList;

import org.hyperic.db2monitor.DB2Monitor;
import org.hyperic.db2monitor.DB2MonitorException;
import org.hyperic.db2monitor.SqlmDbase;
import org.hyperic.db2monitor.SqlmTable;
import org.hyperic.db2monitor.SqlmTablespace;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DB2RuntimeAutoDiscoveryPlugin
    implements RuntimeDiscoverer 
{
    private Log log = LogFactory.getLog("DB2RuntimeAutoDiscoveryPlugin");

    private String version;

    private DB2Monitor db2Monitor = null;

    public DB2RuntimeAutoDiscoveryPlugin(String version) 
    {
        this.version = version;
    }

    public RuntimeResourceReport discoverResources(int serverId, 
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config)
        throws PluginException 
    {
        try {
            DB2Monitor.load();
        } catch (UnsatisfiedLinkError le) {
            throw new PluginException(le.getMessage());
        } catch (DB2MonitorException e) {
            throw new PluginException(e.getMessage());
        }
        
        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);
        SqlmDbase[] dbase;
        AIServerExtValue server;
        ArrayList services, tables, tablespaces;

        if (this.db2Monitor == null) {
            String nodename = 
                config.getValue(DB2ProductPlugin.PROP_NODENAME);
            String user = 
                config.getValue(DB2ProductPlugin.PROP_USER, "");
            String password =
                config.getValue(DB2ProductPlugin.PROP_PASSWORD, "");
            
            try {
                if (nodename != null) {
                    // Attach to the instance specified
                    this.log.debug("Attaching to instance: " + nodename);
                    this.db2Monitor = new DB2Monitor(nodename, user, 
                                                     password);
                } else {
                    // Attach to the default local instance
                    this.log.debug("Attaching to default local instance");
                    this.db2Monitor = new DB2Monitor();
                }
            } catch (DB2MonitorException e) {
                this.log.error("Error attaching to DB2 instance: " +
                               e.getMessage());
            }

            if (this.db2Monitor == null) {
                throw new PluginException("DB2 Monitor not loaded");
            }
        }

        // Check if we should enable the monitor switches
        // XXX: Maybe we should only do this once.. Sure would be nice
        //      if this could be done in MeasurmentPlugin's init(), but
        //      we have no config response there.
        String monEnable = config.getValue(DB2ProductPlugin.PROP_MON_ENABLE);
        if (monEnable != null) {
            try {
                this.log.debug("Enabling DB2 monitoring switches");
                this.db2Monitor.enableMonitorSwitches();
            } catch (DB2MonitorException e) {
                this.log.error("Unable to enable DB2 monitor switches: " +
                               e.getMessage());
            }
        }

        try {
            dbase = this.db2Monitor.getSqlmDbase();
        } catch (DB2MonitorException e) {
            throw new PluginException(e.getMessage());
        }

        server = new AIServerExtValue();
        server.setId(new Integer(serverId));
        server.setPlaceholder(true);
        
        services = new ArrayList();

        for(int i = 0; i < dbase.length; i++) {
            AIServiceValue service = new AIServiceValue();
            ConfigResponse productResponse, metricResponse;
            String dbName = dbase[i].getName();

            if (this.version.equals(DB2ProductPlugin.VERSION_7)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_DATABASE_NAME_V7);
            } else if (this.version.equals(DB2ProductPlugin.VERSION_8)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_DATABASE_NAME_V8);
            } else {
                // Assume version 9.x
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_DATABASE_NAME_V9);
            }

            service.setName("%serverName%" + " " + dbName +
                            " " + DB2ProductPlugin.DATABASE);
            productResponse = new ConfigResponse();
            metricResponse  = new ConfigResponse();

            try {
                metricResponse.setValue(DB2ProductPlugin.PROP_DATABASE,
                                        dbName);
                service.setProductConfig(productResponse.encode());
                service.setMeasurementConfig(metricResponse.encode());
            } catch(Exception exc){
                throw new PluginException("Unable to generate config");
            }

            // For each database discover tables
            tables = discoverTables(dbName);
            services.addAll(tables);

            // For each database discover tablespaces
            tablespaces = discoverTablespaces(dbName);
            services.addAll(tablespaces);

            services.add(service);
        }

        server.setAIServiceValues((AIServiceValue[])services.
                                  toArray(new AIServiceValue[0]));
        aiplatform.addAIServerValue(server);
        rrr.addAIPlatform(aiplatform);

        return rrr;
    }

    private ArrayList discoverTables(String dbName)
        throws PluginException
    {
        ArrayList services = new ArrayList();
        SqlmTable[] tables;

        try {
            tables = this.db2Monitor.getSqlmTable(dbName);
        } catch (UnsatisfiedLinkError e) {
            throw new PluginException(e.getMessage());
        } catch (DB2MonitorException e) {
            throw new PluginException(e.getMessage());
        }
        
        for (int i = 0; i < tables.length; i++) {

            AIServiceValue service = new AIServiceValue();
            ConfigResponse productResponse, metricResponse;
            String tableName = tables[i].getName();
            
            if (this.version.equals(DB2ProductPlugin.VERSION_7)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLE_NAME_V7);
            } else if (this.version.equals(DB2ProductPlugin.VERSION_8)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLE_NAME_V8);
            } else {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLE_NAME_V9);
            }

            service.setName("%serverName%" + " " + dbName +
                            " " + tableName + " " + DB2ProductPlugin.TABLE);
            productResponse = new ConfigResponse();
            metricResponse  = new ConfigResponse();

            try {
                metricResponse.setValue(DB2ProductPlugin.PROP_DATABASE,
                                        dbName);
                metricResponse.setValue(DB2ProductPlugin.PROP_TABLE,
                                        tableName);
                service.setProductConfig(productResponse.encode());
                service.setMeasurementConfig(metricResponse.encode());
            } catch(Exception exc){
                throw new PluginException("Unable to generate config");
            }

            services.add(service);
        }
        
        return services;
    }

    private ArrayList discoverTablespaces(String dbName)
        throws PluginException
    {
        ArrayList services = new ArrayList();
        SqlmTablespace[] ts;

        try {
            ts = this.db2Monitor.getSqlmTablespace(dbName);
        } catch (UnsatisfiedLinkError e) {
            throw new PluginException(e.getMessage());
        } catch (DB2MonitorException e) {
            throw new PluginException(e.getMessage());
        }
        
        for (int i = 0; i < ts.length; i++) {

            AIServiceValue service = new AIServiceValue();
            ConfigResponse productResponse, metricResponse;
            String tsName = ts[i].getName();

            if (this.version.equals(DB2ProductPlugin.VERSION_7)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLESPACE_NAME_V7);
            } else if (this.version.equals(DB2ProductPlugin.VERSION_8)) {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLESPACE_NAME_V8);
            } else {
                service.setServiceTypeName(DB2ProductPlugin.
                                           FULL_TABLESPACE_NAME_V9);
            }

            service.setName("%serverName%" + " " + dbName +
                            " " + tsName + " " + DB2ProductPlugin.TABLESPACE);
            productResponse = new ConfigResponse();
            metricResponse  = new ConfigResponse();

            try {
                metricResponse.setValue(DB2ProductPlugin.PROP_DATABASE,
                                        dbName);
                metricResponse.setValue(DB2ProductPlugin.PROP_TABLESPACE,
                                        tsName);
                service.setProductConfig(productResponse.encode());
                service.setMeasurementConfig(metricResponse.encode());
            } catch(Exception exc){
                throw new PluginException("Unable to generate config");
            }

            services.add(service);
        }
        
        return services;
    }
}
