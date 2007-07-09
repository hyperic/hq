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

package org.hyperic.hq.plugin.alfresco;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.jdbc.DBUtil;

import org.hyperic.sigar.win32.RegistryKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AlfrescoServerDetector
    extends ServerDetector
    implements FileServerDetector, RegistryServerDetector, AutoServerDetector
{
    private static final boolean DEBUG = AlfrescoPluginUtil.DEBUG;

    static final String JDBC_DRIVER   = AlfrescoPluginUtil.JDBC_DRIVER,
                        TYPE_LUCENE   = "Lucene Index",
                        PROP_PROTOCOL   = AlfrescoPluginUtil.PROP_PROTOCOL,
                        PROP_IDENTIFIER = AlfrescoPluginUtil.PROP_IDENTIFIER,
                        PROP_URL      = AlfrescoMeasurementPlugin.PROP_URL,
                        PROP_USER     = AlfrescoMeasurementPlugin.PROP_USER,
                        PROP_PASSWORD = AlfrescoMeasurementPlugin.PROP_PASSWORD;

    private static Log log = LogFactory.getLog("AlfrescoServerDetector");

    private static final String PTQL_QUERY =
        "State.Name.re=java,Args.*.re=/alfresco/";

    private static final String ENGINE_QUERY = "select engine from sysengines";

    // Resource types and versions
    static final String SERVER_NAME = "Alfresco",
                        VERSION_2_0_x = "2.0.x";

    private static List getServerProcessList()
    {
        List servers = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        for (int i=0; i<pids.length; i++)
        {
            String exe = getProcExe(pids[i]);
            if (exe == null)
                continue;
            File binary = new File(exe);
            if (!binary.isAbsolute())
                continue;
            servers.add(binary.getAbsolutePath());
        }
        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();
        List paths = getServerProcessList();
        for (int i=0; i<paths.size(); i++)
        {
            String dir = (String)paths.get(i);
            List found = getServerList(dir);
            if (!found.isEmpty())
                servers.addAll(found);
        }   
        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path)
        throws PluginException
    {
        // Normal file scan
        return getServerList(path);
    }

    public List getServerResources (ConfigResponse platformConfig, 
                                    String path, RegistryKey current) 
        throws PluginException
    {
        return getServerList(path);
    }

    public List getServerList(String path) throws PluginException
    {
        ConfigResponse productConfig = new ConfigResponse();
        List servers = new ArrayList();
        String version = VERSION_2_0_x;

        String installdir = getParentDir(path, 3);
        ServerResource server = createServerResource(installdir);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, productConfig);
        server.setMeasurementConfig();
        server.setName(getPlatformName()+" "+SERVER_NAME+" "+version);
        server.setControlConfig();
        servers.add(server);

        return servers;
    }

    protected List discoverServices(ConfigResponse config) 
        throws PluginException
    {
        String url  = config.getValue(PROP_URL);
        String user = config.getValue(PROP_USER);
        String pass = config.getValue(PROP_PASSWORD);

        pass = (pass == null) ? "" : pass;
        pass = (pass.matches("^\\s*$")) ? "" : pass;

        try {
            Class.forName(JDBC_DRIVER);
        }
        catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            String msg = "Unable to load JDBC "+"Driver: "+e.getMessage();
            throw new PluginException(msg);
        }
        List rtn = new ArrayList();
        Connection conn = null;
        Statement stmt  = null;
        ResultSet rs    = null;
        try
        {
            conn = DriverManager.getConnection(url, user, pass);
            // Discover all Lucene Index Paths.
            stmt = conn.createStatement();
            setLuceneServices(rtn, conn, stmt);
        }
        catch (SQLException e) {
            String msg = "Error querying for services: "+e.getMessage();
            throw new PluginException(msg, e);
        }
        finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }
        String installpath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        rtn.add(getService("Tomcat", installpath));
        rtn.add(getService("Database Backend", installpath));
        return rtn;
    }

    private void setLuceneServices(List services,
                                   Connection conn,
                                   Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "select identifier,protocol from alf_store";
            rs = stmt.executeQuery(sql);
            int protocol_col   = rs.findColumn("protocol");
            int identifier_col = rs.findColumn("identifier");
            while (rs.next())
            {
                String protocol = rs.getString(protocol_col);
                String identifier = rs.getString(identifier_col);
                ServiceResource service = new ServiceResource();
                service.setType(this, TYPE_LUCENE);
                service.setServiceName(protocol+" / "+identifier);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_PROTOCOL, protocol);
                productConfig.setValue(PROP_IDENTIFIER, identifier);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        }
        finally
        {
            if (rs != null) rs.close();
        }
    }

    private ServiceResource getService(String name, String installpath)
    {
        ServiceResource service = new ServiceResource();
        service.setType(this, name);
        service.setServiceName(name);
        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue(ProductPlugin.PROP_INSTALLPATH, installpath);
        setProductConfig(service, productConfig);
        // set an empty measurement config
        service.setMeasurementConfig();
        return service;
    }
}
