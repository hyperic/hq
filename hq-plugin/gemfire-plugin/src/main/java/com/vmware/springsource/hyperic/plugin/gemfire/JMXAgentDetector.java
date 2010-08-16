package com.vmware.springsource.hyperic.plugin.gemfire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class JMXAgentDetector extends ServerDetector
        implements AutoServerDetector {

    Log log;

    public JMXAgentDetector() {
        this.log = getLog();
    }

    public List getServerResources(ConfigResponse pc) throws PluginException {
        this.log.debug("[getServerResources] pc=" + pc);
        List servers = new ArrayList();
        JMXConnector connector = null;
        try {
            connector = MxUtil.getMBeanConnector(pc.toProperties());
            MBeanServerConnection mServer = connector.getMBeanServerConnection();

            ObjectName mbean = new ObjectName("GemFire:type=MemberInfoWithStatsMBean");
            String version = (String) mServer.getAttribute(mbean, "Version");
            String id = (String) mServer.getAttribute(mbean, "Id");
            boolean versionOK = version.startsWith(getTypeInfo().getVersion());
            this.log.debug("Agent version='" + version + " " + (versionOK?"OK":"") + " (" + getTypeInfo().getVersion() + ")");
            ServerResource server;
            if (versionOK) {
                server = createServerResource("");
                server.setName(getTypeInfo().getName() + " " + id);
                server.setIdentifier(id);
                setProductConfig(server, new ConfigResponse());
                setMeasurementConfig(server, pc);
                servers.add(server);
            }

//            Set<ObjectInstance> mbs = mServer.queryMBeans(new ObjectName("*:*"), null);
//            for(ObjectInstance mb:mbs){
//                System.out.println("---> "+mb.getObjectName());
//            }

            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        } catch (Exception ex) {
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        }
        return servers;
    }
}
