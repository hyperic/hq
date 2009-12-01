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

package org.hyperic.hq.plugin.samba;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.hyperic.sigar.win32.RegistryKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SambaServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    private static final String VERSION_3_x = "3.x";
    private static final String SERVER_NAME = "Samba";
    // generic process name, generic server daemon
    private static final String PROCESS_NAME = "smbd";
    // this PTQL query matches the PROCESS_NAME and returns the parent process id
    private static final String PTQL_QUERY = 
        "State.Name.eq="+PROCESS_NAME+",State.Name.Pne=$1";

    private static Log log = LogFactory.getLog(SambaServerDetector.class);

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

    private String getVersion(String path)
    {
        String[] cmd = {path, "-V"};
        try
        {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader fp = new BufferedReader( new InputStreamReader(process.getInputStream()) );
            String output = "";
            String line;
            while( null != (line = fp.readLine()) )
                output = output + line;
            Pattern p_3_x = Pattern.compile("\\s+3\\.[0-9]");

            if (p_3_x.matcher(output).find())
                return VERSION_3_x;
        }
        catch (IOException e) {
            log.warn("Cannot get version info from "+path, e);
        }
        return null;
    }

    public List getServerList(String path) throws PluginException
    {
        List servers = new ArrayList();
        String installpath = getParentDir(path, 2);

        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("installpath", installpath);

        String version = "";
        if ( null == (version = getVersion(path)) )
            return servers;

        // Only check the binaries if they match the path we expect
        if (path.indexOf(PROCESS_NAME) == -1)
            return servers;

        ServerResource server = createServerResource(installpath);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, productConfig);
        // sets a default Measurement Config property with no values
        server.setMeasurementConfig();
        server.setName(SERVER_NAME+" "+version);
        servers.add(server);

        return servers;
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {
        String installpath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        List services = new ArrayList();
        return services;
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
        // set an empty control config
        return service;
    }
}
