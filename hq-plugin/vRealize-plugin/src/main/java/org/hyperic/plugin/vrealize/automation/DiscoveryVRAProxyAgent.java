/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFQDNFromURI;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAProxyAgent extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAProxyAgent.class);

    @Override
    protected ServerResource newServerResource(long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);
        
        String vRAIaasWebLB = executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFile);
        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            vRAIaasWebLB = getFQDNFromURI(vRAIaasWebLB);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebLB + "'");

        String managerLB = executeXMLQuery("//applicationSettings/*/setting[@name='DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService']/value/text()", configFile);
                if (!StringUtils.isEmpty(managerLB)) {
            managerLB = getFQDNFromURI(managerLB);
        }
        log.debug("[newServerResource] managerLB (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '" + managerLB + "'");

        //TODO: relations with vRAIaasWebLB and managerLB

        return server;
    }
}
