/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.jmx.MBeanUtil;
import org.hyperic.hq.product.jmx.ServiceTypeFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class JBossWeb20ServerDetector extends JBossWebServerDetector {

    private ServiceTypeFactory serviceTypeFactory = new ServiceTypeFactory();
    private static final String MEASUREMENT_CLASS_PROPERTY = "measurement-class";
    private static final String CONTROL_CLASS_PROPERTY = "control-class";
    private static final String TEMPLATE_PROPERTY = "template";

    @Override
    protected String getJMXURL(MxProcess process) {
        return "jnp://127.0.0.1:1099";
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
}
