package org.hyperic.hq.plugin.zimbra;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

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

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.hyperic.sigar.win32.RegistryKey;

public class ZimbraServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    private static final String VERSION = "4.5.x";
    private static final String SERVER_NAME = "Zimbra";
    // generic process name, generic server daemon
    private static final String PROCESS_NAME = "zmlogger";
    // this PTQL query matches the PROCESS_NAME and returns the parent process id
    private static final String PTQL_QUERY = 
        "State.Name.re=perl|"+PROCESS_NAME+",Args.1.re=.*"+PROCESS_NAME+"$";
//rpm -q --queryformat "%{version}_%{release}" zimbra-core

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

/*
    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {
        List services = new ArrayList();
        Iterator options = getOptions();
        while (options.hasNext())
        {
            ZimbraPluginOptions obj = (ZimbraPluginOptions)options.next();
            ServiceResource service = new ServiceResource();
            service.setType(this, "Zimbra Service");
            service.setServiceName(obj.getServicename());
//            ConfigResponse productConfig = new ConfigResponse();
//            service.setProductConfig(productConfig);
            service.setProductConfig();
            // set an empty measurement config
            service.setMeasurementConfig();
            // set an empty control config
            service.setControlConfig();
            services.add(service);
        }
        return services;
    }
*/

    private Iterator getOptions()
    {
        List list = new ArrayList();
        ZimbraPluginOptions obj = new ZimbraPluginOptions("service", "option1", "option2");
        list.add(obj);
        return list.iterator();
    }

    private static List getServerProcessList()
    {
        List servers = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        for (int i=0; i<pids.length; i++)
        {
            String[] args = getProcArgs(pids[i]);
            if (args[1] == null || !args[1].matches(".*zmlogger$"))
                continue;
            File binary = new File(args[1]);
            if (!binary.isAbsolute())
                continue;
            servers.add(binary.getAbsolutePath());
        }
        return servers;
    }

    public List getServerList(String path) throws PluginException
    {
        ConfigResponse productConfig = new ConfigResponse();

        List servers = new ArrayList();
        String installdir = getParentDir(path, 2);
        String version = VERSION;

        // Only check the binaries if they match the path we expect
        if (path.indexOf(PROCESS_NAME) == -1)
            return servers;

        ServerResource server = createServerResource(installdir);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        server.setProductConfig(productConfig);
        // sets a default Measurement Config property with no values
        server.setMeasurementConfig();
        server.setName(SERVER_NAME+" "+version);
        servers.add(server);

        return servers;
    }
}
