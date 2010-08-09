package com.vmware.springsource.hyperic.plugin.gemfire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public abstract class MemberDetector extends ServerDetector
        implements AutoServerDetector {

    Log log = getLog();

    public List getServerResources(ConfigResponse pc) throws PluginException {
        log.debug("[getServerResources] pc=" + pc);
        List servers = new ArrayList();
        JMXConnector connector = null;

        try {
            connector = MxUtil.getMBeanConnector(pc.toProperties());
            MBeanServerConnection mServer = connector.getMBeanServerConnection();

            Object[] args = {};
            String[] def = {};
            String[] members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);
            log.debug("[getServerResources] members=" + Arrays.asList(members));
            for (String menber : members) {
                Object[] args2 = {menber};
                String[] def2 = {String.class.getName()};
                Map memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
                log.debug("[getServerResources] memberDetails.size()=" + memberDetails.size());
                if (isValidMember(memberDetails)) {
                    ServerResource server = createServerResource("");
                    server.setName(getTypeInfo().getName() + " " + menber);
                    server.setIdentifier(menber);
                    ConfigResponse c = new ConfigResponse();
                    c.setValue("memberID", menber);
                    setMeasurementConfig(server, c);
                    servers.add(server);
                }
            }

        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
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

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List services = new ArrayList();
        JMXConnector connector = null;


        try {
            connector = MxUtil.getMBeanConnector(config.toProperties());
            MBeanServerConnection mServer = connector.getMBeanServerConnection();

            String memberId = config.getValue("memberID");
            Object[] args2 = {memberId};
            String[] def2 = {String.class.getName()};
            Map<String, Object> memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            Map<String, Map> regions = (Map) memberDetails.get("gemfire.member.regions.map");


            if (regions != null) {
                for (Map region : regions.values()) {
                    String name = (String) region.get("gemfire.region.name.string");

                    ServiceResource service = createServiceResource("Region");
                    service.setName("Region " + name);
                    ConfigResponse c = new ConfigResponse();
                    c.setValue("regionID", (String) region.get("gemfire.region.path.string"));
                    c.setValue("name", name);
                    c.setValue("memberID", memberId);
                    log.debug("[discoverServices] c=" + c);
                    setMeasurementConfig(service, c);
                    services.add(service);
                }
            } else {
                for (String key : memberDetails.keySet()) {
                    log.debug(key + "=" + memberDetails.get(key));
                }
            }

        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            }
        }
        return services;
    }

    abstract boolean isValidMember(Map memberDetails);
}
