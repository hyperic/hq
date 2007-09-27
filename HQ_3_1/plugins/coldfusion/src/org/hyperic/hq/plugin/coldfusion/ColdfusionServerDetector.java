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

package org.hyperic.hq.plugin.coldfusion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

public class ColdfusionServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    private static Log myLog =
        LogFactory.getLog(ColdfusionServerDetector.class);

    private static final String SERVER_NAME = "Coldfusion";
    private static final String VERSION_7 = "7.x";
    private static final String JAVA_PROCESS = "java";
    private static final String CFMX7_PROCESS = "cfmx7";
    private static final String PROCESS_PATTERN =
        CFMX7_PROCESS+"|"+JAVA_PROCESS;
    //this PTQL query matches the PROCESS_NAME and returns the parent process id
    private static final String PTQL_QUERY = 
        "State.Name.re="+PROCESS_PATTERN+",State.Name.Pne=$1,"+
        "Args.*.ct=jrun.jar";
//        "Args.*.ct=jrun.jar,Args.*.ct="+SERVER_NAME.toLowerCase();

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

    public List getServerList(String path) throws PluginException
    {
        List servers = new ArrayList();
        String installpath = getParentDir(path, 3);

        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("installpath", installpath);

        String version = VERSION_7;
        // Only check the binaries if they match the path we expect
        if (path.indexOf(JAVA_PROCESS) == -1 &&
            path.indexOf(CFMX7_PROCESS) == -1)
            return servers;

        ServerResource server = createServerResource(installpath);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, productConfig);
        // sets a default Measurement Config property with no values
        server.setMeasurementConfig();
        server.setName(getPlatformName()+" "+SERVER_NAME+" "+version);
        servers.add(server);

        return servers;
    }
}
