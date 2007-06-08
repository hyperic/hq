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
    private static final String VERSION_4_5_x = "4.5.x";
    private static final String VERSION_4_5_x_JAVA = "jdk1.5.0_08";
    private static final String SERVER_NAME = "Zimbra";
    // generic process name, generic server daemon
    private static final String PROCESS_NAME = "zmtomcatmgr";
    // this PTQL query matches the PROCESS_NAME and returns the parent process id
    private static final String PTQL_QUERY = 
        "State.Name.eq="+PROCESS_NAME+",State.Name.Pne=$1,Args.0.re=.*"+PROCESS_NAME+"$";

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
        String installpath = getParentDir(path, 2);

        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("installpath", installpath);

        String version = "";
        if ((new File(installpath+"/"+VERSION_4_5_x_JAVA+"/")).exists())
            version = VERSION_4_5_x;

        // Only check the binaries if they match the path we expect
        if (path.indexOf(PROCESS_NAME) == -1)
            return servers;

        ServerResource server = createServerResource(installpath);
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

    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {
        List services = new ArrayList();
        services.add(getService("Zimbra Tomcat"));
        services.add(getService("Zimbra Logger MySQL"));
        services.add(getService("Zimbra MySQL"));
        services.add(getService("Zimbra OpenLDAP"));
        services.add(getService("Zimbra Cyrus SASL"));
        services.add(getService("Zimbra ClamAV"));
        services.add(getService("Zimbra Apache Httpd"));
        services.add(getService("Zimbra Postfix"));
        services.add(getService("Zimbra AMaViS"));
        services.add(getService("Zimbra Log Watch"));
        services.add(getService("Zimbra Swatch"));
        services.add(getService("Zimbra MTA Config"));
        return services;
    }

    private ServiceResource getService(String name)
    {
        ServiceResource service = new ServiceResource();
        service.setType(this, name);
        service.setServiceName(name);
        ConfigResponse productConfig = new ConfigResponse();
        service.setProductConfig(productConfig);
        // set an empty measurement config
        service.setMeasurementConfig();
        // set an empty control config
        service.setControlConfig();
        return service;
    }
}
