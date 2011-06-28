/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.jmx.MBeanUtil;
import org.hyperic.hq.product.jmx.ServiceTypeFactory;
import org.hyperic.util.config.ConfigResponse;

public class JBossWebServerDetector extends TomcatServerDetector {

    private ServiceTypeFactory serviceTypeFactory = new ServiceTypeFactory();
    private static final String MEASUREMENT_CLASS_PROPERTY = "measurement-class";
    private static final String CONTROL_CLASS_PROPERTY = "control-class";
    private static final String TEMPLATE_PROPERTY = "template";

    @Override
    public String getTypeProperty(String type, String name) {
        String val = super.getTypeProperty(type, name);
        if (name.equals("OBJECT_NAME")) {
            val = val.replace("Catalina", "jboss.web");
        }
        return val;
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        getLog().debug("[discoverServices] serverConfig=" + serverConfig);

        JMXConnector connector;
        MBeanServerConnection mServer;

        try {
            mServer = JBossUtil.getMBeanServerConnection(serverConfig.toProperties());
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        return discoverMxServices(mServer, serverConfig);
    }

    @Override
    public Set discoverServiceTypes(ConfigResponse serverConfig) throws PluginException {
        JMXConnector connector;
        MBeanServerConnection mServer;
        Set serviceTypes = new HashSet();

        //plugins need to define these properties at the plugin level to discover dynamic service types
        if (getProductPlugin().getPluginData().getProperty(MEASUREMENT_CLASS_PROPERTY) == null || getProductPlugin().getPluginData().getProperty(CONTROL_CLASS_PROPERTY) == null || getProductPlugin().getPluginData().getProperty(TEMPLATE_PROPERTY) == null) {
            return serviceTypes;
        }

        try {
            mServer = JBossUtil.getMBeanServerConnection(serverConfig.toProperties());
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        try {
            final Set objectNames = mServer.queryNames(new ObjectName(MBeanUtil.DYNAMIC_SERVICE_DOMAIN + ":*"), null);
            serviceTypes = serviceTypeFactory.create(getProductPlugin(), (ServerTypeInfo) getTypeInfo(), mServer, objectNames);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        return serviceTypes;
    }

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        getLog().debug("[getServerResources] platformConfig="+platformConfig);
        List servers = new ArrayList();

        List procs = getServerProcessList();
        for (int i = 0; i < procs.size(); i++) {
            MxProcess process = (MxProcess) procs.get(i);

            String config = "default";
            List args = Arrays.asList(process.getArgs());
            getLog().debug("[getServerResources] args="+args);
            if (args.contains("-c")) {
                config = (String) args.get(args.indexOf("-c") + 1);
            }
            
            final String fileVersion = process.getInstallPath() + "/../server/" + config;
            getLog().debug("[getServerResources] fileVersion="+fileVersion);
            if (isInstallTypeVersion(fileVersion)) {
                ServerResource server = getServerResource(process);
                ConfigResponse cfg = new ConfigResponse();
                cfg.setValue("jmx.url", "jnp://127.0.0.1:1099");
                cfg.setValue("process.query", getProcQuery() + ",Args.*.ct=" + process.getInstallPath());
                setProductConfig(server, cfg);
                servers.add(server);
            }
        }
        return servers;
    }

    @Override
    protected boolean isInstallTypeVersion(String path) {
        String versionFile = getTypeProperty("VERSION_FILE");
        String jbossVersion = getTypeProperty("JBOSS_VERSION");
        boolean ok = false;
        String v = "null";

        File f = new File(path, versionFile);
        if (f.exists()) {
            JarFile jarfile;
            try {
                jarfile = new JarFile(f);
                Manifest manifest = jarfile.getManifest();
                Attributes attrs = (Attributes) manifest.getMainAttributes();
                v = attrs.getValue("Specification-Version");
            } catch (IOException ex) {
                getLog().debug("[isInstallTypeVersion] " + ex.getMessage(), ex);
            }
        }

        ok = v.startsWith(jbossVersion);
        getLog().debug("[isInstallTypeVersion] ok=" + ok + " version=" + v + "(" + jbossVersion + ") file=" + f);
        return ok;
    }

    @Override
    protected void setJmxUrl(MxProcess process, ConfigResponse config) {
    }
}
