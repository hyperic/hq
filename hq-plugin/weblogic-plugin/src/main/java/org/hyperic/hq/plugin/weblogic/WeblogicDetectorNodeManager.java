/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.weblogic;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class WeblogicDetectorNodeManager extends ServerDetector implements AutoServerDetector {

    private static final String PTQL_QUERY =
            "State.Name.eq=java,Args.*.eq=weblogic.NodeManager";
    private static final Log log = LogFactory.getLog(WeblogicDetector.class);

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList();
        if(!WeblogicProductPlugin.NEW_DISCOVERY) return servers;
        
        long[] pids = getPids(PTQL_QUERY);
        for (long pid : pids) {
            try {
                String cwd = getSigar().getProcExe(pid).getCwd();
                File confFile = new File(cwd, "nodemanager.properties");
                Properties props = new Properties();
                log.debug("[" + pid + "] loadin config: " + confFile);
                props.load(new FileInputStream(confFile));
                log.debug("[" + pid + "] version=" + props.getProperty("PropertiesVersion"));
                if (props.getProperty("PropertiesVersion").equalsIgnoreCase(getTypeInfo().getVersion())) {
                    String port = props.getProperty("ListenPort");
                    String host = props.getProperty("ListenAddress", getPlatformName());
                    if (host.equals("")) {
                        host = getPlatformName();
                    }
                    ServerResource server = createServerResource(cwd);
                    
                    String name = getTypeInfo().getName()+" "+host + ":" + port;
                    if (WeblogicProductPlugin.usePlatformName && WeblogicProductPlugin.NEW_DISCOVERY) {
                        name = getPlatformName() + " " + name;
                    }
                    server.setName(name);

                    ConfigResponse cf = new ConfigResponse();
                    cf.setValue("nodemgr.address", host);
                    cf.setValue("nodemgr.port", port);
                    populateListeningPorts(pid, cf, true);
                    setProductConfig(server, cf);

                    servers.add(server);
                }
            } catch (Exception e) {
                log.debug("[" + pid + "] Error getting process info, reason: '" + e.getMessage() + "'", e);
            }
        }
        return servers;
    }
    
    private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}
