/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.*;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;

/**
 * @author glaullon
 */
public class DiscoveryVRAProxyAgent extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAProxyAgent.class);
    private static final String appName = CommonModelUtils.getParametrizedName(KEY_APPLICATION_NAME);

    @Override
    protected ServerResource newServerResource(
                long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        String configFilePath = exe + ".config";
        log.debug("[newServerResource] configFile=" + configFilePath);

        String vRAIaasWebOrLoadBalancer =
                    executeXMLQuery("//appSettings/add[@key='repositoryAddress']/@value", configFilePath);
        if (!StringUtils.isEmpty(vRAIaasWebOrLoadBalancer)) {
            vRAIaasWebOrLoadBalancer = getFqdn(vRAIaasWebOrLoadBalancer);
        }
        log.debug("[newServerResource] vRAIaasWebLB (repositoryAddress) = '" + vRAIaasWebOrLoadBalancer + "'");

        String managerServerOrLoadBalancer = executeXMLQuery(
                    "//applicationSettings/*/setting[@name='DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService']/value/text()",
                    configFilePath);
        if (!StringUtils.isEmpty(managerServerOrLoadBalancer)) {
            managerServerOrLoadBalancer = getFqdn(managerServerOrLoadBalancer);
        }
        log.debug("[newServerResource] managerLB (DynamicOps_Vmps_Agent_Core_VMPSAgentService_ProxyAgentService) = '"
                    + managerServerOrLoadBalancer + "'");

        Resource modelResource = getCommonModel(server, vRAIaasWebOrLoadBalancer, managerServerOrLoadBalancer);
        String modelXml = marshallResource(modelResource);
        setModelProperty(server, modelXml);

        return server;
    }

    public static Resource getCommonModel(
                ServerResource server, String vRAIaasWebOrLoadBalancer, String managerServerOrLoadBalancer) {
        String proxyServerGroupName = appName;

        String parameterizedApplicationTagName = appName;
        ObjectFactory objectFactory = new ObjectFactory();

        Resource proxyServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST, server.getType(), server.getName(),
                    ResourceTier.SERVER);
        Resource proxyGroup = objectFactory.createLogicalResource(TYPE_PROXY_AGENT_SERVER_GROUP, proxyServerGroupName);

        Resource application =
                    objectFactory.createApplicationResource(TYPE_VRA_APPLICATION, parameterizedApplicationTagName);

        proxyServer.addRelations(objectFactory.createRelation(proxyGroup, RelationType.PARENT));
        proxyGroup.addRelations(objectFactory.createRelation(application, RelationType.PARENT));

        Resource loadBalancerSuperTag = objectFactory.createLogicalResource(TYPE_LOAD_BALANCER_TAG, appName);

        createRelationIaasWebOrLoadBalancer(vRAIaasWebOrLoadBalancer, parameterizedApplicationTagName, objectFactory,
                    proxyServer, loadBalancerSuperTag);

        createRelationManagerServerOrLoadBalncer(managerServerOrLoadBalancer, parameterizedApplicationTagName,
                    objectFactory, proxyServer, loadBalancerSuperTag);

        return proxyServer;
    }

    private static void createRelationManagerServerOrLoadBalncer(
                String managerServerOrLoadBalancer,
                String parameterizedApplicationTagName,
                ObjectFactory objectFactory,
                Resource proxyServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(managerServerOrLoadBalancer))
            return;
        Resource managerLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER,
                                managerServerOrLoadBalancer, ResourceTier.SERVER);
        proxyServer.addRelations(objectFactory.createRelation(managerLoadBalancer, RelationType.SIBLING));

        Resource managerLoadBalancerTag =
                    objectFactory.createLogicalResource(TYPE_VRA_MANAGER_SERVER_LOAD_BALANCER_TAG, appName);
        managerLoadBalancer.addRelations(objectFactory.createRelation(managerLoadBalancerTag, PARENT));
        managerLoadBalancerTag.addRelations(objectFactory.createRelation(loadBalancerSuperTag, PARENT));

        Resource managerServer = objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_MANAGER_SERVER,
                    managerServerOrLoadBalancer, ResourceTier.SERVER);
        Resource managerServerTag =
                    objectFactory.createLogicalResource(TYPE_VRA_MANAGER_SERVER_TAG, parameterizedApplicationTagName);
        managerServer.addRelations(objectFactory.createRelation(managerServerTag, RelationType.PARENT));
        proxyServer.addRelations(objectFactory.createRelation(managerServer, RelationType.SIBLING));
        managerLoadBalancer.addRelations(objectFactory.createRelation(managerServerTag, PARENT));
    }

    private static void createRelationIaasWebOrLoadBalancer(
                String vRAIaasWebOrLoadBalancer,
                String parameterizedApplicationTagName,
                ObjectFactory objectFactory,
                Resource proxyServer,
                Resource loadBalancerSuperTag) {
        if (StringUtils.isEmpty(vRAIaasWebOrLoadBalancer))
            return;
        Resource vraIaasWebLoadBalancer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                                vRAIaasWebOrLoadBalancer, ResourceTier.SERVER);
        Resource iaasWebLoadBalancerTag = objectFactory.createLogicalResource(TYPE_VRA_IAAS_WEB_TAG, appName);
        vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(iaasWebLoadBalancerTag, PARENT));

        Resource vRAIaasWebServer =
                    objectFactory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB, vRAIaasWebOrLoadBalancer,
                                ResourceTier.SERVER);
        Resource vRAIaasWebServerTag =
                    objectFactory.createLogicalResource(TYPE_VRA_IAAS_WEB_TAG, parameterizedApplicationTagName);
        vRAIaasWebServer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, RelationType.PARENT));
        proxyServer.addRelations(objectFactory.createRelation(vRAIaasWebServer, RelationType.SIBLING),
                    objectFactory.createRelation(vraIaasWebLoadBalancer, RelationType.SIBLING));
        vraIaasWebLoadBalancer.addRelations(objectFactory.createRelation(vRAIaasWebServerTag, PARENT));
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
