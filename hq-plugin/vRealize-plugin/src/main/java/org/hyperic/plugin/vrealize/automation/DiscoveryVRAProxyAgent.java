/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.createLogialResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFQDNFromURI;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getParameterizedName;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_PROXY_AGENT_SERVER_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_MANAGER_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_MANAGER_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_MANAGER_SERVER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_TAG;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAProxyAgent extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAProxyAgent.class);

    @Override
    protected ServerResource newServerResource(long pid,
                                               String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);

        String vRAIaasWebLB = executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFile);
        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            vRAIaasWebLB = getFQDNFromURI(vRAIaasWebLB);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebLB + "'");

        String managerLB =
                    executeXMLQuery(
                                "//applicationSettings/*/setting[@name='DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService']/value/text()",
                                configFile);
        if (!StringUtils.isEmpty(managerLB)) {
            managerLB = getFQDNFromURI(managerLB);
        }
        log.debug("[newServerResource] managerLB (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '"
                    + managerLB + "'");

        Resource modelResource = getCommonModel(server, vRAIaasWebLB, managerLB);
        String modelXml = marshallResource(modelResource);
        setModelProperty(server, modelXml);

        return server;
    }

    public static Resource getCommonModel(ServerResource server,
                                          String vRAIaasWebLB,
                                          String managerLB) {
        String proxyServerGroupName = getParameterizedName(KEY_APPLICATION_NAME, TYPE_PROXY_AGENT_SERVER_GROUP);

        String parameterizedApplicationTagName = getParameterizedName(KEY_APPLICATION_NAME);
        ObjectFactory objectFactory = new ObjectFactory();

        Resource proxyServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST,
                    server.getType(), server.getName(), ResourceTier.SERVER);
        Resource proxyGroup = createLogialResource(objectFactory, TYPE_PROXY_AGENT_SERVER_GROUP, proxyServerGroupName);

        Resource application = createLogialResource(objectFactory, TYPE_VRA_APPLICATION, parameterizedApplicationTagName);

        proxyServer.addRelations(objectFactory.createRelation(proxyGroup,
                    RelationType.PARENT));
        proxyGroup.addRelations(objectFactory.createRelation(application,
                    RelationType.PARENT));

        Resource loadBalancerSuperTag =
                    createLogialResource(objectFactory, TYPE_LOAD_BALANCER_TAG,
                                getParameterizedName(KEY_APPLICATION_NAME));

        if (!StringUtils.isEmpty(vRAIaasWebLB)) {
            Resource vraIaasWebLoadBalancer = objectFactory.createResource(!CREATE_IF_NOT_EXIST,
                        TYPE_VRA_IAAS_WEB_LOAD_BALANCER, vRAIaasWebLB, ResourceTier.SERVER);

            vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

            Resource vRAIaasWebServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST,
                        TYPE_VRA_IAAS_WEB, vRAIaasWebLB, ResourceTier.SERVER);

            Resource vRAIaasWebServerTag =
                        createLogialResource(objectFactory, TYPE_VRA_IAAS_WEB_TAG,
                                    parameterizedApplicationTagName);

            vRAIaasWebServer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, RelationType.PARENT));

            proxyServer.addRelations(
                        objectFactory.createRelation(vRAIaasWebServer, RelationType.SIBLING),
                        objectFactory.createRelation(vraIaasWebLoadBalancer, RelationType.SIBLING));
        }

        if (!StringUtils.isEmpty(managerLB)) {
            Resource managerLoadBalancer = objectFactory.createResource(!CREATE_IF_NOT_EXIST,
                        TYPE_VRA_IAAS_MANAGER_SERVER_LOAD_BALANCER, managerLB, ResourceTier.SERVER);

            proxyServer.addRelations(objectFactory.createRelation(managerLoadBalancer,
                        RelationType.SIBLING));

            managerLoadBalancer.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

            Resource managerServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST,
                        TYPE_VRA_IAAS_MANAGER_SERVER, managerLB, ResourceTier.SERVER);

            Resource managerServerTag =
                        createLogialResource(objectFactory, TYPE_VRA_IAAS_MANAGER_SERVER_TAG,
                                    parameterizedApplicationTagName);

            managerServer.addRelations(objectFactory.createRelation(managerServerTag, RelationType.PARENT));

            proxyServer.addRelations(objectFactory.createRelation(managerServer,
                        RelationType.SIBLING));
        }

        return proxyServer;
    }

    /* inline unit test
    @Test
    public void test() {
        ServerResource server = new ServerResource();
        server.setName("111");
        server.setType("222");
        Resource modelResource = getCommonModel(server, "AAA", "BBB");
        String modelXml = marshallResource(modelResource);
        Assert.assertNotNull(modelXml);

        System.out.println(modelXml);
    }
    */
}
